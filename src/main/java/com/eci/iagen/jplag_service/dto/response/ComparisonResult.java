package com.eci.iagen.jplag_service.dto.response;

/**
 * Resultado de comparación entre dos entregas
 */
public class ComparisonResult {
    private Long submissionId1;
    private Long submissionId2;
    private double similarity;

    // Constructors
    public ComparisonResult() {
    }

    public ComparisonResult(Long submissionId1, Long submissionId2, double similarity) {
        this.submissionId1 = submissionId1;
        this.submissionId2 = submissionId2;
        this.similarity = similarity;
    }

    // Getters and Setters
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

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
