package de.kryn.waypoints.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Einheitliche Farben und Grundformen für alle Menüs. */
public final class Theme {
    public static final int DIM = 0xA0000000;
    public static final int PANEL = 0xF01B1E24;
    public static final int PANEL_BORDER = 0xFF3A404C;
    public static final int HEADER = 0xFF23272F;
    public static final int BUTTON = 0xFF2B303A;
    public static final int BUTTON_HOVER = 0xFF3B4252;
    public static final int BUTTON_DISABLED = 0xFF202329;
    public static final int FIELD = 0xFF111317;
    public static final int ROW = 0xFF22262E;
    public static final int ACCENT = 0xFF5DADE2;
    public static final int DANGER = 0xFFB03A2E;
    public static final int DANGER_HOVER = 0xFFD0453A;
    public static final int TEXT = 0xFFE8EAED;
    public static final int MUTED = 0xFF9AA0A6;
    public static final int DISABLED_TEXT = 0xFF5F6670;

    private Theme() {}

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, PANEL_BORDER);
    }

    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.outline(x, y, w, h, border);
    }
}
