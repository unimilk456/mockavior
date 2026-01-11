package com.mockavior.runtime.proxy;

import java.net.URI;
import java.util.Objects;

public final class ProxyConfig {

    private final URI baseUri;

    public ProxyConfig(URI baseUri) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri must not be null");
    }

    public URI baseUri() {
        return baseUri;
    }
}
