package com.mockavior.app.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String currentVersion
) {

    public static ErrorResponse versionConflict(String currentVersion) {
        return new ErrorResponse(
                "VERSION_CONFLICT",
                "Contract version mismatch",
                currentVersion
        );
    }

    public static ErrorResponse validationError(String message) {
        return new ErrorResponse(
                "VALIDATION_ERROR",
                message,
                null
        );
    }
}
