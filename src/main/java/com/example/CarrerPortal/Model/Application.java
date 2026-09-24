package com.example.CarrerPortal.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	/*
	 * @Column(name = "job_id", insertable = false, updatable = false) private Long
	 * jobId;
	 */
    @ManyToOne
    @JoinColumn(name = "seeker_id", nullable = false)
    private User seeker;


	// Ensure Job is also linked as an entity relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    private String resumeUrl;

    @Column(length = 1000)
    private String coverLetter;

    @Column(nullable = false)
    private String status = "APPLIED";

    private LocalDateTime appliedAt;

    // Explicit Default Constructor (Fixes new Application() error)
    public Application() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

	/*
	 * public Long getJobId() { return jobId; } public void setJobId(Long jobId) {
	 * this.jobId = jobId; }
	 */
    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    
    public User getSeeker() {
		return seeker;
	}

	public void setSeeker(User seeker) {
		this.seeker = seeker;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setAppliedAt(LocalDateTime appliedAt) {
		this.appliedAt = appliedAt;
	}


    @PrePersist
    protected void onApply() {
        this.appliedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "APPLIED";
        }
    }
}