package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Repository.ApplicationRepository;
import com.example.CarrerPortal.Repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private AdminService adminService;

    private Job job;

    @BeforeEach
    void setUp() {
        job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        job.setDescription("Build things");
        job.setCompanyName("Acme");
        job.setLocation("Remote");
    }

    @Test
    void getAllJobs_returnsEveryJobFromRepository() {
        when(jobRepository.findAll()).thenReturn(List.of(job));

        List<Job> result = adminService.getAllJobs();

        assertThat(result).containsExactly(job);
        verify(jobRepository).findAll();
    }

    @Test
    void getAllApplications_returnsEveryApplicationFromRepository() {
        Application application = new Application();
        application.setId(5L);
        when(applicationRepository.findAll()).thenReturn(List.of(application));

        List<Application> result = adminService.getAllApplications();

        assertThat(result).containsExactly(application);
    }

    @Test
    void updateJob_appliesEveryFieldFromRequestAndSaves() {
        JobRequest request = new JobRequest();
        request.setTitle("Senior Backend Engineer");
        request.setDescription("Own the payments platform");
        request.setCompanyName("Acme Corp");
        request.setLocation("Bengaluru");
        request.setJobType("FULL_TIME");
        request.setSalaryRange(1800000.0);
        request.setDeadline(LocalDate.of(2026, 12, 31));

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job updated = adminService.updateJob(1L, request);

        assertThat(updated.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(updated.getDescription()).isEqualTo("Own the payments platform");
        assertThat(updated.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(updated.getLocation()).isEqualTo("Bengaluru");
        assertThat(updated.getJobType()).isEqualTo("FULL_TIME");
        assertThat(updated.getSalaryRange()).isEqualTo(1800000.0);
        assertThat(updated.getDeadline()).isEqualTo(LocalDate.of(2026, 12, 31));
        verify(jobRepository).save(job);
    }

    @Test
    void updateJob_throwsWhenJobDoesNotExist() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateJob(99L, new JobRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(jobRepository, never()).save(any());
    }

    @Test
    void deleteJob_deletesWhenJobExists() {
        when(jobRepository.existsById(1L)).thenReturn(true);

        adminService.deleteJob(1L);

        verify(jobRepository).deleteById(1L);
    }

    @Test
    void deleteJob_throwsWhenJobDoesNotExist() {
        when(jobRepository.existsById(eq(99L))).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteJob(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(jobRepository, never()).deleteById(any());
    }
}
