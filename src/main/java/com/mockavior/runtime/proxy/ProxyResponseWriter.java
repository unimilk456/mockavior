package com.mockavior.runtime.proxy;

import com.mockavior.behavior.BehaviorResult;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public final class ProxyResponseWriter {

    private ProxyResponseWriter() {
    }

    public static void write(
            HttpResponse<byte[]> proxyResponse,
            HttpServletResponse servletResponse
    ) throws IOException {

        servletResponse.setStatus(proxyResponse.statusCode());

        for (Map.Entry<String, List<String>> entry
                : proxyResponse.headers().map().entrySet()) {

            String headerName = entry.getKey();

            if (!isValidHeaderName(headerName)) {
                continue; // 🔴 КРИТИЧНО
            }

            for (String value : entry.getValue()) {
                if (value != null) {
                    servletResponse.addHeader(headerName, value);
                }
            }
        }

        byte[] body = proxyResponse.body();
        if (body != null && body.length > 0) {
            servletResponse.getOutputStream().write(body);
        }
    }

    private static boolean isValidHeaderName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(
                    c >= 'a' && c <= 'z' ||
                            c >= 'A' && c <= 'Z' ||
                            c >= '0' && c <= '9' ||
                            c == '-'
            )) {
                return false;
            }
        }
        return true;
    }
}
