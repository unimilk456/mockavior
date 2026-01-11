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
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public final class RuntimeInterceptor implements HandlerInterceptor {

    @NonNull
    private final AdminProperties adminProperties;

    @NonNull
    private final RequestProcessor requestProcessor;

    @NonNull
    private final HttpTransportAdapter transportAdapter;

    @NonNull
    private final HttpProxyClient proxyClient;

    @NonNull
    private final SnapshotRegistry snapshotRegistry;

    @NonNull
    private final RuntimeScheduler runtimeScheduler;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        String adminUrlPrefix = adminProperties.getPrefix();
        // 1️⃣ Let Spring handle infra & admin endpoints
        if (isInfraPath(path) || path.startsWith(adminUrlPrefix)) {
            return true;
        }

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
