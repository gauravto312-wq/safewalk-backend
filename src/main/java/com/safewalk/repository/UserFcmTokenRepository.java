package com.safewalk.repository;

import com.safewalk.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByToken(String token);

    List<UserFcmToken> findAllByToken(String token);

    Optional<UserFcmToken> findByTokenAndUserId(String token, Long userId);

    List<UserFcmToken> findByUserIdAndActiveTrue(Long userId);

    List<UserFcmToken> findByUserId(Long userId);
}
