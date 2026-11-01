package de.kryn.waypoints;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.kryn.waypoints.data.IconLibrary;
import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.data.WaypointIcon;
import de.kryn.waypoints.data.WaypointStore;
import de.kryn.waypoints.gui.WaypointEditScreen;
import de.kryn.waypoints.gui.SettingsScreen;
import de.kryn.waypoints.gui.WaypointListScreen;
import de.kryn.waypoints.render.WaypointHud;
import de.kryn.waypoints.share.RelayClient;
import de.kryn.waypoints.share.SharedWaypoints;
import de.kryn.waypoints.ui.KButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KrynsWaypoints implements ClientModInitializer {
    public static final String MOD_ID = "kryns_waypoints";
    public static final Logger LOGGER = LoggerFactory.getLogger("Kryn's Waypoints");

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping createKey;

    @Override
    public void onInitializeClient() {
        WaypointIcon.load();
        IconLibrary.load();
        Settings.load();
        WaypointStore.load();

        createKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.kryns_waypoints.create", InputConstants.KEY_B, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            DeathWaypoints.tick(mc);
            SharedWaypoints.tick(mc);
            while (createKey.consumeClick()) {
                if (mc.player != null) {
                    mc.gui.setScreen(new WaypointEditScreen(null, null));
                }
            }
        });

        // Buttons "Optionen" und "Waypointliste" oben rechts im Escape-Menü
        RelayClient.start();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal(SharedWaypoints.COMMAND)
                        .then(ClientCommands.literal("accept").then(ClientCommands.argument("id", StringArgumentType.word())
                                .executes(c -> {
                                    c.getSource().sendFeedback(SharedWaypoints.accept(StringArgumentType.getString(c, "id")));
                                    return 1;
                                })))
                        .then(ClientCommands.literal("decline").then(ClientCommands.argument("id", StringArgumentType.word())
                                .executes(c -> {
                                    c.getSource().sendFeedback(SharedWaypoints.decline(StringArgumentType.getString(c, "id")));
                                    return 1;
                                })))));

        ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
            if (screen instanceof PauseScreen) {
                Screens.getWidgets(screen).add(new KButton(width - 105, 5, 100, 20, "Waypointliste",
                        () -> mc.gui.setScreen(new WaypointListScreen(screen))));
                Screens.getWidgets(screen).add(new KButton(width - 180, 5, 70, 20, "Optionen",
                        () -> mc.gui.setScreen(new SettingsScreen(screen))));
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints"), WaypointHud::render);
    }
}
