package com.safewalk.dto.response;

public class EmergencySummaryReportResponse {

    private long totalEmergencies;
    private long activeEmergencies;
    private long acceptedEmergencies;
    private long cancelledEmergencies;
    private long resolvedEmergencies;
    private String fromDate;
    private String toDate;

    public EmergencySummaryReportResponse() {
    }

    public EmergencySummaryReportResponse(long totalEmergencies, long activeEmergencies, long acceptedEmergencies, long cancelledEmergencies, long resolvedEmergencies, String fromDate, String toDate) {
        this.totalEmergencies = totalEmergencies;
        this.activeEmergencies = activeEmergencies;
        this.acceptedEmergencies = acceptedEmergencies;
        this.cancelledEmergencies = cancelledEmergencies;
        this.resolvedEmergencies = resolvedEmergencies;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public long getTotalEmergencies() {
        return totalEmergencies;
    }

    public void setTotalEmergencies(long totalEmergencies) {
        this.totalEmergencies = totalEmergencies;
    }

    public long getActiveEmergencies() {
        return activeEmergencies;
    }

    public void setActiveEmergencies(long activeEmergencies) {
        this.activeEmergencies = activeEmergencies;
    }

    public long getAcceptedEmergencies() {
        return acceptedEmergencies;
    }

    public void setAcceptedEmergencies(long acceptedEmergencies) {
        this.acceptedEmergencies = acceptedEmergencies;
    }

    public long getCancelledEmergencies() {
        return cancelledEmergencies;
    }

    public void setCancelledEmergencies(long cancelledEmergencies) {
        this.cancelledEmergencies = cancelledEmergencies;
    }

    public long getResolvedEmergencies() {
        return resolvedEmergencies;
    }

    public void setResolvedEmergencies(long resolvedEmergencies) {
        this.resolvedEmergencies = resolvedEmergencies;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public static EmergencySummaryReportResponseBuilder builder() {
        return new EmergencySummaryReportResponseBuilder();
    }

    public static class EmergencySummaryReportResponseBuilder {
        private long totalEmergencies;
        private long activeEmergencies;
        private long acceptedEmergencies;
        private long cancelledEmergencies;
        private long resolvedEmergencies;
        private String fromDate;
        private String toDate;

        public EmergencySummaryReportResponseBuilder totalEmergencies(long totalEmergencies) {
            this.totalEmergencies = totalEmergencies;
            return this;
        }

        public EmergencySummaryReportResponseBuilder activeEmergencies(long activeEmergencies) {
            this.activeEmergencies = activeEmergencies;
            return this;
        }

        public EmergencySummaryReportResponseBuilder acceptedEmergencies(long acceptedEmergencies) {
            this.acceptedEmergencies = acceptedEmergencies;
            return this;
        }

        public EmergencySummaryReportResponseBuilder cancelledEmergencies(long cancelledEmergencies) {
            this.cancelledEmergencies = cancelledEmergencies;
            return this;
        }

        public EmergencySummaryReportResponseBuilder resolvedEmergencies(long resolvedEmergencies) {
            this.resolvedEmergencies = resolvedEmergencies;
            return this;
        }

        public EmergencySummaryReportResponseBuilder fromDate(String fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public EmergencySummaryReportResponseBuilder toDate(String toDate) {
            this.toDate = toDate;
            return this;
        }

        public EmergencySummaryReportResponse build() {
            return new EmergencySummaryReportResponse(totalEmergencies, activeEmergencies, acceptedEmergencies, cancelledEmergencies, resolvedEmergencies, fromDate, toDate);
        }
    }
}
