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
        summary = "Get active contract",
        description = """
                Returns the currently active contract in raw YAML format.
                The active snapshot version is returned in the response header.
                """
)
@ApiResponse(
        responseCode = "200",
        description = "Contract retrieved successfully"
)
public @interface AdminContractGetApiDoc {
}
