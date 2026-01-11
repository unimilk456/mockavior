package com.mockavior.app.admin.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)

@Operation(
        summary = "Update contract",
        description = """
                Updates and activates a new contract.
                Uses optimistic locking via the If-Match header.
                """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Contract updated successfully"
        ),
        @ApiResponse(
                responseCode = "409",
                description = "Snapshot version conflict"
        )
})
public @interface AdminContractUpdateApiDoc {
}
