package de.kryn.waypoints.data;

import de.kryn.waypoints.head.FaceRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Waypoint {
    public String name = "";
    public int x;
    public int y;
    public int z;
    /** Dimension-ID, z.B. "minecraft:overworld". */
    public String dimension = "minecraft:overworld";
    public String icon = "house";
    /** RGB ohne Alpha. */
    public int color = 0xFFFFFF;
    public boolean visible = true;
    /** Spielername, falls ein Spielerkopf als Symbol dient. */
    public String playerHead;
    /** 8x8-Gesicht (ARGB) des Spielerkopfs, wird mitgespeichert, damit es auch offline angezeigt wird. */
    public int[] face;

    public WaypointIcon getIcon() {
        return WaypointIcon.byId(icon);
    }

    public boolean hasFace() {
        return face != null && face.length == 64;
    }

    /** Zeichnet Spielerkopf oder Symbol. */
    public void drawIcon(GuiGraphicsExtractor g, int x, int y, int size, int alpha) {
        if (hasFace()) {
            FaceRenderer.draw(g, face, x, y, size, alpha);
        } else {
            getIcon().draw(g, x, y, size, color, alpha);
        }
    }

    public String displayName() {
        if (!name.isEmpty()) return name;
        return hasFace() && playerHead != null ? playerHead : getIcon().label;
    }

    public int argb() {
        return 0xFF000000 | color;
    }
}
