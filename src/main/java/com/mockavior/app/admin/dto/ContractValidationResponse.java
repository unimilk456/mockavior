package com.mockavior.app.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ContractValidationResponse",
        description = "Response returned after successful contract validation"
)
public record ContractValidationResponse(
        @Schema(
                description = "Validation result status",
                example = "OK"
        )
        String status,

        @Schema(
                description = "Human-readable validation message",
                example = "Contract validation successful"
        )
        String message,

        @Schema(
                description = "Snapshot version against which the contract was validated",
                example = "d83cefa3-af2b-460a-a6a2-b2ee44d2963d"
        )
        String baseVersion
) {
    public static ContractValidationResponse success(String baseVersion) {
        return new ContractValidationResponse(
                "OK",
                "Contract validation successful",
                baseVersion
        );
    }
}
