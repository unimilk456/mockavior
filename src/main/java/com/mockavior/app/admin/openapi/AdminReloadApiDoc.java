package com.mockavior.app.admin.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)

@Operation(
        summary = "Reload contract from source",
        description = """
                Forces reload of the contract from its configured source.
                If the contract has changed, a new snapshot is created and activated.
                Intended for administrative and operational use.
                """
)
@ApiResponse(
        responseCode = "200",
        description = "Reload operation completed"
)
public @interface AdminReloadApiDoc {
}
