package th.roadmillionaire;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

final class GamePanel extends JPanel {
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;

    private static final Color INK = new Color(22, 37, 60);
    private static final Color CREAM = new Color(255, 248, 220);
    private static final Color GOLD = new Color(255, 190, 52);
    private static final Color GOLD_DARK = new Color(218, 126, 20);
    private static final Color BLUE = new Color(31, 86, 145);
    private static final Color PALE_BLUE = new Color(226, 240, 251);
    private static final Color GREEN = new Color(56, 142, 91);
    private static final Color RED = new Color(174, 57, 58);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private static final Rectangle TITLE_START = new Rectangle(860, 396, 330, 84);
    private static final Rectangle TITLE_HELP = new Rectangle(892, 500, 270, 58);
    private static final Rectangle INTRO_NEXT = new Rectangle(965, 614, 250, 66);
    private static final Rectangle TOWN_STATUE = new Rectangle(48, 596, 328, 78);
    private static final Rectangle TOWN_LOTTERY = new Rectangle(394, 596, 328, 78);
    private static final Rectangle TOWN_HELP = new Rectangle(740, 596, 238, 78);
    private static final Rectangle TOWN_RESTART = new Rectangle(996, 596, 236, 78);
    private static final Rectangle TOWN_STORE = new Rectangle(835, 170, 330, 112);
    private static final Rectangle STORE_BACK = new Rectangle(40, 30, 174, 54);
    private static final Rectangle STORE_BUY = new Rectangle(446, 548, 388, 66);
    private static final Rectangle STATUE_BACK = new Rectangle(40, 635, 182, 58);
    private static final Rectangle STATUE_POSE = new Rectangle(884, 351, 330, 62);
    private static final Rectangle STATUE_REPLAY = new Rectangle(884, 324, 330, 48);
    private static final Rectangle STATUE_DONE = new Rectangle(884, 379, 330, 52);
    private static final Rectangle LOTTERY_BACK = new Rectangle(40, 30, 174, 54);
    private static final Rectangle LOTTERY_TIER_1 = new Rectangle(48, 487, 280, 66);
    private static final Rectangle LOTTERY_TIER_2 = new Rectangle(354, 487, 280, 66);
    private static final Rectangle LOTTERY_TIER_3 = new Rectangle(660, 487, 280, 66);
    private static final Rectangle LOTTERY_TIER_4 = new Rectangle(966, 487, 272, 66);
    private static final Rectangle LOTTERY_TIER_5 = new Rectangle(308, 487, 310, 66);
    private static final Rectangle LOTTERY_SPECIAL = new Rectangle(662, 487, 310, 66);
    private static final Rectangle LOTTERY_PAGE_SWITCH = new Rectangle(1044, 103, 194, 36);
    private static final Rectangle RESULT_LOTTERY = new Rectangle(385, 600, 240, 66);
    private static final Rectangle RESULT_TOWN = new Rectangle(655, 600, 240, 66);
    private static final int RESULT_SYMBOL_CARD_WIDTH = 120;
    private static final int RESULT_SYMBOL_CARD_HEIGHT = 140;
    private static final int RESULT_SYMBOL_GAP = 14;
    private static final int RESULT_SYMBOL_Y = 126;
    private static final Rectangle VICTORY_RESTART = new Rectangle(470, 592, 340, 70);

    private final GameController controller;
    private final AssetManager assets;
    private final Timer animationTimer;
    private Point mouseCanvas = new Point(-1, -1);
    private boolean tutorialOpen;
    private boolean titleTutorialOpen;
    private int lotteryPage;

    GamePanel(GameController controller, AssetManager assets) {
        this.controller = controller;
        this.assets = assets;
        setFocusable(true);
        setBackground(Color.BLACK);
        setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
        installMouseHandlers();
        installKeyHandlers();
        animationTimer = new Timer(16, event -> {
            controller.tick(System.currentTimeMillis());
            repaint();
        });
        animationTimer.start();
    }

