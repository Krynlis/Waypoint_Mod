package de.kryn.waypoints.data;

import de.kryn.waypoints.KrynsWaypoints;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Symbole als 32x32-Pixelgrafik, geladen aus assets/kryns_waypoints/icons.txt.
 * '.' ist transparent, '|' wird in der Farbe des Wegpunkts gezeichnet.
 */
public final class WaypointIcon {
    public static final int SIZE = 32;
    private static final int WAYPOINT_COLOR = -1;

    /** Alte Symbolnamen (Version 1.0) auf die neuen IDs abbilden. */
    private static final Map<String, String> LEGACY = Map.ofEntries(
            Map.entry("HOUSE", "house"), Map.entry("BED", "bed"), Map.entry("ANIMAL", "pig"),
            Map.entry("HEART", "heart"), Map.entry("FRIENDS", "friends"), Map.entry("FRIEND_1", "friends"),
            Map.entry("FRIEND_2", "friends"), Map.entry("FRIEND_3", "friends"), Map.entry("STAR", "star"),
            Map.entry("FLAG", "flag"), Map.entry("CHEST", "chest"), Map.entry("MINE", "pickaxe"),
            Map.entry("DIAMOND", "diamond"), Map.entry("DANGER", "sword"), Map.entry("DEATH", "skull"),
            Map.entry("PORTAL", "netherportal"), Map.entry("TREE", "tree"), Map.entry("BELL", "bell"),
            Map.entry("WHEAT", "wheat"));

    private static final Map<String, WaypointIcon> BY_ID = new LinkedHashMap<>();
    private static List<WaypointIcon> all = List.of();

    public final String id;
    public final String label;
    /** Je Zeile zusammengefasste Pixel: x0, x1 (exklusiv), y, Farbe. */
    private final int[] runs;

    private WaypointIcon(String id, String label, int[] runs) {
        this.id = id;
        this.label = label;
        this.runs = runs;
    }

    public static List<WaypointIcon> all() {
        return all;
    }

    public static WaypointIcon byId(String id) {
        if (id != null && LEGACY.containsKey(id)) id = LEGACY.get(id);
        WaypointIcon icon = BY_ID.get(id);
        if (icon != null) return icon;
        icon = BY_ID.get("flag");
        return icon != null ? icon : all.get(0);
    }

    public static WaypointIcon defaultIcon() {
        return byId("house");
    }

    /**
     * Zeichnet das Symbol mit linker oberer Ecke (x, y) in der Größe size x size.
     * waypointRgb ist die Wegpunktfarbe (ohne Alpha), alpha 0-255 die Deckkraft.
     */
    public void draw(GuiGraphicsExtractor g, int x, int y, float size, int waypointRgb, int alpha) {
        int a = (alpha & 0xFF) << 24;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / SIZE);
        for (int i = 0; i < runs.length; i += 4) {
            int rgb = runs[i + 3] == WAYPOINT_COLOR ? waypointRgb : runs[i + 3];
            g.fill(runs[i], runs[i + 2], runs[i + 1], runs[i + 2] + 1, a | (rgb & 0xFFFFFF));
        }
        g.pose().popMatrix();
    }

    public static void load() {
        try (InputStream in = WaypointIcon.class.getResourceAsStream("/assets/kryns_waypoints/icons.txt")) {
            if (in == null) throw new IllegalStateException("icons.txt fehlt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            Map<Character, Integer> palette = new HashMap<>();
            palette.put('|', WAYPOINT_COLOR);
            List<WaypointIcon> icons = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("color ")) {
                    String[] parts = line.split(" ");
                    palette.put(parts[1].charAt(0), Integer.parseInt(parts[2], 16));
                } else if (line.startsWith("icon ")) {
                    String[] parts = line.split(" ", 3);
                    List<Integer> runs = new ArrayList<>();
                    for (int y = 0; y < SIZE; y++) {
                        String row = reader.readLine();
                        int x = 0;
                        while (x < SIZE) {
                            char c = row.charAt(x);
                            int end = x + 1;
                            while (end < SIZE && row.charAt(end) == c) end++;
                            if (c != '.') {
                                runs.add(x);
                                runs.add(end);
                                runs.add(y);
                                runs.add(palette.getOrDefault(c, 0xFF00FF));
                            }
                            x = end;
                        }
                    }
                    icons.add(new WaypointIcon(parts[1], parts[2], runs.stream().mapToInt(Integer::intValue).toArray()));
                }
            }
            for (WaypointIcon icon : icons) BY_ID.put(icon.id, icon);
            all = Collections.unmodifiableList(icons);
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Symbole nicht laden", e);
        }
    }
}
