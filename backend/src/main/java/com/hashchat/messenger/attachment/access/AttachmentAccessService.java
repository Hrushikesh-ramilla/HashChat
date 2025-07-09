package com.hashchat.messenger.attachment.access;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.attachment.domain.EncryptedAttachment;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;

@Service
@RequiredArgsConstructor
public class AttachmentAccessService {

    private final ChatParticipantRepository chatParticipantRepository;

    public boolean canDownload(EncryptedAttachment attachment, Long userId) {
        if (attachment.getUploaderId().equals(userId)) {
            return true;
        }
        return chatParticipantRepository.shareAnyChat(attachment.getUploaderId(), userId);
    }
}
