package com.example.CarrerPortal.Dto;

import java.time.LocalDate;

public class JobRequest {
    private String title;
    private String description;
    private String companyName;
    private String location;
    private String jobType;
    private Double salaryRange;
    private Long employerId;
    private LocalDate deadline;

    // Default Constructor
    public JobRequest() {}

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public Double getSalaryRange() { return salaryRange; }
    public void setSalaryRange(Double salaryRange) { this.salaryRange = salaryRange; }

    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}