package ca.umika.api.auth;

import ca.umika.api.referral.ReferralEntity;
import ca.umika.api.referral.ReferralRepository;
import ca.umika.api.user.UserDto;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserProfileDto;
import ca.umika.api.user.UserProfileService;
import ca.umika.api.user.UserWriteRequest;
import ca.umika.api.user.UserRepository;
import ca.umika.api.user.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final String AUTH_PROVIDER_GOOGLE = "GOOGLE";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final ReferralRepository referralRepository;
    private final AccountRoleService accountRoleService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UserService userService,
            UserProfileService userProfileService,
            ReferralRepository referralRepository,
            AccountRoleService accountRoleService,
            @Value("${google.oauth.client-id}") String googleClientId
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.referralRepository = referralRepository;
        this.accountRoleService = accountRoleService;
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId))
                .build();
    }

    public LoginResponse login(LoginDto loginDto) {
        UserEntity user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!passwordEncoder.matches(loginDto.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token, accountRoleService.resolveRoleName(user.getId()));
    }

    @Transactional
    public LoginResponse register(RegisterRequest registerRequest) {
        String email = registerRequest.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String referralCode = blankToNull(registerRequest.referralCode());
        UserEntity referrer = null;
        if (referralCode != null) {
            referrer = userRepository.findByReferralCode(referralCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid referral code"));
        }

        UserWriteRequest userRequest = new UserWriteRequest(
                email,
                blankToNull(registerRequest.phone()),
                registerRequest.password(),
                null,
                false,
                null,
                null,
                referrer != null ? referrer.getId() : null,
                null,
                true,
                null,
                null,
                null
        );
        UserDto user = userService.create(userRequest);
        userProfileService.create(new UserProfileDto(
                null,
                user.id(),
                blankToNull(registerRequest.firstName()),
                blankToNull(registerRequest.lastName()),
                null,
                null,
                blankToNull(registerRequest.preferredLanguage()),
                false,
                null,
                null
        ));

        if (referrer != null) {
            createPendingReferral(referrer, user.id(), referralCode);
        }

        String token = jwtUtil.generateToken(user.email());
        return new LoginResponse(token, accountRoleService.resolveRoleName(user.id()));
    }

    @Transactional
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleCredential(request.credential());
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }

        String googleSubject = payload.getSubject();
        String email = normalizeEmail(payload.getEmail());
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account did not provide an email");
        }

        UserEntity user = userRepository.findByGoogleSubject(googleSubject)
                .orElseGet(() -> findOrCreateGoogleUser(payload, email, googleSubject, request.referralCode()));

        if (!email.equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account is linked to a different email");
        }
        if (user.getGoogleSubject() == null || user.getGoogleSubject().isBlank()) {
            user.setGoogleSubject(googleSubject);
            user.setAuthProvider(AUTH_PROVIDER_GOOGLE);
        }
        user.setEmailVerified(Boolean.TRUE);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);
        accountRoleService.ensureDefaultCustomerRole(user.getId());

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token, accountRoleService.resolveRoleName(user.getId()));
    }

    private GoogleIdToken.Payload verifyGoogleCredential(String credential) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(credential);
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential");
            }
            return idToken.getPayload();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify Google credential");
        }
    }

    private UserEntity findOrCreateGoogleUser(GoogleIdToken.Payload payload, String email, String googleSubject, String referralCodeValue) {
        UserEntity existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            if (existingUser.getGoogleSubject() != null && !existingUser.getGoogleSubject().equals(googleSubject)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already linked to another Google account");
            }
            return existingUser;
        }

        UserEntity referrer = resolveReferrer(referralCodeValue);
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setEmailVerified(Boolean.TRUE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setGoogleSubject(googleSubject);
        user.setAuthProvider(AUTH_PROVIDER_GOOGLE);
        user.setReferredBy(referrer != null ? referrer.getId() : null);
        user.setIsActive(Boolean.TRUE);
        user.setReferralCode(generateUniqueReferralCode());
        user.setLastLoginAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(user);

        userProfileService.create(new UserProfileDto(
                null,
                saved.getId(),
                stringClaim(payload, "given_name"),
                stringClaim(payload, "family_name"),
                null,
                stringClaim(payload, "picture"),
                null,
                false,
                null,
                null
        ));
        accountRoleService.ensureDefaultCustomerRole(saved.getId());

        String referralCode = blankToNull(referralCodeValue);
        if (referrer != null) {
            createPendingReferral(referrer, saved.getId(), referralCode);
        }

        return saved;
    }

    private UserEntity resolveReferrer(String referralCodeValue) {
        String referralCode = blankToNull(referralCodeValue);
        if (referralCode == null) {
            return null;
        }
        return userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid referral code"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String stringClaim(GoogleIdToken.Payload payload, String claimName) {
        Object value = payload.get(claimName);
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : null;
    }

    private String generateUniqueReferralCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = "UMIKA" + randomBase36(6);
            if (!userRepository.existsByReferralCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique referral code");
    }

    private String randomBase36(int length) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private void createPendingReferral(UserEntity referrer, java.util.UUID referredUserId, String referralCode) {
        ReferralEntity referral = new ReferralEntity();
        referral.setReferrerId(referrer.getId());
        referral.setReferredUserId(referredUserId);
        referral.setStatus("REGISTERED");
        referral.setReferralCode(referralCode);
        referralRepository.save(referral);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
