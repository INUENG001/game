package th.roadmillionaire;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class LotterySystem {
    private static final long SPECIAL_PRIZE = GameModel.STARTING_DEBT;
    private final Random random;

    LotterySystem(Random random) {
        this.random = random;
    }

    RegularLotteryOutcome drawRegular(TicketTier tier, TicketOdds odds) {
        List<TicketSymbol> symbols = new ArrayList<>(tier.symbolCount());
        int majorCount = 0;
        int consolationCount = 0;
        int skullCount = 0;

        for (int i = 0; i < tier.symbolCount(); i++) {
            double roll = random.nextDouble();
            TicketSymbol symbol;
            if (roll < odds.majorChance()) {
                symbol = TicketSymbol.MAJOR_PRIZE;
                majorCount++;
            } else if (roll < odds.majorChance() + odds.consolationChance()) {
                symbol = TicketSymbol.CONSOLATION;
                consolationCount++;
            } else if (roll < odds.majorChance() + odds.consolationChance() + odds.skullChance()) {
                symbol = TicketSymbol.SKULL;
                skullCount++;
            } else {
                symbol = TicketSymbol.EMPTY;
            }
            symbols.add(symbol);
        }

        long rawPrize = (long) majorCount * tier.price() * tier.majorMultiplier()
                + (long) consolationCount * tier.price()
                - (long) skullCount * tier.price();
        long prize = Math.max(0L, rawPrize);
        return new RegularLotteryOutcome(tier, symbols, tier.price(), prize);
    }

    double specialChance(long currentCash, double luckBonus) {
        if (currentCash <= 0L) {
            return 0.0;
        }
        double baseChance = currentCash / (double) SPECIAL_PRIZE / 2.0;
        return Math.min(0.50, baseChance + luckBonus);
    }

    SpecialLotteryOutcome drawSpecial(long currentCash, double luckBonus) {
        double chance = specialChance(currentCash, luckBonus);
        boolean won = currentCash > 0L && random.nextDouble() < chance;
        long prize = won ? SPECIAL_PRIZE : 0L;
        return new SpecialLotteryOutcome(won, currentCash, prize, chance);
    }
}
