import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * DatabaseManager — all JDBC / MySQL 8.0 interaction.
 *
 * Connection parameters are loaded from {@code db.properties} on the
 * classpath. Example file:
 * <pre>
 *   db.url      = jdbc:mysql://localhost:3306/museumdb?useSSL=false&amp;serverTimezone=UTC&amp;allowPublicKeyRetrieval=true
 *   db.user     = root
 *   db.password = your_password
 * </pre>
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    private static String DB_URL =
        "jdbc:mysql://localhost:3306/museumdb?useSSL=false" +
        "&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static String DB_USER = "root";
    private static String DB_PASS = "root@123";

    private static Connection connection;

    static {
        try (InputStream is =
                DatabaseManager.class.getClassLoader()
                                     .getResourceAsStream("db.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                DB_URL  = p.getProperty("db.url",      DB_URL);
                DB_USER = p.getProperty("db.user",     DB_USER);
                DB_PASS = p.getProperty("db.password", DB_PASS);
                LOG.info("Loaded credentials from db.properties");
            }
        } catch (Exception e) {
            LOG.warning("db.properties unreadable: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed())
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        return connection;
    }

    public static void close() {
        try { if (connection != null) connection.close(); }
        catch (SQLException e) { LOG.warning("Close error: " + e.getMessage()); }
    }

    public static void loadDynasties(Map<Integer, Models.Dynasty> map) throws SQLException {
        boolean extended = true;
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT dynasty_id,name,period_start,period_end,region," +
                "       description,capital,rulers,religion,language " +
                "FROM dynasty ORDER BY dynasty_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Models.Dynasty d = new Models.Dynasty(
                    rs.getInt("dynasty_id"), rs.getString("name"),
                    rs.getInt("period_start"), rs.getInt("period_end"),
                    rs.getString("region"));
                d.description = rs.getString("description");
                d.capital     = rs.getString("capital");
                d.rulers      = rs.getString("rulers");
                d.religion    = rs.getString("religion");
                d.language    = rs.getString("language");
                map.put(d.id, d);
            }
        } catch (SQLException ex) {
            extended = false;
            LOG.warning("Extended dynasty columns missing, falling back: " + ex.getMessage());
        }
        if (!extended) {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "SELECT dynasty_id,name,period_start,period_end,region " +
                    "FROM dynasty ORDER BY dynasty_id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    map.put(rs.getInt("dynasty_id"),
                        new Models.Dynasty(rs.getInt("dynasty_id"), rs.getString("name"),
                                    rs.getInt("period_start"), rs.getInt("period_end"),
                                    rs.getString("region")));
            }
        }
    }

    public static void loadRegions(Map<Integer, Models.Region> map) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT region_id,name,modern_state FROM region ORDER BY region_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                map.put(rs.getInt("region_id"),
                    new Models.Region(rs.getInt("region_id"), rs.getString("name"),
                               rs.getString("modern_state")));
        }
    }

    public static void loadDeities(Map<Integer, Models.Deity> map) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT deity_id,name,tradition FROM deity ORDER BY deity_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                map.put(rs.getInt("deity_id"),
                    new Models.Deity(rs.getInt("deity_id"), rs.getString("name"),
                              rs.getString("tradition")));
        }
    }

    public static void loadDeityMotifs(Models.Artefact a,
                                       Map<Integer, Models.Deity> deities) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT d.name, ad.motif_note " +
                "FROM artefact_deity ad " +
                "JOIN deity d ON d.deity_id = ad.deity_id " +
                "WHERE ad.artefact_id = ?")) {
            ps.setInt(1, a.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String d = rs.getString("name");
                    String m = rs.getString("motif_note");
                    a.deityMotifs.add(m != null && !m.isBlank() ? d + " – " + m : d);
                }
            }
        }
    }

    /** NOTE: SELECT includes the {@code image} column that stores the file path. */
    public static void loadArtefacts(List<Models.Artefact> list,
                                     Map<Integer, Models.Dynasty> dynasties,
                                     Map<Integer, Models.Region>  regions,
                                     Map<Integer, Models.Deity>   deities) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT artefact_id,name,type,material,period," +
                "       dynasty_id,region_id,museum,description,image " +
                "FROM artefact ORDER BY artefact_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int     id      = rs.getInt("artefact_id");
                int     dynId   = rs.getInt("dynasty_id");
                boolean dynNull = rs.wasNull();
                int     regId   = rs.getInt("region_id");

                Models.Artefact a = new Models.Artefact(id,
                    rs.getString("name"),  rs.getString("type"),
                    rs.getString("material"), rs.getString("period"),
                    dynNull ? null : dynasties.get(dynId),
                    regions.get(regId),
                    rs.getString("museum"), rs.getString("description"),
                    rs.getString("image")
                );
                loadDeityMotifs(a, deities);
                list.add(a);
            }
        }
    }

    private DatabaseManager() {} // utility class — no instances
}
