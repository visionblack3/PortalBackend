package com.example.CarrerPortal.Controller;

import com.example.CarrerPortal.Config.JwtUtils;
import com.example.CarrerPortal.Dto.ForgotPasswordDto;
import com.example.CarrerPortal.Dto.JwtResponse;
import com.example.CarrerPortal.Dto.LoginRequest;
import com.example.CarrerPortal.Dto.ResetPasswordDto;
import com.example.CarrerPortal.Model.Role;
import com.example.CarrerPortal.Model.User;
import com.example.CarrerPortal.Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Built locally rather than @Autowired: in Spring Boot 4.1,
    // spring-boot-starter-webmvc puts Jackson on the classpath but
    // @WebMvcTest doesn't trigger JacksonAutoConfiguration to actually
    // register an ObjectMapper bean in the sliced context (a known gap —
    // see spring-projects/spring-boot#47864).
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private UserService userService;

    // JwtAuthenticationFilter is a Filter bean, and Filter beans ARE picked up
    // by @WebMvcTest's component scan (unlike plain @Component/@Service beans).
    // It @Autowireds JwtUtils, so without this mock the ApplicationContext
    // fails to load with NoSuchBeanDefinitionException.
    @MockitoBean
    private JwtUtils jwtUtils;

    // Built by hand rather than via objectMapper.writeValueAsString(User): the entity's
    // password field is @JsonProperty(access = WRITE_ONLY), so Jackson would silently
    // drop it from the *serialized* request body, and the controller would receive no
    // password at all.
    private String registerJson(String name, String email, String password, String phone, String role) {
        return """
                {"name":"%s","email":"%s","password":"%s","phone":"%s","role":"%s"}
                """.formatted(name, email, password, phone, role);
    }

    @Test
    void registerUser_returns201OnSuccess() throws Exception {
        User saved = new User("Asha Seeker", "asha@seeker.com", "plaintext", "9998887777", Role.JOB_SEEKER);
        when(userService.registerUser(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Asha Seeker", "asha@seeker.com", "plaintext", "9998887777", "JOB_SEEKER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @Test
    void registerUser_returns400WhenEmailTaken() throws Exception {
        when(userService.registerUser(any(User.class)))
                .thenThrow(new RuntimeException("Error: Email is already in use!"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Dup", "asha@seeker.com", "x", "1", "JOB_SEEKER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error: Email is already in use!"));
    }

    @Test
    void loginUser_returnsJwtResponseOnSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("asha@seeker.com");
        request.setPassword("plaintext");
        JwtResponse response = new JwtResponse("signed-jwt", 3L, "asha@seeker.com", "Asha Seeker", Role.JOB_SEEKER);
        when(userService.loginUser(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-jwt"))
                .andExpect(jsonPath("$.role").value("JOB_SEEKER"));
    }

    @Test
    void loginUser_returns401OnBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("asha@seeker.com");
        request.setPassword("wrong");
        when(userService.loginUser(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Error: Invalid password!"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Error: Invalid password!"));
    }

    @Test
    void sendOtp_returnsConfirmationOnSuccess() throws Exception {
        ForgotPasswordDto request = new ForgotPasswordDto("asha@seeker.com");

        mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void sendOtp_returns400WhenUserMissing() throws Exception {
        ForgotPasswordDto request = new ForgotPasswordDto("ghost@x.com");
        org.mockito.Mockito.doThrow(new RuntimeException("User not found with email: ghost@x.com"))
                .when(userService).sendPasswordResetOtp("ghost@x.com");

        mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void resetPassword_returnsConfirmationOnSuccess() throws Exception {
        ResetPasswordDto request = new ResetPasswordDto("asha@seeker.com", "123456", "newPass123");

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_returns400OnInvalidOtp() throws Exception {
        ResetPasswordDto request = new ResetPasswordDto("asha@seeker.com", "000000", "newPass123");
        org.mockito.Mockito.doThrow(new RuntimeException("Invalid or expired OTP."))
                .when(userService).resetPasswordWithOtp("asha@seeker.com", "000000", "newPass123");

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired OTP."));
    }
}