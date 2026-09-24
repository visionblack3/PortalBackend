package com.example.CarrerPortal.Repository;

import com.example.CarrerPortal.Model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Find all applications submitted for a specific Job ID (navigates application.job.id)
    List<Application> findByJobId(Long jobId);

    // Find all applications submitted by a specific Job Seeker User ID (navigates application.seeker.id)
    List<Application> findBySeekerId(Long seekerId);

    // Check if a seeker has already applied for a specific job (prevents duplicate applications)
    boolean existsByJobIdAndSeekerId(Long jobId, Long seekerId);
}