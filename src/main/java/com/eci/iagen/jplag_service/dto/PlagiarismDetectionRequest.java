package com.eci.iagen.jplag_service.dto;

import java.util.List;

/**
 * DTO para recibir requests de detección de plagio del API Gateway
 */
public class PlagiarismDetectionRequest {
    private Long assignmentId;
    private String assignmentTitle;
    private List<SubmissionDto> submissions;

    public PlagiarismDetectionRequest() {}

    public PlagiarismDetectionRequest(Long assignmentId, String assignmentTitle, List<SubmissionDto> submissions) {
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.submissions = submissions;
    }

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

    @Override
    public String toString() {
        return "PlagiarismDetectionRequest{" +
                "assignmentId=" + assignmentId +
                ", assignmentTitle='" + assignmentTitle + '\'' +
                ", submissions=" + submissions +
                '}';
    }
}
