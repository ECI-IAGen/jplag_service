package com.eci.iagen.jplag_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para solicitudes de detección de plagio
 */
public class PlagiarismRequest {

    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;

    @NotBlank(message = "Assignment title is required")
    private String assignmentTitle;

    @NotEmpty(message = "Submissions list cannot be empty")
    @Valid
    private List<SubmissionInfo> submissions;

    // Constructors
    public PlagiarismRequest() {
    }

    public PlagiarismRequest(Long assignmentId, String assignmentTitle, List<SubmissionInfo> submissions) {
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.submissions = submissions;
    }

    // Getters and Setters
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

    public List<SubmissionInfo> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<SubmissionInfo> submissions) {
        this.submissions = submissions;
    }
}
