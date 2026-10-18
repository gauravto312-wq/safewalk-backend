package com.safewalk.dto.response;

import java.util.Map;

public class EmergencyStatusReportResponse {

    private long activeCount;
    private long acceptedCount;
    private long resolvedCount;
    private long cancelledCount;
    private Map<String, Long> statusDistribution;

    public EmergencyStatusReportResponse() {
    }

    public EmergencyStatusReportResponse(long activeCount, long acceptedCount, long resolvedCount, long cancelledCount, Map<String, Long> statusDistribution) {
        this.activeCount = activeCount;
        this.acceptedCount = acceptedCount;
        this.resolvedCount = resolvedCount;
        this.cancelledCount = cancelledCount;
        this.statusDistribution = statusDistribution;
    }

    public long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(long activeCount) {
        this.activeCount = activeCount;
    }

    public long getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(long acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public Map<String, Long> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<String, Long> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public static EmergencyStatusReportResponseBuilder builder() {
        return new EmergencyStatusReportResponseBuilder();
    }

    public static class EmergencyStatusReportResponseBuilder {
        private long activeCount;
        private long acceptedCount;
        private long resolvedCount;
        private long cancelledCount;
        private Map<String, Long> statusDistribution;

        public EmergencyStatusReportResponseBuilder activeCount(long activeCount) {
            this.activeCount = activeCount;
            return this;
        }

        public EmergencyStatusReportResponseBuilder acceptedCount(long acceptedCount) {
            this.acceptedCount = acceptedCount;
            return this;
        }

        public EmergencyStatusReportResponseBuilder resolvedCount(long resolvedCount) {
            this.resolvedCount = resolvedCount;
            return this;
        }

        public EmergencyStatusReportResponseBuilder cancelledCount(long cancelledCount) {
            this.cancelledCount = cancelledCount;
            return this;
        }

        public EmergencyStatusReportResponseBuilder statusDistribution(Map<String, Long> statusDistribution) {
            this.statusDistribution = statusDistribution;
            return this;
        }

        public EmergencyStatusReportResponse build() {
            return new EmergencyStatusReportResponse(activeCount, acceptedCount, resolvedCount, cancelledCount, statusDistribution);
        }
    }
}
