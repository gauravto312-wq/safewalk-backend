package com.safewalk.dto.response;

public class GeocodingResponse {

    private String formattedAddress;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String provider;

    public GeocodingResponse() {
    }

    public GeocodingResponse(String formattedAddress, String city, String state, String country, String postalCode, Double latitude, Double longitude, String provider) {
        this.formattedAddress = formattedAddress;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.provider = provider;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public static GeocodingResponseBuilder builder() {
        return new GeocodingResponseBuilder();
    }

    public static class GeocodingResponseBuilder {
        private String formattedAddress;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private Double latitude;
        private Double longitude;
        private String provider;

        public GeocodingResponseBuilder formattedAddress(String formattedAddress) {
            this.formattedAddress = formattedAddress;
            return this;
        }

        public GeocodingResponseBuilder city(String city) {
            this.city = city;
            return this;
        }

        public GeocodingResponseBuilder state(String state) {
            this.state = state;
            return this;
        }

        public GeocodingResponseBuilder country(String country) {
            this.country = country;
            return this;
        }

        public GeocodingResponseBuilder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public GeocodingResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public GeocodingResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public GeocodingResponseBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public GeocodingResponse build() {
            return new GeocodingResponse(formattedAddress, city, state, country, postalCode, latitude, longitude, provider);
        }
    }
}
