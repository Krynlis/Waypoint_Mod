package de.kryn.waypoints;

import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Legt beim Tod automatisch einen Wegpunkt am Todespunkt an. */
public final class DeathWaypoints {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int COLOR = 0xD63A2E;

    /** Verhindert mehrere Wegpunkte für denselben Tod (der Todesbildschirm wird z.B. bei Größenänderung neu aufgebaut). */
    private static boolean recorded;

    private DeathWaypoints() {}

    /** Wird aufgerufen, sobald der Todesbildschirm erscheint. */
    public static void onDeathScreen(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (recorded || player == null || mc.level == null || !Settings.get().deathWaypoints) return;
        recorded = true;

        Waypoint wp = new Waypoint();
        wp.name = "Tod " + LocalDateTime.now().format(TIME);
        wp.x = player.getBlockX();
        wp.y = player.getBlockY();
        wp.z = player.getBlockZ();
        wp.dimension = WaypointStore.currentDimension();
        wp.icon = "skull";
        wp.color = COLOR;
        WaypointStore.add(wp);
        player.sendSystemMessage(Component.literal("§cTodespunkt gespeichert: §f" + wp.x + " / " + wp.y + " / " + wp.z));
    }

    /** Jeden Tick: nach dem Respawn ist der nächste Tod wieder ein neuer. */
    public static void tick(Minecraft mc) {
        if (recorded && mc.player != null && mc.player.isAlive()) recorded = false;
    }
}
