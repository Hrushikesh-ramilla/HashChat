package com.hashchat.messenger.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.hashchat.messenger.auth.domain.VerificationCode;
import com.hashchat.messenger.auth.repository.VerificationCodeRepository;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.domain.UserStatus;
import com.hashchat.messenger.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DemoController.class);

    private final UserRepository userRepository;
    private final VerificationCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hashchat.demo.enabled:false}")
    private boolean demoEnabled;

    @GetMapping("/seed")
    public Map<String, Object> seed() {
        if (!demoEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo mode is disabled");
        }

        String phone = "+19999999999";
        String code = "000000";
        String username = "demo_user";

        if (userRepository.existsByPhone(phone)) {
            return Map.of(
                    "message", "Demo user already exists",
                    "phone", phone,
                    "code", code,
                    "username", username
            );
        }

        User user = new User();
        user.setPhone(phone);
        user.setUsername(username);
        user.setEmail("demo@hashchat.local");
        user.setPasswordHash("DEMO_USER");
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstName("Demo");
        user.setLastName("User");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        VerificationCode vc = new VerificationCode();
        vc.setPhone(phone);
        vc.setCode(passwordEncoder.encode(code));
        vc.setExpiresAt(LocalDateTime.now().plusDays(30));
        vc.setVia("DEMO");
        vc.setAttempts(0);
        vc.setCreatedAt(LocalDateTime.now());
        codeRepository.save(vc);

        log.info("Demo user created: phone={}, code={}, username={}", phone, code, username);

        return Map.of(
                "message", "Demo user created",
                "phone", phone,
                "code", code,
                "username", username
        );
    }
}
