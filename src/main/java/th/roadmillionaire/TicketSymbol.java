package th.roadmillionaire;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

enum TicketSymbol {
    MAJOR_PRIZE(new Color(255, 229, 154), "รางวัลใหญ่") {
        @Override
        void drawIcon(GamePanel panel, Graphics2D g, int cardX, int y,
                      int cardWidth, int iconSize) {
            int iconX = cardX + (cardWidth - iconSize) / 2;
            panel.drawImage(g, "coin.png", iconX, y + 15, iconSize, iconSize);
        }
    },
    CONSOLATION(new Color(222, 235, 247), "ปลอบใจ") {
        @Override
        void drawIcon(GamePanel panel, Graphics2D g, int cardX, int y,
                      int cardWidth, int iconSize) {
            int iconX = cardX + (cardWidth - iconSize) / 2;
            panel.drawImage(g, "coin_01.png", iconX, y + 15, iconSize, iconSize);
        }
    },
    SKULL(new Color(244, 203, 203), "หักล้าง") {
        @Override
        void drawIcon(GamePanel panel, Graphics2D g, int cardX, int y,
                      int cardWidth, int iconSize) {
            panel.drawSkull(g, cardX + (cardWidth - 36) / 2, y + 19, 36);
        }
    },
    EMPTY(new Color(229, 235, 239), "ว่าง") {
        @Override
        void drawIcon(GamePanel panel, Graphics2D g, int cardX, int y,
                      int cardWidth, int iconSize) {
            panel.drawText(g, "—", cardX + cardWidth / 2, y + 58,
                    38, Font.BOLD, new Color(112, 129, 144), false);
        }
    };

    private final Color tint;
    private final String label;

    TicketSymbol(Color tint, String label) {
        this.tint = tint;
        this.label = label;
    }

    Color tint() {
        return tint;
    }

    String label() {
        return label;
    }

    abstract void drawIcon(GamePanel panel, Graphics2D g, int cardX, int y,
                           int cardWidth, int iconSize);
}
