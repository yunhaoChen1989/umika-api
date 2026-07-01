package ca.umika.api.reward;

import java.math.BigDecimal;

public record CurrentUserRewardsDto(
        Integer pointsBalance,
        Integer lifetimePointsEarned,
        Integer lifetimePointsRedeemed,
        BigDecimal pointsValueCents,
        BigDecimal pointsPerDollar,
        BigDecimal maxRedemptionPercent,
        Integer nextRewardPoints,
        BigDecimal nextRewardAmount,
        Integer birthdayBonusPoints,
        Integer referralRegisterPoints,
        Integer referralFirstOrderPoints,
        BigDecimal referralFirstOrderMinimum,
        BigDecimal minReferralOrderAmount,
        String referralCode,
        String referralInviteUrl
) {
}
