package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.ApplicationRequest;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSeekerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TwilioSmsService twilioSmsService;

    @InjectMocks
    private JobSeekerService jobSeekerService;

    private User seeker;
    private User employer;
    private Job job;

    @BeforeEach
    void setUp() {
        seeker = new User("Asha Seeker", "asha@seeker.com", "hash", "9998887777", Role.JOB_SEEKER);
        seeker.setId(3L);

        employer = new User("Acme HR", "hr@acme.com", "hash", "9998886666", Role.EMPLOYER);
        employer.setId(10L);

        job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        job.setCompanyName("Acme");
        job.setEmployer(employer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllActiveJobs_delegatesToSearchJobsWithEmptyFiltersAndTodaysDate() {
        when(jobRepository.searchJobs(eq(""), eq(""), any(LocalDate.class))).thenReturn(List.of(job));

        List<Job> result = jobSeekerService.getAllActiveJobs();

        assertThat(result).containsExactly(job);
    }

    @Test
    void searchJobs_passesKeywordAndLocationThrough() {
        when(jobRepository.searchJobs(eq("engineer"), eq("bengaluru"), any(LocalDate.class)))
                .thenReturn(List.of(job));

        List<Job> result = jobSeekerService.searchJobs("engineer", "bengaluru");

        assertThat(result).containsExactly(job);
    }

    @Test
    void applyForJob_savesApplicationAndNotifiesSeekerAndEmployer() {
        when(userRepository.findByEmail(seeker.getEmail())).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobIdAndSeekerId(1L, 3L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeUrl("https://resume.example/asha.pdf");
        request.setCoverLetter("I'd love to join.");

        Application result = jobSeekerService.applyForJob(1L, request, seeker.getEmail());

        assertThat(result.getJob()).isEqualTo(job);
        assertThat(result.getSeeker()).isEqualTo(seeker);
        assertThat(result.getStatus()).isEqualTo("APPLIED");
        assertThat(result.getResumeUrl()).isEqualTo("https://resume.example/asha.pdf");

        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
        verify(twilioSmsService, times(2)).sendSms(phoneCaptor.capture(), any());
        assertThat(phoneCaptor.getAllValues()).containsExactlyInAnyOrder(
                seeker.getPhone(), employer.getPhone());
    }

    @Test
    void applyForJob_skipsNotificationWhenPhoneNumberMissing() {
        seeker.setPhone("");
        employer.setPhone(null);
        when(userRepository.findByEmail(seeker.getEmail())).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobIdAndSeekerId(1L, 3L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        jobSeekerService.applyForJob(1L, new ApplicationRequest(), seeker.getEmail());

        verify(twilioSmsService, never()).sendSms(any(), any());
    }

    @Test
    void applyForJob_rejectsDuplicateApplication() {
        when(userRepository.findByEmail(seeker.getEmail())).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobIdAndSeekerId(1L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> jobSeekerService.applyForJob(1L, new ApplicationRequest(), seeker.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyForJob_throwsWhenJobMissing() {
        when(userRepository.findByEmail(seeker.getEmail())).thenReturn(Optional.of(seeker));
        when(jobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobSeekerService.applyForJob(404L, new ApplicationRequest(), seeker.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");
    }

    @Test
    void applyForJob_throwsWhenSeekerMissing() {
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobSeekerService.applyForJob(1L, new ApplicationRequest(), "ghost@x.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seeker not found");
    }

    @Test
    void getMyApplications_returnsApplicationsForAuthenticatedSeeker() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(seeker.getEmail(), null));
        when(userRepository.findByEmail(seeker.getEmail())).thenReturn(Optional.of(seeker));
        Application application = new Application();
        when(applicationRepository.findBySeekerId(3L)).thenReturn(List.of(application));

        List<Application> result = jobSeekerService.getMyApplications();

        assertThat(result).containsExactly(application);
    }
}
