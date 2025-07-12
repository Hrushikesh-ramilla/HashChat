package com.hashchat.messenger.backup.dto;

public record BackupExportResponse(
        Integer version,
        String encryptedPayload,
        String salt,
        String iv,
        String backupType,
        String createdAt
) {
}
