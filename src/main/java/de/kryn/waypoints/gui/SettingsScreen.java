package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.ui.KButton;
import de.kryn.waypoints.ui.KCheckbox;
import de.kryn.waypoints.ui.KScreen;
import de.kryn.waypoints.ui.KSlider;
import net.minecraft.client.gui.screens.Screen;

public class SettingsScreen extends KScreen {
    private static final int DISTANCE_STEPS = (Settings.MAX_DISTANCE - Settings.MIN_DISTANCE) / Settings.DISTANCE_STEP;

    public SettingsScreen(Screen parent) {
        super("Kryn´s Waypoints – Optionen", parent);
    }

    @Override
    protected void layout() {
        Settings settings = Settings.get();
        int w = 240;
        int x = width / 2 - w / 2;
        int y = height / 2 - 75;
        setPanel(x - 12, y, w + 24, 150);

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
        addRenderableWidget(new KButton(width / 2 - 60, y + 124, 120, 20, "Fertig", this::onClose));
    }

    private static int distance(int step) {
        return Settings.MIN_DISTANCE + step * Settings.DISTANCE_STEP;
    }

    @Override
    public void onClose() {
        Settings.save();
        super.onClose();
    }
}
