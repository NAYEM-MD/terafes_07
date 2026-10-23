package com.tronai.cv.dto;

import java.util.Objects;

public class ExperienceItemDto {

    private String organization = "";
    private String role = "";
    private String period = "";
    private String detail = "";

    public ExperienceItemDto() {
    }

    public ExperienceItemDto(String organization, String role, String period, String detail) {
        this.organization = organization != null ? organization : "";
        this.role = role != null ? role : "";
        this.period = period != null ? period : "";
        this.detail = detail != null ? detail : "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization != null ? organization : "";
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role : "";
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period != null ? period : "";
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail != null ? detail : "";
    }

    public static class Builder {
        private String organization = "";
        private String role = "";
        private String period = "";
        private String detail = "";

        public Builder organization(String organization) {
            this.organization = organization != null ? organization : "";
            return this;
        }

        public Builder role(String role) {
            this.role = role != null ? role : "";
            return this;
        }

        public Builder period(String period) {
            this.period = period != null ? period : "";
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail != null ? detail : "";
            return this;
        }

        public ExperienceItemDto build() {
            return new ExperienceItemDto(organization, role, period, detail);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperienceItemDto that)) return false;
        return Objects.equals(organization, that.organization) &&
                Objects.equals(role, that.role) &&
                Objects.equals(period, that.period) &&
                Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, role, period, detail);
    }

    @Override
    public String toString() {
        return "ExperienceItemDto{" +
                "organization='" + organization + '\'' +
                ", role='" + role + '\'' +
                ", period='" + period + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
