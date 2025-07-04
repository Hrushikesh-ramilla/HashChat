package com.hashchat.messenger.backup.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.hashchat.messenger.backup.dto.BackupExportResponse;
import com.hashchat.messenger.backup.dto.BackupImportRequest;
import com.hashchat.messenger.backup.dto.BackupInfoResponse;
import com.hashchat.messenger.backup.service.BackupService;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.service.UserIdentityService;

@Tag(name = "E2EE Backup", description = "Encrypted key backup export/import")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final UserIdentityService userIdentityService;

    @Operation(summary = "Get backup info")
    @GetMapping("/info")
    public BackupInfoResponse getBackupInfo(Authentication auth) {
        User user = userIdentityService.require(auth.getName());
        return backupService.getBackupInfo(user.getId());
    }

    @Operation(summary = "Export latest backup (requires passphrase header)")
    @GetMapping("/export")
    public BackupExportResponse exportBackup(
            Authentication auth,
            @RequestHeader("X-Backup-Passphrase") String passphrase
    ) {
        User user = userIdentityService.require(auth.getName());
        return backupService.exportBackup(user.getId(), passphrase);
    }

    @Operation(summary = "Import/upload encrypted backup")
    @PostMapping("/import")
    public void importBackup(
            Authentication auth,
            @Valid @RequestBody BackupImportRequest request
    ) {
        User user = userIdentityService.require(auth.getName());
        backupService.importBackup(user.getId(), request);
    }
}
