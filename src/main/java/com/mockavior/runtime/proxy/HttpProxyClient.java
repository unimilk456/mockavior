package com.mockavior.runtime.proxy;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.transport.http.HttpMethod;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public final class HttpProxyClient {

    private final HttpClient client;
    private final MeterRegistry meterRegistry;

    private static final String STATUS = "status";
    private static final String METHOD = "method";

    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "host",
            "content-length",
            "connection"
    );

    public HttpProxyClient(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        this.client = HttpClient.newHttpClient();
    }

    public HttpResponse<byte[]> forward(
            ProxyConfig config,
            GenericRequest request
    ) throws Exception {

        URI targetUri = URI.create(
                config.baseUri().toString() + request.operation()
        );

        HttpMethod method = (HttpMethod) request.metadata(METHOD);

        log.debug(
                "Preparing proxy HTTP request: method={}, targetUri={}",
                method,
                targetUri
        );

        Counter.builder("mockavior_proxy_requests_total")
                .tag(METHOD, method.name())
                .register(meterRegistry)
                .increment();

        long startNanos = System.nanoTime();

        try {

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .method(
                            method.name(),
                            request.payload() == null
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofString(
                                    request.payload().toString()
                            )
                    );

            @SuppressWarnings("unchecked")
            Map<String, Object> headers =
                    (Map<String, Object>) request.metadata("headers");

            if (headers != null) {
                headers.forEach((k, v) -> {
                    if (v == null) {
                        return;
                    }

                    if (FORBIDDEN_HEADERS.contains(k.toLowerCase())) {
                        log.trace("Skipping forbidden header: {}", k);
                        return;
                    }

                    builder.header(k, v.toString());
                });
            }

            HttpResponse<byte[]> response =
                    client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());


            Timer.builder("mockavior_proxy_latency_seconds")
                    .tag(METHOD, method.name())
                    .tag(STATUS, String.valueOf(response.statusCode()))
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - startNanos));

            Counter.builder("mockavior_proxy_responses_total")
                    .tag(STATUS, String.valueOf(response.statusCode()))
                    .register(meterRegistry)
                    .increment();

            if (log.isDebugEnabled()) {
                log.debug(
                        "Proxy HTTP response received: status={}, targetUri={}",
                        response.statusCode(),
                        targetUri
                );
            }

            return response;

        } catch (Exception e) {
            Timer.builder("mockavior_proxy_latency_seconds")
                    .tag(METHOD, method.name())
                    .tag(STATUS, "ERROR")
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - startNanos));

            Counter.builder("mockavior_proxy_errors_total")
                    .tag(METHOD, method.name())
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();

            log.error("HTTP proxy request failed", e);
            throw e;
        }
    }
}
