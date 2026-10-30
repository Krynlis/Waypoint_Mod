package de.kryn.waypoints.head;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.kryn.waypoints.KrynsWaypoints;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Lädt Spielerköpfe (8x8-Gesicht inkl. Kopf-Overlay) über die Mojang-API.
 * Ergebnisse werden für die laufende Sitzung zwischengespeichert.
 */
public final class PlayerHeads {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<String, CompletableFuture<int[]>> CACHE = new ConcurrentHashMap<>();

    private PlayerHeads() {}

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Startet die Suche (falls nötig) und liefert das Gesicht, sobald es geladen ist, sonst null. */
    public static int[] request(String name) {
        if (!isValidName(name)) return null;
        CompletableFuture<int[]> future = CACHE.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> fetch(name));
        return future.isDone() ? future.getNow(null) : null;
    }

    /** true, solange die Suche für diesen Namen noch läuft. */
    public static boolean isLoading(String name) {
        if (!isValidName(name)) return false;
        CompletableFuture<int[]> future = CACHE.get(name.toLowerCase(Locale.ROOT));
        return future != null && !future.isDone();
    }

    private static CompletableFuture<int[]> fetch(String name) {
        return getString("https://api.mojang.com/users/profiles/minecraft/" + name)
                .thenCompose(profile -> {
                    if (profile == null) return CompletableFuture.completedFuture(null);
                    String id = JsonParser.parseString(profile).getAsJsonObject().get("id").getAsString();
                    return getString("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
                })
                .thenCompose(session -> {
                    if (session == null) return CompletableFuture.completedFuture(null);
                    String skinUrl = skinUrl(session);
                    if (skinUrl == null) return CompletableFuture.completedFuture(null);
                    return HTTP.sendAsync(HttpRequest.newBuilder(URI.create(skinUrl)).timeout(Duration.ofSeconds(10)).build(),
                                    HttpResponse.BodyHandlers.ofByteArray())
                            .thenApply(r -> r.statusCode() == 200 ? r.body() : null);
                })
                .thenApply(png -> {
                    if (png == null) return null;
                    try {
                        return face(PngDecoder.decode(png));
                    } catch (Exception e) {
                        KrynsWaypoints.LOGGER.warn("Skin von {} konnte nicht gelesen werden: {}", name, e.getMessage());
                        return null;
                    }
                })
                .exceptionally(e -> null);
    }

    private static CompletableFuture<String> getString(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(r -> r.statusCode() == 200 && !r.body().isBlank() ? r.body() : null);
    }

    private static String skinUrl(String session) {
        JsonObject root = JsonParser.parseString(session).getAsJsonObject();
        for (var property : root.getAsJsonArray("properties")) {
            JsonObject p = property.getAsJsonObject();
            if (!"textures".equals(p.get("name").getAsString())) continue;
            String json = new String(Base64.getDecoder().decode(p.get("value").getAsString()), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
            if (textures != null && textures.has("SKIN")) {
                return textures.getAsJsonObject("SKIN").get("url").getAsString().replace("http://", "https://");
            }
        }
        return null;
    }

    /** Schneidet das Gesicht (8,8) aus und legt das Kopf-Overlay (40,8) darüber. */
    private static int[] face(PngDecoder skin) {
        if (skin.width < 64 || skin.height < 32) return null;
        int[] face = new int[64];
        boolean overlayOpaque = true;
        for (int i = 0; i < 64; i++) {
            if ((skin.argb[(8 + i / 8) * skin.width + 40 + i % 8] >>> 24) < 255) overlayOpaque = false;
        }
        // Alte 64x32-Skins haben oft ein vollständig deckendes Overlay, das Minecraft ignoriert.
        boolean useOverlay = !(skin.height == 32 && overlayOpaque);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int base = skin.argb[(8 + y) * skin.width + 8 + x] | 0xFF000000;
                if (useOverlay) {
                    int over = skin.argb[(8 + y) * skin.width + 40 + x];
                    base = blend(base, over);
                }
                face[y * 8 + x] = base;
            }
        }
        return face;
    }

    private static int blend(int base, int over) {
        int a = over >>> 24;
        if (a == 0) return base;
        if (a == 255) return over;
        int r = (((over >> 16) & 0xFF) * a + ((base >> 16) & 0xFF) * (255 - a)) / 255;
        int g = (((over >> 8) & 0xFF) * a + ((base >> 8) & 0xFF) * (255 - a)) / 255;
        int b = ((over & 0xFF) * a + (base & 0xFF) * (255 - a)) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
