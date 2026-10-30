package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.WaypointIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Auswahlraster für alle Symbole, als ein einziges Widget. */
public class IconGrid extends AbstractWidget {
    private final int columns;
    private final int cell;
    private final Supplier<WaypointIcon> selected;
    private final java.util.function.Consumer<WaypointIcon> onSelect;
    private final IntSupplier color;

    public IconGrid(int x, int y, int columns, int cell, Supplier<WaypointIcon> selected,
                    java.util.function.Consumer<WaypointIcon> onSelect, IntSupplier color) {
        super(x, y, columns * cell, rows(columns) * cell, Component.literal("Symbol"));
        this.columns = columns;
        this.cell = cell;
        this.selected = selected;
        this.onSelect = onSelect;
        this.color = color;
    }

    private static int rows(int columns) {
        return (WaypointIcon.all().size() + columns - 1) / columns;
    }

    private int indexAt(double mouseX, double mouseY) {
        int col = (int) ((mouseX - getX()) / cell);
        int row = (int) ((mouseY - getY()) / cell);
        if (col < 0 || col >= columns || row < 0) return -1;
        int index = row * columns + col;
        return index < WaypointIcon.all().size() ? index : -1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        List<WaypointIcon> icons = WaypointIcon.all();
        g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0x80000000);
        int hovered = isHovered() ? indexAt(mouseX, mouseY) : -1;
        int rgb = color.getAsInt();
        int iconSize = cell - 2;
        for (int i = 0; i < icons.size(); i++) {
            WaypointIcon icon = icons.get(i);
            int cx = getX() + (i % columns) * cell;
            int cy = getY() + (i / columns) * cell;
            if (icon == selected.get()) {
                g.fill(cx, cy, cx + cell, cy + cell, 0x60FFFFFF);
                g.outline(cx, cy, cell, cell, 0xFFFFFFFF);
            } else if (i == hovered) {
                g.fill(cx, cy, cx + cell, cy + cell, 0x30FFFFFF);
            }
            icon.draw(g, cx + 1, cy + 1, iconSize, rgb, 0xFF);
        }
        if (hovered >= 0) {
            g.setTooltipForNextFrame(Minecraft.getInstance().font, Component.literal(icons.get(hovered).label), mouseX, mouseY);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int index = indexAt(event.x(), event.y());
        if (index >= 0) onSelect.accept(WaypointIcon.all().get(index));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
