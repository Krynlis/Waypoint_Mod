package de.kryn.waypoints.fabric;

import de.kryn.waypoints.KrynsWaypoints;
import de.kryn.waypoints.render.WaypointHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;

public class KrynsWaypointsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KrynsWaypoints.init(FabricLoader.getInstance().getConfigDir());
        KeyMappingHelper.registerKeyMapping(KrynsWaypoints.CREATE_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(KrynsWaypoints::onClientTick);
        ScreenEvents.AFTER_INIT.register((mc, screen, width, height) ->
                KrynsWaypoints.onScreenInit(mc, screen, width, Screens.getWidgets(screen)::add));
        HudElementRegistry.addLast(KrynsWaypoints.HUD_ID, WaypointHud::render);
    }
}
