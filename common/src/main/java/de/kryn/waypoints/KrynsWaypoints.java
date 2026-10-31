package de.kryn.waypoints;

import com.mojang.blaze3d.platform.InputConstants;
import de.kryn.waypoints.data.IconLibrary;
import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.data.WaypointIcon;
import de.kryn.waypoints.data.WaypointStore;
import de.kryn.waypoints.gui.SettingsScreen;
import de.kryn.waypoints.gui.WaypointEditScreen;
import de.kryn.waypoints.gui.WaypointListScreen;
import de.kryn.waypoints.ui.KButton;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Gemeinsamer Kern für Fabric, NeoForge und Forge. Die Loader-Einstiegspunkte rufen
 * {@link #init(Path)} auf, registrieren {@link #CREATE_KEY} und leiten Tick, Menüaufbau und HUD hierher weiter.
 */
public final class KrynsWaypoints {
    public static final String MOD_ID = "kryns_waypoints";
    public static final Logger LOGGER = LoggerFactory.getLogger("Kryn's Waypoints");
    public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "waypoints");

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    public static final KeyMapping CREATE_KEY =
            new KeyMapping("key.kryns_waypoints.create", InputConstants.KEY_B, CATEGORY);

    private static Path configDir = Path.of("config");

    private KrynsWaypoints() {}

    public static Path configDir() {
        return configDir;
    }

    public static void init(Path loaderConfigDir) {
        configDir = loaderConfigDir;
        WaypointIcon.load();
        IconLibrary.load();
        Settings.load();
        WaypointStore.load();
    }

    /** Am Ende jedes Client-Ticks. */
    public static void onClientTick(Minecraft mc) {
        DeathWaypoints.tick(mc);
        while (CREATE_KEY.consumeClick()) {
            if (mc.player != null) {
                mc.gui.setScreen(new WaypointEditScreen(null, null));
            }
        }
    }

    /** Nach dem Aufbau eines Menüs: Buttons "Optionen" und "Waypointliste" oben rechts im Escape-Menü. */
    public static void onScreenInit(Minecraft mc, Screen screen, int width, Consumer<AbstractWidget> addWidget) {
        if (screen instanceof PauseScreen) {
            addWidget.accept(new KButton(width - 105, 5, 100, 20, "Waypointliste",
                    () -> mc.gui.setScreen(new WaypointListScreen(screen))));
            addWidget.accept(new KButton(width - 180, 5, 70, 20, "Optionen",
                    () -> mc.gui.setScreen(new SettingsScreen(screen))));
        }
    }
}
