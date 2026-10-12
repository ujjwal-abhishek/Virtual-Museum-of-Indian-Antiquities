import javax.swing.*;
import java.awt.*;

/**
 * DetailPanel — full artefact detail view.
 *
 * Left sidebar: image (or styled placeholder), type badge, and metadata table.
 * Right area:   scrollable name, description, and deity / iconographic chips.
 *
 * Content is loaded dynamically by calling {@link #load(Models.Artefact)}.
 */
public class DetailPanel extends JPanel {

    public static final int IMG_W = 262;
    public static final int IMG_H = 196;

    private JPanel    imageContainer;
    private JLabel    typeLabel, titleLabel;
    private JPanel    metaPanel, deityPanel;
    private JTextArea descArea;

    public DetailPanel(VirtualMuseum app) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        add(new UIComponents.NavBar("", app), BorderLayout.NORTH);

        // ── Left sidebar ──────────────────────────────────────────────────────
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.BG_PANEL);
        sidebar.setPreferredSize(new Dimension(300, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.DIVIDER));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Back button
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        backRow.setOpaque(false);
        JButton back = UIComponents.outlineBtn("← Back to Gallery");
        back.setPreferredSize(new Dimension(180, 32));
        back.addActionListener(e -> app.navigate(VirtualMuseum.CARD_GALLERY));
        backRow.add(back);
        sidebar.add(backRow);

        // Image container — replaced on every load() call
        imageContainer = new JPanel(new BorderLayout());
        imageContainer.setOpaque(false);
        imageContainer.setAlignmentX(CENTER_ALIGNMENT);
        imageContainer.setMaximumSize(new Dimension(300, IMG_H + 16));
        imageContainer.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        sidebar.add(imageContainer);
        sidebar.add(Box.createVerticalStrut(10));

        typeLabel = new JLabel("", SwingConstants.CENTER);
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        typeLabel.setForeground(Theme.TERRA);
        typeLabel.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(typeLabel);
        sidebar.add(Box.createVerticalStrut(14));

        metaPanel = new JPanel();
        metaPanel.setOpaque(false);
        metaPanel.setLayout(new BoxLayout(metaPanel, BoxLayout.Y_AXIS));
        metaPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        metaPanel.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(metaPanel);
        sidebar.add(Box.createVerticalGlue());

        // ── Main content area ─────────────────────────────────────────────────
        JPanel main = new JPanel();
        main.setBackground(Theme.BG_DARK);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        titleLabel = new JLabel("Title");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        titleLabel.setForeground(Theme.CREAM);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        main.add(titleLabel);
        main.add(Box.createVerticalStrut(12));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        main.add(sep);
        main.add(Box.createVerticalStrut(20));

        JLabel descHead = new JLabel("About this Artefact");
        descHead.setFont(new Font("SansSerif", Font.BOLD, 11));
        descHead.setForeground(Theme.MUTED);
        descHead.setAlignmentX(LEFT_ALIGNMENT);
        main.add(descHead);
        main.add(Box.createVerticalStrut(8));

        descArea = new JTextArea();
        descArea.setFont(new Font("Serif", Font.PLAIN, 15));
        descArea.setForeground(Theme.CREAM);
        descArea.setBackground(Theme.BG_DARK);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setBorder(null);
        descArea.setAlignmentX(LEFT_ALIGNMENT);
        main.add(descArea);
        main.add(Box.createVerticalStrut(24));

        deityPanel = new JPanel();
        deityPanel.setOpaque(false);
        deityPanel.setLayout(new BoxLayout(deityPanel, BoxLayout.Y_AXIS));
        deityPanel.setAlignmentX(LEFT_ALIGNMENT);
        main.add(deityPanel);

        JScrollPane scrollMain = new JScrollPane(main);
        scrollMain.setBorder(null);
        scrollMain.getViewport().setBackground(Theme.BG_DARK);
        scrollMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollMain.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, scrollMain);
        split.setDividerLocation(300);
        split.setDividerSize(1);
        split.setEnabled(false);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    // ── Load artefact data ────────────────────────────────────────────────────

    public void load(Models.Artefact a) {
        // Sidebar image
        imageContainer.removeAll();
        javax.swing.ImageIcon full = ImageLoader.loadFull(a.imagePath, IMG_W, IMG_H);
        if (full != null) {
            JPanel imgPanel = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), 10, 10));
                    g2.drawImage(full.getImage(),
                        (getWidth()  - full.getIconWidth())  / 2,
                        (getHeight() - full.getIconHeight()) / 2, null);
                    g2.setClip(null);
                    g2.setColor(new Color(Theme.SAND.getRed(), Theme.SAND.getGreen(),
                                          Theme.SAND.getBlue(), 60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            imgPanel.setPreferredSize(new Dimension(IMG_W, IMG_H));
            imgPanel.setMaximumSize(new Dimension(IMG_W, IMG_H));
            imageContainer.add(imgPanel, BorderLayout.CENTER);
        } else {
            imageContainer.add(UIComponents.imagePlaceholder(a, IMG_W, IMG_H), BorderLayout.CENTER);
        }

        typeLabel.setText(a.type.toUpperCase());

        metaPanel.removeAll();
        addMeta("Material", a.material);
        addMeta("Period",   a.period);
        addMeta("Dynasty",
            a.dynasty != null
                ? a.dynasty.name + " (" + a.dynasty.start + "–" + a.dynasty.end + ")"
                : null);
        addMeta("Region",
            a.region != null
                ? a.region.name + ", " + a.region.modernState
                : null);
        addMeta("Museum", a.museum);
        metaPanel.revalidate();

        titleLabel.setText("<html><div style='width:500px;'>" + a.name + "</div></html>");
        descArea.setText(a.description);
        descArea.setCaretPosition(0);

        deityPanel.removeAll();
        if (!a.deityMotifs.isEmpty()) {
            JLabel dh = new JLabel("Deity / Iconographic References");
            dh.setFont(new Font("SansSerif", Font.BOLD, 11));
            dh.setForeground(Theme.MUTED);
            dh.setAlignmentX(LEFT_ALIGNMENT);
            deityPanel.add(dh);
            deityPanel.add(Box.createVerticalStrut(8));

            for (String m : a.deityMotifs) {
                JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                chip.setOpaque(false);
                chip.setAlignmentX(LEFT_ALIGNMENT);
                JLabel cl = new JLabel("  " + m + "  ");
                cl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                cl.setForeground(Theme.SAND);
                cl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        new Color(Theme.SAND.getRed(), Theme.SAND.getGreen(),
                                  Theme.SAND.getBlue(), 70), 1, true),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                chip.add(cl);
                deityPanel.add(chip);
                deityPanel.add(Box.createVerticalStrut(6));
            }
        }
        deityPanel.revalidate();
        revalidate();
        repaint();
    }

    private void addMeta(String label, String value) {
        metaPanel.add(UIComponents.metaRow(label, value));
        metaPanel.add(Box.createVerticalStrut(6));
    }
}
