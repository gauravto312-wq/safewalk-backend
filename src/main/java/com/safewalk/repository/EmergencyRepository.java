package com.safewalk.repository;

import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyRepository extends JpaRepository<Emergency, Long> {

    boolean existsByRequesterIdAndStatus(Long requesterId, EmergencyStatus status);

    boolean existsByRequesterIdAndStatusIn(Long requesterId, java.util.List<EmergencyStatus> statuses);

    Optional<Emergency> findByRequesterIdAndStatus(Long requesterId, EmergencyStatus status);

    Optional<Emergency> findFirstByRequesterIdAndStatusInOrderByCreatedAtDesc(Long requesterId, java.util.List<EmergencyStatus> statuses);

    Page<Emergency> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    long countByStatus(EmergencyStatus status);

    long countByType(EmergencyType type);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndCreatedAtBetween(EmergencyStatus status, LocalDateTime from, LocalDateTime to);

    long countByTypeAndCreatedAtBetween(EmergencyType type, LocalDateTime from, LocalDateTime to);

    Page<Emergency> findByStatus(EmergencyStatus status, Pageable pageable);

    Page<Emergency> findByType(EmergencyType type, Pageable pageable);

    Page<Emergency> findByStatusAndType(EmergencyStatus status, EmergencyType type, Pageable pageable);

    Page<Emergency> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Emergency> findTop5ByOrderByCreatedAtDesc();

    List<Emergency> findByStatusInOrderByCreatedAtDesc(List<EmergencyStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT e FROM Emergency e LEFT JOIN e.responders r WHERE (e.requester.id = :userId OR r.id = :userId OR e.responder.id = :userId) AND e.status IN :statuses")
    List<Emergency> findByParticipantUserIdAndStatusIn(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("statuses") List<EmergencyStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Emergency e LEFT JOIN FETCH e.responders WHERE e.id = :id")
    Optional<Emergency> findByIdWithResponders(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Emergency e WHERE e.id = :id")
    Optional<Emergency> findByIdWithPessimisticLock(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT e FROM Emergency e LEFT JOIN e.responders r WHERE e.requester.id = :userId OR r.id = :userId OR e.responder.id = :userId ORDER BY e.createdAt DESC",
            countQuery = "SELECT count(DISTINCT e) FROM Emergency e LEFT JOIN e.responders r WHERE e.requester.id = :userId OR r.id = :userId OR e.responder.id = :userId"
    )
    Page<Emergency> findByParticipantUserIdOrderByCreatedAtDesc(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Emergency e WHERE e.status IN ('ACTIVE', 'ACCEPTED') AND e.createdAt <= :cutoff ORDER BY e.createdAt ASC")
    List<Emergency> findOverdueEmergencies(@org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff);

    @org.springframework.data.jpa.repository.Query("SELECT e.id FROM Emergency e WHERE e.status IN ('ACTIVE', 'ACCEPTED') AND e.createdAt <= :cutoff ORDER BY e.createdAt ASC")
    List<Long> findOverdueEmergencyIds(@org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Emergency e WHERE e.status = 'ACTIVE' AND e.createdAt >= :cutoff ORDER BY e.createdAt ASC")
    List<Emergency> findActiveEmergenciesForDiscovery(@org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff);
}
