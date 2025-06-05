package com.hashchat.messenger.crypto.device;


import com.hashchat.messenger.user.service.UserIdentityService;
import com.hashchat.messenger.common.exception.*;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CurrentDeviceService {

    private final HttpServletRequest request;
    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final UserIdentityService userIdentityService;

    public UserDevice requireCurrentDevice() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new AuthException("Unauthenticated request");
        }

        String username = authentication.getName();
        String deviceId = request.getHeader("X-Device-Id");

        if (deviceId == null || deviceId.isBlank()) {
            throw new AuthException("X-Device-Id header is required");
        }

        User user = userIdentityService.require(username);

        return userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(user.getId(), deviceId)
                .orElseThrow(() -> new AuthException("Active device not found for current user"));
    }
}