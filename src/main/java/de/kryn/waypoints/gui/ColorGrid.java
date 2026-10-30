package de.kryn.waypoints.gui;

import de.kryn.waypoints.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** 8x8-Farbwahl als ein einziges Widget. */
public class ColorGrid extends AbstractWidget {
    private final int cell;
    private final IntSupplier selected;
    private final IntConsumer onSelect;

    public ColorGrid(int x, int y, int cell, IntSupplier selected, IntConsumer onSelect) {
        super(x, y, ColorPalette.SIZE * cell, ColorPalette.SIZE * cell, Component.literal("Farbe"));
        this.cell = cell;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    private int indexAt(double mouseX, double mouseY) {
        int col = (int) ((mouseX - getX()) / cell);
        int row = (int) ((mouseY - getY()) / cell);
        if (col < 0 || col >= ColorPalette.SIZE || row < 0 || row >= ColorPalette.SIZE) return -1;
        return row * ColorPalette.SIZE + col;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Theme.box(g, getX() - 1, getY() - 1, width + 2, height + 2, 0xFF000000, Theme.PANEL_BORDER);
        int hovered = isHovered() ? indexAt(mouseX, mouseY) : -1;
        int selectedIndex = -1;
        for (int i = 0; i < ColorPalette.COLORS.length; i++) {
            int cx = getX() + (i % ColorPalette.SIZE) * cell;
            int cy = getY() + (i / ColorPalette.SIZE) * cell;
            g.fill(cx, cy, cx + cell, cy + cell, 0xFF000000 | ColorPalette.COLORS[i]);
            if (ColorPalette.COLORS[i] == selected.getAsInt()) selectedIndex = i;
        }
        if (hovered >= 0) {
            g.outline(getX() + (hovered % ColorPalette.SIZE) * cell, getY() + (hovered / ColorPalette.SIZE) * cell, cell, cell, 0xFF808080);
        }
        if (selectedIndex >= 0) {
            int cx = getX() + (selectedIndex % ColorPalette.SIZE) * cell;
            int cy = getY() + (selectedIndex / ColorPalette.SIZE) * cell;
            g.outline(cx - 1, cy - 1, cell + 2, cell + 2, 0xFF000000);
            g.outline(cx, cy, cell, cell, Theme.TEXT);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int index = indexAt(event.x(), event.y());
        if (index >= 0) onSelect.accept(ColorPalette.COLORS[index]);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
