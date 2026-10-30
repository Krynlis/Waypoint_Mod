package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.IconChoice;
import de.kryn.waypoints.data.IconLibrary;
import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointIcon;
import de.kryn.waypoints.data.WaypointStore;
import de.kryn.waypoints.head.PlayerHeads;
import de.kryn.waypoints.ui.KButton;
import de.kryn.waypoints.ui.KScreen;
import de.kryn.waypoints.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

/**
 * Wegpunkt erstellen (existing == null) oder bearbeiten.
 * Ist der Name ein Minecraft-Spielername, wird dessen Kopf automatisch gesucht und als Vorschau angezeigt;
 * über "Hinzufügen" landet er in der Symbolliste. Auf Servern gibt es zusätzlich die Köpfe der Spieler online.
 */
public class WaypointEditScreen extends KScreen {
    private static final int ICON_CELL = 20;
    private static final int ICON_COLS = 12;
    private static final int ICON_ROWS = 6;
    private static final int COLOR_CELL = 13;
    private static final int LEFT = 178;
    private static final int PANEL_HEIGHT = 266;
    /** Wartezeit nach dem letzten Tastendruck, bevor ein Spielerkopf gesucht wird. */
    private static final long LOOKUP_DELAY_MS = 300;

    private final Waypoint existing;

    private EditBox nameBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private String name;
    private String xText;
    private String yText;
    private String zText;
    private IconChoice choice;
    private IconChoice lastBuiltinChoice;
    private int color;

    /** Kopf zum aktuell eingetippten Namen (Vorschau), noch nicht in der Liste. */
    private IconChoice previewHead;
    private KButton addHeadButton;
    private String lastText;
    private long changedAt;
    private boolean showOnlinePlayers;

    private int top;

    public WaypointEditScreen(Screen parent, Waypoint existing) {
        super(existing == null ? "Neuer Wegpunkt" : "Wegpunkt bearbeiten", parent);
        this.existing = existing;

        if (existing != null) {
            name = existing.name;
            xText = String.valueOf(existing.x);
            yText = String.valueOf(existing.y);
            zText = String.valueOf(existing.z);
            color = existing.color;
            lastBuiltinChoice = IconChoice.of(existing.getIcon());
            choice = existing.hasFace() && existing.playerHead != null
                    ? IconChoice.head(existing.playerHead, existing.face)
                    : lastBuiltinChoice;
        } else {
            name = "";
            LocalPlayer player = Minecraft.getInstance().player;
            xText = String.valueOf(player == null ? 0 : player.getBlockX());
            yText = String.valueOf(player == null ? 0 : player.getBlockY());
            zText = String.valueOf(player == null ? 0 : player.getBlockZ());
            color = ColorPalette.COLORS[ColorPalette.SIZE * 4 + 5];
            lastBuiltinChoice = IconChoice.of(WaypointIcon.defaultIcon());
            choice = lastBuiltinChoice;
        }
        lastText = "";
    }

    @Override
    protected void layout() {
        // Eingaben über rebuildWidgets() (z.B. Fenstergröße) hinweg behalten
        if (nameBox != null) {
            name = nameBox.getValue();
            xText = xBox.getValue();
            yText = yBox.getValue();
            zText = zBox.getValue();
        }

        int cx = width / 2;
        top = Math.max(2, (height - PANEL_HEIGHT) / 2);
        setPanel(cx - LEFT - 8, top, 2 * LEFT + 16, PANEL_HEIGHT);
        int left = cx - LEFT;

        nameBox = field(left, top + 36, 180, 18, name, 32, "Name oder Spielername");
        addHeadButton = addRenderableWidget(new KButton(left + 184, top + 56, 66, 14, "Hinzufügen", this::addPreviewHead));
        updateAddButton();

        xBox = coordField(left + 10, xText);
        yBox = coordField(left + 92, yText);
        zBox = coordField(left + 174, zText);
        addRenderableWidget(new KButton(left + 256, top + 82, 100, 18, "Aktuelle Pos.", this::useCurrentPosition));

        int pickTop = top + 114;
        IconGrid iconGrid = addRenderableWidget(new IconGrid(left, pickTop, ICON_COLS, ICON_ROWS, ICON_CELL,
                IconLibrary::choices, () -> choice.key(), this::selectChoice, this::removeHead, () -> color));
        PlayerHeadGrid playerGrid = addRenderableWidget(new PlayerHeadGrid(left, pickTop, ICON_COLS, ICON_ROWS, ICON_CELL,
                PlayerHeadGrid::onlinePlayers, () -> choice.key(), this::selectChoice, this::addHead));
        boolean onServer = Minecraft.getInstance().getSingleplayerServer() == null;
        if (!onServer) showOnlinePlayers = false;
        iconGrid.visible = !showOnlinePlayers;
        playerGrid.visible = showOnlinePlayers;
        KButton iconsTab = addRenderableWidget(new KButton(left, top + 100, 70, 12, "Symbole", () -> {
            showOnlinePlayers = false;
            rebuildWidgets();
        }));
        iconsTab.selected = !showOnlinePlayers;
        if (onServer) {
            KButton playersTab = addRenderableWidget(new KButton(left + 72, top + 100, 90, 12, "Spieler online", () -> {
                showOnlinePlayers = true;
                rebuildWidgets();
            }));
            playersTab.selected = showOnlinePlayers;
        }
        addRenderableWidget(new ColorGrid(cx + LEFT - ColorPalette.SIZE * COLOR_CELL, pickTop, COLOR_CELL,
                () -> color, value -> color = value));

        int buttonsY = top + PANEL_HEIGHT - 26;
        addRenderableWidget(new KButton(cx - 152, buttonsY, 150, 20, "Speichern", this::save));
        addRenderableWidget(new KButton(cx + 2, buttonsY, 150, 20, "Abbrechen", this::onClose));

        setInitialFocus(nameBox);
    }

