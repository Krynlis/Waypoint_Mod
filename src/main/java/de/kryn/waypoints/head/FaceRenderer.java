package de.kryn.waypoints.head;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Zeichnet ein 8x8-Gesicht (ARGB) skaliert auf size x size. */
public final class FaceRenderer {
    private FaceRenderer() {}

    public static void draw(GuiGraphicsExtractor g, int[] face, int x, int y, int size, int alpha) {
        int a = (alpha & 0xFF) << 24;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 8f);
        for (int row = 0; row < 8; row++) {
            int col = 0;
            while (col < 8) {
                int argb = face[row * 8 + col];
                int end = col + 1;
                while (end < 8 && face[row * 8 + end] == argb) end++;
                g.fill(col, row, end, row + 1, a | (argb & 0xFFFFFF));
                col = end;
            }
        }
        g.pose().popMatrix();
    }
}
