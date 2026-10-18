package com.safewalk.repository;

import com.safewalk.entity.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {

    List<VerificationDocument> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<VerificationDocument> findByIdAndUserId(Long id, Long userId);
}
