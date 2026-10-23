package com.tronai.chat.dto;

import java.util.Objects;

public class SearchHit {

    private String title = "";
    private String url;
    private String snippet = "";

    public SearchHit() {
    }

    public SearchHit(String title, String url, String snippet) {
        this.title = title != null ? title : "";
        this.url = url;
        this.snippet = snippet != null ? snippet : "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public static class Builder {
        private String title = "";
        private String url;
        private String snippet = "";

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        public SearchHit build() {
            return new SearchHit(title, url, snippet);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchHit searchHit)) return false;
        return Objects.equals(title, searchHit.title) &&
                Objects.equals(url, searchHit.url) &&
                Objects.equals(snippet, searchHit.snippet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, url, snippet);
    }

    @Override
    public String toString() {
        return "SearchHit{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", snippet='" + snippet + '\'' +
                '}';
    }
}
