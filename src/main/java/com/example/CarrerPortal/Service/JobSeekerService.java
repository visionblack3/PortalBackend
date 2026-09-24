package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.ApplicationRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Model.User;
import com.example.CarrerPortal.Repository.ApplicationRepository;
import com.example.CarrerPortal.Repository.JobRepository;
import com.example.CarrerPortal.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobSeekerService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwilioSmsService twilioSmsService;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public List<Job> getAllActiveJobs() {
        return jobRepository.searchJobs("", "", LocalDate.now());
    }

    public List<Job> searchJobs(String keyword, String location) {
        return jobRepository.searchJobs(keyword, location, LocalDate.now());
    }

    public Application applyForJob(Long jobId, ApplicationRequest request, String seekerEmail) {
        User seeker = userRepository.findByEmail(seekerEmail)
                .orElseThrow(() -> new RuntimeException("Seeker not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));

        if (applicationRepository.existsByJobIdAndSeekerId(jobId, seeker.getId())) {
            throw new RuntimeException("You have already applied for this position.");
        }

        Application application = new Application();
        application.setJob(job);
        application.setSeeker(seeker);
        application.setResumeUrl(request.getResumeUrl());
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus("APPLIED");

        Application savedApplication = applicationRepository.save(application);

        // 1. Send SMS notification to Job Seeker
        if (seeker.getPhone() != null && !seeker.getPhone().isEmpty()) {
            String seekerMsg = "Application Submitted: You applied for '" + job.getTitle() + "' at " + job.getCompanyName() + ".";
            twilioSmsService.sendSms(seeker.getPhone(), seekerMsg);
        }

        // 2. Send SMS notification to Employer
        User employer = job.getEmployer();
        if (employer != null && employer.getPhone() != null && !employer.getPhone().isEmpty()) {
            String employerMsg = "New Applicant: " + seeker.getName() + " applied for your posting '" + job.getTitle() + "'.";
            twilioSmsService.sendSms(employer.getPhone(), employerMsg);
        }

        return savedApplication;
    }

    public List<Application> getMyApplications() {
        User seeker = getAuthenticatedUser();
        return applicationRepository.findBySeekerId(seeker.getId());
    }
}