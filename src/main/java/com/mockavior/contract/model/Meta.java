package com.mockavior.contract.model;

import java.util.List;
import java.util.Map;

public record Meta(
        String name,
        String owner,
        List<String> tags
) {

    public Meta {
        tags = (tags == null) ? List.of() : List.copyOf(tags);
    }

    @SuppressWarnings("unchecked")
    public static Meta fromMap(Map<String, Object> data) {
        if (data == null) {
            return new Meta(null, null, List.of());
        }
        String name = (String) data.get("name");
        String owner = (String) data.get("owner");

        List<String> tags = null;
        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List<?>) {
            tags = (List<String>) tagsObj;
        }

        return new Meta(name, owner, tags);
    }
}

