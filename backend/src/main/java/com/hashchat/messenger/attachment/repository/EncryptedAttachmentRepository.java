package com.hashchat.messenger.attachment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hashchat.messenger.attachment.domain.EncryptedAttachment;

import java.util.Optional;

public interface EncryptedAttachmentRepository extends JpaRepository<EncryptedAttachment, Long> {
    Optional<EncryptedAttachment> findByAttachmentId(String attachmentId);
}
