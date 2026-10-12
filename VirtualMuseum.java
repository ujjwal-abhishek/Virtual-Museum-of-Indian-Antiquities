import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * VirtualMuseum — main application frame and entry point.
 *
 * Manages the CardLayout screen stack and the application-wide data stores.
 * All database loading happens in the static initialiser before the UI is built.
 *
 * MySQL JDBC driver JAR required (mysql-connector-j-X.X.X.jar).
 * Download: https://dev.mysql.com/downloads/connector/j/
 *
 * Compile (Linux/macOS):
 *   javac -cp .:mysql-connector-j.jar *.java
 *   java  -cp .:mysql-connector-j.jar VirtualMuseum
 *
 * Compile (Windows):
 *   javac -cp .;mysql-connector-j.jar *.java
 *   java  -cp .;mysql-connector-j.jar VirtualMuseum
 *
 * db.properties (place on classpath, next to .class files):
 *   db.url      = jdbc:mysql://localhost:3306/museumdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
 *   db.user     = root
 *   db.password = your_password
 */
public class VirtualMuseum extends JFrame {

    // ── Screen card names ─────────────────────────────────────────────────────
    public static final String CARD_HOME    = "HOME";
    public static final String CARD_GALLERY = "GALLERY";
    public static final String CARD_DETAIL  = "DETAIL";
    public static final String CARD_ABOUT   = "ABOUT";
    public static final String CARD_COMPARE = "COMPARE";

    // ── Artefact types ────────────────────────────────────────────────────────
    public static final String[] TYPES = {"All", "Sculpture", "Coin", "Inscription", "Ritual Object"};

    // ── Application-wide data stores (loaded once at startup) ─────────────────
    public static final Map<Integer, Models.Dynasty> DYNASTIES = new LinkedHashMap<>();
    public static final Map<Integer, Models.Region>  REGIONS   = new LinkedHashMap<>();
    public static final Map<Integer, Models.Deity>   DEITIES   = new LinkedHashMap<>();
    public static final List<Models.Artefact>        ARTEFACTS = new ArrayList<>();

    // ── Bootstrap: load all data from MySQL at class init ─────────────────────
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DatabaseManager.loadDynasties(DYNASTIES);
            DatabaseManager.loadRegions(REGIONS);
            DatabaseManager.loadDeities(DEITIES);
            DatabaseManager.loadArtefacts(ARTEFACTS, DYNASTIES, REGIONS, DEITIES);
        } catch (ClassNotFoundException e) {
            showFatalError(
                "MySQL JDBC driver not found.\n\n" +
                "Download mysql-connector-j-X.X.X.jar from:\n" +
                "  https://dev.mysql.com/downloads/connector/j/\n" +
                "Choose 'Platform Independent' ZIP, then add the JAR to classpath:\n\n" +
                "  javac -cp .:mysql-connector-j.jar *.java\n" +
                "  java  -cp .:mysql-connector-j.jar VirtualMuseum\n\n" +
                "On Windows use semicolons:  -cp .;mysql-connector-j.jar");
        } catch (SQLException e) {
            showFatalError(
                "Could not connect to the MySQL database.\n\n" +
                "Check db.properties:\n\n" +
                "  db.url      = jdbc:mysql://localhost:3306/museumdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true\n" +
                "  db.user     = root\n" +
                "  db.password = your_password\n\n" +
                "Also make sure:\n" +
                "  • MySQL 8 server is running on port 3306\n" +
                "  • The 'museumdb' database exists (run museum_mysql.sql first)\n" +
                "  • Your user has SELECT privileges on museumdb\n\n" +
                "SQL error: " + e.getMessage());
        }
    }

    public static void showFatalError(String msg) {
        JOptionPane.showMessageDialog(null,
            "<html><body style='width:420px'>" + msg.replace("\n", "<br>") + "</body></html>",
            "Database Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    // ── CardLayout navigation ─────────────────────────────────────────────────
    private final CardLayout  cardLayout    = new CardLayout();
    private final JPanel      cardContainer = new JPanel(cardLayout);
    private final DetailPanel detailPanel;

    public VirtualMuseum() {
        super("Virtual Museum of Indian Antiquities");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        cardContainer.setBackground(Theme.BG_DARK);

        detailPanel = new DetailPanel(this);

        cardContainer.add(new HomePanel(this),    CARD_HOME);
        cardContainer.add(new GalleryPanel(this), CARD_GALLERY);
        cardContainer.add(detailPanel,            CARD_DETAIL);
        cardContainer.add(new ComparePanel(this), CARD_COMPARE);
        cardContainer.add(new AboutPanel(this),   CARD_ABOUT);

        add(cardContainer);
        cardLayout.show(cardContainer, CARD_HOME);
    }

    public void navigate(String card) {
        cardLayout.show(cardContainer, card);
    }

    public void showArtefact(Models.Artefact a) {
        detailPanel.load(a);
        navigate(CARD_DETAIL);
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::close));
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new VirtualMuseum().setVisible(true);
        });
    }
}
