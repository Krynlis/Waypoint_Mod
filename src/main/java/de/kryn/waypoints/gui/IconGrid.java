package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.IconChoice;
import de.kryn.waypoints.data.IconLibrary;
import de.kryn.waypoints.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Scrollbares Auswahlraster für Symbole und Spielerköpfe.
 * Sternchen oben links: Favorit, rotes X oben rechts: Spielerkopf entfernen.
 */
public class IconGrid extends AbstractWidget {
    private static final int MARK = 6;
    private static final String[] STAR = {"..#..", ".###.", "#####", ".###.", ".#.#."};
    private static final String[] CROSS = {"#...#", ".#.#.", "..#..", ".#.#.", "#...#"};

    private final int columns;
    private final int visibleRows;
    private final int cell;
    private final Supplier<List<IconChoice>> choices;
    private final Supplier<String> selectedKey;
    private final Consumer<IconChoice> onSelect;
    private final Consumer<IconChoice> onRemoveHead;
    private final IntSupplier color;
    private int scroll;

    public IconGrid(int x, int y, int columns, int visibleRows, int cell, Supplier<List<IconChoice>> choices,
                    Supplier<String> selectedKey, Consumer<IconChoice> onSelect, Consumer<IconChoice> onRemoveHead,
                    IntSupplier color) {
        super(x, y, columns * cell + 4, visibleRows * cell, Component.literal("Symbol"));
        this.columns = columns;
        this.visibleRows = visibleRows;
        this.cell = cell;
        this.choices = choices;
        this.selectedKey = selectedKey;
        this.onSelect = onSelect;
        this.onRemoveHead = onRemoveHead;
        this.color = color;
    }

    private int maxScroll(int count) {
        return Math.max(0, (count + columns - 1) / columns - visibleRows);
    }

    private int indexAt(double mouseX, double mouseY, int count) {
        int col = (int) Math.floor((mouseX - getX()) / cell);
        int row = (int) Math.floor((mouseY - getY()) / cell);
        if (col < 0 || col >= columns || row < 0 || row >= visibleRows) return -1;
        int index = (row + scroll) * columns + col;
        return index < count ? index : -1;
    }

    private int cellX(int index) {
        return getX() + (index % columns) * cell;
    }

    private int cellY(int index) {
        return getY() + (index / columns - scroll) * cell;
    }

    private static void pixels(GuiGraphicsExtractor g, String[] pattern, int x, int y, int argb) {
        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length(); col++) {
                if (pattern[row].charAt(col) == '#') g.fill(x + col, y + row, x + col + 1, y + row + 1, argb);
            }
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        List<IconChoice> list = choices.get();
        scroll = Math.min(scroll, maxScroll(list.size()));
        Theme.box(g, getX() - 1, getY() - 1, columns * cell + 2, height + 2, Theme.FIELD, Theme.PANEL_BORDER);

        int hovered = isHovered() ? indexAt(mouseX, mouseY, list.size()) : -1;
        String selected = selectedKey.get();
        int rgb = color.getAsInt();
        int first = scroll * columns;
        int last = Math.min(list.size(), first + visibleRows * columns);
        for (int i = first; i < last; i++) {
            IconChoice choice = list.get(i);
            int cx = cellX(i);
            int cy = cellY(i);
            if (choice.key().equals(selected)) {
                g.fill(cx, cy, cx + cell, cy + cell, 0x605DADE2);
                g.outline(cx, cy, cell, cell, Theme.ACCENT);
            } else if (i == hovered) {
                g.fill(cx, cy, cx + cell, cy + cell, 0x30FFFFFF);
            }
            int pad = (cell - 16) / 2;
            choice.draw(g, cx + pad, cy + pad, 16, rgb, 0xFF);

            boolean favorite = IconLibrary.isFavorite(choice.key());
            if (favorite) {
                pixels(g, STAR, cx, cy, 0xFFF4D03F);
            } else if (i == hovered) {
                pixels(g, STAR, cx, cy, 0xFF8A8F96);
            }
            if (choice.isHead() && i == hovered) {
                pixels(g, CROSS, cx + cell - 5, cy, 0xFFE53935);
            }
        }

        // Scrollleiste
        int maxScroll = maxScroll(list.size());
        if (maxScroll > 0) {
            int trackX = getX() + columns * cell + 1;
            int barH = Math.max(8, height * visibleRows / (visibleRows + maxScroll));
            int barY = getY() + (height - barH) * scroll / maxScroll;
            g.fill(trackX, getY(), trackX + 3, getY() + height, 0xFF2A2E36);
            g.fill(trackX, barY, trackX + 3, barY + barH, Theme.MUTED);
        }

        if (hovered >= 0) {
            IconChoice choice = list.get(hovered);
            int lx = mouseX - cellX(hovered);
            int ly = mouseY - cellY(hovered);
            String tip = choice.label();
            if (lx < MARK && ly < MARK) tip = IconLibrary.isFavorite(choice.key()) ? "Favorit entfernen" : "Als Favorit markieren";
            else if (choice.isHead() && lx >= cell - MARK && ly < MARK) tip = "Spielerkopf entfernen";
            g.setTooltipForNextFrame(Minecraft.getInstance().font, Component.literal(tip), mouseX, mouseY);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        List<IconChoice> list = choices.get();
        int index = indexAt(event.x(), event.y(), list.size());
        if (index < 0) return;
        IconChoice choice = list.get(index);
        double lx = event.x() - cellX(index);
        double ly = event.y() - cellY(index);
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        if (lx < MARK && ly < MARK) {
            IconLibrary.toggleFavorite(choice.key());
        } else if (choice.isHead() && lx >= cell - MARK && ly < MARK) {
            onRemoveHead.accept(choice);
        } else {
            onSelect.accept(choice);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int max = maxScroll(choices.get().size());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
