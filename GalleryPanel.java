import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GalleryPanel — artefact grid with type-filter pills and live text search.
 *
 * Each artefact is rendered as a card showing a thumbnail (or placeholder),
 * a colour stripe indicating type, the artefact name, period, and dynasty.
 * Clicking a card navigates to the DetailPanel for that artefact.
 */
public class GalleryPanel extends JPanel {

    public static final int THUMB_W = 210;
    public static final int THUMB_H = 145;

    private JPanel             grid;
    private String             activeFilter = "All";
    private String             searchQuery  = "";
    private final List<JButton> filterBtns  = new ArrayList<>();
    private JTextField         searchField;

    private final VirtualMuseum app;

    public GalleryPanel(VirtualMuseum app) {
        this.app = app;
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        add(new UIComponents.NavBar("Gallery", app), BorderLayout.NORTH);

        // ── Control bar ───────────────────────────────────────────────────────
        JPanel ctrlBar = new JPanel(new BorderLayout());
        ctrlBar.setBackground(Theme.BG_DARK);
        ctrlBar.setBorder(BorderFactory.createEmptyBorder(10, 16, 6, 16));

        // Filter pills (left)
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        JLabel fl = new JLabel("Filter:");
        fl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fl.setForeground(Theme.MUTED);
        filterRow.add(fl);

        for (String type : VirtualMuseum.TYPES) {
            JButton fb = new JButton(type) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean active = getText().equals(activeFilter);
                    g2.setColor(active ? Theme.TERRA : new Color(0x3A, 0x28, 0x18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    if (!active) {
                        g2.setColor(Theme.DIVIDER);
                        g2.setStroke(new BasicStroke(0.8f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                    }
                    g2.setColor(active ? Theme.CREAM : Theme.MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            fb.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int w = type.equals("Ritual Object") ? 115
                  : type.equals("Inscription")   ? 100
                  : type.equals("Sculpture")      ? 90
                  : type.equals("Coin")            ? 60 : 48;
            fb.setPreferredSize(new Dimension(w, 30));
            fb.setBorderPainted(false);
            fb.setFocusPainted(false);
            fb.setContentAreaFilled(false);
            fb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            fb.addActionListener(e -> {
                activeFilter = type;
                rebuildGrid();
                filterBtns.forEach(Component::repaint);
            });
            filterBtns.add(fb);
            filterRow.add(fb);
        }

        // Search box (right)
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchRow.setOpaque(false);
        searchField = new JTextField(18) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Theme.DIVIDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setForeground(Theme.MUTED);
        searchField.setCaretColor(Theme.SAND);
        searchField.setBackground(new Color(0, 0, 0, 0));
        searchField.setOpaque(false);
        searchField.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        searchField.setText("Search artefacts…");
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search artefacts…")) {
                    searchField.setText("");
                    searchField.setForeground(Theme.CREAM);
                }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setText("Search artefacts…");
                    searchField.setForeground(Theme.MUTED);
                }
            }
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { onSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { onSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
        });
        searchRow.add(searchField);

        ctrlBar.add(filterRow, BorderLayout.WEST);
        ctrlBar.add(searchRow, BorderLayout.EAST);

        // ── Grid ──────────────────────────────────────────────────────────────
        grid = new JPanel(new GridLayout(0, 4, 14, 14));
        grid.setBackground(Theme.BG_DARK);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 16, 16, 16));

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BG_DARK);
        center.add(ctrlBar, BorderLayout.NORTH);
        center.add(scroll,  BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        rebuildGrid();
    }

    // ── Search / filter logic ─────────────────────────────────────────────────

    private void onSearch() {
        String raw = searchField.getText().trim();
        searchQuery = raw.equals("Search artefacts…") ? "" : raw.toLowerCase();
        rebuildGrid();
    }

