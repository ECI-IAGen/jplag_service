package com.eci.iagen.jplag_service.dto.response;

import java.util.List;

/**
 * DTO para respuestas de detección de plagio
 */
public class PlagiarismResponse {

    private Long assignmentId;
    private String assignmentTitle;
    private List<ComparisonResult> comparisons;

    // Constructors
    public PlagiarismResponse() {
    }

    public PlagiarismResponse(Long assignmentId, String assignmentTitle, List<ComparisonResult> comparisons) {
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.comparisons = comparisons;
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

    public List<ComparisonResult> getComparisons() {
        return comparisons;
    }

    public void setComparisons(List<ComparisonResult> comparisons) {
        this.comparisons = comparisons;
    }
}
