package th.roadmillionaire;

import java.util.EnumMap;

final class GameModel {
    static final long STARTING_DEBT = 10_000_000L;

    private final EnumMap<TicketTier, Integer> ticketPurchasesByTier =
            new EnumMap<>(TicketTier.class);
    private long cash;
    private long debt;
    private int performances;
    private int ticketsBought;
    private boolean cursorPurchased;

    GameModel() {
        reset();
    }

    void reset() {
        cash = 0L;
        debt = STARTING_DEBT;
        performances = 0;
        ticketsBought = 0;
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
