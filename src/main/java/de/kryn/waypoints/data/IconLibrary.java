package de.kryn.waypoints.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.kryn.waypoints.KrynsWaypoints;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Favoriten und gespeicherte Spielerköpfe in config/kryns_waypoints_icons.json. */
public final class IconLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();

    private static final class Data {
        Set<String> favorites = new LinkedHashSet<>();
        List<Head> heads = new ArrayList<>();
    }

    private static final class Head {
        String name;
        int[] face;

        Head(String name, int[] face) {
            this.name = name;
            this.face = face;
        }
    }

    private IconLibrary() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("kryns_waypoints_icons.json");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                if (loaded.favorites == null) loaded.favorites = new LinkedHashSet<>();
                if (loaded.heads == null) loaded.heads = new ArrayList<>();
                loaded.heads.removeIf(h -> h.name == null || h.face == null || h.face.length != 64);
                data = loaded;
            }
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Symbolbibliothek nicht laden", e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Symbolbibliothek nicht speichern", e);
        }
    }

    public static boolean isFavorite(String key) {
        return data.favorites.contains(key);
    }

    public static void toggleFavorite(String key) {
        if (!data.favorites.remove(key)) data.favorites.add(key);
        save();
    }

    public static IconChoice findHead(String name) {
        for (Head head : data.heads) {
            if (head.name.equalsIgnoreCase(name)) return IconChoice.head(head.name, head.face);
        }
        return null;
    }

    public static void addHead(String name, int[] face) {
        data.heads.removeIf(h -> h.name.equalsIgnoreCase(name));
        data.heads.add(0, new Head(name, face));
        save();
    }

    public static void removeHead(String name) {
        data.heads.removeIf(h -> h.name.equalsIgnoreCase(name));
        data.favorites.remove(IconChoice.HEAD_PREFIX + name);
        save();
    }

    /** Alle Einträge der Auswahl: Favoriten zuerst, dann Spielerköpfe, dann die eingebauten Symbole. */
    public static List<IconChoice> choices() {
        List<IconChoice> all = new ArrayList<>();
        for (Head head : data.heads) all.add(IconChoice.head(head.name, head.face));
        for (WaypointIcon icon : WaypointIcon.all()) all.add(IconChoice.of(icon));

        List<IconChoice> sorted = new ArrayList<>();
        for (IconChoice choice : all) if (isFavorite(choice.key())) sorted.add(choice);
        for (IconChoice choice : all) if (!isFavorite(choice.key())) sorted.add(choice);
        return sorted;
    }
}
