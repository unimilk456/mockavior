package com.mockavior.reload;

/**
 * Result of contract reload attempt.
 * Immutable value object used for logging, admin APIs and observability.
 */
public final class ReloadResult {

    private final boolean success;
    private final String sourceId;
    private final String newVersion;
    private final String error;

    private ReloadResult(boolean success, String sourceId, String newVersion, String error) {
        this.success = success;
        this.sourceId = sourceId;
        this.newVersion = newVersion;
        this.error = error;
    }

    public static ReloadResult success(String sourceId, String newVersion) {
        return new ReloadResult(true, sourceId, newVersion, null);
    }

    public static ReloadResult failure(String sourceId, Exception e) {
        return new ReloadResult(false, sourceId, null, e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    public boolean success() { return success; }
    public String sourceId() { return sourceId; }
    public String newVersion() { return newVersion; }
    public String error() { return error; }
}
