package com.example.CarrerPortal.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TwilioSmsService reaches out to the real Twilio SDK from sendSms(), and
 * Twilio.init() only runs via @PostConstruct inside a Spring context. These
 * tests instantiate the service directly (no Spring context) and stub out
 * sendSms() on a spy, so only the OTP generation/storage/verification logic
 * — the part with real business rules — is under test.
 */
class TwilioSmsServiceTest {

    private TwilioSmsService service;

    @BeforeEach
    void setUp() {
        service = spy(new TwilioSmsService());
        doNothing().when(service).sendSms(any(), any());
    }

    @Test
    void generateAndSendOtp_returnsSixDigitCodeAndSendsIt() {
        String otp = service.generateAndSendOtp("asha@seeker.com", "9998887777");

        assertThat(otp).matches("\\d{6}");
        verify(service).sendSms(eq("9998887777"), org.mockito.ArgumentMatchers.contains(otp));
    }

    @Test
    void verifyOtp_succeedsOnceThenConsumesTheCode() {
        String otp = service.generateAndSendOtp("asha@seeker.com", "9998887777");

        boolean firstAttempt = service.verifyOtp("asha@seeker.com", otp);
        boolean secondAttempt = service.verifyOtp("asha@seeker.com", otp);

        assertThat(firstAttempt).isTrue();
        assertThat(secondAttempt).isFalse();
    }

    @Test
    void verifyOtp_failsOnWrongCode() {
        service.generateAndSendOtp("asha@seeker.com", "9998887777");

        boolean result = service.verifyOtp("asha@seeker.com", "000000");

        assertThat(result).isFalse();
    }

    @Test
    void verifyOtp_failsWhenNoOtpWasEverRequested() {
        boolean result = service.verifyOtp("never-requested@x.com", "123456");

        assertThat(result).isFalse();
    }
}
