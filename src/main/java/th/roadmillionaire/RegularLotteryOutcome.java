package th.roadmillionaire;

import java.util.List;

final class RegularLotteryOutcome extends LotteryOutcome {
    private final TicketTier tier;
    private final List<TicketSymbol> symbols;

    RegularLotteryOutcome(TicketTier tier, List<TicketSymbol> symbols,
                          long wager, long prize) {
        super(prize > 0L, wager, prize);
        this.tier = tier;
        this.symbols = List.copyOf(symbols);
    }

    public TicketTier tier() {
        return tier;
    }

    public List<TicketSymbol> symbols() {
        return symbols;
    }

    @Override
    public boolean special() {
        return false;
    }
}