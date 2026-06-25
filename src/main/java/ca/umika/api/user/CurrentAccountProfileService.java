package ca.umika.api.user;

import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.reward.RewardWalletRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CurrentAccountProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RewardWalletRepository rewardWalletRepository;
    private final AccountRoleService accountRoleService;

    public CurrentAccountProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            RewardWalletRepository rewardWalletRepository,
            AccountRoleService accountRoleService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.rewardWalletRepository = rewardWalletRepository;
        this.accountRoleService = accountRoleService;
    }

    @Transactional(readOnly = true)
    public CurrentAccountProfileDto getByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email));

        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        Integer loyaltyPoints = rewardWalletRepository.findByUserId(user.getId())
                .map(wallet -> Optional.ofNullable(wallet.getAvailableBalance()).orElse(0))
                .orElse(0);

        return new CurrentAccountProfileDto(
                user.getId(),
                user.getEmail(),
                accountRoleService.resolveRoleName(user.getId()),
                user.getPhone(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getBirthday() : null,
                profile != null ? profile.getPreferredLanguage() : null,
                loyaltyPoints,
                user.getReferralCode()
        );
    }

    public CurrentAccountProfileDto updateByEmail(String email, CurrentAccountProfileUpdateRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email));

        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        userRepository.save(user);

        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfileEntity created = new UserProfileEntity();
                    created.setUserId(user.getId());
                    return created;
                });

        if (request.firstName() != null) {
            profile.setFirstName(blankToNull(request.firstName()));
        }
        if (request.lastName() != null) {
            profile.setLastName(blankToNull(request.lastName()));
        }
        if (request.birthday() != null) {
            profile.setBirthday(request.birthday());
        }
        if (request.preferredLanguage() != null) {
            profile.setPreferredLanguage(blankToNull(request.preferredLanguage()));
        }

        userProfileRepository.save(profile);
        return getByEmail(email);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
