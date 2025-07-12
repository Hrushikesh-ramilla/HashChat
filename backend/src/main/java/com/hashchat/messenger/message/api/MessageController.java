package com.hashchat.messenger.message.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hashchat.messenger.crypto.device.CurrentDeviceService;
import com.hashchat.messenger.crypto.dto.EncryptedEditMessageRequestV2;
import com.hashchat.messenger.crypto.dto.EncryptedSendMessageRequestV2;
import com.hashchat.messenger.message.dto.DeleteMessageResponse;
import com.hashchat.messenger.message.dto.DeviceMessageEventResponse;
import com.hashchat.messenger.message.dto.MessageTimelineItemResponse;
import com.hashchat.messenger.message.dto.ReactionRequest;
import com.hashchat.messenger.message.dto.UpdateMessageStatusRequest;
import com.hashchat.messenger.message.service.MessageService;
import com.hashchat.messenger.message.dto.ReactionEvent;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CurrentDeviceService currentDeviceService;

    @PostMapping("/encrypted/v2")
    public DeviceMessageEventResponse sendEncryptedMessage(
            @Valid @RequestBody EncryptedSendMessageRequestV2 request,
            Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return messageService.sendEncryptedMessageV2(authentication.getName(), request);
    }

    @GetMapping("/chat/{chatId}/timeline")
    public List<MessageTimelineItemResponse> getChatTimeline(
            @PathVariable Long chatId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return messageService.getChatTimeline(authentication.getName(), chatId, beforeMessageId, limit);
    }

    @PostMapping("/chat/{chatId}/read")
    public void markChatRead(@PathVariable Long chatId, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        messageService.markChatAsRead(authentication.getName(), chatId);
    }

    @PostMapping("/chat/{chatId}/delivered")
    public void markChatDelivered(@PathVariable Long chatId, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        messageService.markChatAsDelivered(authentication.getName(), chatId);
    }

    @PostMapping("/status")
    public void updateStatus(@Valid @RequestBody UpdateMessageStatusRequest request, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        messageService.updateMessageStatus(authentication.getName(), request.messageId(), request.status());
    }

    @PutMapping("/{messageId}/encrypted/v2")
    public DeviceMessageEventResponse editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EncryptedEditMessageRequestV2 request,
            Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return messageService.editEncryptedMessageV2(authentication.getName(), messageId, request);
    }

    @PutMapping("/{messageId}/reactions")
    public ReactionEvent toggleReaction(
            @PathVariable Long messageId,
            @Valid @RequestBody ReactionRequest request,
            Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return messageService.toggleReaction(authentication.getName(), messageId, request.emoji());
    }

    @DeleteMapping("/{messageId}")
    public DeleteMessageResponse deleteMessage(@PathVariable Long messageId, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        messageService.deleteMessage(authentication.getName(), messageId);
        return new DeleteMessageResponse(true);
    }
}