    private void installMouseHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                Point point = toCanvas(event.getPoint());
                if (point == null) return;
                handleClick(point.x, point.y);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                mouseCanvas = toCanvas(event.getPoint());
                if (mouseCanvas != null) revealHoveredSymbol(mouseCanvas);
                repaint();
            }
        });
    }

    private void installKeyHandlers() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "pose-now");
        getActionMap().put("pose-now", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                controller.poseNow(System.currentTimeMillis());
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "continue-screen");
        getActionMap().put("continue-screen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (controller.screen() == Screen.TITLE && !titleTutorialOpen) {
                    controller.startStory();
                } else if (controller.screen() == Screen.INTRO) {
                    controller.advanceStory();
                } else if (controller.screen() == Screen.STATUE) {
                    controller.poseNow(System.currentTimeMillis());
                }
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "back-screen");
        getActionMap().put("back-screen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (controller.screen() == Screen.LOTTERY) {
                    controller.returnToTown();
                } else if (controller.screen() == Screen.LOTTERY_RESULT) {
                    if (controller.allSymbolsRevealed()) controller.returnToLottery();
                } else if (controller.screen() == Screen.STORE) {
                    controller.returnToTown();
                } else if (controller.screen() == Screen.STATUE) {
                    controller.returnToTown();
                }
            }
        });
    }

    private Point toCanvas(Point point) {
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        int drawnWidth = (int) (WIDTH * scale);
        int drawnHeight = (int) (HEIGHT * scale);
        int offsetX = (getWidth() - drawnWidth) / 2;
        int offsetY = (getHeight() - drawnHeight) / 2;
        int x = (int) ((point.x - offsetX) / scale);
        int y = (int) ((point.y - offsetY) / scale);
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return null;
        return new Point(x, y);
    }

    private void handleClick(int x, int y) {
        switch (controller.screen()) {
            case TITLE -> {
                if (titleTutorialOpen) {
                    if (new Rectangle(510, 570, 260, 64).contains(x, y)) titleTutorialOpen = false;
                } else if (TITLE_START.contains(x, y)) {
                    controller.startStory();
                } else if (TITLE_HELP.contains(x, y)) {
                    titleTutorialOpen = true;
                }
            }
            case INTRO -> {
                if (INTRO_NEXT.contains(x, y)) controller.advanceStory();
            }
            case TOWN -> {
                if (tutorialOpen) {
                    if (new Rectangle(510, 565, 260, 64).contains(x, y)) tutorialOpen = false;
                } else if (TOWN_STATUE.contains(x, y)) {
                    controller.startPerformance(System.currentTimeMillis());
                } else if (TOWN_LOTTERY.contains(x, y)) {
                    lotteryPage = 0;
                    controller.openLottery();
                } else if (TOWN_STORE.contains(x, y)) {
                    controller.openStore();
                } else if (TOWN_HELP.contains(x, y)) {
                    tutorialOpen = true;
                } else if (TOWN_RESTART.contains(x, y)) {
                    controller.startNewGame();
                }
            }
            case STATUE -> {
                long nowMs = System.currentTimeMillis();
                controller.tick(nowMs);
                if (STATUE_BACK.contains(x, y)) {
                    controller.returnToTown();
                } else if (controller.statueSession() != null
                        && controller.statueSession().isComplete(nowMs)) {
                    if (STATUE_REPLAY.contains(x, y)) {
                        controller.startPerformance(nowMs);
                    } else if (STATUE_DONE.contains(x, y)) {
                        controller.returnToTown();
                    }
                } else if (STATUE_POSE.contains(x, y)) {
                    controller.poseNow(nowMs);
                }
            }
            case LOTTERY -> {
                if (LOTTERY_BACK.contains(x, y)) {
                    controller.returnToTown();
                } else if (LOTTERY_PAGE_SWITCH.contains(x, y)) {
                    lotteryPage = lotteryPage == 0 ? 1 : 0;
                } else if (lotteryPage == 0) {
                    if (LOTTERY_TIER_1.contains(x, y)) {
                        controller.buyTicket(TicketTier.LEVEL_1);
                    } else if (LOTTERY_TIER_2.contains(x, y)) {
                        controller.buyTicket(TicketTier.LEVEL_2);
                    } else if (LOTTERY_TIER_3.contains(x, y)) {
                        controller.buyTicket(TicketTier.LEVEL_3);
                    } else if (LOTTERY_TIER_4.contains(x, y)) {
                        controller.buyTicket(TicketTier.LEVEL_4);
                    }
                } else {
                    if (LOTTERY_TIER_5.contains(x, y)) {
                        controller.buyTicket(TicketTier.LEVEL_5);
                    } else if (LOTTERY_SPECIAL.contains(x, y)) {
                        controller.buySpecialTicket();
                    }
                }
            }
            case LOTTERY_RESULT -> {
                if (!controller.allSymbolsRevealed()) {
                    int symbolIndex = resultSymbolIndexAt(x, y,
                            controller.latestOutcome().symbols().size());
                    if (symbolIndex >= 0) controller.revealSymbol(symbolIndex);
                } else if (RESULT_LOTTERY.contains(x, y)) {
                    controller.returnToLottery();
                } else if (RESULT_TOWN.contains(x, y)) {
                    LotteryOutcome outcome = controller.latestOutcome();
                    if (outcome != null && !outcome.special()) {
                        controller.buySameTicketAgain();
                    } else {
                        controller.returnToTown();
                    }
                }
            }
            case STORE -> {
                if (STORE_BACK.contains(x, y)) {
                    controller.returnToTown();
                } else if (STORE_BUY.contains(x, y)) {
                    controller.purchaseCursor();
                }
            }
            case VICTORY -> {
                if (VICTORY_RESTART.contains(x, y)) controller.startNewGame();
            }
        }
        repaint();
    }

    private void revealHoveredSymbol(Point point) {
        if (controller.screen() != Screen.LOTTERY_RESULT
                || !controller.model().cursorPurchased()
                || controller.allSymbolsRevealed()) {
            return;
        }
        LotteryOutcome outcome = controller.latestOutcome();
        if (outcome == null || outcome.special()) return;
        int symbolIndex = resultSymbolIndexAt(point.x, point.y, outcome.symbols().size());
        if (symbolIndex >= 0) controller.revealSymbol(symbolIndex);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        int drawnWidth = (int) (WIDTH * scale);
        int drawnHeight = (int) (HEIGHT * scale);
        int offsetX = (getWidth() - drawnWidth) / 2;
        int offsetY = (getHeight() - drawnHeight) / 2;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);

        switch (controller.screen()) {
            case TITLE -> drawTitle(g);
            case INTRO -> drawIntro(g);
            case TOWN -> drawTown(g);
            case STATUE -> drawStatue(g);
            case LOTTERY -> drawLottery(g);
            case LOTTERY_RESULT -> drawLotteryResult(g);
            case STORE -> drawStore(g);
            case VICTORY -> drawVictory(g);
        }
        g.dispose();
    }

    private void drawTitle(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(31, 77, 115), 0, HEIGHT,
                new Color(247, 178, 75)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawImage(g, "bg_start.png", 0, 0, WIDTH, HEIGHT);
        fillRound(g, new Color(13, 26, 49, 222), 786, 54, 450, 612, 34);
        strokeRound(g, new Color(255, 214, 122, 210), 786, 54, 450, 612, 34, 3f);

        drawText(g, "เกมจำลองการเงิน", 1011, 105, 18, Font.PLAIN, new Color(255, 219, 138), true);
        drawText(g, "เส้นทางเศรษฐี", 1011, 176, 50, Font.BOLD, CREAM, true);
        drawText(g, "ROAD TO MILLIONAIRE", 1011, 214, 21, Font.BOLD, new Color(159, 203, 241), true);
        drawText(g, "ปลดหนี้ 10 ล้านบาท ด้วยทักษะและจังหวะ", 1011, 277, 20, Font.PLAIN, CREAM, true);
        drawText(g, "เป็นรูปปั้นมีชีวิต รับทิป แล้วเลือกเสี่ยงโชค", 1011, 311, 18, Font.PLAIN, CREAM, true);

        drawImage(g, "button_start.png", TITLE_START.x - 12, TITLE_START.y - 10,
                TITLE_START.width + 24, TITLE_START.height + 16);
        drawButton(g, TITLE_HELP, "วิธีเล่น", new Color(33, 66, 103), new Color(116, 164, 207), true);
        drawText(g, "Java Swing  •  เล่นด้วยเมาส์และปุ่ม Space", 1011, 628,
                14, Font.PLAIN, new Color(196, 211, 225), true);

        if (titleTutorialOpen) drawTutorialOverlay(g);
    }

    private void drawIntro(Graphics2D g) {
        String[] scenes = {"cutscene_01.png", "cutscene_02.png", "cutscene_03.png"};
        String[] captions = {
                "คุณตกงาน พร้อมหนี้ก้อนใหญ่ " + money(GameModel.STARTING_DEBT) + " บาท",
                "ในกระเป๋าไม่มีเงินเหลือ เส้นทางหาเงินจึงเริ่มต้นขึ้น",
                "ออกไปแสดง Living Statue รับทิป แล้ววางแผนเสี่ยงโชค"
        };
        int page = controller.introPage();
        if (page == GameController.INTRO_PAGE_COUNT - 1) {
            drawImage(g, "bg_start.png", 0, 0, WIDTH, HEIGHT);
            drawCenteredCutsceneText(g);
            drawText(g, "บทนำ  " + (page + 1) + " / " + GameController.INTRO_PAGE_COUNT,
                    48, 688, 17, Font.BOLD, CREAM, false);
            drawButton(g, INTRO_NEXT, "เข้าสู่เมือง", new Color(38, 109, 88),
                    new Color(117, 225, 166), true);
            return;
        }
        g.setPaint(new GradientPaint(0, 0, new Color(25, 44, 77), 0, HEIGHT,
                new Color(11, 20, 38)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawImage(g, scenes[page], 0, 0, WIDTH, HEIGHT);
        g.setPaint(new GradientPaint(0, 470, new Color(10, 16, 30, 0),
                0, HEIGHT, new Color(10, 16, 30, 240)));
        g.fillRect(0, 430, WIDTH, 290);
        fillRound(g, new Color(10, 24, 46, 220), 42, 510, 890, 146, 22);
        drawText(g, "บทนำ  " + (page + 1) + " / " + GameController.INTRO_PAGE_COUNT, 82, 548,
                17, Font.BOLD, GOLD, false);
        drawText(g, captions[page], 82, 598,
                24, Font.BOLD, Color.WHITE, false);
        drawButton(g, INTRO_NEXT, "ถัดไป", new Color(38, 109, 88),
                new Color(117, 225, 166), true);
    }

    private void drawCenteredCutsceneText(Graphics2D g) {
        String assetName = "cutscene_text_01.png";
        BufferedImage image = assets.image(assetName);
        if (image == null) return;
        Rectangle visible = assets.visibleBounds(assetName);
        if (visible.width <= 0 || visible.height <= 0) return;
        double scale = Math.min(1100.0 / visible.width, 550.0 / visible.height);
        int drawWidth = Math.max(1, (int) Math.round(visible.width * scale));
        int drawHeight = Math.max(1, (int) Math.round(visible.height * scale));
        int drawX = (WIDTH - drawWidth) / 2;
        int drawY = (HEIGHT - drawHeight) / 2;
        g.drawImage(image, drawX, drawY, drawX + drawWidth, drawY + drawHeight,
                visible.x, visible.y, visible.x + visible.width, visible.y + visible.height, null);
    }

    private void drawTown(Graphics2D g) {
        drawImage(g, "bg_01.png", 0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(10, 24, 40, 78));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(13, 27, 48, 222));
        g.fillRect(0, 0, WIDTH, 108);
        drawText(g, "เส้นทางเศรษฐี", 54, 62, 29, Font.BOLD, CREAM, false);
        drawText(g, "แสดงให้ดี เก็บเงินให้พอ แล้วค่อยเสี่ยง", 56, 89,
                15, Font.PLAIN, new Color(191, 213, 230), false);
        drawText(g, "หนี้คงเหลือ  ฿" + money(controller.model().debt()), 470, 66,
                18, Font.BOLD, CREAM, false);

        drawCashTab(g);

        fillRound(g, new Color(14, 31, 50, 224), 364, 139, 864, 395, 28);
        strokeRound(g, new Color(239, 198, 122, 205), 364, 139, 864, 395, 28, 2f);
        drawText(g, "แผนปลดหนี้", 411, 190, 31, Font.BOLD, CREAM, false);
        drawText(g, "รับทิปจากการแสดง Living Statue", 411, 230,
                22, Font.BOLD, new Color(255, 211, 129), false);
        drawText(g, "ซื้อสลากระดับ 1 ถึง 5 เพื่อสะสมเงินรางวัล", 411, 270,
                19, Font.PLAIN, CREAM, false);
        drawText(g, "สลากพิเศษใช้เงินทั้งหมดที่มี แลกกับโอกาสปลดหนี้ก้อนเดียว", 411, 306,
                17, Font.PLAIN, new Color(215, 225, 235), false);
        drawImage(g, "store.png", TOWN_STORE.x, TOWN_STORE.y,
                TOWN_STORE.width, TOWN_STORE.height);

        int barX = 412, barY = 350, barWidth = 760;
        fillRound(g, new Color(55, 70, 91), barX, barY, barWidth, 20, 10);
        double progress = Math.min(1.0, controller.model().cash() / (double) GameModel.STARTING_DEBT);
        fillRound(g, new Color(255, 188, 54), barX, barY,
                Math.max(12, (int) (barWidth * progress)), 20, 10);
        drawText(g, "เงินในกระเป๋า " + money(controller.model().cash()), barX, barY + 49,
                17, Font.BOLD, Color.WHITE, false);
        drawText(g, "เป้าหมาย " + money(GameModel.STARTING_DEBT) + " บาท",
                barX + barWidth, barY + 49,
                17, Font.BOLD, Color.WHITE, false, true);

        drawText(g, "การแสดง", 412, 456, 16, Font.BOLD, new Color(193, 211, 229), false);
        drawText(g, Integer.toString(controller.model().performances()), 412, 490,
                28, Font.BOLD, GOLD, false);
        drawText(g, "สลากที่ซื้อ", 650, 456, 16, Font.BOLD, new Color(193, 211, 229), false);
        drawText(g, Integer.toString(controller.model().ticketsBought()), 650, 490,
                28, Font.BOLD, GOLD, false);
        drawText(g, "แตะปุ่มค้างท่าให้ตรงจังหวะ เพื่อรับทิปเพิ่ม", 848, 477,
                17, Font.PLAIN, new Color(220, 231, 241), false);

        drawImage(g, "Character.png", 67, 201, 267, 366);
        g.setColor(new Color(18, 34, 56, 233));
        g.fillRect(0, 570, WIDTH, 150);
        drawButton(g, TOWN_STATUE, "แสดง Living Statue", new Color(151, 91, 24), GOLD, true);
        drawButton(g, TOWN_LOTTERY, "เลือกซื้อสลาก", new Color(44, 92, 138),
                new Color(130, 193, 237), true);
        drawButton(g, TOWN_HELP, "วิธีเล่น", new Color(49, 74, 100),
                new Color(155, 188, 212), true);
        drawButton(g, TOWN_RESTART, "เริ่มเกมใหม่", new Color(93, 55, 66),
                new Color(202, 126, 135), true);
        if (tutorialOpen) drawTutorialOverlay(g);
    }

    private void drawStatue(Graphics2D g) {
        drawImage(g, "bg_01.png", 0, 0, WIDTH, HEIGHT);
        LivingStatueSession session = controller.statueSession();
        if (session == null) return;
        boolean finished = session.isComplete(System.currentTimeMillis());
        long nowMs = System.currentTimeMillis();

        g.setColor(new Color(17, 27, 40, 105));
        g.fillOval(565, 449, 150, 25);
        drawPoseOnStand(g, session.poseIndex());

        drawText(g, money(controller.model().cash()) + " บาท", 1080, 62,
                25, Font.BOLD, INK, true);

        fillRound(g, new Color(13, 29, 48, 220), 28, 116, 372, 242, 22);
        strokeRound(g, new Color(247, 197, 99, 220), 28, 116, 372, 242, 22, 2f);
        drawText(g, "LIVING STATUE", 54, 155, 22, Font.BOLD, GOLD, false);
        drawText(g, "ยืนนิ่งบนแท่น รับทิปจากผู้ชม", 54, 188,
                17, Font.BOLD, CREAM, false);
        drawText(g, "รอบ " + session.roundNumber(),
                54, 231, 18, Font.BOLD, Color.WHITE, false);
        drawText(g, "ทิปสะสม  ฿" + money(session.reward()), 54, 267,
                18, Font.BOLD, new Color(255, 215, 119), false);
        drawText(g, session.latestFeedback(), 54, 305,
                15, Font.PLAIN, new Color(220, 232, 244), false);
        drawText(g, String.format(Locale.ROOT, "ความเร็ว %.1f×", session.speedMultiplier()),
                54, 346, 14, Font.BOLD, GOLD, false);
        drawSpeedMeter(g, session.speedProgress(), 173, 331, 207, 14);

        fillRound(g, new Color(13, 29, 48, 222), 852, 150, 394, 294, 22);
        strokeRound(g, new Color(247, 197, 99, 220), 852, 150, 394, 294, 22, 2f);
        drawText(g, finished ? "พลาดแล้ว! จบการแสดง" : "รอจังหวะถ่ายรูป", 1049, 191,
                21, Font.BOLD, CREAM, true);
        drawText(g, finished ? "ทิปที่ได้รับทั้งหมด" : "กดถูกต่อเนื่องเพื่อสะสมทิป",
                1049, 224, 15, Font.PLAIN, new Color(211, 225, 238), true);

        if (!finished) {
            drawTimingBar(g, session, nowMs, 895, 267, 308, 22);
            drawButton(g, STATUE_POSE, "ค้างท่า!  (Space)", new Color(154, 94, 20), GOLD, true);
            drawText(g, "กดถูกแล้วแถบจะเร็วขึ้น • พลาดครั้งเดียวจบ", 1049, 433,
                    14, Font.PLAIN, new Color(216, 229, 240), true);
        } else {
            drawText(g, "฿" + money(session.finalReward()), 1049, 278,
                    34, Font.BOLD, new Color(255, 215, 119), true);
            drawText(g, "ค้างท่า " + session.hitCount() + " ครั้ง", 1049, 310,
                    17, Font.PLAIN, CREAM, true);
            drawButton(g, STATUE_REPLAY, "เล่นอีกครั้ง", new Color(42, 97, 144),
                    new Color(122, 183, 229), true);
            drawButton(g, STATUE_DONE, "รับทิปและกลับเมือง", new Color(38, 109, 88),
                    new Color(117, 225, 166), true);
        }

        drawButton(g, STATUE_BACK, "กลับเมือง", new Color(48, 75, 104),
                new Color(142, 185, 218), true);
    }

    private void drawPoseOnStand(Graphics2D g, int poseIndex) {
        String assetName = String.format(Locale.ROOT, "Character_LS_%02d.png", poseIndex);
        BufferedImage image = assets.image(assetName);
        if (image == null) return;

        Rectangle visible = assets.visibleBounds(assetName);
        if (visible.width <= 0 || visible.height <= 0) return;
        double scale = Math.min(250.0 / visible.width, 300.0 / visible.height);
        int drawWidth = Math.max(1, (int) Math.round(visible.width * scale));
        int drawHeight = Math.max(1, (int) Math.round(visible.height * scale));
        int drawX = 640 - drawWidth / 2;
        int drawBottom = 470;
        g.drawImage(image, drawX, drawBottom - drawHeight, drawX + drawWidth, drawBottom,
                visible.x, visible.y, visible.x + visible.width, visible.y + visible.height, null);
    }

    private void drawTimingBar(Graphics2D g, LivingStatueSession session, long nowMs,
                               int x, int y, int width, int height) {
        fillRound(g, new Color(190, 205, 220), x, y, width, height, height / 2);
        int targetX = x + (int) (session.target() * width);
        g.setColor(new Color(250, 204, 75));
        g.fillRoundRect(targetX - 20, y - 7, 40, height + 14, 12, 12);
        int markerX = x + (int) (session.marker(nowMs) * width);
        g.setColor(INK);
        g.fillOval(markerX - 9, y - 8, 18, height + 16);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(markerX - 9, y - 8, 18, height + 16);
    }

    private void drawSpeedMeter(Graphics2D g, double progress, int x, int y,
                                int width, int height) {
        fillRound(g, new Color(61, 78, 96), x, y, width, height, height / 2);
        int fillWidth = (int) Math.round(width * Math.max(0.0, Math.min(1.0, progress)));
        if (fillWidth > 0) {
            fillRound(g, new Color(255, 190, 52), x, y, fillWidth, height, height / 2);
        }
    }

    private void drawLottery(Graphics2D g) {
        drawDarkBackdrop(g);
        drawButton(g, LOTTERY_BACK, "กลับเมือง", new Color(40, 71, 105),
                new Color(125, 172, 215), true);
        drawText(g, "เลือกระดับสลาก", 640, 61, 32, Font.BOLD, CREAM, true);
        drawCashTab(g);
        drawText(g, lotteryPage == 0
                        ? "เลือกระดับ 1–4  •  หน้าถัดไปมีระดับ 5 และสลากพิเศษ"
                        : "เลือกระดับ 5 หรือสลากพิเศษ",
                640, 99,
                16, Font.PLAIN, new Color(181, 202, 222), true);
        drawButton(g, LOTTERY_PAGE_SWITCH, lotteryPage == 0 ? "หน้าถัดไป  ›" : "‹  หน้าก่อน",
                new Color(40, 71, 105), new Color(125, 172, 215), true);

        if (lotteryPage == 0) {
            drawTicketCard(g, TicketTier.LEVEL_1,
                    new Rectangle(42, 141, 286, 412), LOTTERY_TIER_1);
            drawTicketCard(g, TicketTier.LEVEL_2,
                    new Rectangle(348, 141, 286, 412), LOTTERY_TIER_2);
            drawTicketCard(g, TicketTier.LEVEL_3,
                    new Rectangle(654, 141, 286, 412), LOTTERY_TIER_3);
            drawTicketCard(g, TicketTier.LEVEL_4,
                    new Rectangle(960, 141, 278, 412), LOTTERY_TIER_4);
        } else {
            drawTicketCard(g, TicketTier.LEVEL_5,
                    new Rectangle(298, 141, 330, 412), LOTTERY_TIER_5);
            drawSpecialCard(g, new Rectangle(652, 141, 330, 412));
        }

        fillRound(g, new Color(24, 42, 65), 42, 574, 1196, 89, 18);
        drawText(g, "ตัวคูณรางวัลใหญ่ระดับ 1–5: 15× / 20× / 30× / 35× / 45× ของราคา  •  ปลอบใจ +1×  •  กะโหลก −1×",
                640, 610, 17, Font.BOLD, new Color(224, 233, 243), true);
        drawText(g, "สลากพิเศษใช้เงินทั้งหมดที่มี และมีโอกาสชนะสูงสุด 50% ตามเงินที่ถืออยู่",
                640, 641, 16, Font.PLAIN, new Color(185, 203, 221), true);
        if (!controller.message().isBlank()) {
            drawText(g, controller.message(), 640, 699, 15, Font.BOLD,
                    new Color(255, 163, 146), true);
        }
    }

    private void drawTicketCard(Graphics2D g, TicketTier tier, Rectangle card, Rectangle button) {
        boolean affordable = controller.model().canAfford(tier.price());
        Color border = affordable ? new Color(240, 196, 93) : new Color(99, 118, 139);
        fillRound(g, new Color(242, 247, 250), card.x, card.y, card.width, card.height, 22);
        strokeRound(g, border, card.x, card.y, card.width, card.height, 22, 3f);
        drawText(g, "ระดับ " + tier.level(), card.x + card.width / 2, card.y + 42,
                24, Font.BOLD, INK, true);
        drawImage(g, tier.coinAsset(), card.x + 22, card.y + 61, 82, 82);
        drawText(g, "฿" + money(tier.price()), card.x + card.width - 18, card.y + 83,
                28, Font.BOLD, new Color(153, 92, 25), false, true);
        drawText(g, tier.symbolCount() + " ช่องสัญลักษณ์", card.x + card.width - 18, card.y + 113,
                15, Font.PLAIN, new Color(68, 86, 105), false, true);

        int cellWidth = tier.symbolCount() >= 7 ? 34 : tier.symbolCount() >= 5 ? 39 : 45;
        int gap = tier.symbolCount() >= 6 ? 5 : 9;
        int total = tier.symbolCount() * cellWidth + (tier.symbolCount() - 1) * gap;
        int startX = card.x + (card.width - total) / 2;
        int cellY = card.y + 177;
        for (int i = 0; i < tier.symbolCount(); i++) {
            int cellX = startX + i * (cellWidth + gap);
            fillRound(g, new Color(218, 229, 239), cellX, cellY, cellWidth, 52, 8);
            strokeRound(g, new Color(129, 154, 178), cellX, cellY, cellWidth, 52, 8, 1.5f);
            drawText(g, "?", cellX + cellWidth / 2, cellY + 35,
                    27, Font.BOLD, BLUE, true);
        }
        drawText(g, "ใหญ่ ฿" + money(tier.price() * tier.majorMultiplier()),
                card.x + 24, card.y + 267, 17, Font.BOLD, new Color(144, 83, 18), false);
        drawText(g, "ปลอบใจ ฿" + money(tier.price()) + "  •  กะโหลก −฿" + money(tier.price()),
                card.x + 24, card.y + 298, 14, Font.PLAIN, new Color(70, 87, 103), false);
        drawText(g, "โอกาสต่อช่อง  " + percent(tier.majorChance()) + " / "
                        + percent(tier.consolationChance()) + " / " + percent(tier.skullChance()),
                card.x + 24, card.y + 332, 14, Font.PLAIN, new Color(70, 87, 103), false);
        drawButton(g, button, affordable ? "ซื้อและเปิดสลาก" : "เงินไม่พอ",
                affordable ? new Color(40, 101, 152) : new Color(119, 132, 146),
                affordable ? new Color(127, 183, 226) : new Color(164, 174, 184), affordable);
    }

    private void drawSpecialCard(Graphics2D g, Rectangle card) {
        fillRound(g, new Color(57, 35, 70), card.x, card.y, card.width, card.height, 22);
        strokeRound(g, new Color(237, 172, 255), card.x, card.y, card.width, card.height, 22, 3f);
        drawText(g, "สลากพิเศษ", card.x + card.width / 2, card.y + 43,
                24, Font.BOLD, new Color(255, 232, 255), true);
        drawImage(g, "coin_06.png", card.x + (card.width - 94) / 2, card.y + 69, 94, 94);
        drawText(g, "ชิงเงินรางวัล", card.x + card.width / 2, card.y + 194,
                20, Font.BOLD, new Color(255, 219, 136), true);
        drawText(g, "฿" + money(GameModel.STARTING_DEBT),
                card.x + card.width / 2, card.y + 232,
                25, Font.BOLD, Color.WHITE, true);
        drawText(g, "เดิมพันทั้งหมด  ฿" + money(controller.model().cash()),
                card.x + card.width / 2, card.y + 282, 16, Font.BOLD,
                new Color(230, 215, 239), true);
        drawText(g, "โอกาสชนะ  " + percent(controller.specialChance()),
                card.x + card.width / 2, card.y + 322, 18, Font.BOLD,
                new Color(247, 196, 110), true);
        boolean canBuy = controller.model().cash() > 0L;
        drawButton(g, LOTTERY_SPECIAL, canBuy ? "ทุ่มเงินทั้งหมด" : "ต้องมีเงินก่อน",
                canBuy ? new Color(135, 62, 137) : new Color(105, 95, 111),
                canBuy ? new Color(211, 125, 221) : new Color(150, 141, 158), canBuy);
    }

    private void drawLotteryResult(Graphics2D g) {
        drawDarkBackdrop(g);
        LotteryOutcome outcome = controller.latestOutcome();
        if (outcome == null) {
            drawCashTab(g);
            drawButton(g, RESULT_TOWN, "กลับเมือง", BLUE, new Color(115, 175, 225), true);
            return;
        }
        fillRound(g, new Color(237, 244, 249), 214, 60, 852, 603, 30);
        strokeRound(g, new Color(243, 199, 105), 214, 60, 852, 603, 30, 3f);
        drawCashTab(g);
        if (outcome.special()) {
            drawImage(g, "coin_06.png", 600, 139, 80, 80);
            drawText(g, "สลากพิเศษ", 640, 278, 28, Font.BOLD, new Color(103, 49, 111), true);
            if (outcome.won()) {
                drawText(g, "คุณถูกรางวัลพิเศษ!", 640, 324, 27, Font.BOLD, GREEN, true);
            } else {
                drawText(g, "ครั้งนี้ยังไม่ถูกรางวัล", 640, 324,
                        27, Font.BOLD, RED, true);
            }
            drawText(g, "จ่าย ฿" + money(outcome.wager()), 520, 380,
                    21, Font.BOLD, RED, true);
            drawText(g, "ได้รับ ฿" + money(outcome.prize()), 760, 380,
                    21, Font.BOLD, GREEN, true);
            drawText(g, "โอกาสชนะ " + percent(outcome.chance()), 640, 425,
                    18, Font.BOLD, new Color(132, 79, 145), true);
            if (!outcome.won()) {
                drawText(g, "กลับไปแสดง Living Statue เพื่อเก็บเงินใหม่ได้", 640, 468,
                        17, Font.PLAIN, new Color(69, 87, 105), true);
            }
        } else {
            drawSymbolCards(g, outcome.symbols(), 640, RESULT_SYMBOL_Y);
            if (controller.allSymbolsRevealed()) {
                drawText(g, "ผลสลากระดับ " + outcome.tier().level(), 640, 322,
                        27, Font.BOLD, INK, true);
                drawText(g, outcome.prize() > 0 ? "มีรางวัล!" : "ยังไม่ได้รางวัลในรอบนี้",
                        640, 364, 23, Font.BOLD, outcome.prize() > 0 ? GREEN : RED, true);
                drawText(g, "จ่าย ฿" + money(outcome.wager()), 520, 410,
                        20, Font.BOLD, RED, true);
                drawText(g, "ได้รับ ฿" + money(outcome.prize()), 760, 410,
                        20, Font.BOLD, GREEN, true);
                drawText(g, "สัญลักษณ์กะโหลกหักล้างมูลค่ารางวัลในใบเดียวกัน",
                        640, 455, 15, Font.PLAIN, new Color(78, 95, 112), true);
            } else {
                String revealStatus = (controller.model().cursorPurchased()
                        ? "ชี้เมาส์เพื่อเปิด  " : "เปิดแล้ว ")
                        + controller.revealedSymbolCount() + " / " + outcome.symbols().size();
                drawText(g, revealStatus,
                        640, 365, 16, Font.PLAIN, new Color(78, 95, 112), true);
            }
        }
        boolean resultReady = controller.allSymbolsRevealed();
        drawButton(g, RESULT_LOTTERY, "เลือกสลากแบบอื่น", new Color(42, 97, 144),
                new Color(122, 183, 229), resultReady);
        String repeatLabel = outcome.special() ? "กลับเมือง" : "ซื้ออีกครั้ง";
        drawButton(g, RESULT_TOWN, repeatLabel, new Color(44, 106, 78),
                new Color(116, 211, 154), resultReady);
        if (!controller.message().isBlank()) {
            drawText(g, controller.message(), 640, 510, 16, Font.BOLD,
                    new Color(174, 57, 58), true);
        }
    }

    private void drawSymbolCards(Graphics2D g, List<TicketSymbol> symbols, int centerX, int y) {
        int cardWidth = resultSymbolCardWidth(symbols.size());
        int gap = resultSymbolGap(symbols.size());
        int totalWidth = symbols.size() * cardWidth + (symbols.size() - 1) * gap;
        int startX = centerX - totalWidth / 2;
        for (int i = 0; i < symbols.size(); i++) {
            int x = startX + i * (cardWidth + gap);
            if (!controller.symbolRevealed(i)) {
                drawCoveredSymbolCard(g, x, y, cardWidth,
                        controller.model().cursorPurchased());
                continue;
            }
            TicketSymbol symbol = symbols.get(i);
            Color tint = switch (symbol) {
                case MAJOR_PRIZE -> new Color(255, 229, 154);
                case CONSOLATION -> new Color(222, 235, 247);
                case SKULL -> new Color(244, 203, 203);
                case EMPTY -> new Color(229, 235, 239);
            };
            fillRound(g, tint, x, y, cardWidth, RESULT_SYMBOL_CARD_HEIGHT, 16);
            strokeRound(g, Color.WHITE, x, y, cardWidth,
                    RESULT_SYMBOL_CARD_HEIGHT, 16, 2f);
            int iconSize = Math.min(54, cardWidth - 24);
            int iconX = x + (cardWidth - iconSize) / 2;
            switch (symbol) {
                case MAJOR_PRIZE -> drawImage(g, "coin.png", iconX, y + 15, iconSize, iconSize);
                case CONSOLATION -> drawImage(g, "coin_01.png", iconX, y + 15, iconSize, iconSize);
                case SKULL -> drawSkull(g, x + (cardWidth - 36) / 2, y + 19, 36);
                case EMPTY -> drawText(g, "—", x + cardWidth / 2, y + 58,
                        38, Font.BOLD, new Color(112, 129, 144), false);
            }
            String label = switch (symbol) {
                case MAJOR_PRIZE -> "รางวัลใหญ่";
                case CONSOLATION -> "ปลอบใจ";
                case SKULL -> "หักล้าง";
                case EMPTY -> "ว่าง";
            };
            drawText(g, label, x + cardWidth / 2, y + 111,
                    16, Font.BOLD, INK, true);
        }
    }

    private void drawCoveredSymbolCard(Graphics2D g, int x, int y, int cardWidth,
                                       boolean cursorPurchased) {
        fillRound(g, new Color(70, 82, 94), x, y, cardWidth,
                RESULT_SYMBOL_CARD_HEIGHT, 16);
        strokeRound(g, new Color(205, 215, 223), x, y, cardWidth,
                RESULT_SYMBOL_CARD_HEIGHT, 16, 2f);
        fillRound(g, new Color(85, 98, 110), x + 9, y + 9,
                cardWidth - 18, RESULT_SYMBOL_CARD_HEIGHT - 18, 12);
        g.setColor(new Color(120, 132, 143));
        g.setStroke(new BasicStroke(2f));
        for (int offset = 0; offset < 4; offset++) {
            int lineY = y + 37 + offset * 16;
            g.drawLine(x + 18, lineY, x + cardWidth - 18, lineY - 8);
        }
        drawText(g, cursorPurchased ? "ชี้เพื่อเปิด" : "คลิกเพื่อขูด", x + cardWidth / 2,
                y + 113, 14, Font.BOLD, new Color(238, 241, 243), true);
    }

    private int resultSymbolIndexAt(int x, int y, int symbolCount) {
        int cardWidth = resultSymbolCardWidth(symbolCount);
        int gap = resultSymbolGap(symbolCount);
        int totalWidth = symbolCount * cardWidth + (symbolCount - 1) * gap;
        int startX = (WIDTH - totalWidth) / 2;
        for (int i = 0; i < symbolCount; i++) {
            int cardX = startX + i * (cardWidth + gap);
            if (new Rectangle(cardX, RESULT_SYMBOL_Y, cardWidth,
                    RESULT_SYMBOL_CARD_HEIGHT).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private int resultSymbolCardWidth(int symbolCount) {
        if (symbolCount >= 7) return 106;
        if (symbolCount >= 6) return 112;
        return RESULT_SYMBOL_CARD_WIDTH;
    }

    private int resultSymbolGap(int symbolCount) {
        if (symbolCount >= 7) return 8;
        if (symbolCount >= 6) return 10;
        return RESULT_SYMBOL_GAP;
    }

    private void drawStore(Graphics2D g) {
        drawDarkBackdrop(g);
        drawButton(g, STORE_BACK, "กลับเมือง", new Color(40, 71, 105),
                new Color(125, 172, 215), true);
        drawText(g, "ร้านค้า", 640, 62, 32, Font.BOLD, CREAM, true);
        drawCashTab(g);

        fillRound(g, new Color(237, 244, 249), 320, 124, 640, 528, 28);
        strokeRound(g, new Color(243, 199, 105), 320, 124, 640, 528, 28, 3f);
        drawImage(g, "Cursor.png", 540, 153, 200, 200);
        drawText(g, "Cursor อัตโนมัติ", 640, 390, 28, Font.BOLD, INK, true);
        drawText(g, "ชี้เมาส์บนช่องสัญลักษณ์เพื่อเปิดสลากอัตโนมัติ", 640, 429,
                18, Font.PLAIN, new Color(68, 86, 105), true);
        drawText(g, "ราคา  ฿" + money(GameController.CURSOR_PRICE), 640, 475,
                25, Font.BOLD, new Color(153, 92, 25), true);

        boolean purchased = controller.model().cursorPurchased();
        drawButton(g, STORE_BUY, purchased ? "ซื้อแล้ว" : "ซื้อ Cursor",
                purchased ? new Color(104, 116, 128) : new Color(40, 101, 152),
                purchased ? new Color(151, 162, 170) : new Color(127, 183, 226), !purchased);
        if (!controller.message().isBlank()) {
            Color messageColor = controller.message().equals("ยอดเงินไม่เพียงพอ")
                    ? RED : GREEN;
            drawText(g, controller.message(), 640, 625, 17, Font.BOLD, messageColor, true);
        }
    }

    private void drawSkull(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(119, 48, 61));
        g.fillOval(x, y, size, size);
        g.setColor(new Color(255, 236, 226));
        g.fillOval(x + 8, y + 7, size - 16, size - 16);
        g.setColor(new Color(119, 48, 61));
        g.fillOval(x + 12, y + 15, 5, 6);
        g.fillOval(x + 20, y + 15, 5, 6);
        int[] xs = {x + 17, x + 13, x + 21};
        int[] ys = {y + 23, y + 29, y + 29};
        g.fillPolygon(xs, ys, 3);
        g.fillRoundRect(x + 12, y + 28, 4, 7, 2, 2);
        g.fillRoundRect(x + 20, y + 28, 4, 7, 2, 2);
    }

    private void drawVictory(Graphics2D g) {
        drawImage(g, "bg_01.png", 0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(10, 21, 39, 205));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        fillRound(g, new Color(22, 44, 68, 235), 170, 56, 940, 608, 34);
        strokeRound(g, new Color(255, 209, 113), 170, 56, 940, 608, 34, 3f);
        drawImage(g, "Character_LS_01.png", 513, 112, 254, 320);
        drawText(g, "ปลดหนี้สำเร็จ!", 640, 478, 42, Font.BOLD, GOLD, false);
        drawText(g, "คุณชนะสลากพิเศษและชำระหนี้ "
                        + money(GameModel.STARTING_DEBT) + " บาทครบแล้ว", 640, 525,
                21, Font.BOLD, Color.WHITE, false);
        drawText(g, "จากการแสดง Living Statue สู่วันที่เริ่มต้นใหม่", 640, 561,
                18, Font.PLAIN, new Color(208, 223, 237), false);
        drawButton(g, VICTORY_RESTART, "เริ่มการเดินทางอีกครั้ง", new Color(39, 112, 79),
                new Color(120, 222, 160), true);
    }

    private void drawTutorialOverlay(Graphics2D g) {
        g.setColor(new Color(7, 14, 25, 192));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        fillRound(g, new Color(246, 249, 250), 306, 115, 668, 504, 28);
        strokeRound(g, new Color(240, 192, 83), 306, 115, 668, 504, 28, 3f);
        drawText(g, "วิธีเล่น", 640, 166, 32, Font.BOLD, INK, false);
        String[] lines = {
                "1. แสดง Living Statue เพื่อรับทิปจากผู้ชม",
                "2. กด SPACE หรือปุ่มค้างท่า ให้ตรงกับแถบสีทอง",
                "3. ซื้อ Cursor ที่ร้านค้า แล้วชี้เมาส์เพื่อเปิดสลากอัตโนมัติ",
                "4. ใช้เงินซื้อสลากระดับ 1 ถึง 5 แล้วลุ้นรางวัล",
                "5. สลากพิเศษใช้เงินทั้งหมด โอกาสชนะสูงสุด 50%",
                "6. ถ้าชนะสลากพิเศษ จะปลดหนี้ "
                        + money(GameModel.STARTING_DEBT) + " บาททันที"
        };
        for (int i = 0; i < lines.length; i++) {
            drawText(g, lines[i], 370, 229 + i * 52, 18, Font.PLAIN, INK, false);
        }
        drawButton(g, new Rectangle(510, 545, 260, 64), "เข้าใจแล้ว",
                new Color(44, 102, 148), new Color(124, 183, 227), true);
    }

    private void drawDarkBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(26, 51, 82), 0, HEIGHT,
                new Color(9, 18, 34)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(121, 165, 199, 22));
        for (int i = 0; i < 10; i++) {
            g.fillOval(70 + i * 145, 120 + (i % 3) * 170, 190, 190);
        }
    }

    private void drawCashTab(Graphics2D g) {
        drawImage(g, "tab_coin.png", 780, 12, 455, 88);
        drawText(g, money(controller.model().cash()) + " บาท", 1030, 61,
                29, Font.BOLD, INK, true);
    }

    private void drawButton(Graphics2D g, Rectangle bounds, String label,
                            Color fill, Color border, boolean enabled) {
        boolean hovered = enabled && bounds.contains(mouseCanvas);
        Color actualFill = enabled ? fill : new Color(104, 116, 128);
        Color actualBorder = enabled ? border : new Color(151, 162, 170);
        if (hovered) {
            actualFill = actualFill.brighter();
        }
        fillRound(g, new Color(0, 0, 0, 90), bounds.x + 2, bounds.y + 5,
                bounds.width, bounds.height, 18);
        fillRound(g, actualFill, bounds.x, bounds.y, bounds.width, bounds.height, 18);
        strokeRound(g, actualBorder, bounds.x, bounds.y, bounds.width, bounds.height, 18,
                hovered ? 3f : 2f);
        drawText(g, label, bounds.x + bounds.width / 2,
                bounds.y + bounds.height / 2 + 7, 20, Font.BOLD, Color.WHITE, true);
    }

    private void drawImage(Graphics2D g, String name, int x, int y, int width, int height) {
        BufferedImage image = assets.image(name);
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            fillRound(g, new Color(83, 96, 110), x, y, width, height, 12);
            drawText(g, "ภาพไม่พบ", x + width / 2, y + height / 2,
                    14, Font.PLAIN, Color.WHITE, false);
        }
    }

    private void fillRound(Graphics2D g, Color color, int x, int y, int width, int height, int radius) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Float(x, y, width, height, radius, radius));
    }

    private void strokeRound(Graphics2D g, Color color, int x, int y, int width, int height,
                             int radius, float lineWidth) {
        g.setColor(color);
        g.setStroke(new BasicStroke(lineWidth));
        g.draw(new RoundRectangle2D.Float(x, y, width, height, radius, radius));
    }

    private void drawText(Graphics2D g, String text, int x, int baseline,
                          int size, int style, Color color, boolean centered) {
        drawText(g, text, x, baseline, size, style, color, centered, false);
    }

    private void drawText(Graphics2D g, String text, int x, int baseline,
                          int size, int style, Color color, boolean centered, boolean rightAligned) {
        Font font = new Font("Tahoma", style, size);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        int drawX = centered ? x - metrics.stringWidth(text) / 2
                : rightAligned ? x - metrics.stringWidth(text) : x;
        g.setColor(color);
        g.drawString(text, drawX, baseline);
    }

    private String money(long value) {
        return MONEY.format(value);
    }

    private String percent(double value) {
        double percentage = value * 100.0;
        if (percentage > 0.0 && percentage < 0.01) {
            return "<0.01%";
        }
        return String.format(Locale.US, "%.2f%%", percentage);
    }
}
