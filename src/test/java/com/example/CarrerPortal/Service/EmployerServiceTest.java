package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.JobRequest;
import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import com.example.CarrerPortal.Repository.ApplicationRepository;
import com.example.CarrerPortal.Repository.JobRepository;
import com.example.CarrerPortal.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployerService employerService;

    private User employer;
    private User otherEmployer;
    private Job job;

    @BeforeEach
    void setUp() {
        employer = new User("Priya Employer", "priya@acme.com", "hash", "9990001111", Role.EMPLOYER);
        employer.setId(10L);

        otherEmployer = new User("Someone Else", "someone@else.com", "hash", "9990002222", Role.EMPLOYER);
        otherEmployer.setId(20L);

        job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        job.setEmployer(employer);

        authenticateAs(employer.getEmail());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    // ---------- postJob ----------

    @Test
    void postJob_savesJobOwnedByAuthenticatedEmployer() {
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        JobRequest request = new JobRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("desc");
        request.setCompanyName("Acme");
        request.setLocation("Remote");
        request.setJobType("FULL_TIME");
        request.setSalaryRange(1000000.0);
        request.setDeadline(LocalDate.now().plusDays(30));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job saved = employerService.postJob(request);

        assertThat(saved.getEmployer()).isEqualTo(employer);
        assertThat(saved.getTitle()).isEqualTo("Backend Engineer");
    }

    @Test
    void postJob_rejectsNonEmployerRole() {
        User seeker = new User("Seeker", "seeker@x.com", "hash", "111", Role.JOB_SEEKER);
        authenticateAs("seeker@x.com");
        when(userRepository.findByEmail("seeker@x.com")).thenReturn(Optional.of(seeker));

        assertThatThrownBy(() -> employerService.postJob(new JobRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access Denied");

        verify(jobRepository, never()).save(any());
    }

    // ---------- getMyPostedJobs ----------

    @Test
    void getMyPostedJobs_returnsJobsForAuthenticatedEmployerId() {
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(jobRepository.findByEmployerId(10L)).thenReturn(List.of(job));

        List<Job> result = employerService.getMyPostedJobs();

        assertThat(result).containsExactly(job);
    }

    // ---------- getApplicationsForJob ----------

    @Test
    void getApplicationsForJob_returnsApplicationsWhenOwner() {
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        Application application = new Application();
        when(applicationRepository.findByJobId(1L)).thenReturn(List.of(application));

        List<Application> result = employerService.getApplicationsForJob(1L);

        assertThat(result).containsExactly(application);
    }

    @Test
    void getApplicationsForJob_rejectsNonOwner() {
        job.setEmployer(otherEmployer);
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> employerService.getApplicationsForJob(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access Denied");
    }

    @Test
    void getApplicationsForJob_throwsWhenJobMissing() {
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(jobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employerService.getApplicationsForJob(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");
    }

    // ---------- updateApplicantStatus ----------

    @Test
    void updateApplicantStatus_acceptsAllowedStatusCaseInsensitively() {
        Application application = new Application();
        application.setId(7L);
        application.setJob(job);
        application.setStatus("APPLIED");

        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = employerService.updateApplicantStatus(7L, "  shortlisted ");

        assertThat(result.getStatus()).isEqualTo("SHORTLISTED");
    }

    @Test
    void updateApplicantStatus_rejectsInvalidStatus() {
        Application application = new Application();
        application.setId(7L);
        application.setJob(job);
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> employerService.updateApplicantStatus(7L, "MAYBE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAYBE");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void updateApplicantStatus_rejectsNonOwner() {
        job.setEmployer(otherEmployer);
        Application application = new Application();
        application.setId(7L);
        application.setJob(job);
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> employerService.updateApplicantStatus(7L, "HIRED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access Denied");
    }

    // ---------- updateJob ----------

    @Test
    void updateJob_updatesOwnedJob() {
        JobRequest request = new JobRequest();
        request.setTitle("Updated title");
        request.setDescription("Updated desc");
        request.setCompanyName("Acme");
        request.setLocation("Remote");
        request.setJobType("REMOTE");
        request.setSalaryRange(2000000.0);
        request.setDeadline(LocalDate.now().plusMonths(1));

        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = employerService.updateJob(1L, request);

        assertThat(result.getTitle()).isEqualTo("Updated title");
    }

    @Test
    void updateJob_rejectsEditingAnotherEmployersJob() {
        job.setEmployer(otherEmployer);
        when(userRepository.findByEmail(employer.getEmail())).thenReturn(Optional.of(employer));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> employerService.updateJob(1L, new JobRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot edit");

        verify(jobRepository, never()).save(any());
    }

    // ---------- deleteJob ----------

    @Test
    void deleteJob_deletesWhenEmailMatchesOwner() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        employerService.deleteJob(1L, employer.getEmail());

        verify(jobRepository).delete(job);
    }

    @Test
    void deleteJob_rejectsWhenEmailDoesNotMatchOwner() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> employerService.deleteJob(1L, "not-the-owner@x.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        verify(jobRepository, never()).delete(any());
    }

    @Test
    void deleteJob_throwsWhenJobMissing() {
        when(jobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employerService.deleteJob(404L, employer.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");
    }
}
