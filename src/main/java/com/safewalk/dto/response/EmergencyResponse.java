package com.safewalk.dto.response;

import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EmergencyResponse {

    private Long id;
    private Long requesterId;
    private String requesterName;
    private String requesterMobile;
    private Long responderId;
    private String responderName;
    private String responderMobile;
    private List<UserResponse> responders = new ArrayList<>();
    private EmergencyType type;
    private EmergencyStatus status;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime expiredAt;
    private Long arrivedResponderId;
    private String arrivedResponderName;
    private LocalDateTime arrivedAt;
    private boolean requesterResolveConfirmed;
    private boolean responderResolveConfirmed;
    private GeocodingResponse addressDetails;
    private RouteDistanceResponse distanceAndEta;
    private Double distanceMeters;
    private boolean trustedContactViewer;

    public EmergencyResponse() {
    }

    public EmergencyResponse(Long id, Long requesterId, String requesterName, String requesterMobile, Long responderId, String responderName, String responderMobile, EmergencyType type, EmergencyStatus status, Double latitude, Double longitude, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime acceptedAt, LocalDateTime resolvedAt, LocalDateTime cancelledAt, GeocodingResponse addressDetails, RouteDistanceResponse distanceAndEta) {
        this(id, requesterId, requesterName, requesterMobile, responderId, responderName, responderMobile, new ArrayList<>(), type, status, latitude, longitude, createdAt, updatedAt, acceptedAt, resolvedAt, cancelledAt, null, null, null, false, false, addressDetails, distanceAndEta);
    }

    public EmergencyResponse(Long id, Long requesterId, String requesterName, String requesterMobile, Long responderId, String responderName, String responderMobile, List<UserResponse> responders, EmergencyType type, EmergencyStatus status, Double latitude, Double longitude, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime acceptedAt, LocalDateTime resolvedAt, LocalDateTime cancelledAt, GeocodingResponse addressDetails, RouteDistanceResponse distanceAndEta) {
        this(id, requesterId, requesterName, requesterMobile, responderId, responderName, responderMobile, responders, type, status, latitude, longitude, createdAt, updatedAt, acceptedAt, resolvedAt, cancelledAt, null, null, null, false, false, addressDetails, distanceAndEta);
    }

    public EmergencyResponse(Long id, Long requesterId, String requesterName, String requesterMobile, Long responderId, String responderName, String responderMobile, List<UserResponse> responders, EmergencyType type, EmergencyStatus status, Double latitude, Double longitude, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime acceptedAt, LocalDateTime resolvedAt, LocalDateTime cancelledAt, Long arrivedResponderId, String arrivedResponderName, LocalDateTime arrivedAt, boolean requesterResolveConfirmed, boolean responderResolveConfirmed, GeocodingResponse addressDetails, RouteDistanceResponse distanceAndEta) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterMobile = requesterMobile;
        this.responderId = responderId;
        this.responderName = responderName;
        this.responderMobile = responderMobile;
        this.responders = responders != null ? responders : new ArrayList<>();
        this.type = type;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.acceptedAt = acceptedAt;
        this.resolvedAt = resolvedAt;
        this.cancelledAt = cancelledAt;
        this.arrivedResponderId = arrivedResponderId;
        this.arrivedResponderName = arrivedResponderName;
        this.arrivedAt = arrivedAt;
        this.requesterResolveConfirmed = requesterResolveConfirmed;
        this.responderResolveConfirmed = responderResolveConfirmed;
        this.addressDetails = addressDetails;
        this.distanceAndEta = distanceAndEta;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterMobile() {
        return requesterMobile;
    }

    public void setRequesterMobile(String requesterMobile) {
        this.requesterMobile = requesterMobile;
    }

    public EmergencyType getType() {
        return type;
    }

    public void setType(EmergencyType type) {
        this.type = type;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getResponderId() {
        return responderId;
    }

    public void setResponderId(Long responderId) {
        this.responderId = responderId;
    }

    public String getResponderName() {
        return responderName;
    }

    public void setResponderName(String responderName) {
        this.responderName = responderName;
    }

    public String getResponderMobile() {
        return responderMobile;
    }

    public void setResponderMobile(String responderMobile) {
        this.responderMobile = responderMobile;
    }

    public List<UserResponse> getResponders() {
        return responders;
    }

    public void setResponders(List<UserResponse> responders) {
        this.responders = responders != null ? responders : new ArrayList<>();
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public GeocodingResponse getAddressDetails() {
        return addressDetails;
    }

    public void setAddressDetails(GeocodingResponse addressDetails) {
        this.addressDetails = addressDetails;
    }

    public RouteDistanceResponse getDistanceAndEta() {
        return distanceAndEta;
    }

    public void setDistanceAndEta(RouteDistanceResponse distanceAndEta) {
        this.distanceAndEta = distanceAndEta;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Long getArrivedResponderId() {
        return arrivedResponderId;
    }

    public void setArrivedResponderId(Long arrivedResponderId) {
        this.arrivedResponderId = arrivedResponderId;
    }

    public String getArrivedResponderName() {
        return arrivedResponderName;
    }

    public void setArrivedResponderName(String arrivedResponderName) {
        this.arrivedResponderName = arrivedResponderName;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(LocalDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public boolean isRequesterResolveConfirmed() {
        return requesterResolveConfirmed;
    }

    public void setRequesterResolveConfirmed(boolean requesterResolveConfirmed) {
        this.requesterResolveConfirmed = requesterResolveConfirmed;
    }

    public boolean isResponderResolveConfirmed() {
        return responderResolveConfirmed;
    }

    public void setResponderResolveConfirmed(boolean responderResolveConfirmed) {
        this.responderResolveConfirmed = responderResolveConfirmed;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public boolean isTrustedContactViewer() {
        return trustedContactViewer;
    }

    public void setTrustedContactViewer(boolean trustedContactViewer) {
        this.trustedContactViewer = trustedContactViewer;
    }

    public static EmergencyResponseBuilder builder() {
        return new EmergencyResponseBuilder();
    }

    public static class EmergencyResponseBuilder {
        private Long id;
        private Long requesterId;
        private String requesterName;
        private Double distanceMeters;
        private String requesterMobile;
        private Long responderId;
        private String responderName;
        private String responderMobile;
        private List<UserResponse> responders = new ArrayList<>();
        private EmergencyType type;
        private EmergencyStatus status;
        private Double latitude;
        private Double longitude;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime expiredAt;
        private Long arrivedResponderId;
        private String arrivedResponderName;
        private LocalDateTime arrivedAt;
        private boolean requesterResolveConfirmed;
        private boolean responderResolveConfirmed;
        private GeocodingResponse addressDetails;
        private RouteDistanceResponse distanceAndEta;

        public EmergencyResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmergencyResponseBuilder requesterId(Long requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public EmergencyResponseBuilder requesterName(String requesterName) {
            this.requesterName = requesterName;
            return this;
        }

        public EmergencyResponseBuilder requesterMobile(String requesterMobile) {
            this.requesterMobile = requesterMobile;
            return this;
        }

        public EmergencyResponseBuilder responderId(Long responderId) {
            this.responderId = responderId;
            return this;
        }

        public EmergencyResponseBuilder responderName(String responderName) {
            this.responderName = responderName;
            return this;
        }

        public EmergencyResponseBuilder responderMobile(String responderMobile) {
            this.responderMobile = responderMobile;
            return this;
        }

        public EmergencyResponseBuilder responders(List<UserResponse> responders) {
            this.responders = responders != null ? responders : new ArrayList<>();
            return this;
        }

        public EmergencyResponseBuilder type(EmergencyType type) {
            this.type = type;
            return this;
        }

        public EmergencyResponseBuilder status(EmergencyStatus status) {
            this.status = status;
            return this;
        }

        public EmergencyResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public EmergencyResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public EmergencyResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmergencyResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public EmergencyResponseBuilder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public EmergencyResponseBuilder resolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public EmergencyResponseBuilder cancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public EmergencyResponseBuilder expiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
            return this;
        }

        public EmergencyResponseBuilder arrivedResponderId(Long arrivedResponderId) {
            this.arrivedResponderId = arrivedResponderId;
            return this;
        }

        public EmergencyResponseBuilder arrivedResponderName(String arrivedResponderName) {
            this.arrivedResponderName = arrivedResponderName;
            return this;
        }

        public EmergencyResponseBuilder arrivedAt(LocalDateTime arrivedAt) {
            this.arrivedAt = arrivedAt;
            return this;
        }

        public EmergencyResponseBuilder requesterResolveConfirmed(boolean requesterResolveConfirmed) {
            this.requesterResolveConfirmed = requesterResolveConfirmed;
            return this;
        }

        public EmergencyResponseBuilder responderResolveConfirmed(boolean responderResolveConfirmed) {
            this.responderResolveConfirmed = responderResolveConfirmed;
            return this;
        }

        public EmergencyResponseBuilder addressDetails(GeocodingResponse addressDetails) {
            this.addressDetails = addressDetails;
            return this;
        }

        public EmergencyResponseBuilder distanceAndEta(RouteDistanceResponse distanceAndEta) {
            this.distanceAndEta = distanceAndEta;
            return this;
        }

        public EmergencyResponseBuilder distanceMeters(Double distanceMeters) {
            this.distanceMeters = distanceMeters;
            return this;
        }

        private boolean trustedContactViewer;

        public EmergencyResponseBuilder trustedContactViewer(boolean trustedContactViewer) {
            this.trustedContactViewer = trustedContactViewer;
            return this;
        }

        public EmergencyResponse build() {
            EmergencyResponse resp = new EmergencyResponse(id, requesterId, requesterName, requesterMobile, responderId, responderName, responderMobile, responders, type, status, latitude, longitude, createdAt, updatedAt, acceptedAt, resolvedAt, cancelledAt, arrivedResponderId, arrivedResponderName, arrivedAt, requesterResolveConfirmed, responderResolveConfirmed, addressDetails, distanceAndEta);
            resp.setExpiredAt(expiredAt);
            resp.setDistanceMeters(distanceMeters);
            resp.setTrustedContactViewer(trustedContactViewer);
            return resp;
        }
    }
}
