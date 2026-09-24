package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Dto.ApplicationRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Service.JobSeekerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seeker")
public class JobSeekerController {

    @Autowired
    private JobSeekerService jobSeekerService;

    // View active jobs (Public / open for seekers)
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getAllActiveJobs() {
        return ResponseEntity.ok(jobSeekerService.getAllActiveJobs());
    }

    // Search active jobs by title/description keyword or location
    @GetMapping("/jobs/search")
    public ResponseEntity<List<Job>> searchJobs(@RequestParam(defaultValue = "") String keyword,
                                                 @RequestParam(defaultValue = "") String location) {
        return ResponseEntity.ok(jobSeekerService.searchJobs(keyword, location));
    }

    // Apply for a job (Seeker identity extracted safely)
    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<?> applyForJob(
            @PathVariable Long jobId,
            @RequestBody(required = false) ApplicationRequest request) {
        try {
            if (request == null) {
                request = new ApplicationRequest();
            }

            // Extract email cleanly from SecurityContextHolder
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String seekerEmail = authentication.getName();

            Application application = jobSeekerService.applyForJob(jobId, request, seekerEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Applied"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get application history for logged-in job seeker
    @GetMapping("/applications")
    public ResponseEntity<List<Application>> getMyApplications() {
        return ResponseEntity.ok(jobSeekerService.getMyApplications());
    }
}