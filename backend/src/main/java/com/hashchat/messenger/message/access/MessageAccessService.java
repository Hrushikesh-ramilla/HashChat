package com.hashchat.messenger.message.access;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.common.exception.ChatException;
import com.hashchat.messenger.crypto.device.CurrentDeviceService;
import com.hashchat.messenger.crypto.device.UserDevice;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.service.UserIdentityService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageAccessService {

    private final UserIdentityService userIdentityService;
    private final CurrentDeviceService currentDeviceService;
    private final ChatParticipantRepository participantRepository;

    public User requireUser(String username) {
        return userIdentityService.require(username);
    }

    public UserDevice requireCurrentDevice() {
        return currentDeviceService.requireCurrentDevice();
    }

    public void requireParticipant(Long chatId, Long userId) {
        if (!participantRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new ChatException("You are not a participant of this chat");
        }
    }

    public List<Long> participantIds(Long chatId) {
        return participantRepository.findUserIdsByChatId(chatId)
                .stream()
                .distinct()
                .toList();
    }

    public String deviceIdOrFallback(UserDevice currentDevice) {
        return currentDevice == null ? "unknown-device" : currentDevice.getDeviceId();
    }
}