    private EditBox coordField(int x, String value) {
        return field(x, top + 82, 70, 18, value, 9, "");
    }

    private void selectChoice(IconChoice value) {
        String current = nameBox.getValue().trim();
        if (current.isEmpty() || current.equals(choice.label())) {
            nameBox.setValue(value.label());
            lastText = value.label();
        }
        choice = value;
        if (!value.isHead()) lastBuiltinChoice = value;
    }

    private void removeHead(IconChoice head) {
        IconLibrary.removeHead(head.label());
        if (choice.key().equals(head.key())) choice = lastBuiltinChoice;
        updateAddButton();
    }

    private void addHead(IconChoice head) {
        IconLibrary.addHead(head.label(), head.face());
        updateAddButton();
    }

    private void addPreviewHead() {
        if (previewHead == null) return;
        IconLibrary.addHead(previewHead.label(), previewHead.face());
        selectChoice(previewHead);
        updateAddButton();
    }

    private void updateAddButton() {
        if (addHeadButton == null) return;
        boolean known = previewHead != null && IconLibrary.findHead(previewHead.label()) != null;
        addHeadButton.active = previewHead != null && !(known && choice.key().equals(previewHead.key()));
        addHeadButton.setLabel(known ? "Auswählen" : "Hinzufügen");
    }

    @Override
    public void tick() {
        super.tick();
        String text = nameBox.getValue().trim();
        long now = System.currentTimeMillis();
        if (!text.equals(lastText)) {
            lastText = text;
            changedAt = now;
            previewHead = null;
        }
        if (previewHead == null && now - changedAt >= LOOKUP_DELAY_MS && PlayerHeads.isValidName(text)) {
            IconChoice saved = IconLibrary.findHead(text);
            int[] face = saved != null ? saved.face() : PlayerHeads.request(text);
            if (face != null) previewHead = saved != null ? saved : IconChoice.head(text, face);
        }
        updateAddButton();
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
            return Integer.parseInt(text.trim());
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
        wp.color = color;
        if (choice.isHead()) {
            wp.playerHead = choice.label();
            wp.face = choice.face();
            wp.icon = lastBuiltinChoice.key();
        } else {
            wp.playerHead = null;
            wp.face = null;
            wp.icon = choice.key();
        }
        if (existing == null) {
            wp.dimension = WaypointStore.currentDimension();
            WaypointStore.add(wp);
        } else {
            WaypointStore.save();
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int left = cx - LEFT;

        g.text(font, "Name", left, top + 26, Theme.MUTED, false);

        // Spielerkopf-Vorschau neben dem Namen, darunter "Hinzufügen"
        String text = nameBox.getValue().trim();
        int hx = left + 203, hy = top + 24;
        Theme.box(g, hx, hy, 28, 28, Theme.FIELD, previewHead != null ? Theme.ACCENT : Theme.PANEL_BORDER);
        if (previewHead != null) {
            previewHead.draw(g, hx + 2, hy + 2, 24, color, 0xFF);
        } else {
            g.centeredText(font, PlayerHeads.isLoading(text) ? "…" : "?", hx + 14, hy + 10, Theme.DISABLED_TEXT);
        }
        String status = "";
        if (previewHead != null) status = "Spielerkopf gefunden";
        else if (PlayerHeads.isLoading(text)
                || (PlayerHeads.isValidName(text) && System.currentTimeMillis() - changedAt < LOOKUP_DELAY_MS)) {
            status = "Suche Spielerkopf …";
        }
        g.text(font, status, left, top + 58, previewHead != null ? Theme.ACCENT : Theme.MUTED, false);

        // Vorschau rechts neben dem Namen
        int size = 32;
        int px = cx + LEFT - size - 8, py = top + 28;
        Theme.box(g, px - 4, py - 4, size + 8, size + 8, Theme.FIELD, 0xFF000000 | color);
        choice.draw(g, px, py, size, color, 0xFF);

        g.text(font, "Koordinaten", left, top + 72, Theme.MUTED, false);
        g.text(font, "X", left, top + 87, Theme.TEXT, false);
        g.text(font, "Y", left + 82, top + 87, Theme.TEXT, false);
        g.text(font, "Z", left + 164, top + 87, Theme.TEXT, false);

        g.text(font, showOnlinePlayers ? "+ = hinzufügen" : "☆ = Favorit",
                left + 168, top + 102, Theme.MUTED, false);
        g.text(font, "Farbe", cx + LEFT - ColorPalette.SIZE * COLOR_CELL, top + 104, Theme.MUTED, false);
    }
}
