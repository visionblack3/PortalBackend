package com.example.CarrerPortal.Repository;

import com.example.CarrerPortal.Model.Application;
import com.example.CarrerPortal.Model.Job;
import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private User seekerA;
    private User seekerB;
    private Job job;

    @BeforeEach
    void setUp() {
        User employer = userRepository.save(
                new User("Acme HR", "hr@acme.com", "hash", "9990001111", Role.EMPLOYER));
        seekerA = userRepository.save(
                new User("Asha Seeker", "asha@seeker.com", "hash", "9998887777", Role.JOB_SEEKER));
        seekerB = userRepository.save(
                new User("Ravi Seeker", "ravi@seeker.com", "hash", "9998886666", Role.JOB_SEEKER));

        job = new Job();
        job.setTitle("Backend Engineer");
        job.setDescription("desc");
        job.setCompanyName("Acme");
        job.setLocation("Remote");
        job.setEmployer(employer);
        job = jobRepository.save(job);

        Application application = new Application();
        application.setJob(job);
        application.setSeeker(seekerA);
        application.setStatus("APPLIED");
        applicationRepository.save(application);
    }

    @Test
    void findByJobId_returnsApplicationsForThatJob() {
        List<Application> result = applicationRepository.findByJobId(job.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeeker().getEmail()).isEqualTo("asha@seeker.com");
    }

    @Test
    void findBySeekerId_returnsApplicationsForThatSeeker() {
        List<Application> forSeekerA = applicationRepository.findBySeekerId(seekerA.getId());
        List<Application> forSeekerB = applicationRepository.findBySeekerId(seekerB.getId());

        assertThat(forSeekerA).hasSize(1);
        assertThat(forSeekerB).isEmpty();
    }

    @Test
    void existsByJobIdAndSeekerId_isTrueOnlyForTheMatchingPair() {
        assertThat(applicationRepository.existsByJobIdAndSeekerId(job.getId(), seekerA.getId())).isTrue();
        assertThat(applicationRepository.existsByJobIdAndSeekerId(job.getId(), seekerB.getId())).isFalse();
    }

    @Test
    void appliedAtAndDefaultStatus_arePopulatedOnPersist() {
        Application application = new Application();
        application.setJob(job);
        application.setSeeker(seekerB);
        // status intentionally left unset to exercise the entity default + @PrePersist guard

        Application saved = applicationRepository.save(application);

        assertThat(saved.getAppliedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("APPLIED");
    }
}
