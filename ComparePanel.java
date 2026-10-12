import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * ComparePanel — Dynasty Timeline screen.
 *
 * Renders an interactive horizontal timeline of all dynasties loaded from the
 * database, grouped into non-overlapping rows via a greedy interval-packing
 * algorithm. Clicking a bar shows a detail card with artefact statistics,
 * optional description, and info pills for capital, rulers, religion, language.
 */
public class ComparePanel extends JPanel {

    // ── Region colour map ─────────────────────────────────────────────────────
    private static final Map<String, Color[]> REGION_COLORS = new LinkedHashMap<>();
    static {
        REGION_COLORS.put("Magadha",    new Color[]{ new Color(0x3C,0x34,0x89), new Color(0xEE,0xED,0xFE) });
        REGION_COLORS.put("Tamil Nadu", new Color[]{ new Color(0x0F,0x6E,0x56), new Color(0xE1,0xF5,0xEE) });
        REGION_COLORS.put("Deccan",     new Color[]{ new Color(0x99,0x3C,0x1D), new Color(0xFA,0xEC,0xE7) });
        REGION_COLORS.put("Gandhara",   new Color[]{ new Color(0x18,0x5F,0xA5), new Color(0xE6,0xF1,0xFB) });
        REGION_COLORS.put("Mathura",    new Color[]{ new Color(0x85,0x4F,0x0B), new Color(0xFA,0xEE,0xDA) });
        REGION_COLORS.put("Amaravati",  new Color[]{ new Color(0x3B,0x6D,0x11), new Color(0xEA,0xF3,0xDE) });
        REGION_COLORS.put("Odisha",     new Color[]{ new Color(0x72,0x24,0x3E), new Color(0xFB,0xEA,0xF0) });
    }

    private static final Color[] FALLBACK_COLOR = {
        new Color(0x44, 0x44, 0x41), new Color(0xF1, 0xEF, 0xE8)
    };

    // ── Timeline layout constants ─────────────────────────────────────────────
    private static final int AXIS_H      = 28;
    private static final int ROW_H       = 32;
    private static final int ROW_GAP     = 6;
    private static final int BAR_PAD_V   = 3;
    private static final int LEFT_MARGIN = 16;
    private static final int RIGHT_MARGIN= 16;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<Models.Dynasty>       sortedDynasties = new ArrayList<>();
    private final List<List<Models.Dynasty>> rows            = new ArrayList<>();
    private       Models.Dynasty             selectedDynasty = null;

    private int            yearMin, yearMax;
    private TimelineCanvas timelineCanvas;
    private JPanel         detailPanel;

    public ComparePanel(VirtualMuseum app) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        add(new UIComponents.NavBar("Compare", app), BorderLayout.NORTH);

        sortedDynasties.addAll(VirtualMuseum.DYNASTIES.values());
        sortedDynasties.sort(Comparator.comparingInt(d -> d.start));

        if (sortedDynasties.isEmpty()) {
            yearMin = -500; yearMax = 1900;
        } else {
            yearMin = sortedDynasties.stream().mapToInt(d -> d.start).min().orElse(-500) - 50;
            yearMax = sortedDynasties.stream().mapToInt(d -> d.end).max().orElse(1900)   + 50;
        }
        packRows();

