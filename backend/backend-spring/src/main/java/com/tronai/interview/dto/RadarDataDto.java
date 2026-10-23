package com.tronai.interview.dto;

/**
 * Radar chart data point for interview analysis.
 */
public class RadarDataDto {

    private String metric;
    private String metricJa;
    private int score;
    private int benchmark;

    public RadarDataDto() {
    }

    public RadarDataDto(String metric, String metricJa, int score, int benchmark) {
        this.metric = metric;
        this.metricJa = metricJa;
        this.score = score;
        this.benchmark = benchmark;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getMetricJa() {
        return metricJa;
    }

    public void setMetricJa(String metricJa) {
        this.metricJa = metricJa;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(int benchmark) {
        this.benchmark = benchmark;
    }
}
