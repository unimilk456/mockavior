package com.mockavior.transport.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mockavior.behavior.BehaviorResult;
import com.mockavior.core.request.GenericRequest;
import com.mockavior.core.request.Protocol;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP transport adapter.
 * Responsibilities:
 *  - Convert HttpServletRequest -> GenericRequest
 *  - Convert BehaviorResult -> HttpServletResponse
 * This class contains HTTP-specific logic only.
 * Core and behavior layers are HTTP-agnostic.
 */
@Slf4j
public final class HttpTransportAdapter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    static {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // ----------------------------------------------------------------------
    // Request mapping
    // ----------------------------------------------------------------------

    public GenericRequest toGenericRequest(HttpServletRequest request) throws IOException {
        Objects.requireNonNull(request, "request must not be null");

        HttpMethod method = HttpMethod.valueOf(request.getMethod().toUpperCase(Locale.ROOT));
        String path = extractPath(request);

        log.debug("Incoming HTTP request: {} {}", method, path);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", method);

        Map<String, String> headers = extractHeaders(request);
        if (!headers.isEmpty()) {
            metadata.put("headers", headers);
            log.trace("Request headers: {}", headers);
        }

        Map<String, List<String>> queryParams = extractQueryParams(request);
        if (!queryParams.isEmpty()) {
            metadata.put("query", queryParams);
            log.trace("Query params: {}", queryParams);
        }

        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress != null) {
            metadata.put("remoteAddress", remoteAddress);
        }

        String contentType = request.getContentType();
        if (contentType != null) {
            metadata.put("contentType", contentType);
        }

        String body = readBody(request);
        if (body != null && log.isTraceEnabled()) {
            log.trace("Request body: {}", body);
        }

        return GenericRequest.of(
                Protocol.HTTP,
                path,
                body,
                metadata
        );
    }

    // ----------------------------------------------------------------------
    // Response mapping
    // ----------------------------------------------------------------------

    public void writeHttpResponse(
            GenericRequest request,
            BehaviorResult result,
            HttpServletResponse response,
            ProxyHandler proxyHandler
    ) throws IOException {

        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(response, "response must not be null");
        Objects.requireNonNull(result, "BehaviorResult must not be null");
        Objects.requireNonNull(proxyHandler, "proxyHandler must not be null");

        log.debug("Writing HTTP response: behaviorType={}", result.type());

        switch (result.type()) {
            case MOCK -> writeMockResponse(result, response);
            case ERROR -> writeErrorResponse(result, response) ;
            case PROXY -> proxyHandler.handleProxy(request, response);
            default -> throw new IllegalStateException(
                    "Unsupported BehaviorType: " + result.type()
            );
        }
    }

    // ----------------------------------------------------------------------
    // Response writers
    // ----------------------------------------------------------------------

    private void writeMockResponse(
            BehaviorResult result,
            HttpServletResponse response
    ) throws IOException {

        int status = getInt(result.metadata().get("status"), 200);
        response.setStatus(status);

        log.debug("Mock response status={}", status);

        applyHeaders(result, response);

        writeBody(result.payload(), response);

    }

    private void writeErrorResponse  (
            BehaviorResult result,
            HttpServletResponse response
    ) {

        int status = getInt(result.metadata().get("status"), 500);
        response.setStatus(status);

        log.debug("Error response status={}", status);

    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private void applyHeaders(BehaviorResult result, HttpServletResponse response) {
        Object headersObj = result.metadata().get("headers");
        if (!(headersObj instanceof Map<?, ?> headers)) {
            return;
        }

        log.trace("Response headers: {}", headers);

        for (Map.Entry<?, ?> e : headers.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                response.setHeader(
                        String.valueOf(e.getKey()),
                        String.valueOf(e.getValue())
                );
            }
        }
    }

    private void writeBody(Object payload, HttpServletResponse response) throws IOException {
        if (payload == null) {
            return;
        }

        String body;
        if (payload instanceof String s) {
            body = s;
        } else {
            // MVP: simple toString(); later JSON serialization
            body = OBJECT_MAPPER.writeValueAsString(payload);
        }

        log.trace("Response body: {}", body);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (response.getContentType() == null) {
            response.setContentType("application/json");
        }
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private static String extractPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();

        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    private static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Map.of();
        }

        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name.toLowerCase(Locale.ROOT), request.getHeader(name));
        }
        return Map.copyOf(headers);
    }

    private static Map<String, List<String>> extractQueryParams(HttpServletRequest request) {
        Map<String, String[]> raw = request.getParameterMap();
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, String[]> e : raw.entrySet()) {
            String[] values = e.getValue();
            if (values.length == 1) {
                result.put(e.getKey(), List.of(values[0]));
            } else {
                result.put(e.getKey(), List.of(values));
            }

        }
        return Map.copyOf(result);
    }

    private static String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(request.getInputStream(), StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                if (scanner.hasNextLine()) {
                    sb.append('\n');
                }
            }
        }
        String body = sb.toString();
        return body.isEmpty() ? null : body;
    }

    private static int getInt(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                //will add smth soon
            }
        }
        return defaultValue;
    }

    // ----------------------------------------------------------------------
    // Proxy handler
    // ----------------------------------------------------------------------

    @FunctionalInterface
    public interface ProxyHandler {
        void handleProxy(GenericRequest request, HttpServletResponse response) throws IOException;
    }
}
