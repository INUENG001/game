package th.roadmillionaire;

import java.util.EnumMap;

final class GameModel {
    static final long STARTING_DEBT = 10_000_000L;
    static final int MAX_DONATIONS = 10;
    private static final long[] DONATION_COSTS = {
            1_000L, 2_000L, 5_000L, 10_000L, 25_000L,
            50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L
    };

    private final EnumMap<TicketTier, Integer> ticketPurchasesByTier =
            new EnumMap<>(TicketTier.class);
    private long cash;
    private long debt;
    private int performances;
    private int ticketsBought;
    private int donations;
    private boolean cursorPurchased;

    GameModel() {
        reset();
    }

    void reset() {
        cash = 0L;
        debt = STARTING_DEBT;
        performances = 0;
        ticketsBought = 0;
        donations = 0;
        ticketPurchasesByTier.clear();
        cursorPurchased = false;
    }

    long cash() {
        return cash;
    }

    long debt() {
        return debt;
    }

    int performances() {
        return performances;
    }

    int ticketsBought() {
        return ticketsBought;
    }

    boolean cursorPurchased() {
        return cursorPurchased;
    }

    int donations() {
        return donations;
    }

    long nextDonationCost() {
        return donations >= MAX_DONATIONS ? DONATION_COSTS[DONATION_COSTS.length - 1]
                : DONATION_COSTS[donations];
    }

    double luckBonus() {
        return Math.min(0.01, donations * 0.001);
    }

    boolean recordDonation() {
        if (donations >= MAX_DONATIONS) {
            return false;
        }
        donations++;
        return true;
    }

    void unlockCursor() {
        cursorPurchased = true;
    }

    boolean canAfford(long amount) {
        return amount >= 0L && cash >= amount;
    }

    boolean spend(long amount) {
        if (!canAfford(amount)) {
            return false;
        }
        cash -= amount;
        return true;
    }

    void addCash(long amount) {
        if (amount > 0L) {
            cash = Math.addExact(cash, amount);
        }
    }

    void recordPerformance(long tips) {
        addCash(tips);
        performances++;
    }

    void recordTicketPurchase() {
        ticketsBought++;
    }

    void recordTicketPurchase(TicketTier tier) {
        recordTicketPurchase();
        ticketPurchasesByTier.merge(tier, 1, Integer::sum);
    }

    int ticketPurchases(TicketTier tier) {
        return ticketPurchasesByTier.getOrDefault(tier, 0);
    }

    void repayDebt(long amount) {
        if (amount <= 0L || cash <= 0L || debt <= 0L) {
            return;
        }
        long payment = Math.min(Math.min(amount, cash), debt);
        cash -= payment;
        debt -= payment;
    }

    void clearDebt() {
        debt = 0L;
    }
}
