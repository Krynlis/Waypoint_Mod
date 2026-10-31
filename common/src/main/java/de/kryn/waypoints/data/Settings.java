package de.kryn.waypoints.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.kryn.waypoints.KrynsWaypoints;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Optionen in config/kryns_waypoints_options.json. */
public final class Settings {
    public static final int MIN_DISTANCE = 50;
    public static final int MAX_DISTANCE = 2000;
    public static final int DISTANCE_STEP = 50;
    public static final int[] MARKER_SIZES = {8, 12, 16, 24, 32};
    public static final String[] MARKER_SIZE_NAMES = {"Sehr klein", "Klein", "Mittel", "Groß", "Sehr groß"};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings instance = new Settings();

    /** Ab dieser Entfernung wird der Wegpunkt auf der XP-Leiste statt in der Welt angezeigt. */
    public int switchDistance = 300;
    /** Index in MARKER_SIZES. */
    public int markerSize = 2;
    /** Marker mit 50% Transparenz zeichnen. */
    public boolean transparent = false;
    /** Beim Tod automatisch einen Wegpunkt am Todespunkt anlegen. */
    public boolean deathWaypoints = true;

    public static Settings get() {
        return instance;
    }

    public int markerPixels() {
        return MARKER_SIZES[Math.max(0, Math.min(MARKER_SIZES.length - 1, markerSize))];
    }

    public int alpha() {
        return transparent ? 0x80 : 0xFF;
    }

    private static Path file() {
        return KrynsWaypoints.configDir().resolve("kryns_waypoints_options.json");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Settings loaded = GSON.fromJson(reader, Settings.class);
            if (loaded != null) instance = loaded;
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Optionen nicht laden", e);
        }
        instance.switchDistance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, instance.switchDistance));
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.error("Konnte Optionen nicht speichern", e);
        }
    }
}
