package com.hashchat.messenger.backup.dto;

public record BackupInfoResponse(
        boolean hasBackup,
        Integer latestVersion,
        Integer backupCount,
        String createdAt
) {}