    private void rebuildGrid() {
        grid.removeAll();
        List<Models.Artefact> shown = new ArrayList<>();
        for (Models.Artefact a : VirtualMuseum.ARTEFACTS) {
            boolean typeOk = activeFilter.equals("All") || a.type.equals(activeFilter);
            boolean srchOk = searchQuery.isBlank()
                || a.name.toLowerCase().contains(searchQuery)
                || (a.material != null && a.material.toLowerCase().contains(searchQuery))
                || (a.period   != null && a.period  .toLowerCase().contains(searchQuery))
                || (a.dynasty  != null && a.dynasty.name.toLowerCase().contains(searchQuery))
                || (a.region   != null && a.region .name.toLowerCase().contains(searchQuery));
            if (typeOk && srchOk) shown.add(a);
        }
        for (Models.Artefact a : shown) grid.add(makeCard(a));

        // Pad last row with invisible fillers
        int cols = 4, rem = shown.size() % cols;
        if (rem != 0)
            for (int i = 0; i < cols - rem; i++) {
                JPanel b = new JPanel(); b.setOpaque(false); grid.add(b);
            }
        grid.revalidate();
        grid.repaint();
    }

    // ── Card factory ──────────────────────────────────────────────────────────

    private JPanel makeCard(Models.Artefact art) {
        JPanel card = new JPanel() {
            boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { app.showArtefact(art); }
                    public void mouseEntered(MouseEvent e) {
                        hov = true; repaint();
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                    public void mouseExited(MouseEvent e) {
                        hov = false; repaint();
                        setCursor(Cursor.getDefaultCursor());
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0x3D, 0x28, 0x18) : Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(hov ? Theme.SAND : Theme.DIVIDER);
                g2.setStroke(new BasicStroke(hov ? 1.4f : 0.8f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                // Coloured type stripe at top
                g2.setColor(Theme.typeStripeColor(art.type));
                g2.fillRoundRect(0, 0, getWidth(), 8, 10, 10);
                g2.fillRect(0, 4, getWidth(), 4);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setLayout(new BorderLayout());

        // ── Image area ────────────────────────────────────────────────────────
        javax.swing.ImageIcon thumb = ImageLoader.loadThumbnail(art.imagePath, THUMB_W, THUMB_H);
        JComponent imageArea;
        if (thumb != null) {
            imageArea = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight() + 10, 10, 10));
                    g2.drawImage(thumb.getImage(), 0, 0, getWidth(), getHeight(), null);
                    g2.setClip(null);
                    g2.setPaint(new GradientPaint(
                        0, getHeight() * 2 / 3, new Color(0, 0, 0, 0),
                        0, getHeight(),          new Color(0, 0, 0, 140)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            imageArea.setPreferredSize(new Dimension(THUMB_W, THUMB_H));
        } else {
            imageArea = UIComponents.imagePlaceholder(art, THUMB_W, THUMB_H);
        }

        // ── Text block ────────────────────────────────────────────────────────
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        JLabel badge = new JLabel(art.type);
        badge.setFont(new Font("SansSerif", Font.PLAIN, 10));
        badge.setForeground(Theme.MUTED);
        badge.setAlignmentX(LEFT_ALIGNMENT);

        JLabel name = new JLabel("<html><div style='width:185px'>" + art.name + "</div></html>");
        name.setFont(new Font("Serif", Font.BOLD, 13));
        name.setForeground(Theme.CREAM);
        name.setAlignmentX(LEFT_ALIGNMENT);

        String meta = (art.period != null ? art.period : "")
                    + (art.dynasty != null ? "  ·  " + art.dynasty.name : "");
        JLabel period = new JLabel("<html><div style='width:185px'>" + meta + "</div></html>");
        period.setFont(new Font("SansSerif", Font.PLAIN, 11));
        period.setForeground(Theme.MUTED);
        period.setAlignmentX(LEFT_ALIGNMENT);

        bottom.add(badge);
        bottom.add(Box.createVerticalStrut(3));
        bottom.add(name);
        bottom.add(Box.createVerticalStrut(2));
        bottom.add(period);

        card.add(imageArea, BorderLayout.CENTER);
        card.add(bottom,    BorderLayout.SOUTH);
        return card;
    }
}
