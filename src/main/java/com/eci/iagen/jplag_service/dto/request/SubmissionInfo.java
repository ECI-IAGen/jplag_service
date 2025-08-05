package com.eci.iagen.jplag_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Información de entrega para análisis de plagio
 */
public class SubmissionInfo {

    @NotNull(message = "Submission ID is required")
    private Long submissionId;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotBlank(message = "Team name is required")
    private String teamName;

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    private List<String> memberNames;

    // Constructors
    public SubmissionInfo() {
    }

    public SubmissionInfo(Long submissionId, String teamName, String repositoryUrl) {
        this.submissionId = submissionId;
        this.teamName = teamName;
        this.repositoryUrl = repositoryUrl;
    }

    public SubmissionInfo(Long submissionId, Long teamId, String teamName, String repositoryUrl,
            List<String> memberNames) {
        this.submissionId = submissionId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.repositoryUrl = repositoryUrl;
        this.memberNames = memberNames;
    }

    // Getters and Setters
    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public List<String> getMemberNames() {
        return memberNames;
    }

    public void setMemberNames(List<String> memberNames) {
        this.memberNames = memberNames;
    }
}