        // Title bar
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        titleBar.setBackground(Theme.BG_DARK);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER));
        JLabel heading = new JLabel("Dynasty Timeline");
        heading.setFont(new Font("Serif", Font.BOLD, 22));
        heading.setForeground(Theme.CREAM);
        JLabel hint = new JLabel("  Click a dynasty bar to see its details");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(Theme.MUTED);
        titleBar.add(heading);
        titleBar.add(hint);

        JPanel legendRow = buildLegend();

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.setBackground(Theme.BG_DARK);
        topArea.add(titleBar,  BorderLayout.NORTH);
        topArea.add(legendRow, BorderLayout.SOUTH);

        // Timeline canvas (fixed height)
        timelineCanvas = new TimelineCanvas();
        int canvasHeight = AXIS_H + rows.size() * (ROW_H + ROW_GAP) + 24;
        timelineCanvas.setPreferredSize(new Dimension(900, canvasHeight));
        timelineCanvas.setBackground(Theme.BG_DARK);

        JScrollPane timelineScroll = new JScrollPane(timelineCanvas) {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = canvasHeight;
                return d;
            }
        };
        timelineScroll.setBorder(null);
        timelineScroll.getViewport().setBackground(Theme.BG_DARK);
        timelineScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        timelineScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Detail panel (scrollable, below the timeline)
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(Theme.BG_DARK);

        JScrollPane detailScroll = new JScrollPane(detailPanel);
        detailScroll.setBorder(null);
        detailScroll.getViewport().setBackground(Theme.BG_DARK);
        detailScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mainCenter = new JPanel(new BorderLayout());
        mainCenter.setBackground(Theme.BG_DARK);
        mainCenter.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        mainCenter.add(timelineScroll, BorderLayout.NORTH);
        mainCenter.add(detailScroll,   BorderLayout.CENTER);

        add(topArea,    BorderLayout.NORTH);
        add(mainCenter, BorderLayout.CENTER);
    }

    // ── Greedy row-packing ────────────────────────────────────────────────────

    private void packRows() {
        List<Integer>              rowEnds = new ArrayList<>();
        List<List<Models.Dynasty>> result  = new ArrayList<>();
        for (Models.Dynasty dyn : sortedDynasties) {
            boolean placed = false;
            for (int i = 0; i < rowEnds.size(); i++) {
                if (dyn.start >= rowEnds.get(i) + 5) {
                    result.get(i).add(dyn);
                    rowEnds.set(i, dyn.end);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Models.Dynasty> newRow = new ArrayList<>();
                newRow.add(dyn);
                result.add(newRow);
                rowEnds.add(dyn.end);
            }
        }
        rows.addAll(result);
    }

    // ── Coordinate conversion ─────────────────────────────────────────────────

    private int yearToX(int year, int canvasWidth) {
        int usable = canvasWidth - LEFT_MARGIN - RIGHT_MARGIN;
        double fraction = (double)(year - yearMin) / (yearMax - yearMin);
        return LEFT_MARGIN + (int)(fraction * usable);
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    private Color[] colorsFor(Models.Dynasty dyn) {
        if (dyn.region == null) return FALLBACK_COLOR;
        for (Map.Entry<String, Color[]> e : REGION_COLORS.entrySet()) {
            if (dyn.region.contains(e.getKey()) || e.getKey().contains(dyn.region))
                return e.getValue();
        }
        // Hash-based fallback for unknown regions
        int hash = Math.abs(dyn.region.hashCode()) % 7;
        Color[][] palette = {
            { new Color(0x3C,0x34,0x89), new Color(0xEE,0xED,0xFE) },
            { new Color(0x0F,0x6E,0x56), new Color(0xE1,0xF5,0xEE) },
            { new Color(0x99,0x3C,0x1D), new Color(0xFA,0xEC,0xE7) },
            { new Color(0x18,0x5F,0xA5), new Color(0xE6,0xF1,0xFB) },
            { new Color(0x85,0x4F,0x0B), new Color(0xFA,0xEE,0xDA) },
            { new Color(0x3B,0x6D,0x11), new Color(0xEA,0xF3,0xDE) },
            { new Color(0x72,0x24,0x3E), new Color(0xFB,0xEA,0xF0) },
        };
        return palette[hash];
    }

    // ── Legend row ────────────────────────────────────────────────────────────

    private JPanel buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        legend.setBackground(Theme.BG_DARK);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 16, 4, 16));
        Set<String> seen = new LinkedHashSet<>();
        for (Models.Dynasty d : sortedDynasties) {
            String region = d.region != null ? d.region : "Unknown";
            if (seen.add(region)) {
                Color[] cols = colorsFor(d);
                JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                item.setOpaque(false);
                JPanel dot = new JPanel() {
                    protected void paintComponent(Graphics g) {
                        g.setColor(cols[0]);
                        g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
                    }
                    public boolean isOpaque() { return false; }
                };
                dot.setPreferredSize(new Dimension(10, 10));
                JLabel lbl = new JLabel(region);
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
                lbl.setForeground(Theme.MUTED);
                item.add(dot); item.add(lbl);
                legend.add(item);
            }
        }
        return legend;
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    private void selectDynasty(Models.Dynasty dyn) {
        if (dyn == selectedDynasty) {
            selectedDynasty = null;
            detailPanel.removeAll();
            detailPanel.revalidate();
            detailPanel.repaint();
        } else {
            selectedDynasty = dyn;
            showDetail(dyn);
        }
        timelineCanvas.repaint();
    }

    // ── Detail card ───────────────────────────────────────────────────────────

    private void showDetail(Models.Dynasty dyn) {
        detailPanel.removeAll();

        // Aggregate artefact stats
        int total = 0;
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        Set<String>          artRegions = new LinkedHashSet<>();
        Set<String>          deities    = new LinkedHashSet<>();
        for (String t : new String[]{"Sculpture", "Coin", "Inscription", "Ritual Object"})
            typeCounts.put(t, 0);
        for (Models.Artefact a : VirtualMuseum.ARTEFACTS) {
            if (a.dynasty != null && a.dynasty.id == dyn.id) {
                total++;
                typeCounts.merge(a.type, 1, Integer::sum);
                if (a.region != null) artRegions.add(a.region.name);
                for (String m : a.deityMotifs) deities.add(Models.Artefact.deityName(m));
            }
        }

        Color[] cols = colorsFor(dyn);

        // Card shell
        JPanel card = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Theme.DIVIDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setColor(cols[0]);
                g2.fillRoundRect(0, 0, getWidth(), 5, 12, 12);
                g2.fillRect(0, 2, getWidth(), 3);
                g2.dispose();
                super.paintComponent(g);
            }
            public boolean isOpaque() { return false; }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));

        // Dynasty name
        JLabel nameLabel = new JLabel(dyn.name);
        nameLabel.setFont(new Font("Serif", Font.BOLD, 26));
        nameLabel.setForeground(Theme.CREAM);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));

        // Period / duration / region
        String startStr  = dyn.start < 0 ? Math.abs(dyn.start) + " BCE" : dyn.start + " CE";
        String endStr    = dyn.end   < 0 ? Math.abs(dyn.end)   + " BCE" : dyn.end   + " CE";
        int    span      = Math.abs(dyn.end - dyn.start);
        String regionStr = dyn.region != null ? dyn.region : "";
        JLabel periodLabel = new JLabel(
            startStr + "  -  " + endStr + "   |   " + span + " years   |   " + regionStr);
        periodLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        periodLabel.setForeground(Theme.MUTED);
        periodLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(periodLabel);
        card.add(Box.createVerticalStrut(16));

        // Optional description paragraph
        if (dyn.description != null && !dyn.description.isBlank()) {
            JTextArea desc = new JTextArea(dyn.description);
            desc.setFont(new Font("Serif", Font.ITALIC, 14));
            desc.setForeground(new Color(0xC2, 0xB6, 0xA0));
            desc.setBackground(Theme.BG_CARD);
            desc.setLineWrap(true); desc.setWrapStyleWord(true);
            desc.setEditable(false); desc.setOpaque(false); desc.setBorder(null);
            desc.setAlignmentX(LEFT_ALIGNMENT);
            card.add(desc);
            card.add(Box.createVerticalStrut(18));
        }

        // Info pills: Capital, Rulers, Religion, Language
        JPanel pillsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pillsRow.setOpaque(false);
        pillsRow.setAlignmentX(LEFT_ALIGNMENT);
        addInfoPill(pillsRow, "Capital",  dyn.capital,  cols[0]);
        addInfoPill(pillsRow, "Rulers",   dyn.rulers,   cols[0]);
        addInfoPill(pillsRow, "Religion", dyn.religion, cols[0]);
        addInfoPill(pillsRow, "Language", dyn.language, cols[0]);
        if (pillsRow.getComponentCount() > 0) {
            card.add(pillsRow);
            card.add(Box.createVerticalStrut(18));
        }

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(14));

        // Artefact count
        JLabel totLabel = new JLabel("Artefacts in collection: " + total);
        totLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        totLabel.setForeground(Theme.SAND);
        totLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(totLabel);
        card.add(Box.createVerticalStrut(12));

        // Type bar chart
        JLabel typeHead = new JLabel("BY TYPE");
        typeHead.setFont(new Font("SansSerif", Font.BOLD, 10));
        typeHead.setForeground(Theme.MUTED);
        typeHead.setAlignmentX(LEFT_ALIGNMENT);
        card.add(typeHead);
        card.add(Box.createVerticalStrut(6));

        int maxCount = Math.max(total, 1);
        for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
            final int   cnt    = e.getValue();
            if (cnt == 0) continue;//change
            final Color barCol = Theme.typeStripeColor(e.getKey());

            JPanel barRow = new JPanel(new BorderLayout(8, 0));
            barRow.setOpaque(false);
            barRow.setAlignmentX(LEFT_ALIGNMENT);
            barRow.setMaximumSize(new Dimension(560, 22));

            JLabel typeLbl = new JLabel(e.getKey());
            typeLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            typeLbl.setForeground(Theme.CREAM);
            typeLbl.setPreferredSize(new Dimension(120, 18));

            JPanel bar = new JPanel() {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.DIVIDER);
                    g2.fillRoundRect(0, 4, getWidth(), 12, 4, 4);
                    if (cnt > 0) {
                        g2.setColor(barCol);
                        int filled = (int)(getWidth() * (double) cnt / maxCount);
                        g2.fillRoundRect(0, 4, filled, 12, 4, 4);
                    }
                    g2.dispose();
                }
            };

            JLabel cntLbl = new JLabel(String.valueOf(cnt));
            cntLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            cntLbl.setForeground(Theme.SAND);
            cntLbl.setPreferredSize(new Dimension(24, 18));

            barRow.add(typeLbl, BorderLayout.WEST);
            barRow.add(bar,     BorderLayout.CENTER);
            barRow.add(cntLbl,  BorderLayout.EAST);
            card.add(barRow);
            card.add(Box.createVerticalStrut(4));
        }

        // Regions represented
        if (!artRegions.isEmpty()) {
            card.add(Box.createVerticalStrut(14));
            JLabel rHead = new JLabel("REGIONS REPRESENTED IN COLLECTION");
            rHead.setFont(new Font("SansSerif", Font.BOLD, 10));
            rHead.setForeground(Theme.MUTED);
            rHead.setAlignmentX(LEFT_ALIGNMENT);
            card.add(rHead);
            card.add(Box.createVerticalStrut(6));
            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            chips.setOpaque(false);
            chips.setAlignmentX(LEFT_ALIGNMENT);
            for (String r : artRegions) {
                JLabel chip = new JLabel("  " + r + "  ");
                chip.setFont(new Font("SansSerif", Font.PLAIN, 12));
                chip.setForeground(Theme.CREAM);
                chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.DIVIDER, 1, true),
                    BorderFactory.createEmptyBorder(3, 6, 3, 6)));
                chips.add(chip);
            }
            card.add(chips);
        }

        // Deity associations
        if (!deities.isEmpty()) {
            card.add(Box.createVerticalStrut(14));
            JLabel dHead = new JLabel("DEITY / ICONOGRAPHIC REFERENCES");
            dHead.setFont(new Font("SansSerif", Font.BOLD, 10));
            dHead.setForeground(Theme.MUTED);
            dHead.setAlignmentX(LEFT_ALIGNMENT);
            card.add(dHead);
            card.add(Box.createVerticalStrut(6));
            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            chips.setOpaque(false);
            chips.setAlignmentX(LEFT_ALIGNMENT);
            for (String d : deities) {
                JLabel chip = new JLabel("  " + d + "  ");
                chip.setFont(new Font("SansSerif", Font.PLAIN, 12));
                chip.setForeground(Theme.SAND);
                chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        new Color(Theme.SAND.getRed(), Theme.SAND.getGreen(),
                                  Theme.SAND.getBlue(), 70), 1, true),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)));
                chips.add(chip);
            }
            card.add(chips);
        }

        // No-artefacts message
        if (total == 0) {
            card.add(Box.createVerticalStrut(10));
            JLabel noData = new JLabel(
                "No artefacts from this dynasty are currently in the collection.");
            noData.setFont(new Font("Serif", Font.ITALIC, 13));
            noData.setForeground(Theme.MUTED);
            noData.setAlignmentX(LEFT_ALIGNMENT);
            card.add(noData);
        }

        detailPanel.add(card, BorderLayout.NORTH);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    // ── Info pill helper ──────────────────────────────────────────────────────

    private void addInfoPill(JPanel parent, String label, String value, Color accent) {
        if (value == null || value.isBlank()) return;

        JPanel pill = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            public boolean isOpaque() { return false; }
        };
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 12));

        JLabel labelLbl = new JLabel(label.toUpperCase());
        labelLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        labelLbl.setForeground(Theme.MUTED);
        labelLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        valueLbl.setForeground(Theme.CREAM);
        valueLbl.setAlignmentX(LEFT_ALIGNMENT);

        pill.add(labelLbl);
        pill.add(Box.createVerticalStrut(2));
        pill.add(valueLbl);
        parent.add(pill);
    }

    // =========================================================================
    // TimelineCanvas — custom-painted Swing component
    // =========================================================================
    class TimelineCanvas extends JPanel {

        private final Map<Models.Dynasty, Rectangle> hitRects = new LinkedHashMap<>();

        TimelineCanvas() {
            setBackground(Theme.BG_DARK);

            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    for (Map.Entry<Models.Dynasty, Rectangle> entry : hitRects.entrySet()) {
                        if (entry.getValue().contains(e.getPoint())) {
                            selectDynasty(entry.getKey());
                            return;
                        }
                    }
                    // Clicked empty space — deselect
                    selectedDynasty = null;
                    detailPanel.removeAll();
                    detailPanel.revalidate();
                    detailPanel.repaint();
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    boolean over = hitRects.values().stream()
                        .anyMatch(r -> r.contains(e.getPoint()));
                    setCursor(over
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            hitRects.clear();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth();

            // Year axis baseline
            g2.setColor(Theme.DIVIDER);
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawLine(LEFT_MARGIN, AXIS_H - 1, W - RIGHT_MARGIN, AXIS_H - 1);

            // Choose a readable tick interval
            int range = yearMax - yearMin;
            int[] candidates = { 50, 100, 200, 250, 500 };
            int tickInterval = 250;
            for (int c : candidates) {
                if (range / c <= 12) { tickInterval = c; break; }
            }

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            int firstTick = (int)(Math.ceil((double) yearMin / tickInterval) * tickInterval);

            for (int yr = firstTick; yr <= yearMax; yr += tickInterval) {
                int x = yearToX(yr, W);

                // Grid line
                g2.setColor(new Color(Theme.DIVIDER.getRed(), Theme.DIVIDER.getGreen(),
                                      Theme.DIVIDER.getBlue(), 100));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawLine(x, AXIS_H, x, AXIS_H + rows.size() * (ROW_H + ROW_GAP) + 8);

                // Tick label
                String lbl;
                if      (yr < 0)  lbl = Math.abs(yr) + " BCE";
                else if (yr == 0) lbl = "1 CE";
                else              lbl = yr + " CE";
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Theme.MUTED);
                g2.drawString(lbl, x - fm.stringWidth(lbl) / 2, AXIS_H - 6);
            }

            // Dynasty bars
            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                int yTop = AXIS_H + rowIdx * (ROW_H + ROW_GAP);
                int yBar = yTop + BAR_PAD_V;
                int barH = ROW_H - BAR_PAD_V * 2;

                for (Models.Dynasty dyn : rows.get(rowIdx)) {
                    int xStart = yearToX(dyn.start, W);
                    int xEnd   = yearToX(dyn.end,   W);
                    int barW   = Math.max(xEnd - xStart, 4);

                    Color[]  cols     = colorsFor(dyn);
                    boolean  selected = (dyn == selectedDynasty);

                    g2.setColor(cols[0]);
                    g2.fillRoundRect(xStart, yBar, barW, barH, 6, 6);

                    if (selected) {
                        g2.setColor(Theme.SAND_LT);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawRoundRect(xStart, yBar, barW - 1, barH - 1, 6, 6);
                    }

                    // Dynasty name, clipped to bar width
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.setColor(cols[1]);
                    FontMetrics fm  = g2.getFontMetrics();
                    int textY = yBar + (barH + fm.getAscent() - fm.getDescent()) / 2;
                    Shape oldClip = g2.getClip();
                    g2.setClip(xStart + 4, yBar, barW - 8, barH);
                    String label = dyn.name;
                    while (label.length() > 1 && fm.stringWidth(label + "...") > barW - 10)
                        label = label.substring(0, label.length() - 1);
                    if (!label.equals(dyn.name)) label += "...";
                    g2.drawString(label, xStart + 6, textY);
                    g2.setClip(oldClip);

                    hitRects.put(dyn, new Rectangle(xStart, yBar, barW, barH));
                }
            }
            g2.dispose();
        }
    }
}
