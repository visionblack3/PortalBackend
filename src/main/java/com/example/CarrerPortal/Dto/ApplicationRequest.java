package com.example.CarrerPortal.Dto;

public class ApplicationRequest {

    private Long jobId;
    //private Long seekerId;
    private String resumeUrl;
    private String coverLetter;

    public ApplicationRequest() {}

    public ApplicationRequest(Long jobId, Long seekerId, String resumeUrl, String coverLetter) {
        this.jobId = jobId;
        //this.seekerId = seekerId;
        this.resumeUrl = resumeUrl;
        this.coverLetter = coverLetter;
    }

    // Getters and Setters
    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

	/*
	 * public Long getSeekerId() { return seekerId; }
	 * 
	 * public void setSeekerId(Long seekerId) { this.seekerId = seekerId; }
	 */
    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
}