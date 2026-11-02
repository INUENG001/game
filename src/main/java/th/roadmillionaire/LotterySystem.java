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

    LotteryOutcome drawRegular(TicketTier tier) {
        List<TicketSymbol> symbols = new ArrayList<>(tier.symbolCount());
        int majorCount = 0;
        int consolationCount = 0;
        int skullCount = 0;

        for (int i = 0; i < tier.symbolCount(); i++) {
            double roll = random.nextDouble();
            TicketSymbol symbol;
            if (roll < tier.majorChance()) {
                symbol = TicketSymbol.MAJOR_PRIZE;
                majorCount++;
            } else if (roll < tier.majorChance() + tier.consolationChance()) {
                symbol = TicketSymbol.CONSOLATION;
                consolationCount++;
            } else if (roll < tier.majorChance() + tier.consolationChance() + tier.skullChance()) {
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
        return new LotteryOutcome(false, prize > 0L, tier, symbols, tier.price(), prize, prize, 0.0);
    }

    double specialChance(long currentCash) {
        if (currentCash <= 0L) {
            return 0.0;
        }
        return Math.min(0.50, currentCash / (double) SPECIAL_PRIZE / 2.0);
    }

    LotteryOutcome drawSpecial(long currentCash) {
        double chance = specialChance(currentCash);
        boolean won = currentCash > 0L && random.nextDouble() < chance;
        long prize = won ? SPECIAL_PRIZE : 0L;
        return new LotteryOutcome(true, won, null, List.of(), currentCash, prize, prize, chance);
    }
}
