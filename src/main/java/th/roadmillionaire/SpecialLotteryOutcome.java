package th.roadmillionaire;

final class SpecialLotteryOutcome extends LotteryOutcome {
    private final double chance;

    SpecialLotteryOutcome(boolean won, long wager, long prize, double chance) {
        super(won, wager, prize);
        this.chance = chance;
    }

    public double chance() {
        return chance;
    }

    @Override
    public boolean special() {
        return true;
    }
}