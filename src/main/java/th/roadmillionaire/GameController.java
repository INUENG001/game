package th.roadmillionaire;

import java.util.Random;

final class GameController {
    static final int INTRO_PAGE_COUNT = 4;
    static final long CURSOR_PRICE = 2_000_000L;

    private final GameModel model = new GameModel();
    private final Random random = new Random();
    private final LotterySystem lotterySystem = new LotterySystem(random);

    private Screen screen = Screen.TITLE;
    private int introPage;
    private LivingStatueSession statueSession;
    private LotteryOutcome latestOutcome;
    private TicketTier latestRegularTier;
    private boolean[] revealedSymbols = new boolean[0];
    private boolean regularPayoutApplied;
    private boolean performancePaid;
    private boolean storeMessageForDonation;
    private String message = "";

    GameModel model() {
        return model;
    }

    Screen screen() {
        return screen;
    }

    int introPage() {
        return introPage;
    }

    LivingStatueSession statueSession() {
        return statueSession;
    }

    LotteryOutcome latestOutcome() {
        return latestOutcome;
    }

    String message() {
        return message;
    }

    double specialChance() {
        return lotterySystem.specialChance(model.cash(), model.luckBonus());
    }

    TicketOdds ticketOdds(TicketTier tier) {
        TicketOdds odds = tier.oddsAfterPurchases(model.ticketPurchases(tier));
        double luck = model.luckBonus();
        return new TicketOdds(
                odds.majorChance() + luck,
                odds.consolationChance(),
                Math.max(0.0, odds.skullChance() - luck),
                odds.emptyChance());
    }

    int ticketPurchases(TicketTier tier) {
        return model.ticketPurchases(tier);
    }

    void startStory() {
        introPage = 0;
        screen = Screen.INTRO;
    }

    void advanceStory() {
        if (introPage < INTRO_PAGE_COUNT - 1) {
            introPage++;
        } else {
            screen = Screen.TOWN;
        }
    }

    void startNewGame() {
        resetGameState();
        startStory();
    }

    void finishGame() {
        resetGameState();
        screen = Screen.TITLE;
    }

    private void resetGameState() {
        model.reset();
        introPage = 0;
        latestOutcome = null;
        latestRegularTier = null;
        revealedSymbols = new boolean[0];
        regularPayoutApplied = false;
        statueSession = null;
        performancePaid = false;
        storeMessageForDonation = false;
        message = "";
    }

    void startPerformance(long nowMs) {
        statueSession = new LivingStatueSession(nowMs, random);
        performancePaid = false;
        screen = Screen.STATUE;
    }

    void poseNow(long nowMs) {
        if (screen == Screen.STATUE && statueSession != null && !statueSession.isComplete()) {
            statueSession.pressPose(nowMs);
        }
    }

    void tick(long nowMs) {
        if (screen == Screen.STATUE && statueSession != null) {
            statueSession.advanceTimedOutRounds(nowMs);
            if (statueSession.isComplete() && !performancePaid) {
                model.recordPerformance(statueSession.reward());
                performancePaid = true;
            }
        }
    }

    void openLottery() {
        screen = Screen.LOTTERY;
    }

    void openStore() {
        message = "";
        storeMessageForDonation = false;
        screen = Screen.STORE;
    }

    boolean purchaseCursor() {
        storeMessageForDonation = false;
        if (model.cursorPurchased()) {
            message = "ซื้อ Cursor ไปแล้ว";
            return false;
        }
        if (!model.spend(CURSOR_PRICE)) {
            message = "ยอดเงินไม่เพียงพอ";
            return false;
        }
        model.unlockCursor();
        message = "ซื้อ Cursor สำเร็จ";
        return true;
    }

    boolean donate() {
        storeMessageForDonation = true;
        if (model.donations() >= GameModel.MAX_DONATIONS) {
            message = "ทำบุญครบ 10 ครั้งแล้ว";
            return false;
        }
        long cost = model.nextDonationCost();
        if (!model.spend(cost)) {
            message = "ยอดเงินไม่เพียงพอ";
            return false;
        }
        model.recordDonation();
        message = "ทำบุญสำเร็จ รับโชค +0.1%";
        return true;
    }

    boolean payDebt() {
        if (model.debt() <= 0L || !model.canAfford(GameModel.STARTING_DEBT)) {
            return false;
        }
        model.repayDebt(GameModel.STARTING_DEBT);
        if (model.debt() == 0L) {
            screen = Screen.VICTORY;
            message = "";
            return true;
        }
        return false;
    }

    boolean storeMessageForDonation() {
        return storeMessageForDonation;
    }

    boolean buyTicket(TicketTier tier) {
        if (!model.spend(tier.price())) {
            message = "เงินไม่พอสำหรับสลากระดับ " + tier.level();
            return false;
        }
        message = "";
        TicketOdds odds = ticketOdds(tier);
        model.recordTicketPurchase(tier);
        latestRegularTier = tier;
        RegularLotteryOutcome regularOutcome = lotterySystem.drawRegular(tier, odds);
        latestOutcome = regularOutcome;
        revealedSymbols = new boolean[regularOutcome.symbols().size()];
        regularPayoutApplied = false;
        screen = Screen.LOTTERY_RESULT;
        return true;
    }

    boolean buySpecialTicket() {
        long wager = model.cash();
        if (wager <= 0L) {
            message = "ต้องมีเงินในกระเป๋าก่อนจึงจะซื้อสลากพิเศษได้";
            return false;
        }
        message = "";
        model.spend(wager);
        model.recordTicketPurchase();
        SpecialLotteryOutcome specialOutcome = lotterySystem.drawSpecial(wager, model.luckBonus());
        latestOutcome = specialOutcome;
        revealedSymbols = new boolean[0];
        regularPayoutApplied = false;
        if (specialOutcome.won()) {
            model.addCash(specialOutcome.prize());
            model.repayDebt(GameModel.STARTING_DEBT);
            model.clearDebt();
            screen = Screen.VICTORY;
        } else {
            screen = Screen.LOTTERY_RESULT;
        }
        return true;
    }

    boolean buySameTicketAgain() {
        if (latestRegularTier == null) {
            message = "ยังไม่มีสลากระดับเดิมให้ซื้อซ้ำ";
            return false;
        }
        return buyTicket(latestRegularTier);
    }

    void returnToTown() {
        screen = Screen.TOWN;
    }

    boolean symbolRevealed(int index) {
        return index >= 0 && index < revealedSymbols.length && revealedSymbols[index];
    }

    int revealedSymbolCount() {
        int count = 0;
        for (boolean revealed : revealedSymbols) {
            if (revealed) count++;
        }
        return count;
    }

    boolean allSymbolsRevealed() {
        if (!(latestOutcome instanceof RegularLotteryOutcome)) return true;
        for (boolean revealed : revealedSymbols) {
            if (!revealed) return false;
        }
        return true;
    }

    void revealSymbol(int index) {
        if (!(latestOutcome instanceof RegularLotteryOutcome)
                || index < 0 || index >= revealedSymbols.length || revealedSymbols[index]) {
            return;
        }
        revealedSymbols[index] = true;
        if (allSymbolsRevealed() && !regularPayoutApplied) {
            model.addCash(latestOutcome.prize());
            regularPayoutApplied = true;
        }
    }

    void returnToLottery() {
        screen = Screen.LOTTERY;
    }

}
