package ca.umika.api.reward;

import java.math.BigDecimal;

public record CurrentUserRewardRedemptionStatusDto(
        Integer pointsBalance,
        Integer redeemablePoints,
        BigDecimal redeemableAmount,
        Integer minimumRedeemPoints,
        BigDecimal pointValueCents
) {
}
