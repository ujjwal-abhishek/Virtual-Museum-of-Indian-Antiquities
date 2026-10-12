import javax.swing.*;
import java.awt.*;

/**
 * AboutPanel — static informational screen describing the museum,
 * its collection, the database design, and the academic context.
 * Also shows live collection statistics as card tiles.
 */
public class AboutPanel extends JPanel {

    public AboutPanel(VirtualMuseum app) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        add(new UIComponents.NavBar("About", app), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setBackground(Theme.BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));

        JLabel heading = new JLabel("About This Museum");
        heading.setFont(new Font("Serif", Font.BOLD, 36));
        heading.setForeground(Theme.CREAM);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(6));

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(sep);
        content.add(Box.createVerticalStrut(24));

        // ── Section prose blocks ──────────────────────────────────────────────
        String[][] sections = {
            {"Our Mission",
             "The Virtual Museum of Indian Antiquities is an educational platform designed " +
             "to make India's rich archaeological and artistic heritage accessible to all. " +
             "We present a curated selection of 40–60 artefacts drawn from major Indian " +
             "museum collections, spanning five millennia of civilisation."},
            {"The Collection",
             "The collection is organised into four categories — Sculptures (stone and bronze), " +
             "Coins (punch-marked and dynastic), Inscriptions (copper-plate, rock, and pillar), " +
             "and Ritual Objects (lamps, vessels, and icons). Each artefact is contextualised " +
             "within its dynasty, region, and iconographic tradition."},
            {"Database Design",
             "Artefact records are linked to Dynasty, Region, and Deity tables via foreign " +
             "keys, enabling rich cross-referencing. The many-to-many Artefact_Deity junction " +
             "table allows each artefact to record multiple deity associations with individual " +
             "motif notes — capturing the iconographic complexity of Indian religious art."},
            {"Academic Use",
             "This application was developed as an academic project demonstrating relational " +
             "database design (DDL, foreign keys, junction tables), Java Swing/AWT GUI " +
             "programming, real-time MySQL integration via JDBC, and dynamic image " +
             "loading with in-memory caching."},
        };

        for (String[] s : sections) {
            JLabel sh = new JLabel(s[0]);
            sh.setFont(new Font("SansSerif", Font.BOLD, 14));
            sh.setForeground(Theme.SAND);
            sh.setAlignmentX(LEFT_ALIGNMENT);

            JTextArea body = new JTextArea(s[1]);
            body.setFont(new Font("Serif", Font.PLAIN, 15));
            body.setForeground(Theme.CREAM);
            body.setBackground(Theme.BG_DARK);
            body.setLineWrap(true);
            body.setWrapStyleWord(true);
            body.setEditable(false);
            body.setOpaque(false);
            body.setBorder(null);
            body.setAlignmentX(LEFT_ALIGNMENT);

            content.add(sh);
            content.add(Box.createVerticalStrut(8));
            content.add(body);
            content.add(Box.createVerticalStrut(24));
        }

        // ── Statistics cards ──────────────────────────────────────────────────
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(680, 82));
        stats.setAlignmentX(LEFT_ALIGNMENT);

        String[][] sd = {
            {String.valueOf(VirtualMuseum.ARTEFACTS.size()), "Artefacts"},
            {String.valueOf(VirtualMuseum.DYNASTIES.size()), "Dynasties"},
            {String.valueOf(VirtualMuseum.REGIONS.size()),   "Regions"},
            {String.valueOf(VirtualMuseum.DEITIES.size()),   "Deities indexed"},
        };
        for (String[] d : sd) {
            JPanel box = new JPanel() {
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
                @Override public boolean isOpaque() { return false; }
            };
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

            JLabel num = new JLabel(d[0]);
            num.setFont(new Font("Serif", Font.BOLD, 30));
            num.setForeground(Theme.SAND_LT);
            num.setAlignmentX(CENTER_ALIGNMENT);

            JLabel lbl = new JLabel(d[1]);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lbl.setForeground(Theme.MUTED);
            lbl.setAlignmentX(CENTER_ALIGNMENT);

            box.add(num);
            box.add(lbl);
            stats.add(box);
        }
        content.add(stats);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }
}
