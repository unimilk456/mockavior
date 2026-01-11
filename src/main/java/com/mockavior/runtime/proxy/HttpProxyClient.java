package com.mockavior.runtime.proxy;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.transport.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

@Slf4j
public final class HttpProxyClient {

    private final HttpClient client = HttpClient.newHttpClient();

    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "host",
            "content-length",
            "connection"
    );
    public HttpResponse<byte[]> forward(
            ProxyConfig config,
            GenericRequest request
    ) throws Exception {

        URI targetUri = URI.create(
                config.baseUri().toString() + request.operation()
        );

        HttpMethod method = (HttpMethod) request.metadata("method");

        log.debug(
                "Preparing proxy HTTP request: method={}, targetUri={}",
                method,
                targetUri
        );

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

                if (v != null) {
                    builder.header(k, v.toString());
                }
            });
        }

        try {
            HttpResponse<byte[]> response =
                    client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

            if (log.isDebugEnabled()) {
                log.debug(
                        "Proxy HTTP response received: status={}, targetUri={}",
                        response.statusCode(),
                        targetUri
                );
            }

            return response;

        } catch (Exception e) {
            log.error(
                    "HTTP proxy request failed: method={}, targetUri={}",
                    method,
                    targetUri,
                    e
            );
            throw e;
        }
    }
}
