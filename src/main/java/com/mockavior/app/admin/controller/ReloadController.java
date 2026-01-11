package com.mockavior.app.admin.controller;

import com.mockavior.app.admin.openapi.AdminReloadApiDoc;
import com.mockavior.reload.ReloadResult;
import com.mockavior.reload.ReloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
        name = "Admin / Reload",
        description = "Administrative API for reloading contracts from configured sources"
)
public final class ReloadController {

    @NonNull
    private final ReloadService reloadService;

    @AdminReloadApiDoc
    @PostMapping("/reload")
    public ReloadResult reload() {
        log.info("Received contract reload request");
        ReloadResult result = reloadService.reload();

        if (result.success()) {
            log.info("Contract reload succeeded: source={}, version={}", result.sourceId(), result.newVersion());
        } else {
            log.error("Contract reload failed: source={}, error={}", result.sourceId(), result.error());
        }

        return result;
    }
}
