package com.mockavior.app.admin.controller;

import com.mockavior.app.admin.service.ContractAdminService;
import com.mockavior.app.admin.openapi.AdminContractGetApiDoc;
import com.mockavior.app.admin.openapi.AdminContractUpdateApiDoc;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin HTTP API for contract inspection and update.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
        name = "Admin / Contract",
        description = "Administrative API for reading and updating Mockavior contracts"
)
public final class ContractAdminController {

    private static final String VERSION_HEADER = "Mockavior-Contract-Version";
    private static final String CURRENT_VERSION_HEADER = "X-Current-Version";
    private static final MediaType YAML_MEDIA_TYPE =
            MediaType.parseMediaType("application/yaml");

    @NonNull
    private final ContractAdminService service;

    @AdminContractGetApiDoc
    @GetMapping(value = "/contract", produces = "application/yaml")
    public ResponseEntity<String> getContract() {

        log.debug("ADMIN → GET /contract : fetching current contract");

        ContractAdminService.ContractView contract =
                service.getCurrentContract();

        log.debug(
                "ADMIN ← GET /contract : success (version={})",
                contract.snapshotVersion()
        );

        return ResponseEntity.ok()
                .contentType(YAML_MEDIA_TYPE)
                .header(VERSION_HEADER, String.valueOf(contract.snapshotVersion()))
                .body(contract.rawYaml());
    }

    @AdminContractUpdateApiDoc
    @PutMapping(
            value = "/contract",
            consumes = {
                    MediaType.TEXT_PLAIN_VALUE,
                    "application/yaml",
                    "text/yaml",
                    "application/x-yaml"
            }
    )
    public ResponseEntity<ContractAdminService.ContractUpdateResult> updateContract(
            @RequestHeader(value = "If-Match") String ifMatch,
            @RequestBody String body
    ) {

        log.debug(
                "ADMIN → PUT /contract : update requested (If-Match={})",
                ifMatch
        );

        try {
            ContractAdminService.ContractUpdateResult result =
                    service.updateContract(ifMatch, body);

            log.debug(
                    "ADMIN ← PUT /contract : update successful (newVersion={})",
                    result.snapshotVersion()
            );

            return ResponseEntity.ok(result);

        } catch (ContractAdminService.OptimisticLockException e) {

            log.warn(
                    "ADMIN ← PUT /contract : optimistic lock conflict (expected={}, actual={})",
                    ifMatch,
                    e.actualVersion()
            );

            return ResponseEntity.status(409)
                    .header(CURRENT_VERSION_HEADER, e.actualVersion())
                    .build();
        }
    }
}
