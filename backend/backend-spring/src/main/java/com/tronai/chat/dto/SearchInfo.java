package com.tronai.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class SearchInfo {

    private String mode = "auto";
    private boolean performed = false;
    private String status = "unavailable";
    private String query = null;
    private String detail = "";
    private List<SearchHit> results = new ArrayList<>();

    public SearchInfo() {
    }

    public SearchInfo(String mode, boolean performed, String status, String query, String detail, List<SearchHit> results) {
        this.mode = mode != null ? mode : "auto";
        this.performed = performed;
        this.status = status != null ? status : "unavailable";
        this.query = query;
        this.detail = detail != null ? detail : "";
        this.results = results != null ? results : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isPerformed() {
        return performed;
    }

    public void setPerformed(boolean performed) {
        this.performed = performed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public List<SearchHit> getResults() {
        return results;
    }

    public void setResults(List<SearchHit> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public static class Builder {
        private String mode = "auto";
        private boolean performed = false;
        private String status = "unavailable";
        private String query = null;
        private String detail = "";
        private List<SearchHit> results = new ArrayList<>();

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder performed(boolean performed) {
            this.performed = performed;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder results(List<SearchHit> results) {
            this.results = results != null ? results : new ArrayList<>();
            return this;
        }

        public SearchInfo build() {
            return new SearchInfo(mode, performed, status, query, detail, results);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchInfo that)) return false;
        return performed == that.performed &&
                Objects.equals(mode, that.mode) &&
                Objects.equals(status, that.status) &&
                Objects.equals(query, that.query) &&
                Objects.equals(detail, that.detail) &&
                Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, performed, status, query, detail, results);
    }

    @Override
    public String toString() {
        return "SearchInfo{" +
                "mode='" + mode + '\'' +
                ", performed=" + performed +
                ", status='" + status + '\'' +
                ", query='" + query + '\'' +
                ", detail='" + detail + '\'' +
                ", results=" + results +
                '}';
    }
}
