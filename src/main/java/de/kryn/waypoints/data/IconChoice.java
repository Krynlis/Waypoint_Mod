package de.kryn.waypoints.data;

import de.kryn.waypoints.head.FaceRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Ein Eintrag der Symbolauswahl: eingebautes Symbol oder Spielerkopf. */
public record IconChoice(String key, String label, WaypointIcon icon, int[] face) {
    public static final String HEAD_PREFIX = "head:";

    public static IconChoice of(WaypointIcon icon) {
        return new IconChoice(icon.id, icon.label, icon, null);
    }

    public static IconChoice head(String name, int[] face) {
        return new IconChoice(HEAD_PREFIX + name, name, null, face);
    }

    public boolean isHead() {
        return face != null;
    }

    public void draw(GuiGraphicsExtractor g, int x, int y, int size, int waypointRgb, int alpha) {
        if (face != null) {
            FaceRenderer.draw(g, face, x, y, size, alpha);
        } else {
            icon.draw(g, x, y, size, waypointRgb, alpha);
        }
    }
}
