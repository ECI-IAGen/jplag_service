package com.eci.iagen.jplag_service.dto;

import java.util.List;

/**
 * DTO para la respuesta de detección de plagio
 */
public class PlagiarismDetectionResponse {
    private Long assignmentId;
    private String assignmentTitle;
    private List<ComparisonResult> comparisons;
    private String reportUrl;
    private boolean success;
    private String message;
    private Statistics statistics;

    public PlagiarismDetectionResponse() {}

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public List<ComparisonResult> getComparisons() {
        return comparisons;
    }

    public void setComparisons(List<ComparisonResult> comparisons) {
        this.comparisons = comparisons;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public static class ComparisonResult {
        private String submission1;
        private String submission2;
        private String team1;
        private String team2;
        private double similarity;
        private int matchedTokens;
        private String status;

        public ComparisonResult() {}

        public ComparisonResult(String submission1, String submission2, String team1, String team2, 
                              double similarity, int matchedTokens, String status) {
            this.submission1 = submission1;
            this.submission2 = submission2;
            this.team1 = team1;
            this.team2 = team2;
            this.similarity = similarity;
            this.matchedTokens = matchedTokens;
            this.status = status;
        }

        public String getSubmission1() {
            return submission1;
        }

        public void setSubmission1(String submission1) {
            this.submission1 = submission1;
        }

        public String getSubmission2() {
            return submission2;
        }

        public void setSubmission2(String submission2) {
            this.submission2 = submission2;
        }

        public String getTeam1() {
            return team1;
        }

        public void setTeam1(String team1) {
            this.team1 = team1;
        }

        public String getTeam2() {
            return team2;
        }

        public void setTeam2(String team2) {
            this.team2 = team2;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }

        public int getMatchedTokens() {
            return matchedTokens;
        }

        public void setMatchedTokens(int matchedTokens) {
            this.matchedTokens = matchedTokens;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class Statistics {
        private int totalSubmissions;
        private int totalComparisons;
        private double averageSimilarity;
        private double maxSimilarity;
        private double minSimilarity;

        public Statistics() {}

        public Statistics(int totalSubmissions, int totalComparisons, double averageSimilarity, 
                         double maxSimilarity, double minSimilarity) {
            this.totalSubmissions = totalSubmissions;
            this.totalComparisons = totalComparisons;
            this.averageSimilarity = averageSimilarity;
            this.maxSimilarity = maxSimilarity;
            this.minSimilarity = minSimilarity;
        }

        public int getTotalSubmissions() {
            return totalSubmissions;
        }

        public void setTotalSubmissions(int totalSubmissions) {
            this.totalSubmissions = totalSubmissions;
        }

        public int getTotalComparisons() {
            return totalComparisons;
        }

        public void setTotalComparisons(int totalComparisons) {
            this.totalComparisons = totalComparisons;
        }

        public double getAverageSimilarity() {
            return averageSimilarity;
        }

        public void setAverageSimilarity(double averageSimilarity) {
            this.averageSimilarity = averageSimilarity;
        }

        public double getMaxSimilarity() {
            return maxSimilarity;
        }

        public void setMaxSimilarity(double maxSimilarity) {
            this.maxSimilarity = maxSimilarity;
        }

        public double getMinSimilarity() {
            return minSimilarity;
        }

        public void setMinSimilarity(double minSimilarity) {
            this.minSimilarity = minSimilarity;
        }
    }
}
