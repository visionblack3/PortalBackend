package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Dto.UpdateStatusRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Service.EmployerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employer")
public class EmployerController {

    @Autowired
    private EmployerService employerService;

    // Post a new job (Employer identity pulled automatically from JWT)
    @PostMapping("/jobs")
    public ResponseEntity<?> postJob(@RequestBody JobRequest request) {
        try {
            Job job = employerService.postJob(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Job posted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // View all jobs posted by the logged-in employer
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getMyPostedJobs() {
        return ResponseEntity.ok(employerService.getMyPostedJobs());
    }

    // Edit an existing job post (Service verifies ownership)
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<?> updateJob(@PathVariable Long jobId, @RequestBody JobRequest request) {
        try {
            Job updatedJob = employerService.updateJob(jobId, request);
            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<?> deleteJob(@PathVariable Long jobId, Principal principal) {
        try {
            // principal.getName() gets the logged-in employer's email from JWT
            employerService.deleteJob(jobId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Job deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // View applications for a specific job post owned by this employer
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<?> getApplicationsForJob(@PathVariable Long jobId) {
        try {
            List<Application> applications = employerService.getApplicationsForJob(jobId);
            return ResponseEntity.ok(applications);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // Update applicant status (e.g., SHORTLISTED, REJECTED, HIRED)
    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<?> updateApplicantStatus(
            @PathVariable Long applicationId, 
            @RequestBody UpdateStatusRequest request) {
        try {
            Application application = employerService.updateApplicantStatus(applicationId, request.getStatus());
            return ResponseEntity.ok(application);
        } catch (IllegalArgumentException e) {
            // Bad request input from client (e.g., invalid status string)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Security / Authorization / Resource missing errors
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}