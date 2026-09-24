package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Config.JwtUtils;
import com.example.CarrerPortal.Dto.JwtResponse;
import com.example.CarrerPortal.Dto.LoginRequest;
import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import com.example.CarrerPortal.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private TwilioSmsService twilioSmsService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Asha Seeker", "asha@seeker.com", "encoded-hash", "9998887777", Role.JOB_SEEKER);
        user.setId(3L);
    }

    // ---------- registerUser ----------

    @Test
    void registerUser_encodesPasswordSavesAndSendsWelcomeSms() {
        User incoming = new User("Asha Seeker", "asha@seeker.com", "plaintext", "9998887777", Role.JOB_SEEKER);
        when(userRepository.existsByEmail("asha@seeker.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = userService.registerUser(incoming);

        assertThat(saved.getId()).isEqualTo(3L);
        verify(twilioSmsService).sendSms(eq("9998887777"), any());
    }

    @Test
    void registerUser_skipsSmsWhenNoPhoneOnFile() {
        User incoming = new User("No Phone", "nophone@x.com", "plaintext", "", Role.JOB_SEEKER);
        User savedNoPhone = new User("No Phone", "nophone@x.com", "encoded", "", Role.JOB_SEEKER);
        when(userRepository.existsByEmail("nophone@x.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedNoPhone);

        userService.registerUser(incoming);

        verify(twilioSmsService, never()).sendSms(any(), any());
    }

    @Test
    void registerUser_rejectsDuplicateEmail() {
        User incoming = new User("Dup", "asha@seeker.com", "x", "1", Role.JOB_SEEKER);
        when(userRepository.existsByEmail("asha@seeker.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(incoming))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already in use");

        verify(userRepository, never()).save(any());
    }

    // ---------- loginUser ----------

    @Test
    void loginUser_returnsJwtResponseOnValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("asha@seeker.com");
        request.setPassword("plaintext");

        when(userRepository.findByEmail("asha@seeker.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plaintext", "encoded-hash")).thenReturn(true);
        when(jwtUtils.generateToken(3L, "asha@seeker.com", Role.JOB_SEEKER)).thenReturn("signed-jwt");

        JwtResponse response = userService.loginUser(request);

        assertThat(response.getToken()).isEqualTo("signed-jwt");
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getEmail()).isEqualTo("asha@seeker.com");
        assertThat(response.getRole()).isEqualTo(Role.JOB_SEEKER);
    }

    @Test
    void loginUser_throwsWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@x.com");
        request.setPassword("whatever");
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void loginUser_throwsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("asha@seeker.com");
        request.setPassword("wrong");
        when(userRepository.findByEmail("asha@seeker.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.loginUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid password");

        verify(jwtUtils, never()).generateToken(any(), any(), any());
    }

    // ---------- sendPasswordResetOtp ----------

    @Test
    void sendPasswordResetOtp_generatesAndSendsOtpToRegisteredPhone() {
        when(userRepository.findByEmail("asha@seeker.com")).thenReturn(Optional.of(user));

        userService.sendPasswordResetOtp("asha@seeker.com");

        verify(twilioSmsService).generateAndSendOtp("asha@seeker.com", "9998887777");
    }

    @Test
    void sendPasswordResetOtp_throwsWhenUserMissing() {
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.sendPasswordResetOtp("ghost@x.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void sendPasswordResetOtp_throwsWhenNoPhoneOnFile() {
        user.setPhone("");
        when(userRepository.findByEmail("asha@seeker.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.sendPasswordResetOtp("asha@seeker.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No phone number");

        verify(twilioSmsService, never()).generateAndSendOtp(any(), any());
    }

    // ---------- resetPasswordWithOtp ----------

    @Test
    void resetPasswordWithOtp_updatesEncodedPasswordOnValidOtp() {
        when(twilioSmsService.verifyOtp("asha@seeker.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("asha@seeker.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("new-encoded-hash");

        userService.resetPasswordWithOtp("asha@seeker.com", "123456", "newPass123");

        assertThat(user.getPassword()).isEqualTo("new-encoded-hash");
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordWithOtp_throwsOnInvalidOtp() {
        when(twilioSmsService.verifyOtp("asha@seeker.com", "000000")).thenReturn(false);

        assertThatThrownBy(() -> userService.resetPasswordWithOtp("asha@seeker.com", "000000", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
    }
}
