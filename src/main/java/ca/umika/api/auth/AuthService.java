package ca.umika.api.auth;

import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.referral.ReferralEntity;
import ca.umika.api.referral.ReferralRepository;
import ca.umika.api.reward.RewardTransactionEntity;
import ca.umika.api.reward.RewardTransactionRepository;
import ca.umika.api.reward.RewardWalletEntity;
import ca.umika.api.reward.RewardWalletRepository;
import ca.umika.api.user.UserDto;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserProfileDto;
import ca.umika.api.user.UserProfileService;
import ca.umika.api.user.UserWriteRequest;
import ca.umika.api.user.UserRepository;
import ca.umika.api.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final ReferralRepository referralRepository;
    private final RewardWalletRepository rewardWalletRepository;
    private final RewardTransactionRepository rewardTransactionRepository;
    private final SystemSettingRepository systemSettingRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UserService userService,
            UserProfileService userProfileService,
            ReferralRepository referralRepository,
            RewardWalletRepository rewardWalletRepository,
            RewardTransactionRepository rewardTransactionRepository,
            SystemSettingRepository systemSettingRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.referralRepository = referralRepository;
        this.rewardWalletRepository = rewardWalletRepository;
        this.rewardTransactionRepository = rewardTransactionRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    public LoginResponse login(LoginDto loginDto) {
        UserEntity user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(loginDto.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token);
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
            awardReferralSignupBonus(referrer, user.id(), referralCode);
        }

        String token = jwtUtil.generateToken(user.email());
        return new LoginResponse(token);
    }

    private void awardReferralSignupBonus(UserEntity referrer, java.util.UUID referredUserId, String referralCode) {
        int points = resolveReferralSignupPoints();

        RewardWalletEntity wallet = rewardWalletRepository.findByUserId(referrer.getId())
                .orElseGet(() -> {
                    RewardWalletEntity created = new RewardWalletEntity();
                    created.setUserId(referrer.getId());
                    created.setTotalEarned(0);
                    created.setTotalRedeemed(0);
                    created.setAvailableBalance(0);
                    return created;
                });

        wallet.setTotalEarned(valueOrZero(wallet.getTotalEarned()) + points);
        wallet.setAvailableBalance(valueOrZero(wallet.getAvailableBalance()) + points);
        rewardWalletRepository.save(wallet);

        RewardTransactionEntity transaction = new RewardTransactionEntity();
        transaction.setUserId(referrer.getId());
        transaction.setType("REFERRAL_SIGNUP");
        transaction.setPoints(points);
        transaction.setSource("REFERRAL");
        transaction.setDescription("Referral signup bonus for referred user " + referredUserId);
        rewardTransactionRepository.save(transaction);

        ReferralEntity referral = new ReferralEntity();
        referral.setReferrerId(referrer.getId());
        referral.setReferredUserId(referredUserId);
        referral.setStatus("REGISTERED");
        referral.setReferralCode(referralCode);
        referralRepository.save(referral);
    }

    private int resolveReferralSignupPoints() {
        return systemSettingRepository.findBySettingKey("REFERRAL_SIGNUP_POINTS")
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (Exception ignored) {
                        return 50;
                    }
                })
                .orElse(50);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
