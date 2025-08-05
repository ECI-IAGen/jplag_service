package com.eci.iagen.jplag_service.dto;

public class SubmissionComparisonDto {
    private Long submissionId1;
    private Long submissionId2;
    private double similarityPercentage;

    public Long getSubmissionId1() {
        return submissionId1;
    }

    public void setSubmissionId1(Long submissionId1) {
        this.submissionId1 = submissionId1;
    }

    public Long getSubmissionId2() {
        return submissionId2;
    }

    public void setSubmissionId2(Long submissionId2) {
        this.submissionId2 = submissionId2;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public void setSimilarityPercentage(double similarityPercentage) {
        this.similarityPercentage = similarityPercentage;
    }
}