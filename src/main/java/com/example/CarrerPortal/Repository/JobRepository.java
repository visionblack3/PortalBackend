package com.example.CarrerPortal.Repository;

import com.example.CarrerPortal.Model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

	List<Job> findByEmployerId(Long employerId);

    @Query("SELECT j FROM Job j WHERE " +
           "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', CAST(:location AS text), '%'))) AND " +
           "(j.deadline IS NULL OR j.deadline >= :currentDate)")
    List<Job> searchJobs(@Param("keyword") String keyword, 
                         @Param("location") String location, 
                         @Param("currentDate") LocalDate currentDate);
}