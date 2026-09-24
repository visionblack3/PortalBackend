package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Repository.ApplicationRepository;
import com.example.CarrerPortal.Repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public Job updateJob(Long jobId, JobRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobId));

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompanyName(request.getCompanyName());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalaryRange(request.getSalaryRange());
        job.setDeadline(request.getDeadline());

        return jobRepository.save(job);
    }

    public void deleteJob(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new RuntimeException("Job not found with ID: " + jobId);
        }
        jobRepository.deleteById(jobId);
    }
}