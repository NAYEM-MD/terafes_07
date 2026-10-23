package com.tronai.cv.dto;

import java.util.Objects;

public class EducationItemDto {

    private String school = "";
    private String period = "";
    private String detail = "";

    public EducationItemDto() {
    }

    public EducationItemDto(String school, String period, String detail) {
        this.school = school != null ? school : "";
        this.period = period != null ? period : "";
        this.detail = detail != null ? detail : "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school != null ? school : "";
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
        private String school = "";
        private String period = "";
        private String detail = "";

        public Builder school(String school) {
            this.school = school != null ? school : "";
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

        public EducationItemDto build() {
            return new EducationItemDto(school, period, detail);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EducationItemDto that)) return false;
        return Objects.equals(school, that.school) &&
                Objects.equals(period, that.period) &&
                Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(school, period, detail);
    }

    @Override
    public String toString() {
        return "EducationItemDto{" +
                "school='" + school + '\'' +
                ", period='" + period + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
