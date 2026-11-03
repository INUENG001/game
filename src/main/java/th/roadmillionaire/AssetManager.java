package th.roadmillionaire;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class AssetManager {
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<String, Rectangle> visibleBounds = new HashMap<>();

    BufferedImage image(String fileName) {
        return images.computeIfAbsent(fileName, this::load);
    }

    Rectangle visibleBounds(String fileName) {
        Rectangle bounds = visibleBounds.computeIfAbsent(fileName, this::findVisibleBounds);
        return new Rectangle(bounds);
    }

    private Rectangle findVisibleBounds(String fileName) {
        BufferedImage image = image(fileName);
        if (image == null) {
            return new Rectangle();
        }

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private BufferedImage load(String fileName) {
        try (InputStream resource = AssetManager.class.getResourceAsStream("/image/" + fileName)) {
            if (resource != null) {
                BufferedImage image = ImageIO.read(resource);
                if (image != null) {
                    return image;
                }
            }
        } catch (IOException ignored) {
            // Fall back to the project image folders when running from source.
        }

        Path[] candidates = {
                Path.of("image", fileName),
                Path.of("src", "main", "resources", "image", fileName)
        };
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                try {
                    BufferedImage image = ImageIO.read(path.toFile());
                    if (image != null) {
                        return image;
                    }
                } catch (IOException ignored) {
                    // Try the next supported location.
                }
            }
        }
        return null;
    }
}
