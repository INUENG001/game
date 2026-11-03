package th.roadmillionaire;

import java.util.List;

record LotteryOutcome(
        boolean special,
        boolean won,
        TicketTier tier,
        List<TicketSymbol> symbols,
        long wager,
        long prize,
        double chance
) {
    LotteryOutcome {
        symbols = List.copyOf(symbols);
    }
}
