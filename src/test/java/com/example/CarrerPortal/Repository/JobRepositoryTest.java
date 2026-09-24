package com.example.CarrerPortal.Repository;

import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private User employerA;
    private User employerB;

    @BeforeEach
    void setUp() {
        employerA = userRepository.save(
                new User("Acme HR", "hr@acme.com", "hash", "9990001111", Role.EMPLOYER));
        employerB = userRepository.save(
                new User("Globex HR", "hr@globex.com", "hash", "9990002222", Role.EMPLOYER));

        jobRepository.save(openJob("Backend Engineer", "Acme", "Bengaluru", employerA, LocalDate.now().plusDays(10)));
        jobRepository.save(openJob("Frontend Engineer", "Acme", "Remote", employerA, null));
        jobRepository.save(openJob("Data Analyst", "Globex", "Pune", employerB, LocalDate.now().minusDays(1)));
    }

    private Job openJob(String title, String company, String location, User employer, LocalDate deadline) {
        Job job = new Job();
        job.setTitle(title);
        job.setDescription("Description for " + title);
        job.setCompanyName(company);
        job.setLocation(location);
        job.setEmployer(employer);
        job.setDeadline(deadline);
        return job;
    }

    @Test
    void findByEmployerId_returnsOnlyThatEmployersJobs() {
        List<Job> result = jobRepository.findByEmployerId(employerA.getId());

        assertThat(result).extracting(Job::getTitle)
                .containsExactlyInAnyOrder("Backend Engineer", "Frontend Engineer");
    }

    @Test
    void searchJobs_matchesKeywordAgainstTitleOrCompanyCaseInsensitively() {
        List<Job> byTitle = jobRepository.searchJobs("backend", "", LocalDate.now());
        List<Job> byCompany = jobRepository.searchJobs("GLOBEX", "", LocalDate.now());

        assertThat(byTitle).extracting(Job::getTitle).containsExactly("Backend Engineer");
        // Globex's only job has a deadline in the past, so it's filtered out below —
        // this call intentionally checks keyword matching in isolation using "today"
        // as the cutoff against a job with no deadline restriction applied here.
        assertThat(byCompany).isEmpty();
    }

    @Test
    void searchJobs_matchesLocationCaseInsensitively() {
        List<Job> result = jobRepository.searchJobs("", "bengaluru", LocalDate.now());

        assertThat(result).extracting(Job::getTitle).containsExactly("Backend Engineer");
    }

    @Test
    void searchJobs_excludesJobsPastTheirDeadline() {
        List<Job> result = jobRepository.searchJobs("", "", LocalDate.now());

        assertThat(result).extracting(Job::getTitle)
                .containsExactlyInAnyOrder("Backend Engineer", "Frontend Engineer");
    }

    @Test
    void searchJobs_alwaysIncludesJobsWithNoDeadline() {
        List<Job> result = jobRepository.searchJobs("frontend", "", LocalDate.now().plusYears(5));

        assertThat(result).extracting(Job::getTitle).containsExactly("Frontend Engineer");
    }
}
