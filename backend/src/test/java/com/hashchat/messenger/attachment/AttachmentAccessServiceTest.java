package com.hashchat.messenger.attachment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashchat.messenger.attachment.access.AttachmentAccessService;
import com.hashchat.messenger.attachment.domain.EncryptedAttachment;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentAccessServiceTest {

    @Mock ChatParticipantRepository chatParticipantRepository;

    @InjectMocks AttachmentAccessService accessService;

    @Test
    void canUploadRequiresChatMembership() {
        when(chatParticipantRepository.existsByChatIdAndUserId(10L, 1L)).thenReturn(true);

        assertThat(accessService.canUpload(null, 1L)).isFalse();
        assertThat(accessService.canUpload(10L, 1L)).isTrue();
        assertThat(accessService.canUpload(11L, 1L)).isFalse();
    }

    @Test
    void canDownloadAllowsUploaderOrChatParticipant() {
        EncryptedAttachment attachment = new EncryptedAttachment();
        attachment.setUploaderId(1L);
        attachment.setChatId(10L);

        when(chatParticipantRepository.existsByChatIdAndUserId(10L, 2L)).thenReturn(true);

        assertThat(accessService.canDownload(attachment, 1L)).isTrue();
        assertThat(accessService.canDownload(attachment, 2L)).isTrue();
        assertThat(accessService.canDownload(attachment, 3L)).isFalse();
    }

    @Test
    void canDownloadRejectsStrangerWhenAttachmentHasNoChat() {
        EncryptedAttachment attachment = new EncryptedAttachment();
        attachment.setUploaderId(1L);
        attachment.setChatId(null);

        assertThat(accessService.canDownload(attachment, 2L)).isFalse();
    }
}
