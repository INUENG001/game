package th.roadmillionaire;

enum TicketTier {
    LEVEL_1(1, 100, 3,
            0.04, 0.18, 0.30, 0.045, 0.22, 0.28, 15, 125),
    LEVEL_2(2, 1_000, 4,
            0.03, 0.15, 0.32, 0.034, 0.17, 0.30, 20, 150),
    LEVEL_3(3, 5_000, 5,
            0.02, 0.12, 0.34, 0.023, 0.14, 0.325, 30, 175),
    LEVEL_4(4, 10_000, 6,
            0.015, 0.10, 0.36, 0.017, 0.125, 0.345, 35, 200),
    LEVEL_5(5, 30_000, 7,
            0.01, 0.08, 0.38, 0.0115, 0.105, 0.365, 45, 225);

    private final int level;
    private final long price;
    private final int symbolCount;
    private final double startingMajorChance;
    private final double startingConsolationChance;
    private final double startingSkullChance;
    private final double cappedMajorChance;
    private final double cappedConsolationChance;
    private final double cappedSkullChance;
    private final int majorMultiplier;
    private final int purchasesToCap;

    TicketTier(int level, long price, int symbolCount,
               double startingMajorChance, double startingConsolationChance,
               double startingSkullChance, double cappedMajorChance,
               double cappedConsolationChance, double cappedSkullChance,
               int majorMultiplier, int purchasesToCap) {
        this.level = level;
        this.price = price;
        this.symbolCount = symbolCount;
        this.startingMajorChance = startingMajorChance;
        this.startingConsolationChance = startingConsolationChance;
        this.startingSkullChance = startingSkullChance;
        this.cappedMajorChance = cappedMajorChance;
        this.cappedConsolationChance = cappedConsolationChance;
        this.cappedSkullChance = cappedSkullChance;
        this.majorMultiplier = majorMultiplier;
        this.purchasesToCap = purchasesToCap;
    }

    int level() {
        return level;
    }

    long price() {
        return price;
    }

    int symbolCount() {
        return symbolCount;
    }

    int majorMultiplier() {
        return majorMultiplier;
    }

    int purchasesToCap() {
        return purchasesToCap;
    }

    TicketOdds oddsAfterPurchases(int previousPurchases) {
        double progress = Math.min(1.0,
                Math.max(0, previousPurchases) / (double) (purchasesToCap - 1));
        double major = interpolate(startingMajorChance, cappedMajorChance, progress);
        double consolation = interpolate(startingConsolationChance, cappedConsolationChance, progress);
        double skull = interpolate(startingSkullChance, cappedSkullChance, progress);
        double empty = Math.max(0.0, 1.0 - major - consolation - skull);
        return new TicketOdds(major, consolation, skull, empty);
    }

    private double interpolate(double start, double cap, double progress) {
        return start + (cap - start) * progress;
    }

    String coinAsset() {
        return switch (level) {
            case 1 -> "coin_01.png";
            case 2 -> "coin_02.png";
            case 3 -> "coin_03.png";
            case 4 -> "coin_04.png";
            case 5 -> "coin_05.png";
            default -> "coin_03.png";
        };
    }
}
