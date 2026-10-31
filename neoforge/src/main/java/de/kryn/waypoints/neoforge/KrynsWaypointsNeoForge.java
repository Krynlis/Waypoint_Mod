package de.kryn.waypoints.neoforge;

import de.kryn.waypoints.KrynsWaypoints;
import de.kryn.waypoints.render.WaypointHud;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = KrynsWaypoints.MOD_ID, dist = Dist.CLIENT)
public class KrynsWaypointsNeoForge {
    public KrynsWaypointsNeoForge(IEventBus modBus) {
        KrynsWaypoints.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(KrynsWaypoints.CATEGORY);
            event.register(KrynsWaypoints.CREATE_KEY);
        });
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAboveAll(KrynsWaypoints.HUD_ID, WaypointHud::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                KrynsWaypoints.onClientTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) ->
                KrynsWaypoints.onScreenInit(Minecraft.getInstance(), event.getScreen(), event.getScreen().width,
                        event::addListener));
    }
}
