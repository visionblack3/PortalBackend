package com.example.CarrerPortal.Service;

import com.example.CarrerPortal.Dto.JwtResponse;
import com.example.CarrerPortal.Dto.LoginRequest;
import com.example.CarrerPortal.Model.User;
import com.example.CarrerPortal.Repository.UserRepository;
import com.example.CarrerPortal.Config.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TwilioSmsService twilioSmsService;

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        // Send Welcome SMS
        if (savedUser.getPhone() != null && !savedUser.getPhone().isEmpty()) {
            String message = "Welcome to CareerPortal, " + savedUser.getName() + "! Your account has been registered successfully.";
            twilioSmsService.sendSms(savedUser.getPhone(), message);
        }

        return savedUser;
    }

    public JwtResponse loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found with email " + loginRequest.getEmail()));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Error: Invalid password!");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new JwtResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole());
    }

    public void sendPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            throw new RuntimeException("No phone number registered for this account.");
        }

        twilioSmsService.generateAndSendOtp(email, user.getPhone());
    }

    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        boolean isValid = twilioSmsService.verifyOtp(email, otp);
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}