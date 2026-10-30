package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointIcon;
import de.kryn.waypoints.data.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;


/** Wegpunkt erstellen (existing == null) oder bearbeiten. */
public class WaypointEditScreen extends Screen {
    private static final int ICON_CELL = 18;
    private static final int ICON_COLS = 12;
    private static final int COLOR_CELL = 13;
    private static final int LEFT = 166;

    private final Screen parent;
    private final Waypoint existing;

    private EditBox nameBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private String name;
    private String xText;
    private String yText;
    private String zText;
    private WaypointIcon icon;
    private int color;

    private int top;

    public WaypointEditScreen(Screen parent, Waypoint existing) {
        super(Component.literal(existing == null ? "Neuer Wegpunkt" : "Wegpunkt bearbeiten"));
        this.parent = parent;
        this.existing = existing;

        if (existing != null) {
            name = existing.name;
            xText = String.valueOf(existing.x);
            yText = String.valueOf(existing.y);
            zText = String.valueOf(existing.z);
            icon = existing.getIcon();
            color = existing.color;
        } else {
            name = "";
            LocalPlayer player = Minecraft.getInstance().player;
            int[] pos = player == null ? new int[3] : new int[]{player.getBlockX(), player.getBlockY(), player.getBlockZ()};
            xText = String.valueOf(pos[0]);
            yText = String.valueOf(pos[1]);
            zText = String.valueOf(pos[2]);
            icon = WaypointIcon.defaultIcon();
            color = ColorPalette.COLORS[ColorPalette.SIZE * 4 + 5];
        }
    }

    @Override
    protected void init() {
        // Eingaben über rebuildWidgets() (z.B. Fenstergröße) hinweg behalten
        if (nameBox != null) {
            name = nameBox.getValue();
            xText = xBox.getValue();
            yText = yBox.getValue();
            zText = zBox.getValue();
        }

        int cx = width / 2;
        top = Math.max(4, height / 2 - 116);

        nameBox = new EditBox(font, cx - 100, top + 26, 200, 18, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(name);
        nameBox.setHint(Component.literal("Name des Wegpunkts"));
        addRenderableWidget(nameBox);

        xBox = coordBox(cx - 150, xText, "X");
        yBox = coordBox(cx - 82, yText, "Y");
        zBox = coordBox(cx - 14, zText, "Z");
        addRenderableWidget(Button.builder(Component.literal("Aktuelle Pos."), b -> useCurrentPosition())
                .bounds(cx + 56, top + 58, 94, 18).build());

        int pickTop = top + 96;
        IconGrid iconGrid = addRenderableWidget(new IconGrid(cx - LEFT, pickTop, ICON_COLS, ICON_CELL,
                () -> icon, this::selectIcon, () -> color));
        addRenderableWidget(new ColorGrid(cx + LEFT - ColorPalette.SIZE * COLOR_CELL, pickTop, COLOR_CELL,
                () -> color, value -> color = value));

        int buttonsY = pickTop + Math.max(iconGrid.getHeight(), ColorPalette.SIZE * COLOR_CELL) + 8;
        addRenderableWidget(Button.builder(Component.literal("Speichern"), b -> save())
                .bounds(cx - 152, buttonsY, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> onClose())
                .bounds(cx + 2, buttonsY, 150, 20).build());

        setInitialFocus(nameBox);
    }

    /** Übernimmt den Symbolnamen als Wegpunktnamen, solange kein eigener Name eingetragen ist. */
    private void selectIcon(WaypointIcon value) {
        String current = nameBox.getValue().trim();
        if (current.isEmpty() || current.equals(icon.label)) {
            nameBox.setValue(value.label);
        }
        icon = value;
    }

    private EditBox coordBox(int x, String value, String label) {
        EditBox box = new EditBox(font, x + 12, top + 58, 52, 18, Component.literal(label));
        box.setMaxLength(9);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    private void useCurrentPosition() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        xBox.setValue(String.valueOf(player.getBlockX()));
        yBox.setValue(String.valueOf(player.getBlockY()));
        zBox.setValue(String.valueOf(player.getBlockZ()));
    }

    private static int parse(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void save() {
        LocalPlayer player = Minecraft.getInstance().player;
        Waypoint wp = existing != null ? existing : new Waypoint();
        wp.name = nameBox.getValue().trim();
        wp.x = parse(xBox.getValue(), player != null ? player.getBlockX() : 0);
        wp.y = parse(yBox.getValue(), player != null ? player.getBlockY() : 0);
        wp.z = parse(zBox.getValue(), player != null ? player.getBlockZ() : 0);
        wp.icon = icon.id;
        wp.color = color;
        if (existing == null) {
            wp.dimension = WaypointStore.currentDimension();
            WaypointStore.add(wp);
        } else {
            WaypointStore.save();
        }
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int argb = 0xFF000000 | color;

        g.centeredText(font, title, cx, top + 6, 0xFFFFFFFF);
        g.text(font, "Name", cx - 100, top + 16, 0xFFA0A0A0, false);

        // Vorschau neben dem Namen
        int px = cx + 108, py = top + 23;
        g.fill(px - 2, py - 2, px + 26, py + 26, argb);
        g.fill(px - 1, py - 1, px + 25, py + 25, 0xFF101010);
        icon.draw(g, px + 4, py + 4, 16, color, 0xFF);

        g.text(font, "Koordinaten", cx - 150, top + 48, 0xFFA0A0A0, false);
        g.text(font, "X", cx - 147, top + 63, 0xFFFFFFFF, false);
        g.text(font, "Y", cx - 79, top + 63, 0xFFFFFFFF, false);
        g.text(font, "Z", cx - 11, top + 63, 0xFFFFFFFF, false);

        g.text(font, "Symbol", cx - LEFT, top + 86, 0xFFA0A0A0, false);
        g.text(font, "Farbe", cx + LEFT - ColorPalette.SIZE * COLOR_CELL, top + 86, 0xFFA0A0A0, false);
    }
}
