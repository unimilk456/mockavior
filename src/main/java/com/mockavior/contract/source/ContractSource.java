package com.mockavior.contract.source;

import java.io.IOException;

public interface ContractSource {

    /**
     * @return raw contract content (e.g., YAML string)
     */
    String load() throws IOException;

    /**
     * @return human-readable identifier (file path, URL, etc.)
     */
    String id();

    void save(String rawText) throws IOException;
}
