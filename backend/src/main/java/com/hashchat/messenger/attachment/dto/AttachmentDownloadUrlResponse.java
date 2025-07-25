package com.hashchat.messenger.attachment.dto;

import java.time.Instant;

public record AttachmentDownloadUrlResponse(String downloadUrl, Instant expiresAt) {
}
