package com.mockavior.contract.payload;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public record ResolvedBody(byte[] bytes) {

    public ResolvedBody {
        Objects.requireNonNull(bytes, "bytes must not be null");
    }

    public String asString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResolvedBody(byte[] otherBytes))) {
            return false;
        }
        return Arrays.equals(this.bytes, otherBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "ResolvedBody[" +
                "bytes.length=" + bytes.length +
                "]";
    }
}
