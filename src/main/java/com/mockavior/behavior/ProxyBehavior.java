package com.mockavior.behavior;

import com.mockavior.core.request.GenericRequest;
import com.mockavior.routing.MatchResult;
import com.mockavior.runtime.proxy.ProxyConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Proxy behavior — indicates that request should be forwarded
 * to a real backend using proxy configuration from active snapshot.
 */
@Slf4j
public final class ProxyBehavior implements Behavior {

    public ProxyBehavior() {
    }

    @Override
    public BehaviorResult apply(GenericRequest request, MatchResult match) {
        log.debug("Proxying request: protocol={}, operation={}", request.protocol(), request.operation());

        return BehaviorResult.proxy();
    }

    @Override
    public String toString() {

        return "ProxyBehavior{}";
    }
}
