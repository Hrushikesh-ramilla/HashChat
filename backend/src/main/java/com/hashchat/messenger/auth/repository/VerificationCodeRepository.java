package com.hashchat.messenger.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import com.hashchat.messenger.auth.domain.VerificationCode;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationCode> findTopByPhoneOrderByIdDesc(String phone);
}
