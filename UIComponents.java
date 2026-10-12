import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * UIComponents — shared Swing building-blocks used by every screen.
 *
 * Provides:
 *   NavBar          — top navigation bar (inner class, needs a VirtualMuseum ref)
 *   primaryBtn()    — terracotta filled button
 *   outlineBtn()    — ghost/outline button
 *   metaRow()       — label + value row for the detail sidebar
 *   imagePlaceholder() — styled glyph panel shown when an image is missing
 */
public class UIComponents {

    // ── Shared button factories ───────────────────────────────────────────────

    public static JButton primaryBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()  ? Theme.TERRA.darker()
                          : getModel().isRollover() ? new Color(0xCC, 0x5C, 0x30)
                          : Theme.TERRA);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.setColor(Theme.CREAM);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton outlineBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? Theme.SAND : Theme.MUTED;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
                g2.setColor(c);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Metadata row (label + value) ──────────────────────────────────────────

    public static JPanel metaRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(Theme.MUTED);
        lbl.setPreferredSize(new Dimension(100, 20));

        JLabel val = new JLabel(value == null || value.isBlank() ? "—" : value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 12));
        val.setForeground(Theme.CREAM);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    // ── Styled image placeholder ──────────────────────────────────────────────

    /**
     * Styled placeholder: type-coloured background + fallback glyph.
     * Shown whenever the image file is missing, null, or unreadable.
     */
    public static JPanel imagePlaceholder(Models.Artefact art, int w, int h) {
        Color bg = switch (art.type) {
            case "Sculpture"     -> new Color(0x4A, 0x28, 0x10);
            case "Coin"          -> new Color(0x42, 0x38, 0x10);
            case "Inscription"   -> new Color(0x10, 0x2E, 0x38);
            case "Ritual Object" -> new Color(0x32, 0x14, 0x38);
            default              -> Theme.BG_CARD;
        };
        JPanel ph = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(bg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        ph.setPreferredSize(new Dimension(w, h));
        ph.setMinimumSize(new Dimension(w, h));
        ph.setMaximumSize(new Dimension(w, h));

        JLabel gl = new JLabel(art.fallbackGlyph(), SwingConstants.CENTER);
        gl.setFont(new Font("Serif", Font.PLAIN, h / 3));
        gl.setForeground(new Color(
            Theme.SAND.getRed(), Theme.SAND.getGreen(), Theme.SAND.getBlue(), 120));
        ph.add(gl);
        return ph;
    }

    // ── NavBar ────────────────────────────────────────────────────────────────

    /**
     * Top navigation bar shared by Gallery, Detail, Compare, and About screens.
     * Requires a reference to the main application frame for navigation.
     */
    public static class NavBar extends JPanel {

        public NavBar(String active, VirtualMuseum app) {
            setBackground(Theme.BG_PANEL);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER));
            setPreferredSize(new Dimension(0, 54));

            JLabel brand = new JLabel("  ◈  Indian Antiquities Museum");
            brand.setFont(new Font("Serif", Font.BOLD, 16));
            brand.setForeground(Theme.SAND);
            brand.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            brand.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { app.navigate(VirtualMuseum.CARD_HOME); }
                public void mouseEntered(MouseEvent e) { brand.setForeground(Theme.SAND_LT); }
                public void mouseExited(MouseEvent e)  { brand.setForeground(Theme.SAND); }
            });

            JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            tabs.setOpaque(false);

            for (String[] item : new String[][]{
                    {"Gallery", VirtualMuseum.CARD_GALLERY},
                    {"Compare", VirtualMuseum.CARD_COMPARE},
                    {"About",   VirtualMuseum.CARD_ABOUT}}) {
                boolean on = item[0].equals(active);
                JLabel tab = new JLabel("  " + item[0] + "  ");
                tab.setFont(new Font("SansSerif", Font.PLAIN, 13));
                tab.setForeground(on ? Theme.SAND : Theme.MUTED);
                tab.setOpaque(true);
                tab.setBackground(on ? new Color(0x3D, 0x28, 0x15) : Theme.BG_PANEL);
                tab.setBorder(BorderFactory.createEmptyBorder(16, 14, 16, 14));
                tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                final String dest = item[1];
                tab.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { app.navigate(dest); }
                    public void mouseEntered(MouseEvent e) { if (!on) tab.setForeground(Theme.CREAM); }
                    public void mouseExited(MouseEvent e)  { if (!on) tab.setForeground(Theme.MUTED); }
                });
                tabs.add(tab);
            }

            add(brand, BorderLayout.WEST);
            add(tabs,  BorderLayout.EAST);
        }
    }

    private UIComponents() {} // utility class — no instances
}
