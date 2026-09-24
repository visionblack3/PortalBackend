package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // GET /api/admin/jobs -> View all jobs across system
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        List<Job> jobs = adminService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }

    // GET /api/admin/applications -> View all applications across system
    @GetMapping("/applications")
    public ResponseEntity<List<Application>> getAllApplications() {
        List<Application> applications = adminService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    // PUT /api/admin/jobs/{jobId} -> Moderate/Edit any job
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<Job> updateJob(
            @PathVariable Long jobId,
            @RequestBody JobRequest request) {
        Job updatedJob = adminService.updateJob(jobId, request);
        return ResponseEntity.ok(updatedJob);
    }

    // DELETE /api/admin/jobs/{jobId} -> Delete any job
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<String> deleteJob(@PathVariable Long jobId) {
        adminService.deleteJob(jobId);
        return ResponseEntity.ok("Job deleted by admin successfully.");
    }
}