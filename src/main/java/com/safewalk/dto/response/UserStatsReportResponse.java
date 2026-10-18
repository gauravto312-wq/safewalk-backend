package com.safewalk.dto.response;

public class UserStatsReportResponse {

    private long totalUsers;
    private long activeUsers;
    private long warningUsers;
    private long restrictedUsers;
    private long suspendedUsers;
    private long deactivatedUsers;
    private long policeUsers;
    private long hospitalUsers;

    public UserStatsReportResponse() {
    }

    public UserStatsReportResponse(long totalUsers, long activeUsers, long warningUsers, long restrictedUsers, long suspendedUsers, long deactivatedUsers, long policeUsers, long hospitalUsers) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.warningUsers = warningUsers;
        this.restrictedUsers = restrictedUsers;
        this.suspendedUsers = suspendedUsers;
        this.deactivatedUsers = deactivatedUsers;
        this.policeUsers = policeUsers;
        this.hospitalUsers = hospitalUsers;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getWarningUsers() {
        return warningUsers;
    }

    public void setWarningUsers(long warningUsers) {
        this.warningUsers = warningUsers;
    }

    public long getRestrictedUsers() {
        return restrictedUsers;
    }

    public void setRestrictedUsers(long restrictedUsers) {
        this.restrictedUsers = restrictedUsers;
    }

    public long getSuspendedUsers() {
        return suspendedUsers;
    }

    public void setSuspendedUsers(long suspendedUsers) {
        this.suspendedUsers = suspendedUsers;
    }

    public long getDeactivatedUsers() {
        return deactivatedUsers;
    }

    public void setDeactivatedUsers(long deactivatedUsers) {
        this.deactivatedUsers = deactivatedUsers;
    }

    public long getPoliceUsers() {
        return policeUsers;
    }

    public void setPoliceUsers(long policeUsers) {
        this.policeUsers = policeUsers;
    }

    public long getHospitalUsers() {
        return hospitalUsers;
    }

    public void setHospitalUsers(long hospitalUsers) {
        this.hospitalUsers = hospitalUsers;
    }

    public static UserStatsReportResponseBuilder builder() {
        return new UserStatsReportResponseBuilder();
    }

    public static class UserStatsReportResponseBuilder {
        private long totalUsers;
        private long activeUsers;
        private long warningUsers;
        private long restrictedUsers;
        private long suspendedUsers;
        private long deactivatedUsers;
        private long policeUsers;
        private long hospitalUsers;

        public UserStatsReportResponseBuilder totalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        public UserStatsReportResponseBuilder activeUsers(long activeUsers) {
            this.activeUsers = activeUsers;
            return this;
        }

        public UserStatsReportResponseBuilder warningUsers(long warningUsers) {
            this.warningUsers = warningUsers;
            return this;
        }

        public UserStatsReportResponseBuilder restrictedUsers(long restrictedUsers) {
            this.restrictedUsers = restrictedUsers;
            return this;
        }

        public UserStatsReportResponseBuilder suspendedUsers(long suspendedUsers) {
            this.suspendedUsers = suspendedUsers;
            return this;
        }

        public UserStatsReportResponseBuilder deactivatedUsers(long deactivatedUsers) {
            this.deactivatedUsers = deactivatedUsers;
            return this;
        }

        public UserStatsReportResponseBuilder policeUsers(long policeUsers) {
            this.policeUsers = policeUsers;
            return this;
        }

        public UserStatsReportResponseBuilder hospitalUsers(long hospitalUsers) {
            this.hospitalUsers = hospitalUsers;
            return this;
        }

        public UserStatsReportResponse build() {
            return new UserStatsReportResponse(totalUsers, activeUsers, warningUsers, restrictedUsers, suspendedUsers, deactivatedUsers, policeUsers, hospitalUsers);
        }
    }
}
