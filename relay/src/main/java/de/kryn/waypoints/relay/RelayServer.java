package de.kryn.waypoints.relay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vermittlungsdienst für Kryn's Waypoints.
 * <p>
 * Spieler melden sich über die Mojang-Sitzungsprüfung an (wie beim Betreten eines Servers), danach fragt
 * die Mod regelmäßig nach, welche Spieler aus ihrer Tab-Liste die Mod gerade nutzen, und holt geteilte
 * Wegpunkte ab. Der Dienst speichert nichts dauerhaft.
 * <p>
 * Start: {@code java -jar kryns-waypoints-relay.jar [port]} oder Port über die Umgebungsvariable SERVER_PORT.
 */
public final class RelayServer {
    private static final Duration ACTIVE = Duration.ofSeconds(20);
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration CHALLENGE_TIMEOUT = Duration.ofMinutes(1);
    private static final int MAX_PAYLOAD = 8 * 1024;
    private static final int MAX_INBOX = 20;
    private static final int MAX_SHARES_PER_MINUTE = 10;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern PROFILE_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([A-Za-z0-9_]{3,16})\"");

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient MOJANG = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private record Share(String id, String from, String payload) {}

    private static final class Session {
        final String name;
        volatile long lastSeen = System.currentTimeMillis();
        final List<Share> inbox = new ArrayList<>();
        final List<Long> recentShares = new ArrayList<>();

        Session(String name) {
            this.name = name;
        }
    }

    private final Map<String, Long> challenges = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionsByToken = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionsByName = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String env = System.getenv("SERVER_PORT");
        if (args.length > 0) port = Integer.parseInt(args[0]);
        else if (env != null && !env.isBlank()) port = Integer.parseInt(env.trim());
        new RelayServer().start(port);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/challenge", this::challenge);
        server.createContext("/login", this::login);
        server.createContext("/sync", this::sync);
        server.createContext("/share", this::share);
        server.createContext("/", ex -> send(ex, 200, "Kryn's Waypoints Relay"));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::cleanup, 1, 1, java.util.concurrent.TimeUnit.MINUTES);
        System.out.println("Kryn's Waypoints Relay läuft auf Port " + port);
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> result = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return result;
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            result.put(URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String body(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] data = in.readNBytes(MAX_PAYLOAD + 1);
            if (data.length > MAX_PAYLOAD) return null;
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static void send(HttpExchange ex, int status, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    private Session session(HttpExchange ex) {
        String token = query(ex).get("token");
        Session session = token == null ? null : sessionsByToken.get(token);
        if (session != null) session.lastSeen = System.currentTimeMillis();
        return session;
    }

    private void challenge(HttpExchange ex) throws IOException {
        if (challenges.size() > 10_000) {
            send(ex, 503, "busy");
            return;
        }
        String challenge = randomHex(16);
        challenges.put(challenge, System.currentTimeMillis());
        send(ex, 200, challenge);
    }

    /** Prüft über Mojang, dass sich der Spieler mit dieser Challenge angemeldet hat, und vergibt ein Token. */
    private void login(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex);
        String name = q.get("name");
        String challenge = q.get("challenge");
        Long issued = challenge == null ? null : challenges.remove(challenge);
        if (name == null || !NAME.matcher(name).matches() || issued == null
                || System.currentTimeMillis() - issued > CHALLENGE_TIMEOUT.toMillis()) {
            send(ex, 400, "bad request");
            return;
        }
        String verifiedName;
        try {
            HttpResponse<String> response = MOJANG.send(HttpRequest.newBuilder(URI.create(
                            "https://sessionserver.mojang.com/session/minecraft/hasJoined?username="
                                    + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&serverId=" + challenge))
                    .timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
            Matcher m = PROFILE_NAME.matcher(response.body());
            verifiedName = response.statusCode() == 200 && m.find() ? m.group(1) : null;
        } catch (Exception e) {
            verifiedName = null;
        }
        if (verifiedName == null || !verifiedName.equalsIgnoreCase(name)) {
            send(ex, 403, "not verified");
            return;
        }
        String key = verifiedName.toLowerCase(Locale.ROOT);
        Session old = sessionsByName.get(key);
        Session session = new Session(verifiedName);
        if (old != null) {
            synchronized (old) {
                session.inbox.addAll(old.inbox);
            }
            sessionsByToken.values().remove(old);
        }
        String token = randomHex(24);
        sessionsByToken.put(token, session);
        sessionsByName.put(key, session);
        send(ex, 200, token);
    }

    /**
     * Body: Spielernamen aus der Tab-Liste (eine Zeile pro Name).
     * Antwort: "user\tName" für jeden davon, der die Mod gerade nutzt, und "share\tId\tVon\tDaten" für neue Wegpunkte.
     */
    private void sync(HttpExchange ex) throws IOException {
        Session session = session(ex);
        if (session == null) {
            send(ex, 401, "login");
            return;
        }
        String body = body(ex);
        StringBuilder out = new StringBuilder();
        long now = System.currentTimeMillis();
        if (body != null) {
            for (String line : body.split("\n")) {
                Session other = sessionsByName.get(line.trim().toLowerCase(Locale.ROOT));
                if (other != null && now - other.lastSeen <= ACTIVE.toMillis()) {
                    out.append("user\t").append(other.name).append('\n');
                }
            }
        }
        synchronized (session) {
            for (Share share : session.inbox) {
                out.append("share\t").append(share.id()).append('\t').append(share.from()).append('\t')
                        .append(share.payload()).append('\n');
            }
            session.inbox.clear();
        }
        send(ex, 200, out.toString());
    }

    /** Query: to=Empfänger. Body: Wegpunkt (Base64-codiertes JSON). */
    private void share(HttpExchange ex) throws IOException {
        Session session = session(ex);
        if (session == null) {
            send(ex, 401, "login");
            return;
        }
        String to = query(ex).get("to");
        String payload = body(ex);
        if (to == null || payload == null || payload.isBlank() || payload.contains("\n") || payload.contains("\t")) {
            send(ex, 400, "bad request");
            return;
        }
        Session target = sessionsByName.get(to.toLowerCase(Locale.ROOT));
        long now = System.currentTimeMillis();
        if (target == null || now - target.lastSeen > ACTIVE.toMillis()) {
            send(ex, 404, "offline");
            return;
        }
        synchronized (session) {
            session.recentShares.removeIf(t -> now - t > 60_000);
            if (session.recentShares.size() >= MAX_SHARES_PER_MINUTE) {
                send(ex, 429, "too many");
                return;
            }
            session.recentShares.add(now);
        }
        synchronized (target) {
            if (target.inbox.size() >= MAX_INBOX) {
                send(ex, 429, "inbox full");
                return;
            }
            target.inbox.add(new Share(randomHex(6), session.name, payload.trim()));
        }
        send(ex, 200, "ok");
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        challenges.values().removeIf(t -> now - t > CHALLENGE_TIMEOUT.toMillis());
        sessionsByToken.values().removeIf(s -> now - s.lastSeen > SESSION_TIMEOUT.toMillis());
        sessionsByName.values().removeIf(s -> now - s.lastSeen > SESSION_TIMEOUT.toMillis());
    }
}
