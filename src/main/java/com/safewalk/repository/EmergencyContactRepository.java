package com.safewalk.repository;

import com.safewalk.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByUserId(Long userId);

    @Query("SELECT ec FROM EmergencyContact ec JOIN FETCH ec.contactUser WHERE ec.user.id = :userId")
    List<EmergencyContact> findByUserIdWithContactUser(@Param("userId") Long userId);

    Optional<EmergencyContact> findByUserIdAndContactUserId(Long userId, Long contactUserId);

    boolean existsByUserIdAndContactUserId(Long userId, Long contactUserId);

    long countByUserId(Long userId);

    void deleteByUserIdAndContactUserId(Long userId, Long contactUserId);
}
