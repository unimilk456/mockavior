package com.mockavior.core.request;

/**
 * Supported protocol families.
 * HTTP is MVP. Others are planned for future adapters.
 */
public enum Protocol {
    HTTP,
    GRPC,
    GRAPHQL,
    JSON_RPC
}
