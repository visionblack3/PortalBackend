package com.example.CarrerPortal.Repository;

import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_returnsUserWhenPresent() {
        userRepository.save(new User("Asha Seeker", "asha@seeker.com", "hash", "9998887777", Role.JOB_SEEKER));

        Optional<User> result = userRepository.findByEmail("asha@seeker.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Asha Seeker");
        assertThat(result.get().getRole()).isEqualTo(Role.JOB_SEEKER);
    }

    @Test
    void findByEmail_returnsEmptyWhenAbsent() {
        Optional<User> result = userRepository.findByEmail("nobody@x.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_reflectsWhetherEmailIsTaken() {
        userRepository.save(new User("Asha Seeker", "asha@seeker.com", "hash", "9998887777", Role.JOB_SEEKER));

        assertThat(userRepository.existsByEmail("asha@seeker.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@x.com")).isFalse();
    }

    @Test
    void createdAt_isPopulatedOnPersist() {
        User saved = userRepository.save(
                new User("Asha Seeker", "asha@seeker.com", "hash", "9998887777", Role.JOB_SEEKER));

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
