import java.awt.Color;

/**
 * Theme — shared colour palette and type-colour helpers
 * used by every panel in the Virtual Museum application.
 */
public class Theme {

    // ── Colour palette ────────────────────────────────────────────────────────
    public static final Color BG_DARK  = new Color(0x1C, 0x15, 0x10);
    public static final Color BG_PANEL = new Color(0x26, 0x19, 0x0F);
    public static final Color BG_CARD  = new Color(0x2E, 0x1F, 0x12);
    public static final Color TERRA    = new Color(0xB5, 0x45, 0x1B);
    public static final Color SAND     = new Color(0xC8, 0x95, 0x5A);
    public static final Color SAND_LT  = new Color(0xE2, 0xB9, 0x7A);
    public static final Color CREAM    = new Color(0xF2, 0xE8, 0xD5);
    public static final Color MUTED    = new Color(0x7A, 0x66, 0x52);
    public static final Color DIVIDER  = new Color(0x3E, 0x2A, 0x1A);


    // ── Artefact type colours ─────────────────────────────────────────────────
    public static Color typeStripeColor(String type) {
        return switch (type) {
            case "Sculpture"     -> new Color(0x7A, 0x3E, 0x18);
            case "Coin"          -> new Color(0x6B, 0x5A, 0x18);
            case "Inscription"   -> new Color(0x1A, 0x4A, 0x52);
            case "Ritual Object" -> new Color(0x52, 0x28, 0x52);
            default              -> new Color(0x2A, 0x9D, 0x5C);
        };
    }

    private Theme() {} // utility class — no instances
}
