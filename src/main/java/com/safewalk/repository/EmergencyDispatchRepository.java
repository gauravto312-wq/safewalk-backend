package com.safewalk.repository;

import com.safewalk.entity.EmergencyDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyDispatchRepository extends JpaRepository<EmergencyDispatch, Long> {

    boolean existsByEmergencyIdAndResponderId(Long emergencyId, Long responderId);

    Optional<EmergencyDispatch> findByEmergencyIdAndResponderId(Long emergencyId, Long responderId);

    long countByEmergencyId(Long emergencyId);

    List<EmergencyDispatch> findByEmergencyId(Long emergencyId);
}
