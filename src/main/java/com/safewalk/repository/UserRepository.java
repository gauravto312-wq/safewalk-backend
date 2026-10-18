package com.safewalk.repository;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    long countByAccountStatus(AccountStatus accountStatus);

    long countByRole(Role role);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    Page<User> findByAccountStatus(AccountStatus accountStatus, Pageable pageable);

    Page<User> findByNameContainingIgnoreCaseOrMobileNumberContaining(String name, String mobileNumber, Pageable pageable);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    java.util.List<User> findByVerificationStatus(com.safewalk.entity.VerificationStatus verificationStatus);

    java.util.List<User> findByRoleInAndVerificationStatus(java.util.List<Role> roles, com.safewalk.entity.VerificationStatus verificationStatus);
}
