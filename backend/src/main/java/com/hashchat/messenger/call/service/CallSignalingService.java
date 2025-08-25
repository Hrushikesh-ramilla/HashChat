package com.hashchat.messenger.call.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.call.api.CallEvent;
import com.hashchat.messenger.call.api.CallSignalRequest;
import com.hashchat.messenger.chat.domain.Chat;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.chat.repository.ChatRepository;
import com.hashchat.messenger.crypto.device.UserDevice;
import com.hashchat.messenger.crypto.device.UserDeviceRepository;
import com.hashchat.messenger.realtime.StompEventPublisher;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.service.UserIdentityService;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hashchat.calls.enabled", havingValue = "true")
public class CallSignalingService {

    private static final Set<String> ALLOWED_TYPES = Set.of("offer", "answer", "ice", "hangup", "busy", "media");

    private final UserIdentityService userIdentityService;
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final StompEventPublisher stompEventPublisher;

    public void relay(String username, String fromDeviceId, CallSignalRequest request) {
        if (username == null || username.isBlank() || fromDeviceId == null || fromDeviceId.isBlank()) {
            return;
        }
        if (request == null || request.chatId() == null || request.type() == null) {
            return;
        }

        String type = request.type().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            log.debug("Call signal dropped: unknown type={} from {}", request.type(), username);
            return;
        }

        User user = userIdentityService.resolve(username).orElse(null);
        if (user == null || !participantRepository.existsByChatIdAndUserId(request.chatId(), user.getId())) {
            log.warn("Call signal denied for user={} chatId={}", username, request.chatId());
            return;
        }

        Chat chat = chatRepository.findById(request.chatId()).orElse(null);
        if (chat == null || !"DIRECT".equalsIgnoreCase(chat.getType())) {
            return;
        }
        if (chat.getDirectStatus() != null && !"ACCEPTED".equalsIgnoreCase(chat.getDirectStatus())) {
            return;
        }

        List<Long> userIds = participantRepository.findUserIdsByChatId(request.chatId());
        if (userIds.isEmpty()) {
            return;
        }

        CallEvent event = new CallEvent(
                type,
                request.chatId(),
                username,
                fromDeviceId,
                request.sdp(),
                request.candidate(),
                request.video(),
                request.mediaKeys()
        );

        for (UserDevice device : userDeviceRepository.findByUserIdInAndActiveTrue(userIds)) {
            if (fromDeviceId.equals(device.getDeviceId())) {
                continue;
            }
            stompEventPublisher.publishToDevice(device.getDeviceId(), "/calls", event);
        }
    }
}
