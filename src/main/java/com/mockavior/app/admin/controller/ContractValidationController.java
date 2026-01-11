package com.mockavior.app.admin.controller;

import com.mockavior.app.admin.dto.ContractValidationResponse;
import com.mockavior.app.admin.service.ContractValidationService;
import com.mockavior.app.admin.dto.ErrorResponse;
import com.mockavior.app.admin.openapi.AdminContractValidationApiDoc;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin HTTP API for contract validation.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
        name = "Admin / Contract Validation",
        description = "Administrative API for validating contracts without activation"
)
public final class ContractValidationController {

    private static final String CURRENT_VERSION_HEADER = "X-Current-Version";

    @NonNull
    private final ContractValidationService service;

    @AdminContractValidationApiDoc
    @PostMapping("/contract/validate")
    public ResponseEntity<?> validate(
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody String body
    ) {

        log.debug("Admin request: validate contract (If-Match={})", ifMatch);

        try {
            String baseVersion = service.validate(ifMatch, body);

            log.debug("Contract validation succeeded");

            return ResponseEntity.ok(
                    ContractValidationResponse.success(baseVersion)
            );

        } catch (ContractValidationService.OptimisticLockException e) {

            log.warn(
                    "Contract validation failed: optimistic lock (expected={}, actual={})",
                    ifMatch,
                    e.actualVersion()
            );

            return ResponseEntity.status(409)
                    .header(CURRENT_VERSION_HEADER, e.actualVersion())
                    .body(
                            ErrorResponse.versionConflict(e.actualVersion())
                    );

        } catch (ContractValidationService.ValidationException e) {

            log.warn("Contract validation failed: {}", e.getMessage());

            return ResponseEntity.badRequest()
                    .body(
                            ErrorResponse.validationError(e.getMessage())
                    );
        }
    }
}
