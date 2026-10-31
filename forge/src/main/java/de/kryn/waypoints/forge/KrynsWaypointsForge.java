package de.kryn.waypoints.forge;

import de.kryn.waypoints.KrynsWaypoints;
import de.kryn.waypoints.render.WaypointHud;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(KrynsWaypoints.MOD_ID)
public class KrynsWaypointsForge {
    public KrynsWaypointsForge(FMLJavaModLoadingContext context) {
        KrynsWaypoints.init(FMLPaths.CONFIGDIR.get());
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(KrynsWaypoints.CREATE_KEY));
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(KrynsWaypoints.HUD_ID, WaypointHud::render));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> KrynsWaypoints.onClientTick(Minecraft.getInstance()));
        ScreenEvent.Init.Post.BUS.addListener(event ->
                KrynsWaypoints.onScreenInit(Minecraft.getInstance(), event.getScreen(), event.getScreen().width,
                        event::addListener));
    }
}
