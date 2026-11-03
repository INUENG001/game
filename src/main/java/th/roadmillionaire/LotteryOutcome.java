package th.roadmillionaire;

abstract class LotteryOutcome {
    private final boolean won;
    private final long wager;
    private final long prize;

    LotteryOutcome(boolean won, long wager, long prize) {
        this.won = won;
        this.wager = wager;
        this.prize = prize;
    }

    public final boolean won() {
        return won;
    }

    public final long wager() {
        return wager;
    }

    public final long prize() {
        return prize;
    }

    public abstract boolean special();
}
