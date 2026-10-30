package de.kryn.waypoints.data;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;

/**
 * Symbole als 8x8-Pixelgrafik. '.' ist transparent, 'C' wird in der Farbe des Wegpunkts gezeichnet.
 */
public enum WaypointIcon {
    HOUSE("Zuhause",
            "...rr...",
            "..rrrr..",
            ".rrrrrr.",
            "rrrrrrrr",
            ".wwwwww.",
            ".wbwwdw.",
            ".wwwwdw.",
            ".wwwwdw."),
    BED("Bett",
            "........",
            "........",
            "W.......",
            "WWWrrrrr",
            "WWWrrrrr",
            "pppppppp",
            "p......p",
            "........"),
    ANIMAL("Tiere",
            "P......P",
            "PPPPPPPP",
            "PWkPPkWP",
            "PPPPPPPP",
            "PPSSSSPP",
            "PPdSSdPP",
            "PPSSSSPP",
            "PPPPPPPP"),
    HEART("Herz",
            "........",
            ".rr..rr.",
            "rWrrrrrr",
            "rrrrrrrr",
            ".rrrrrr.",
            "..rrrr..",
            "...rr...",
            "........"),
    FRIEND_1("Freund 1",
            "hhhhhhhh",
            "hhhhhhhh",
            "hsssssss",
            "ssssssss",
            "sWessWes",
            "sssnnsss",
            "ssmssmss",
            "ssmmmmss"),
    FRIEND_2("Freund 2",
            "yyyyyyyy",
            "yyyyyyyy",
            "yttttttt",
            "tttttttt",
            "tWgttWgt",
            "tttnnttt",
            "ttmttmtt",
            "ttmmmmtt"),
    FRIEND_3("Freund 3",
            "kkkkkkkk",
            "kkkkkkkk",
            "kDDDDDDk",
            "DDDDDDDD",
            "DWrDDWrD",
            "DDDnnDDD",
            "DDmDDmDD",
            "DDmmmmDD"),
    STAR("Stern",
            "...yy...",
            "...yy...",
            "yyyyyyyy",
            ".yyyyyy.",
            "..yyyy..",
            ".yyyyyy.",
            ".yy..yy.",
            "yy....yy"),
    FLAG("Ziel",
            "gCCCCC..",
            "gCCCCCC.",
            "gCCCCC..",
            "gCCCC...",
            "g.......",
            "g.......",
            "g.......",
            "g......."),
    CHEST("Lager",
            "oooooooo",
            "obbbbbbo",
            "obbbbbbo",
            "oooGGooo",
            "obbGGbbo",
            "obbbbbbo",
            "obbbbbbo",
            "oooooooo"),
    MINE("Mine",
            "..cccc..",
            ".c....c.",
            "c..pp..c",
            "...pp...",
            "...pp...",
            "...pp...",
            "...pp...",
            "...pp..."),
    DIAMOND("Erze",
            "........",
            "..cccc..",
            ".cWcccc.",
            "cccccccc",
            ".cccccc.",
            "..cccc..",
            "...cc...",
            "........"),
    DANGER("Gefahr",
            ".......W",
            "......Wg",
            ".....Wg.",
            "....Wg..",
            ".y.Wg...",
            "..yg....",
            "..py....",
            ".p..y..."),
    DEATH("Todespunkt",
            ".WWWWWW.",
            "WWWWWWWW",
            "WkkWWkkW",
            "WkkWWkkW",
            "WWWWWWWW",
            ".WWkkWW.",
            ".WkWWkW.",
            "..WWWW.."),
    PORTAL("Portal",
            "oooooooo",
            "ovvvvvvo",
            "ovVvvvvo",
            "ovvvVvvo",
            "ovvvvvvo",
            "ovVvvvvo",
            "ovvvvVvo",
            "oooooooo"),
    TREE("Baum / Farm",
            "..eeee..",
            ".eeeeee.",
            "eeEeeeee",
            "eeeeeEee",
            ".eeeeee.",
            "...pp...",
            "...pp...",
            "...pp...");

    private static final Map<Character, Integer> PALETTE = Map.ofEntries(
            Map.entry('r', 0xFFC0392B), // rot
            Map.entry('w', 0xFFC8A165), // Holzwand
            Map.entry('W', 0xFFF2F2F2), // weiß
            Map.entry('b', 0xFF8B5A2B), // Truhe/Holz
            Map.entry('d', 0xFF5C3A1E), // Tür / Nasenloch
            Map.entry('p', 0xFF7A5230), // Stiel / Planke
            Map.entry('P', 0xFFF0A0A8), // Schwein rosa
            Map.entry('S', 0xFFE07A8A), // Schnauze
            Map.entry('k', 0xFF1E1E1E), // schwarz
            Map.entry('h', 0xFF4A2F1B), // braune Haare
            Map.entry('y', 0xFFF4D03F), // gelb / blond
            Map.entry('s', 0xFFE0AC7E), // Haut hell
            Map.entry('t', 0xFFD49A6A), // Haut mittel
            Map.entry('D', 0xFF8D5524), // Haut dunkel
            Map.entry('e', 0xFF3D9B35), // Blätter
            Map.entry('E', 0xFF2C7326), // Blätter dunkel
            Map.entry('n', 0xFF9E6B4A), // Nase
            Map.entry('m', 0xFF6B3A2A), // Mund
            Map.entry('g', 0xFFA8A8A8), // grau / Eisen
            Map.entry('G', 0xFF8C8C8C), // Schloss
            Map.entry('o', 0xFF2B1F3A), // Obsidian / Umrandung
            Map.entry('v', 0xFF8E44AD), // Portal lila
            Map.entry('V', 0xFFC39BD3), // Portal hell
            Map.entry('c', 0xFF4FD1D9)  // Diamant
    );

    public final String label;
    private final String[] rows;

    WaypointIcon(String label, String... rows) {
        this.label = label;
        this.rows = rows;
    }

    // Augenfarben der Freunde: blau, grün und rot über 'e'/'g'/'r' wären doppelt belegt,
    // daher werden die Pupillen hier je nach Symbol eingefärbt.
    private int colorFor(char c, int waypointColor) {
        if (c == 'C') return waypointColor;
        if (c == 'e' && this == FRIEND_1) return 0xFF3B6FD8;
        if (c == 'g' && this == FRIEND_2) return 0xFF2E9E44;
        Integer argb = PALETTE.get(c);
        return argb == null ? 0 : argb;
    }

    /** Zeichnet das Symbol mit linker oberer Ecke (x, y); jeder Pixel ist scale x scale groß. */
    public void draw(GuiGraphicsExtractor g, int x, int y, int scale, int waypointColor) {
        for (int row = 0; row < 8; row++) {
            String line = rows[row];
            for (int col = 0; col < 8; col++) {
                char c = line.charAt(col);
                if (c == '.') continue;
                int argb = colorFor(c, waypointColor);
                if (argb == 0) continue;
                int px = x + col * scale;
                int py = y + row * scale;
                g.fill(px, py, px + scale, py + scale, argb);
            }
        }
    }

    public static WaypointIcon byName(String name) {
        try {
            return valueOf(name);
        } catch (Exception e) {
            return FLAG;
        }
    }
}
