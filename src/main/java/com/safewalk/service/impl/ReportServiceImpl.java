package com.safewalk.service.impl;

import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.EmergencyStatusReportResponse;
import com.safewalk.dto.response.EmergencySummaryReportResponse;
import com.safewalk.dto.response.EmergencyTypeReportResponse;
import com.safewalk.dto.response.UserStatsReportResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final EmergencyRepository emergencyRepository;
    private final UserRepository userRepository;
    private final EmergencyMapper emergencyMapper;

    @Autowired
    public ReportServiceImpl(
            EmergencyRepository emergencyRepository,
            UserRepository userRepository,
            EmergencyMapper emergencyMapper) {
        this.emergencyRepository = emergencyRepository;
        this.userRepository = userRepository;
        this.emergencyMapper = emergencyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencySummaryReportResponse getEmergencySummaryReport(LocalDateTime from, LocalDateTime to) {
        log.debug("Generating emergency summary report for date range: from={} to={}", from, to);

        long total;
        long active;
        long accepted;
        long cancelled;
        long resolved;

        if (from != null && to != null) {
            total = emergencyRepository.countByCreatedAtBetween(from, to);
            active = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.ACTIVE, from, to);
            accepted = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.ACCEPTED, from, to);
            cancelled = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.CANCELLED, from, to);
            resolved = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.RESOLVED, from, to);
        } else {
            total = emergencyRepository.count();
            active = emergencyRepository.countByStatus(EmergencyStatus.ACTIVE);
            accepted = emergencyRepository.countByStatus(EmergencyStatus.ACCEPTED);
            cancelled = emergencyRepository.countByStatus(EmergencyStatus.CANCELLED);
            resolved = emergencyRepository.countByStatus(EmergencyStatus.RESOLVED);
        }

        return EmergencySummaryReportResponse.builder()
                .totalEmergencies(total)
                .activeEmergencies(active)
                .acceptedEmergencies(accepted)
                .cancelledEmergencies(cancelled)
                .resolvedEmergencies(resolved)
                .fromDate(from != null ? from.toString() : "ALL_TIME")
                .toDate(to != null ? to.toString() : "ALL_TIME")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyTypeReportResponse getEmergencyTypeReport(LocalDateTime from, LocalDateTime to) {
        log.debug("Generating emergency type distribution report");

        long general;
        long hospital;
        long accident;

        if (from != null && to != null) {
            general = emergencyRepository.countByTypeAndCreatedAtBetween(EmergencyType.GENERAL, from, to);
            hospital = emergencyRepository.countByTypeAndCreatedAtBetween(EmergencyType.HOSPITAL, from, to);
            accident = emergencyRepository.countByTypeAndCreatedAtBetween(EmergencyType.ACCIDENT, from, to);
        } else {
            general = emergencyRepository.countByType(EmergencyType.GENERAL);
            hospital = emergencyRepository.countByType(EmergencyType.HOSPITAL);
            accident = emergencyRepository.countByType(EmergencyType.ACCIDENT);
        }

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("GENERAL", general);
        distribution.put("HOSPITAL", hospital);
        distribution.put("ACCIDENT", accident);

        return EmergencyTypeReportResponse.builder()
                .generalCount(general)
                .hospitalCount(hospital)
                .accidentCount(accident)
                .typeDistribution(distribution)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyStatusReportResponse getEmergencyStatusReport(LocalDateTime from, LocalDateTime to) {
        log.debug("Generating emergency status distribution report");

        long active;
        long accepted;
        long resolved;
        long cancelled;

        if (from != null && to != null) {
            active = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.ACTIVE, from, to);
            accepted = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.ACCEPTED, from, to);
            resolved = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.RESOLVED, from, to);
            cancelled = emergencyRepository.countByStatusAndCreatedAtBetween(EmergencyStatus.CANCELLED, from, to);
        } else {
            active = emergencyRepository.countByStatus(EmergencyStatus.ACTIVE);
            accepted = emergencyRepository.countByStatus(EmergencyStatus.ACCEPTED);
            resolved = emergencyRepository.countByStatus(EmergencyStatus.RESOLVED);
            cancelled = emergencyRepository.countByStatus(EmergencyStatus.CANCELLED);
        }

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("ACTIVE", active);
        distribution.put("ACCEPTED", accepted);
        distribution.put("RESOLVED", resolved);
        distribution.put("CANCELLED", cancelled);

        return EmergencyStatusReportResponse.builder()
                .activeCount(active)
                .acceptedCount(accepted)
                .resolvedCount(resolved)
                .cancelledCount(cancelled)
                .statusDistribution(distribution)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsReportResponse getUserStatsReport() {
        log.debug("Generating user statistics report");

        long total = userRepository.count();
        long active = userRepository.countByAccountStatus(AccountStatus.ACTIVE);
        long warning = userRepository.countByAccountStatus(AccountStatus.WARNING);
        long restricted = userRepository.countByAccountStatus(AccountStatus.RESTRICTED);
        long suspended = userRepository.countByAccountStatus(AccountStatus.SUSPENDED);
        long deactivated = userRepository.countByAccountStatus(AccountStatus.DEACTIVATED);

        long police = userRepository.countByRole(Role.POLICE);
        long hospital = userRepository.countByRole(Role.HOSPITAL);

        return UserStatsReportResponse.builder()
                .totalUsers(total)
                .activeUsers(active)
                .warningUsers(warning)
                .restrictedUsers(restricted)
                .suspendedUsers(suspended)
                .deactivatedUsers(deactivated)
                .policeUsers(police)
                .hospitalUsers(hospital)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardReportResponse getDashboardReport(LocalDateTime from, LocalDateTime to) {
        log.debug("Generating unified system dashboard report");

        EmergencySummaryReportResponse summary = getEmergencySummaryReport(from, to);
        EmergencyTypeReportResponse types = getEmergencyTypeReport(from, to);
        EmergencyStatusReportResponse statuses = getEmergencyStatusReport(from, to);
        UserStatsReportResponse userStats = getUserStatsReport();

        List<Emergency> recentList = emergencyRepository.findTop5ByOrderByCreatedAtDesc();
        List<EmergencyResponse> recentResponses = recentList.stream()
                .map(emergencyMapper::toResponse)
                .collect(Collectors.toList());

        return DashboardReportResponse.builder()
                .emergencySummary(summary)
                .typeStats(types)
                .statusStats(statuses)
                .userStats(userStats)
                .recentActivity(recentResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmergencyResponse> getEmergenciesReport(
            EmergencyStatus status, EmergencyType type, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        log.debug("Fetching filtered emergencies report: status={}, type={}, from={}, to={}", status, type, from, to);

        if (status != null && type != null) {
            return emergencyRepository.findByStatusAndType(status, type, pageable)
                    .map(emergencyMapper::toResponse);
        } else if (status != null) {
            return emergencyRepository.findByStatus(status, pageable)
                    .map(emergencyMapper::toResponse);
        } else if (type != null) {
            return emergencyRepository.findByType(type, pageable)
                    .map(emergencyMapper::toResponse);
        } else if (from != null && to != null) {
            return emergencyRepository.findByCreatedAtBetween(from, to, pageable)
                    .map(emergencyMapper::toResponse);
        } else {
            return emergencyRepository.findAll(pageable)
                    .map(emergencyMapper::toResponse);
        }
    }
}
