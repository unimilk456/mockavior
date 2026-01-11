package com.mockavior.contract.parse;

import com.mockavior.contract.model.RawContract;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;
import java.util.Objects;


/**
 * MVP YAML contract parser.
 * Converts raw YAML into RawContract structure.
 */
public final class YamlContractParser implements ContractParser {

    private static final Yaml YAML = new Yaml();

    @Override
    public RawContract parse(String rawContent) {
        Objects.requireNonNull(rawContent, "rawContent must not be null");

        final Map<String, Object> data;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> loaded = YAML.load(rawContent);
            data = loaded;
        } catch (YAMLException e) {
            throw new IllegalArgumentException(
                    "Failed to parse contract YAML: invalid syntax",
                    e
            );
        }

        if (data == null) {
            throw new IllegalArgumentException(
                    "Contract YAML is empty or contains only comments"
            );
        }

        return RawContract.fromMap(data);
    }
}
