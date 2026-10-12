import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * ImageLoader — loads, scales, and in-memory-caches artefact images.
 *
 * Two entry points:
 *   {@link #loadThumbnail(String, int, int)} — crop-to-fill, for gallery cards.
 *   {@link #loadFull(String, int, int)}      — fit-within,  for the detail sidebar.
 *
 * Returns {@code null} when the path is null/blank or the file cannot be read.
 * Callers must handle null and display a styled placeholder instead.
 */
public class ImageLoader {

    private static final Map<String, ImageIcon> THUMB_CACHE = new HashMap<>();
    private static final Map<String, ImageIcon> FULL_CACHE  = new HashMap<>();
    private static final Logger LOG = Logger.getLogger("ImageLoader");

    /** Crop-to-fill thumbnail. Cached by path + dimensions. */
    public static ImageIcon loadThumbnail(String path, int tw, int th) {
        if (path == null || path.isBlank()) return null;
        String key = path + "@" + tw + "x" + th;
        if (THUMB_CACHE.containsKey(key)) return THUMB_CACHE.get(key);

        BufferedImage src = read(path);
        if (src == null) { THUMB_CACHE.put(key, null); return null; }

        // Scale so image covers the target rectangle (larger dimension wins)
        double scale = Math.max((double) tw / src.getWidth(),
                                (double) th / src.getHeight());
        int sw = (int)(src.getWidth()  * scale);
        int sh = (int)(src.getHeight() * scale);
        Image scaled = src.getScaledInstance(sw, sh, Image.SCALE_SMOOTH);

        // Centre-crop to exact size
        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(scaled, -(sw - tw) / 2, -(sh - th) / 2, null);
        g2.dispose();

        ImageIcon icon = new ImageIcon(out);
        THUMB_CACHE.put(key, icon);
        return icon;
    }

    /** Fit-within image — preserves aspect ratio. */
    public static ImageIcon loadFull(String path, int maxW, int maxH) {
        if (path == null || path.isBlank()) return null;
        String key = path + "@full" + maxW + "x" + maxH;
        if (FULL_CACHE.containsKey(key)) return FULL_CACHE.get(key);

        BufferedImage src = read(path);
        if (src == null) { FULL_CACHE.put(key, null); return null; }

        double scale = Math.min((double) maxW / src.getWidth(),
                                (double) maxH / src.getHeight());
        int sw = (int)(src.getWidth()  * scale);
        int sh = (int)(src.getHeight() * scale);
        Image scaled = src.getScaledInstance(sw, sh, Image.SCALE_SMOOTH);

        ImageIcon icon = new ImageIcon(scaled);
        FULL_CACHE.put(key, icon);
        return icon;
    }

    private static BufferedImage read(String path) {
        try {
            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                LOG.warning("Image not found: " + path);
                return null;
            }
            return ImageIO.read(f);
        } catch (IOException e) {
            LOG.warning("Cannot load image: " + path + " — " + e.getMessage());
            return null;
        }
    }

    private ImageLoader() {} // utility class — no instances
}
