package de.kryn.waypoints.share;

import com.google.gson.Gson;
import de.kryn.waypoints.KrynsWaypoints;
import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.data.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Verbindung zum Vermittlungsdienst (relay). Meldet sich über die Mojang-Sitzungsprüfung an,
 * fragt alle paar Sekunden ab, wer aus der Tab-Liste die Mod nutzt, und holt geteilte Wegpunkte ab.
 */
public final class RelayClient {
    public record IncomingShare(String id, String from, Waypoint waypoint) {}

    private static final Gson GSON = new Gson();
    private static final long SYNC_INTERVAL_MS = 3000;
    private static final long RETRY_LOGIN_MS = 60_000;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Kryn's Waypoints Relay");
        t.setDaemon(true);
        return t;
    });

    private static volatile List<String> tabNames = List.of();
    private static volatile Set<String> modUsers = Set.of();
    private static volatile String token;
    private static volatile String status = "Nicht verbunden";
    private static volatile boolean connected;
    private static long lastLoginAttempt;
    private static final ConcurrentLinkedQueue<IncomingShare> INCOMING = new ConcurrentLinkedQueue<>();

    private RelayClient() {}

    public static void start() {
        EXECUTOR.scheduleWithFixedDelay(RelayClient::run, 2000, SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** Im Client-Tick: Namen aus der Tab-Liste für den Hintergrund-Thread bereitstellen. */
    public static void tick(Minecraft mc) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.getSingleplayerServer() != null) {
            tabNames = List.of();
            return;
        }
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) names.add(info.getProfile().name());
        tabNames = names;
    }

    public static IncomingShare pollIncoming() {
        return INCOMING.poll();
    }

    public static boolean isConnected() {
        return connected;
    }

    public static String status() {
        return status;
    }

    /** true, wenn dieser Spieler die Mod gerade nutzt (inkl. einem selbst, sobald verbunden). */
    public static boolean hasMod(String name) {
        if (name == null) return false;
        if (connected && name.equalsIgnoreCase(Minecraft.getInstance().getUser().getName())) return true;
        return modUsers.contains(name.toLowerCase(Locale.ROOT));
    }

    private static String baseUrl() {
        String url = Settings.get().relayUrl == null ? "" : Settings.get().relayUrl.trim();
        if (url.isEmpty()) return null;
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private static void disconnect(String newStatus) {
        connected = false;
        modUsers = Set.of();
        status = newStatus;
    }

    private static void run() {
        try {
            String base = baseUrl();
            if (!Settings.get().sharing || base == null) {
                token = null;
                disconnect(base == null ? "Kein Vermittlungsdienst eingetragen" : "Teilen ausgeschaltet");
                return;
            }
            List<String> names = tabNames;
            if (names.isEmpty()) {
                disconnect("Nur auf Servern verfügbar");
                return;
            }
            if (token == null) {
                long now = System.currentTimeMillis();
                if (now - lastLoginAttempt < RETRY_LOGIN_MS) return;
                lastLoginAttempt = now;
                token = login(base);
                if (token == null) return;
            }
            HttpResponse<String> response = post(base + "/sync?token=" + token, String.join("\n", names));
            if (response.statusCode() == 401) {
                token = null;
                lastLoginAttempt = 0;
                disconnect("Anmeldung abgelaufen");
                return;
            }
            if (response.statusCode() != 200) {
                disconnect("Vermittlungsdienst antwortet nicht (" + response.statusCode() + ")");
                return;
            }
            Set<String> users = ConcurrentHashMap.newKeySet();
            for (String line : response.body().split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length == 2 && parts[0].equals("user")) {
                    users.add(parts[1].toLowerCase(Locale.ROOT));
                } else if (parts.length == 4 && parts[0].equals("share")) {
                    Waypoint wp = decode(parts[3]);
                    if (wp != null) INCOMING.add(new IncomingShare(parts[1], parts[2], wp));
                }
            }
            modUsers = users;
            connected = true;
            status = "Verbunden";
        } catch (Exception e) {
            disconnect("Vermittlungsdienst nicht erreichbar");
        }
    }

    /** Anmeldung: Challenge holen, bei Mojang "beitreten", Dienst prüft das über hasJoined. */
    private static String login(String base) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        User user = mc.getUser();
        HttpResponse<String> challenge = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/challenge"))
                .timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
        if (challenge.statusCode() != 200) {
            disconnect("Vermittlungsdienst antwortet nicht (" + challenge.statusCode() + ")");
            return null;
        }
        String serverId = challenge.body().trim();
        try {
            mc.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
        } catch (Exception e) {
            disconnect("Mojang-Anmeldung fehlgeschlagen");
            return null;
        }
        HttpResponse<String> login = post(base + "/login?name=" + URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)
                + "&challenge=" + serverId, "");
        if (login.statusCode() != 200) {
            disconnect("Anmeldung beim Vermittlungsdienst fehlgeschlagen");
            return null;
        }
        return login.body().trim();
    }

    private static HttpResponse<String> post(String url, String body) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    /** Teilt einen Wegpunkt; das Ergebnis (Fehlermeldung oder null bei Erfolg) kommt im Hauptthread an. */
    public static void share(String to, Waypoint waypoint, Consumer<String> onResult) {
        Minecraft mc = Minecraft.getInstance();
        EXECUTOR.execute(() -> {
            String error;
            try {
                String base = baseUrl();
                if (base == null || token == null) {
                    error = "Nicht mit dem Vermittlungsdienst verbunden";
                } else {
                    HttpResponse<String> response = post(base + "/share?token=" + token + "&to="
                            + URLEncoder.encode(to, StandardCharsets.UTF_8), encode(waypoint));
                    error = switch (response.statusCode()) {
                        case 200 -> null;
                        case 404 -> to + " ist nicht mehr erreichbar";
                        case 429 -> "Zu viele Wegpunkte geteilt, bitte kurz warten";
                        default -> "Teilen fehlgeschlagen (" + response.statusCode() + ")";
                    };
                }
            } catch (Exception e) {
                error = "Vermittlungsdienst nicht erreichbar";
            }
            String result = error;
            mc.execute(() -> onResult.accept(result));
        });
    }

    private static String encode(Waypoint wp) {
        Waypoint copy = new Waypoint();
        copy.name = wp.name;
        copy.x = wp.x;
        copy.y = wp.y;
        copy.z = wp.z;
        copy.dimension = wp.dimension;
        copy.icon = wp.icon;
        copy.color = wp.color;
        copy.playerHead = wp.playerHead;
        copy.face = wp.face;
        return Base64.getEncoder().encodeToString(GSON.toJson(copy).getBytes(StandardCharsets.UTF_8));
    }

    private static Waypoint decode(String data) {
        try {
            Waypoint wp = GSON.fromJson(new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8), Waypoint.class);
            if (wp == null || wp.dimension == null || wp.icon == null) return null;
            if (wp.name == null) wp.name = "";
            if (wp.name.length() > 32) wp.name = wp.name.substring(0, 32);
            if (wp.face != null && wp.face.length != 64) wp.face = null;
            wp.color &= 0xFFFFFF;
            wp.visible = true;
            return wp;
        } catch (Exception e) {
            KrynsWaypoints.LOGGER.warn("Ungültiger geteilter Wegpunkt: {}", e.getMessage());
            return null;
        }
    }
}
