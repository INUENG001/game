package th.roadmillionaire;

enum TicketTier {
    LEVEL_1(1, 100, 3, 0.04, 0.18, 0.30, 15),
    LEVEL_2(2, 1_000, 4, 0.03, 0.15, 0.32, 20),
    LEVEL_3(3, 5_000, 5, 0.02, 0.12, 0.34, 30),
    LEVEL_4(4, 10_000, 6, 0.015, 0.10, 0.36, 35),
    LEVEL_5(5, 30_000, 7, 0.01, 0.08, 0.38, 45);

    private final int level;
    private final long price;
    private final int symbolCount;
    private final double majorChance;
    private final double consolationChance;
    private final double skullChance;
    private final int majorMultiplier;

    TicketTier(int level, long price, int symbolCount, double majorChance,
               double consolationChance, double skullChance, int majorMultiplier) {
        this.level = level;
        this.price = price;
        this.symbolCount = symbolCount;
        this.majorChance = majorChance;
        this.consolationChance = consolationChance;
        this.skullChance = skullChance;
        this.majorMultiplier = majorMultiplier;
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

    double majorChance() {
        return majorChance;
    }

    double consolationChance() {
        return consolationChance;
    }

    double skullChance() {
        return skullChance;
    }

    int majorMultiplier() {
        return majorMultiplier;
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
