package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.IconChoice;
import de.kryn.waypoints.data.IconLibrary;
import de.kryn.waypoints.head.FaceRenderer;
import de.kryn.waypoints.head.PlayerHeads;
import de.kryn.waypoints.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Köpfe aller Spieler, die gerade auf dem Server sind. Grünes Plus oben rechts: in die Symbolliste aufnehmen. */
public class PlayerHeadGrid extends AbstractWidget {
    private static final int MARK = 6;
    private static final String[] PLUS = {"..#..", "..#..", "#####", "..#..", "..#.."};

    private final int columns;
    private final int visibleRows;
    private final int cell;
    private final Supplier<List<String>> names;
    private final Supplier<String> selectedKey;
    private final Consumer<IconChoice> onSelect;
    private final Consumer<IconChoice> onAdd;
    private int scroll;

    public PlayerHeadGrid(int x, int y, int columns, int visibleRows, int cell, Supplier<List<String>> names,
                          Supplier<String> selectedKey, Consumer<IconChoice> onSelect, Consumer<IconChoice> onAdd) {
        super(x, y, columns * cell + 4, visibleRows * cell, Component.literal("Spieler online"));
        this.columns = columns;
        this.visibleRows = visibleRows;
        this.cell = cell;
        this.names = names;
        this.selectedKey = selectedKey;
        this.onSelect = onSelect;
        this.onAdd = onAdd;
    }

    /** Namen der Spieler in der Tab-Liste, alphabetisch. */
    public static List<String> onlinePlayers() {
        List<String> result = new ArrayList<>();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return result;
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (PlayerHeads.isValidName(name)) result.add(name);
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
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

    private static boolean inLibrary(String name) {
        return IconLibrary.findHead(name) != null;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        List<String> list = names.get();
        scroll = Math.min(scroll, maxScroll(list.size()));
        Theme.box(g, getX() - 1, getY() - 1, columns * cell + 2, height + 2, Theme.FIELD, Theme.PANEL_BORDER);
        var font = Minecraft.getInstance().font;
        if (list.isEmpty()) {
            g.centeredText(font, "Keine Spieler gefunden", getX() + columns * cell / 2, getY() + height / 2 - 4, Theme.MUTED);
            return;
        }

        int hovered = isHovered() ? indexAt(mouseX, mouseY, list.size()) : -1;
        String selected = selectedKey.get();
        int first = scroll * columns;
        int last = Math.min(list.size(), first + visibleRows * columns);
        for (int i = first; i < last; i++) {
            String name = list.get(i);
            int cx = cellX(i);
            int cy = cellY(i);
            // Nur sichtbare Köpfe werden geladen
            int[] face = PlayerHeads.request(name);
            if ((IconChoice.HEAD_PREFIX + name).equals(selected)) {
                g.fill(cx, cy, cx + cell, cy + cell, 0x605DADE2);
                g.outline(cx, cy, cell, cell, Theme.ACCENT);
            } else if (i == hovered) {
                g.fill(cx, cy, cx + cell, cy + cell, 0x30FFFFFF);
            }
            int pad = (cell - 16) / 2;
            if (face != null) {
                FaceRenderer.draw(g, face, cx + pad, cy + pad, 16, 0xFF);
            } else {
                g.fill(cx + pad, cy + pad, cx + pad + 16, cy + pad + 16, 0xFF2A2E36);
                g.centeredText(font, PlayerHeads.isLoading(name) ? "…" : "?", cx + cell / 2, cy + pad + 4, Theme.DISABLED_TEXT);
            }
            if (i == hovered && face != null && !inLibrary(name)) {
                for (int row = 0; row < PLUS.length; row++) {
                    for (int col = 0; col < PLUS[row].length(); col++) {
                        if (PLUS[row].charAt(col) == '#') {
                            g.fill(cx + cell - 5 + col, cy + row, cx + cell - 4 + col, cy + row + 1, 0xFF43C463);
                        }
                    }
                }
            }
        }

        int maxScroll = maxScroll(list.size());
        if (maxScroll > 0) {
            int trackX = getX() + columns * cell + 1;
            int barH = Math.max(8, height * visibleRows / (visibleRows + maxScroll));
            int barY = getY() + (height - barH) * scroll / maxScroll;
            g.fill(trackX, getY(), trackX + 3, getY() + height, 0xFF2A2E36);
            g.fill(trackX, barY, trackX + 3, barY + barH, Theme.MUTED);
        }

        if (hovered >= 0) {
            String name = list.get(hovered);
            String tip = name;
            if (mouseX - cellX(hovered) >= cell - MARK && mouseY - cellY(hovered) < MARK && !inLibrary(name)) {
                tip = "Zur Symbolliste hinzufügen";
            } else if (inLibrary(name)) {
                tip = name + " (in der Symbolliste)";
            }
            g.setTooltipForNextFrame(font, Component.literal(tip), mouseX, mouseY);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        List<String> list = names.get();
        int index = indexAt(event.x(), event.y(), list.size());
        if (index < 0) return;
        String name = list.get(index);
        int[] face = PlayerHeads.request(name);
        if (face == null) return;
        IconChoice head = IconChoice.head(name, face);
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        if (event.x() - cellX(index) >= cell - MARK && event.y() - cellY(index) < MARK && !inLibrary(name)) {
            onAdd.accept(head);
        } else {
            onSelect.accept(head);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int max = maxScroll(names.get().size());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
