package de.kryn.waypoints.data;

import de.kryn.waypoints.KrynsWaypoints;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Symbole (32x32) aus dem Textur-Atlas textures/gui/icons.png. Pixel in der Wegpunktfarbe liegen
 * weiß in icons_tint.png und werden beim Zeichnen eingefärbt.
 */
public final class WaypointIcon {
    public static final int SIZE = 32;
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath(KrynsWaypoints.MOD_ID, "textures/gui/icons.png");
    private static final Identifier TINT = Identifier.fromNamespaceAndPath(KrynsWaypoints.MOD_ID, "textures/gui/icons_tint.png");
    private static final int COLUMNS = 8;

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
    private static int atlasWidth = 256;
    private static int atlasHeight = 256;

    public final String id;
    public final String label;
    private final int u;
    private final int v;
    private final boolean tinted;

    private WaypointIcon(String id, String label, int index, boolean tinted) {
        this.id = id;
        this.label = label;
        this.u = (index % COLUMNS) * SIZE;
        this.v = (index / COLUMNS) * SIZE;
        this.tinted = tinted;
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
    public void draw(GuiGraphicsExtractor g, int x, int y, int size, int waypointRgb, int alpha) {
        int a = (alpha & 0xFF) << 24;
        g.blit(RenderPipelines.GUI_TEXTURED, ATLAS, x, y, u, v, size, size, SIZE, SIZE, atlasWidth, atlasHeight, a | 0xFFFFFF);
        if (tinted) {
            g.blit(RenderPipelines.GUI_TEXTURED, TINT, x, y, u, v, size, size, SIZE, SIZE, atlasWidth, atlasHeight,
                    a | (waypointRgb & 0xFFFFFF));
        }
    }

    public static void load() {
        try (InputStream in = WaypointIcon.class.getResourceAsStream("/assets/kryns_waypoints/icons.txt")) {
            if (in == null) throw new IllegalStateException("icons.txt fehlt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<WaypointIcon> icons = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if (parts[0].equals("atlas")) {
                    atlasWidth = Integer.parseInt(parts[1]);
                    atlasHeight = Integer.parseInt(parts[2]);
                } else if (parts[0].equals("icon")) {
                    icons.add(new WaypointIcon(parts[1], parts[3], icons.size(), parts[2].equals("1")));
                }
            }
            for (WaypointIcon icon : icons) BY_ID.put(icon.id, icon);
            all = Collections.unmodifiableList(icons);
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Symbole nicht laden", e);
        }
    }
}
