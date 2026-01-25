package com.mockavior.app.http;

import com.mockavior.app.config.AdminProperties;
import com.mockavior.behavior.BehaviorResult;
import com.mockavior.core.engine.EngineResult;
import com.mockavior.core.request.GenericRequest;
import com.mockavior.core.snapshot.ContractSnapshot;
import com.mockavior.runtime.RequestProcessor;
import com.mockavior.runtime.proxy.HttpProxyClient;
import com.mockavior.runtime.proxy.ProxyConfig;
import com.mockavior.runtime.proxy.ProxyResponseWriter;
import com.mockavior.runtime.scheduler.RuntimeScheduler;
import com.mockavior.runtime.snapshot.SnapshotHandle;
import com.mockavior.runtime.snapshot.SnapshotRegistry;
import com.mockavior.transport.http.HttpTransportAdapter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public final class RuntimeInterceptor implements HandlerInterceptor {

    @NotNull
    private final AdminProperties adminProperties;

    @NotNull
    private final RequestProcessor requestProcessor;

    @NotNull
    private final HttpTransportAdapter transportAdapter;

    @NotNull
    private final HttpProxyClient proxyClient;

    @NotNull
    private final SnapshotRegistry snapshotRegistry;

    @NotNull
    private final RuntimeScheduler runtimeScheduler;

    @NotNull
    private final MeterRegistry meterRegistry;

    private final Counter httpRequestsTotal;
    private final Timer httpRequestTimer;

    public RuntimeInterceptor(
            AdminProperties adminProperties,
            RequestProcessor requestProcessor,
            HttpTransportAdapter transportAdapter,
            HttpProxyClient proxyClient,
            SnapshotRegistry snapshotRegistry,
            RuntimeScheduler runtimeScheduler,
            MeterRegistry meterRegistry
    ) {
        this.adminProperties = adminProperties;
        this.requestProcessor = requestProcessor;
        this.transportAdapter = transportAdapter;
        this.proxyClient = proxyClient;
        this.snapshotRegistry = snapshotRegistry;
        this.runtimeScheduler = runtimeScheduler;
        this.meterRegistry = meterRegistry;

        this.httpRequestsTotal =
                Counter.builder("mockavior.http.requests.total")
                        .description("Total number of runtime HTTP requests")
                        .register(meterRegistry);

        this.httpRequestTimer =
                Timer.builder("mockavior.http.request.duration")
                        .description("End-to-end HTTP request duration")
                        .register(meterRegistry);
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        String adminUrlPrefix = adminProperties.getPrefix();
        // 1️⃣ Let Spring handle infra & admin endpoints
        if (isInfraPath(path) || path.startsWith(adminUrlPrefix)) {
            return true;
        }

        // ===== Metrics: request entered runtime =====
        this.httpRequestsTotal.increment();
        Timer.Sample timerSample = Timer.start(this.meterRegistry);

        log.info("Runtime intercepted request: {} {}", method, path);

        try {
            // 2️⃣ Convert to GenericRequest
            GenericRequest genericRequest =
                    transportAdapter.toGenericRequest(request);

            // 3️⃣ Process via runtime engine
            EngineResult engineResult =
                    requestProcessor.process(genericRequest);

            BehaviorResult behaviorResult =
                    engineResult.behaviorResult();

            ContractSnapshot snapshot =
                    engineResult.snapshot();

            String routeId =
                    engineResult.routeId();

            Duration delay =
                    snapshot.delayForRoute(routeId);

            log.debug(
                    "Resolved route: routeId={}, behavior={}, delay={}",
                    routeId,
                    behaviorResult.type(),
                    delay
            );

            // 4️⃣ Switch to async mode
            AsyncContext asyncContext = request.startAsync();
            asyncContext.setTimeout(0);

            // 5️⃣ Schedule response writing
            runtimeScheduler.scheduleTask(() -> {
                try {
                    HttpServletResponse asyncResponse =
                            (HttpServletResponse) asyncContext.getResponse();

                    transportAdapter.writeHttpResponse(
                            genericRequest,
                            behaviorResult,
                            asyncResponse,
                            (req, resp) -> {

                                // Proxy handling
                                SnapshotHandle handle =
                                        snapshotRegistry.active();

                                ContractSnapshot.Settings settings =
                                        handle.snapshot().settings();

                                URI proxyBaseUri =
                                        settings.proxyBaseUri();

                                if (proxyBaseUri == null) {
                                    resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                                    resp.getWriter().write("Proxy not configured");
                                    return;
                                }

                                ProxyConfig config =
                                        new ProxyConfig(proxyBaseUri);

                                try {
                                    HttpResponse<byte[]> proxyResponse =
                                            proxyClient.forward(config, req);

                                    ProxyResponseWriter.write(
                                            proxyResponse,
                                            resp
                                    );

                                } catch (Exception e) {
                                    log.error("Proxy call failed", e);
                                    resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                                    resp.getWriter().write("Bad Gateway");
                                }
                            }
                    );

                    log.info(
                            "Runtime response completed: {} {} -> {} (delay={})",
                            method,
                            path,
                            asyncResponse.getStatus(),
                            delay
                    );

                } catch (Exception e) {
                    log.error("Failed to write runtime response", e);
                } finally {
                    timerSample.stop(httpRequestTimer);
                    asyncContext.complete();
                }
            }, delay);

            // 6️⃣ IMPORTANT: we handled the response ourselves
            return false;

        } catch (Exception e) {
            log.error(
                    "Runtime processing failed: {} {}",
                    method,
                    path,
                    e
            );

            timerSample.stop(httpRequestTimer);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write(
                    "Internal error while processing request"
            );

            return false;
        }
    }

    private boolean isInfraPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/favicon.ico");
    }
}
