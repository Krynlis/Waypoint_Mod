package de.kryn.waypoints.gui;

/** 8x8-Farbpalette: erste Zeile Graustufen, darunter 8 Farbtöne in 7 Helligkeitsstufen. */
public final class ColorPalette {
    public static final int SIZE = 8;
    public static final int[] COLORS = new int[SIZE * SIZE];

    private static final float[] HUES = {0f, 30f, 55f, 120f, 180f, 215f, 270f, 320f};
    private static final float[] SATURATION = {0.3f, 0.55f, 0.8f, 1f, 1f, 1f, 1f};
    private static final float[] BRIGHTNESS = {1f, 1f, 1f, 1f, 0.8f, 0.6f, 0.4f};

    static {
        for (int col = 0; col < SIZE; col++) {
            int v = Math.round(255f * (1f - col / (float) (SIZE - 1)));
            COLORS[col] = (v << 16) | (v << 8) | v;
        }
        for (int row = 1; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                COLORS[row * SIZE + col] = hsbToRgb(HUES[col] / 360f, SATURATION[row - 1], BRIGHTNESS[row - 1]);
            }
        }
    }

    private ColorPalette() {}

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        float r, g, b;
        switch ((int) h) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }
}
