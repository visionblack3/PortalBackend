package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import com.example.CarrerPortal.Repository.ApplicationRepository;
import com.example.CarrerPortal.Repository.JobRepository;
import com.example.CarrerPortal.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class EmployerService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;
    
    private static final Set<String> ALLOWED_STATUSES = Set.of(
    	    "APPLIED", "SHORTLISTED", "ACCEPTED", "REJECTED", "HIRED"
    	);

    // Helper method to retrieve currently authenticated user
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // Post a new job tied directly to the JWT token owner
    public Job postJob(JobRequest request) {
        User employer = getAuthenticatedUser();

        if (employer.getRole() != Role.EMPLOYER) {
            throw new RuntimeException("Access Denied: Only employers are authorized to post jobs.");
        }

        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompanyName(request.getCompanyName());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalaryRange(request.getSalaryRange());
        job.setDeadline(request.getDeadline());
        job.setEmployer(employer);

        return jobRepository.save(job);
    }

    // Get jobs posted by the currently logged-in employer
    public List<Job> getMyPostedJobs() {
        User employer = getAuthenticatedUser();
        return jobRepository.findByEmployerId(employer.getId());
    }

    // View applications received for a specific job with ownership verification
    public List<Application> getApplicationsForJob(Long jobId) {
        User employer = getAuthenticatedUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));

        if (!job.getEmployer().getId().equals(employer.getId())) {
            throw new RuntimeException("Access Denied: You can only view applications for your own jobs.");
        }

        return applicationRepository.findByJobId(jobId);
    }

    // Update application status
    public Application updateApplicantStatus(Long applicationId, String status) {
        User employer = getAuthenticatedUser();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with ID: " + applicationId));

        if (!application.getJob().getEmployer().getId().equals(employer.getId())) {
            throw new RuntimeException("Access Denied: You can only update applications for your own job posts.");
        }

        // Sanitize and validate input
        String formattedStatus = (status != null) ? status.toUpperCase().trim() : "";

        if (!ALLOWED_STATUSES.contains(formattedStatus)) {
            throw new IllegalArgumentException("Invalid status value: '" + status + "'. Allowed values are: " + ALLOWED_STATUSES);
        }

        application.setStatus(formattedStatus);
        return applicationRepository.save(application);
    }
   // Edit an existing job with strict ownership check
    public Job updateJob(Long jobId, JobRequest request) {
        User employer = getAuthenticatedUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));

        if (!job.getEmployer().getId().equals(employer.getId())) {
            throw new RuntimeException("Access Denied: You cannot edit job posts belonging to another employer.");
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompanyName(request.getCompanyName());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalaryRange(request.getSalaryRange());
        job.setDeadline(request.getDeadline());

        return jobRepository.save(job);
    }
    //delete job 
    public void deleteJob(Long jobId, String employerEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));

        // Security check: Verify the job belongs to the logged-in employer
        if (!job.getEmployer().getEmail().equals(employerEmail)) {
            throw new RuntimeException("Unauthorized: You can only delete your own job postings");
        }

        jobRepository.delete(job);
    }
}