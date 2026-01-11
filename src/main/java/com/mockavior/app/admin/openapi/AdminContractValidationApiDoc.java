package com.mockavior.app.admin.openapi;

import com.mockavior.app.admin.dto.ContractValidationResponse;
import com.mockavior.app.admin.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)

@Operation(
        summary = "Validate contract",
        description = """
                Validates a contract without saving or activating it.
                Performs parsing, compilation and optional optimistic lock check.
                Intended for dry-run validation (UI / CI).
                """
)
@ApiResponse(
        responseCode = "200",
        description = "Contract validation succeeded",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractValidationResponse.class)
        )
)
@ApiResponse(
        responseCode = "400",
        description = "Contract validation error",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
        )
)
@ApiResponse(
        responseCode = "409",
        description = "Snapshot version conflict",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
        )
)
public @interface AdminContractValidationApiDoc {
}
