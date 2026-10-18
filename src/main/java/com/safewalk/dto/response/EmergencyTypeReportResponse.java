package com.safewalk.dto.response;

import java.util.Map;

public class EmergencyTypeReportResponse {

    private long generalCount;
    private long hospitalCount;
    private long accidentCount;
    private Map<String, Long> typeDistribution;

    public EmergencyTypeReportResponse() {
    }

    public EmergencyTypeReportResponse(long generalCount, long hospitalCount, long accidentCount, Map<String, Long> typeDistribution) {
        this.generalCount = generalCount;
        this.hospitalCount = hospitalCount;
        this.accidentCount = accidentCount;
        this.typeDistribution = typeDistribution;
    }

    public long getGeneralCount() {
        return generalCount;
    }

    public void setGeneralCount(long generalCount) {
        this.generalCount = generalCount;
    }

    public long getHospitalCount() {
        return hospitalCount;
    }

    public void setHospitalCount(long hospitalCount) {
        this.hospitalCount = hospitalCount;
    }

    public long getAccidentCount() {
        return accidentCount;
    }

    public void setAccidentCount(long accidentCount) {
        this.accidentCount = accidentCount;
    }

    public Map<String, Long> getTypeDistribution() {
        return typeDistribution;
    }

    public void setTypeDistribution(Map<String, Long> typeDistribution) {
        this.typeDistribution = typeDistribution;
    }

    public static EmergencyTypeReportResponseBuilder builder() {
        return new EmergencyTypeReportResponseBuilder();
    }

    public static class EmergencyTypeReportResponseBuilder {
        private long generalCount;
        private long hospitalCount;
        private long accidentCount;
        private Map<String, Long> typeDistribution;

        public EmergencyTypeReportResponseBuilder generalCount(long generalCount) {
            this.generalCount = generalCount;
            return this;
        }

        public EmergencyTypeReportResponseBuilder hospitalCount(long hospitalCount) {
            this.hospitalCount = hospitalCount;
            return this;
        }

        public EmergencyTypeReportResponseBuilder accidentCount(long accidentCount) {
            this.accidentCount = accidentCount;
            return this;
        }

        public EmergencyTypeReportResponseBuilder typeDistribution(Map<String, Long> typeDistribution) {
            this.typeDistribution = typeDistribution;
            return this;
        }

        public EmergencyTypeReportResponse build() {
            return new EmergencyTypeReportResponse(generalCount, hospitalCount, accidentCount, typeDistribution);
        }
    }
}
