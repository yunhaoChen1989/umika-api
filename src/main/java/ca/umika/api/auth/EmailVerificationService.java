package ca.umika.api.auth;

import ca.umika.api.email.EmailLogEntity;
import ca.umika.api.email.EmailLogRepository;
import ca.umika.api.user.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {

    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_SENDS_PER_HOUR = 5;
    private static final int MAX_SENDS_PER_IP_PER_HOUR = 20;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean enabled;
    private final String fromAddress;
    private final String fromName;
    private final String replyTo;

    public EmailVerificationService(
            EmailVerificationCodeRepository codeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            EmailLogRepository emailLogRepository,
            @Value("${app.email.enabled:false}") boolean enabled,
            @Value("${app.email.from-address:info@umikasushi.ca}") String fromAddress,
            @Value("${app.email.from-name:Umika Sushi}") String fromName,
            @Value("${app.email.reply-to:info@umikasushi.ca}") String replyTo
    ) {
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.replyTo = replyTo;
    }

    @Transactional
    public EmailVerificationResponse sendCode(EmailVerificationRequest request, String requesterIp) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email verification is unavailable");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCodeEntity latest = codeRepository.findTopByEmailOrderByCreatedAtDesc(email).orElse(null);
        if (latest != null && latest.getSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            long seconds = Duration.between(now, latest.getSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS)).toSeconds();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + Math.max(1, seconds) + " seconds before requesting another code");
        }
        if (codeRepository.countByEmailAndCreatedAtAfter(email, now.minusHours(1)) >= MAX_SENDS_PER_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification codes requested. Please try again later");
        }
        if (codeRepository.countByRequesterIpAndCreatedAtAfter(requesterIp, now.minusHours(1))
                >= MAX_SENDS_PER_IP_PER_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification codes requested. Please try again later");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setEmail(email);
        entity.setRequesterIp(requesterIp);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setAttempts(0);
        entity.setExpiresAt(now.plusMinutes(CODE_EXPIRY_MINUTES));
        entity.setSentAt(now);
        codeRepository.saveAndFlush(entity);

        sendVerificationEmail(email, code, normalizeLanguage(request.preferredLanguage()));
        saveEmailLog(email, subject(normalizeLanguage(request.preferredLanguage())), now);
        return new EmailVerificationResponse(true, CODE_EXPIRY_MINUTES * 60, RESEND_COOLDOWN_SECONDS);
    }

    @Transactional
    public void consumeValidCode(String emailValue, String code) {
        String email = normalizeEmail(emailValue);
        EmailVerificationCodeEntity entity = codeRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(this::invalidCode);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getConsumedAt() != null
                || entity.getExpiresAt().isBefore(now)
                || entity.getAttempts() >= MAX_ATTEMPTS) {
            throw invalidCode();
        }
        if (code == null || !code.matches("\\d{6}") || !passwordEncoder.matches(code, entity.getCodeHash())) {
            codeRepository.incrementAttempts(entity.getId());
            throw invalidCode();
        }
        entity.setConsumedAt(now);
        codeRepository.save(entity);
    }

    private void sendVerificationEmail(String email, String code, String language) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setReplyTo(replyTo);
            helper.setTo(email);
            helper.setSubject(subject(language));
            helper.setText(plainText(code, language), html(code, language));
            mailSender.send(message);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send verification email. Please try again", exception);
        }
    }

    private void saveEmailLog(String email, String emailSubject, LocalDateTime sentAt) {
        EmailLogEntity emailLog = new EmailLogEntity();
        emailLog.setRecipientEmail(email);
        emailLog.setSubject(emailSubject);
        emailLog.setStatus("SENT");
        emailLog.setProvider("BREVO_SMTP");
        emailLog.setSentAt(sentAt);
        emailLogRepository.save(emailLog);
    }

    private String subject(String language) {
        return switch (language) {
            case "zh" -> "Umika Sushi 邮箱验证码";
            case "ko" -> "Umika Sushi 이메일 인증 코드";
            default -> "Your Umika Sushi verification code";
        };
    }

    private String plainText(String code, String language) {
        return switch (language) {
            case "zh" -> "你的 Umika Sushi 验证码是：" + code + "\n\n验证码将在 10 分钟后失效。请勿将验证码告诉他人。";
            case "ko" -> "Umika Sushi 인증 코드는 " + code + "입니다.\n\n이 코드는 10분 후 만료됩니다. 다른 사람과 공유하지 마세요.";
            default -> "Your Umika Sushi verification code is: " + code
                    + "\n\nThis code expires in 10 minutes. Do not share it with anyone.";
        };
    }

    private String html(String code, String language) {
        String heading;
        String instruction;
        String expiry;
        if ("zh".equals(language)) {
            heading = "验证你的邮箱";
            instruction = "请输入以下验证码来创建 Umika Sushi 账户：";
            expiry = "验证码将在 10 分钟后失效。请勿将验证码告诉他人。";
        } else if ("ko".equals(language)) {
            heading = "이메일을 인증해 주세요";
            instruction = "Umika Sushi 계정을 만들려면 아래 인증 코드를 입력하세요:";
            expiry = "이 코드는 10분 후 만료됩니다. 다른 사람과 공유하지 마세요.";
        } else {
            heading = "Verify your email";
            instruction = "Enter this verification code to create your Umika Sushi account:";
            expiry = "This code expires in 10 minutes. Do not share it with anyone.";
        }
        return """
                <!doctype html><html><body style="margin:0;background:#f6f3ed;font-family:Arial,sans-serif;color:#173b2d">
                <div style="max-width:560px;margin:0 auto;padding:28px 16px">
                  <div style="background:#fff;border-radius:14px;padding:28px;border:1px solid #e5ded2">
                    <div style="font-size:14px;font-weight:700;color:#b42318;letter-spacing:.08em">UMIKA SUSHI</div>
                    <h1 style="font-size:26px;margin:14px 0">%s</h1>
                    <p style="font-size:16px;line-height:1.6">%s</p>
                    <div style="margin:24px 0;padding:18px;text-align:center;background:#f3f8f5;border-radius:10px;font-size:32px;font-weight:700;letter-spacing:.22em">%s</div>
                    <p style="font-size:14px;line-height:1.6;color:#5f6f67">%s</p>
                  </div>
                </div></body></html>
                """.formatted(heading, instruction, code, expiry);
    }

    private String normalizeLanguage(String value) {
        if (value == null) return "en";
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("zh")) return "zh";
        if (normalized.startsWith("ko")) return "ko";
        return "en";
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
    }
}
