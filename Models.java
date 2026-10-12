import java.util.ArrayList;
import java.util.List;

/**
 * Data model — mirrors the museumdb schema.
 * Contains Dynasty, Region, Deity and Artefact value objects.
 */
public class Models {

    // ── Dynasty ───────────────────────────────────────────────────────────────
    public static class Dynasty {
        public int id;
        public String name;
        public int start, end;
        public String region;

        // Extended fields used by ComparePanel
        public String description, capital, rulers, religion, language;

        public Dynasty(int id, String name, int start, int end, String region) {
            this.id = id; this.name = name;
            this.start = start; this.end = end; this.region = region;
        }

        @Override public String toString() { return name; }
    }

    // ── Region ────────────────────────────────────────────────────────────────
    public static class Region {
        public int id;
        public String name, modernState;

        public Region(int id, String name, String modernState) {
            this.id = id; this.name = name; this.modernState = modernState;
        }
    }

    // ── Deity ─────────────────────────────────────────────────────────────────
    public static class Deity {
        public int id;
        public String name, tradition;

        public Deity(int id, String name, String tradition) {
            this.id = id; this.name = name; this.tradition = tradition;
        }
    }

    // ── Artefact ──────────────────────────────────────────────────────────────
    /**
     * Mirrors the artefact table plus joined Dynasty / Region rows and
     * the Artefact_Deity many-to-many rows.
     * {@code imagePath} comes from the {@code image} column — a filesystem
     * path to the image file (e.g. /home/user/museum/images/nataraja.jpg).
     */
    public static class Artefact {
        public int    id;
        public String name, type, material, period, museum, description;
        public String imagePath;          // from artefact.image column
        public Dynasty dynasty;
        public Region  region;
        public List<String> deityMotifs = new ArrayList<>();

        public Artefact(int id, String name, String type, String material,
                        String period, Dynasty dynasty, Region region,
                        String museum, String description, String imagePath) {
            this.id = id; this.name = name; this.type = type;
            this.material = material; this.period = period;
            this.dynasty = dynasty; this.region = region;
            this.museum = museum; this.description = description;
            this.imagePath = imagePath;
        }

        /** Text glyph shown when no image file is available. */
        public String fallbackGlyph() {
            return switch (type) {
                case "Sculpture"     -> "🗿";
                case "Coin"          -> "⚙";
                case "Inscription"   -> "📜";
                case "Ritual Object" -> "🪔";
                default              -> "◈";
            };
        }

        /**
         * Extracts the deity name from a motif string formatted as
         * "Name – motif note". Used by ComparePanel to aggregate
         * unique deity names.
         */
        public static String deityName(String motif) {
            int dash = motif.indexOf(" \u2013 ");
            return dash >= 0 ? motif.substring(0, dash) : motif;
        }
    }
}
