package de.kryn.waypoints.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.kryn.waypoints.KrynsWaypoints;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Speichert Wegpunkte pro Welt/Server in config/kryns_waypoints.json. */
public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<LinkedHashMap<String, List<Waypoint>>>() {}.getType();

    private static Map<String, List<Waypoint>> worlds = new LinkedHashMap<>();

    private WaypointStore() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("kryns_waypoints.json");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, List<Waypoint>> loaded = GSON.fromJson(reader, TYPE);
            if (loaded != null) worlds = loaded;
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Wegpunkte nicht laden", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                GSON.toJson(worlds, TYPE, writer);
            }
        } catch (IOException e) {
            KrynsWaypoints.LOGGER.error("Konnte Wegpunkte nicht speichern", e);
        }
    }

    /** Alle Wegpunkte der aktuellen Welt bzw. des aktuellen Servers (alle Dimensionen). */
    public static List<Waypoint> forCurrentWorld() {
        return worlds.computeIfAbsent(worldKey(), k -> new ArrayList<>());
    }

    /** Nur die Wegpunkte der Dimension, in der sich der Spieler gerade befindet. */
    public static List<Waypoint> forCurrentDimension() {
        String dim = currentDimension();
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : forCurrentWorld()) {
            if (wp.dimension.equals(dim)) result.add(wp);
        }
        return result;
    }

    public static void add(Waypoint waypoint) {
        forCurrentWorld().add(waypoint);
        save();
    }

    public static void remove(Waypoint waypoint) {
        forCurrentWorld().remove(waypoint);
        save();
    }

    public static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData server = mc.getCurrentServer();
        if (server != null) {
            return "mp:" + server.ip.toLowerCase();
        }
        return "unknown";
    }

    public static String currentDimension() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "minecraft:overworld";
        // ResourceKey#toString: "ResourceKey[minecraft:dimension / minecraft:overworld]"
        String s = String.valueOf(mc.level.dimension());
        int slash = s.lastIndexOf(" / ");
        if (slash >= 0) {
            s = s.substring(slash + 3);
            if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String dimensionLabel(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "Oberwelt";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dimension.startsWith("minecraft:") ? dimension.substring(10) : dimension;
        };
    }
}
