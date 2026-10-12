import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * HomePanel — the landing / hero screen of the Virtual Museum.
 * Shows the app title, a subtitle, type-pill badges, an "Explore" button
 * and live collection statistics pulled from the in-memory data stores.
 */
public class HomePanel extends JPanel {

    public HomePanel(VirtualMuseum app) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        // ── Top bar (About link) ──────────────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        topBar.setBackground(Theme.BG_PANEL);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER));

        JLabel abt = new JLabel("About");
        abt.setFont(new Font("SansSerif", Font.PLAIN, 13));
        abt.setForeground(Theme.MUTED);
        abt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        abt.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { app.navigate(VirtualMuseum.CARD_ABOUT); }
            public void mouseEntered(MouseEvent e) { abt.setForeground(Theme.SAND); }
            public void mouseExited(MouseEvent e)  { abt.setForeground(Theme.MUTED); }
        });
        topBar.add(abt);

        // ── Hero panel ────────────────────────────────────────────────────────
        JPanel hero = new JPanel(new GridBagLayout());
        hero.setBackground(Theme.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // Type-category pills
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 34, 0);
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pills.setOpaque(false);
        for (String t : new String[]{"Sculpture", "Coin", "Inscription", "Ritual Object"}) {
            JLabel p = new JLabel("  " + t + "  ");
            p.setFont(new Font("SansSerif", Font.PLAIN, 11));
            p.setForeground(Theme.MUTED);
            p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.DIVIDER, 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
            pills.add(p);
        }
        hero.add(pills, gbc);

        // "VIRTUAL MUSEUM OF" superscript
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel sup = new JLabel("VIRTUAL MUSEUM OF");
        sup.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sup.setForeground(Theme.MUTED);
        hero.add(sup, gbc);

        // Main title
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 12, 0);
        JLabel title = new JLabel("Indian Antiquities");
        title.setFont(new Font("Serif", Font.BOLD, 54));
        title.setForeground(Theme.CREAM);
        hero.add(title, gbc);

        // Subtitle
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 30, 0);
        JLabel sub = new JLabel(
            "<html><div style='text-align:center;width:460px;'>" +
            "Explore sculptures, coins, inscriptions, and ritual objects " +
            "from the great dynasties of ancient India — Maurya, Gupta, Chola, Kushan, and beyond." +
            "</div></html>");
        sub.setFont(new Font("Serif", Font.ITALIC, 16));
        sub.setForeground(Theme.MUTED);
        hero.add(sub, gbc);

        // Enter button
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 16, 0);
        JButton enter = UIComponents.primaryBtn("Explore the Collection  →");
        enter.setPreferredSize(new Dimension(234, 46));
        enter.addActionListener(e -> app.navigate(VirtualMuseum.CARD_GALLERY));
        hero.add(enter, gbc);

        // Live statistics line
        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 0, 0);
        JLabel stat = new JLabel(
            VirtualMuseum.ARTEFACTS.size() + " Artefacts  ·  " +
            VirtualMuseum.DYNASTIES.size() + " Dynasties  ·  " +
            VirtualMuseum.REGIONS.size()   + " Regions  ·  " +
            VirtualMuseum.DEITIES.size()   + " Deities");
        stat.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stat.setForeground(new Color(0x4A, 0x3A, 0x2A));
        hero.add(stat, gbc);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(Theme.BG_PANEL);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.DIVIDER));
        JLabel ft = new JLabel("© Virtual Museum of Indian Antiquities  ·  Educational Project");
        ft.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ft.setForeground(Theme.MUTED);
        footer.add(ft);

        add(topBar, BorderLayout.NORTH);
        add(hero,   BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }
}
