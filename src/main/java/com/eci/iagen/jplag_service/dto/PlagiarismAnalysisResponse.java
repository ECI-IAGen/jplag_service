package com.eci.iagen.jplag_service.dto;

import java.util.List;

public class PlagiarismAnalysisResponse {
    private Long assignmentId;
    private String assignmentTitle;
    private List<SubmissionDto> submissions;
    private List<SubmissionComparisonDto> comparisons;

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

    public List<SubmissionDto> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<SubmissionDto> submissions) {
        this.submissions = submissions;
    }

    public List<SubmissionComparisonDto> getComparisons() {
        return comparisons;
    }

    public void setComparisons(List<SubmissionComparisonDto> comparisons) {
        this.comparisons = comparisons;
    }
}