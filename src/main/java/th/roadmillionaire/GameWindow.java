package th.roadmillionaire;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

final class GameWindow {
    private final JFrame frame;
    private final GamePanel gamePanel;

    GameWindow() {
        frame = new JFrame("เส้นทางเศรษฐี | Road to Millionaire");
        gamePanel = new GamePanel(new GameController(), new AssetManager());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(gamePanel);
        frame.setMinimumSize(new Dimension(960, 600));
        frame.setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        frame.setResizable(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    void show() {
        frame.setVisible(true);
        SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
    }
}
