package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.share.RelayClient;
import de.kryn.waypoints.ui.KButton;
import de.kryn.waypoints.ui.KCheckbox;
import de.kryn.waypoints.ui.KScreen;
import de.kryn.waypoints.ui.KSlider;
import de.kryn.waypoints.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public class SettingsScreen extends KScreen {
    private static final int DISTANCE_STEPS = (Settings.MAX_DISTANCE - Settings.MIN_DISTANCE) / Settings.DISTANCE_STEP;

    private EditBox relayField;

    public SettingsScreen(Screen parent) {
        super("Kryn´s Waypoints – Optionen", parent);
    }

    @Override
    protected void layout() {
        Settings settings = Settings.get();
        int w = 240;
        int x = width / 2 - w / 2;
        int y = height / 2 - 112;
        setPanel(x - 12, y, w + 24, 224);

        addRenderableWidget(new KSlider(x, y + 30, w, 20, DISTANCE_STEPS,
                (settings.switchDistance - Settings.MIN_DISTANCE) / Settings.DISTANCE_STEP,
                step -> "XP-Leiste ab: " + distance(step) + " Blöcke",
                step -> settings.switchDistance = distance(step)));
        addRenderableWidget(new KSlider(x, y + 56, w, 20, Settings.MARKER_SIZES.length - 1, settings.markerSize,
                step -> "Markergröße: " + Settings.MARKER_SIZE_NAMES[step],
                step -> settings.markerSize = step));
        addRenderableWidget(new KCheckbox(x, y + 84, w, "Marker 50% transparent", settings.transparent,
                value -> settings.transparent = value));
        addRenderableWidget(new KCheckbox(x, y + 102, w, "Todespunkt automatisch speichern", settings.deathWaypoints,
                value -> settings.deathWaypoints = value));
        addRenderableWidget(new KCheckbox(x, y + 122, w, "Wegpunkte teilen", settings.sharing,
                value -> settings.sharing = value));
        relayField = field(x, y + 154, w, 18, settings.relayUrl, 200, "z.B. 123.45.67.89:25570");
        addRenderableWidget(new KButton(width / 2 - 60, y + 198, 120, 20, "Fertig", this::onClose));
    }

    private static int distance(int step) {
        return Settings.MIN_DISTANCE + step * Settings.DISTANCE_STEP;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = width / 2 - 120;
        int y = height / 2 - 112;
        g.text(font, "Vermittlungsdienst (Adresse:Port)", x, y + 142, Theme.MUTED, false);
        g.text(font, "Status: " + RelayClient.status(), x, y + 178,
                RelayClient.isConnected() ? Theme.ACCENT : Theme.MUTED, false);
    }

    @Override
    public void onClose() {
        Settings.get().relayUrl = relayField.getValue().trim();
        Settings.save();
        super.onClose();
    }
}
