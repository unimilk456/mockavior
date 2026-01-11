package com.mockavior.contract.model;

import java.util.Map;

public record RawEndpoint(
        String id,
        Integer priority,
        RawRequest request,
        RawResponse response,
        RawWhen when
) {

    public RawEndpoint {
        if (request == null || response == null) {
            throw new IllegalArgumentException(
                    "endpoint must contain request and response sections"
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static RawEndpoint fromMap(Map<String, Object> data) {
        try {
            Object idObj = data.get("id");
            String id = idObj != null ? idObj.toString() : null;

            Object reqObj = data.get("request");
            Object respObj = data.get("response");

            RawRequest request = RawRequest.fromMap((Map<String, Object>) reqObj);
            RawResponse response = RawResponse.fromMap((Map<String, Object>) respObj);

            Integer priority = null;
            Object prObj = data.get("priority");
            if (prObj instanceof Number number) {
                priority = number.intValue();
            }

            RawWhen when = null;
            Object whenObj = data.get("when");
            if (whenObj instanceof Map<?, ?>) {
                when = RawWhen.fromMap((Map<String, Object>) whenObj);
            }

            return new RawEndpoint(id, priority, request, response, when);
        }
        catch (Exception e) {
            Object idObj = data.get("id");
            String safeId = idObj != null ? idObj.toString() : "<no-id>";

            throw new IllegalArgumentException(
                    "Failed to parse endpoint: " + safeId,
                    e
            );
        }
    }
}

