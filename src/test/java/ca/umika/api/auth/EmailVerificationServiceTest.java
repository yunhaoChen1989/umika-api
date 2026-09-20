package ca.umika.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.umika.api.email.EmailLogEntity;
import ca.umika.api.email.EmailLogRepository;
import ca.umika.api.user.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class EmailVerificationServiceTest {

    private EmailVerificationCodeRepository codeRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;
    private EmailLogRepository emailLogRepository;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        codeRepository = mock(EmailVerificationCodeRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        mailSender = mock(JavaMailSender.class);
        emailLogRepository = mock(EmailLogRepository.class);
        service = new EmailVerificationService(
                codeRepository,
                userRepository,
                passwordEncoder,
                mailSender,
                emailLogRepository,
                true,
                "info@umikasushi.ca",
                "Umika Sushi",
                "info@umikasushi.ca"
        );
    }

    @Test
    void sendsAndStoresHashedSixDigitCode() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());
        when(codeRepository.findTopByEmailOrderByCreatedAtDesc("customer@example.com")).thenReturn(Optional.empty());
        when(codeRepository.countByEmailAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(codeRepository.countByRequesterIpAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed-code");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailVerificationResponse response = service.sendCode(
                new EmailVerificationRequest(" Customer@Example.com ", "en"),
                "203.0.113.10"
        );

        ArgumentCaptor<String> rawCode = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawCode.capture());
        assertThat(rawCode.getValue()).matches("\\d{6}");
        ArgumentCaptor<EmailVerificationCodeEntity> entity = ArgumentCaptor.forClass(EmailVerificationCodeEntity.class);
        verify(codeRepository).saveAndFlush(entity.capture());
        assertThat(entity.getValue().getCodeHash()).isEqualTo("hashed-code");
        assertThat(entity.getValue().getEmail()).isEqualTo("customer@example.com");
        assertThat(entity.getValue().getRequesterIp()).isEqualTo("203.0.113.10");
        verify(mailSender).send(mimeMessage);
        verify(emailLogRepository).save(any(EmailLogEntity.class));
        assertThat(response.sent()).isTrue();
        assertThat(response.expiresInSeconds()).isEqualTo(600);
    }

    @Test
    void consumesAValidCode() {
        EmailVerificationCodeEntity entity = activeCode();
        when(codeRepository.findTopByEmailOrderByCreatedAtDesc("customer@example.com"))
                .thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        service.consumeValidCode("customer@example.com", "123456");

        assertThat(entity.getConsumedAt()).isNotNull();
        verify(codeRepository).save(entity);
    }

    @Test
    void countsAnInvalidAttemptWithoutExposingTheCode() {
        EmailVerificationCodeEntity entity = activeCode();
        when(codeRepository.findTopByEmailOrderByCreatedAtDesc("customer@example.com"))
                .thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("654321", "hashed-code")).thenReturn(false);

        assertThatThrownBy(() -> service.consumeValidCode("customer@example.com", "654321"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid or expired verification code");
        verify(codeRepository).incrementAttempts(entity.getId());
    }

    private EmailVerificationCodeEntity activeCode() {
        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail("customer@example.com");
        entity.setRequesterIp("203.0.113.10");
        entity.setCodeHash("hashed-code");
        entity.setAttempts(0);
        entity.setSentAt(LocalDateTime.now().minusSeconds(10));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return entity;
    }
}
