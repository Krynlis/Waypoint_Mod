package de.kryn.waypoints;

import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Legt beim Tod automatisch einen Wegpunkt am Todespunkt an.
 * Grundlage ist der letzte Todespunkt, den der Server beim Respawn mitschickt (wie beim Bergungskompass).
 * Das funktioniert auch, wenn der Todesbildschirm auf dem Server deaktiviert ist (sofortiger Respawn).
 */
public final class DeathWaypoints {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int COLOR = 0xD63A2E;

    private static String seenWorld;
    private static GlobalPos seenDeath;

    private DeathWaypoints() {}

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            seenWorld = null;
            return;
        }
        String world = WaypointStore.worldKey();
        GlobalPos death = player.getLastDeathLocation().orElse(null);

        // Beim Betreten einer Welt ist der bekannte Todespunkt alt und wird nur gemerkt.
        if (!world.equals(seenWorld)) {
            seenWorld = world;
            seenDeath = death;
            return;
        }
        if (death == null || Objects.equals(death, seenDeath)) return;
        seenDeath = death;
        if (Settings.get().deathWaypoints) create(player, death);
    }

    private static void create(LocalPlayer player, GlobalPos death) {
        Waypoint wp = new Waypoint();
        wp.name = "Tod " + LocalDateTime.now().format(TIME);
        wp.x = death.pos().getX();
        wp.y = death.pos().getY();
        wp.z = death.pos().getZ();
        wp.dimension = WaypointStore.dimensionId(death.dimension());
        wp.icon = "skull";
        wp.color = COLOR;
        WaypointStore.add(wp);
        player.sendSystemMessage(Component.literal("§cTodespunkt gespeichert: §f" + wp.x + " / " + wp.y + " / " + wp.z
                + " §7(" + WaypointStore.dimensionLabel(wp.dimension) + ")"));
    }
}
