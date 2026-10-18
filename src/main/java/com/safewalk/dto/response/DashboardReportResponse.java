package com.safewalk.dto.response;

import java.util.List;

public class DashboardReportResponse {

    private EmergencySummaryReportResponse emergencySummary;
    private EmergencyTypeReportResponse typeStats;
    private EmergencyStatusReportResponse statusStats;
    private UserStatsReportResponse userStats;
    private List<EmergencyResponse> recentActivity;

    public DashboardReportResponse() {
    }

    public DashboardReportResponse(EmergencySummaryReportResponse emergencySummary, EmergencyTypeReportResponse typeStats, EmergencyStatusReportResponse statusStats, UserStatsReportResponse userStats, List<EmergencyResponse> recentActivity) {
        this.emergencySummary = emergencySummary;
        this.typeStats = typeStats;
        this.statusStats = statusStats;
        this.userStats = userStats;
        this.recentActivity = recentActivity;
    }

    public EmergencySummaryReportResponse getEmergencySummary() {
        return emergencySummary;
    }

    public void setEmergencySummary(EmergencySummaryReportResponse emergencySummary) {
        this.emergencySummary = emergencySummary;
    }

    public EmergencyTypeReportResponse getTypeStats() {
        return typeStats;
    }

    public void setTypeStats(EmergencyTypeReportResponse typeStats) {
        this.typeStats = typeStats;
    }

    public EmergencyStatusReportResponse getStatusStats() {
        return statusStats;
    }

    public void setStatusStats(EmergencyStatusReportResponse statusStats) {
        this.statusStats = statusStats;
    }

    public UserStatsReportResponse getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStatsReportResponse userStats) {
        this.userStats = userStats;
    }

    public List<EmergencyResponse> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<EmergencyResponse> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public static DashboardReportResponseBuilder builder() {
        return new DashboardReportResponseBuilder();
    }

    public static class DashboardReportResponseBuilder {
        private EmergencySummaryReportResponse emergencySummary;
        private EmergencyTypeReportResponse typeStats;
        private EmergencyStatusReportResponse statusStats;
        private UserStatsReportResponse userStats;
        private List<EmergencyResponse> recentActivity;

        public DashboardReportResponseBuilder emergencySummary(EmergencySummaryReportResponse emergencySummary) {
            this.emergencySummary = emergencySummary;
            return this;
        }

        public DashboardReportResponseBuilder typeStats(EmergencyTypeReportResponse typeStats) {
            this.typeStats = typeStats;
            return this;
        }

        public DashboardReportResponseBuilder statusStats(EmergencyStatusReportResponse statusStats) {
            this.statusStats = statusStats;
            return this;
        }

        public DashboardReportResponseBuilder userStats(UserStatsReportResponse userStats) {
            this.userStats = userStats;
            return this;
        }

        public DashboardReportResponseBuilder recentActivity(List<EmergencyResponse> recentActivity) {
            this.recentActivity = recentActivity;
            return this;
        }

        public DashboardReportResponse build() {
            return new DashboardReportResponse(emergencySummary, typeStats, statusStats, userStats, recentActivity);
        }
    }
}
