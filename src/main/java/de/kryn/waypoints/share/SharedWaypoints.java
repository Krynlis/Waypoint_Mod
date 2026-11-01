package de.kryn.waypoints.share;

import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Zeigt empfangene Wegpunkte im Chat an und verarbeitet Annehmen/Ablehnen (nur lokal). */
public final class SharedWaypoints {
    public static final String COMMAND = "kwp";
    private static final Map<String, RelayClient.IncomingShare> PENDING = new LinkedHashMap<>();

    private SharedWaypoints() {}

    public static void tick(Minecraft mc) {
        RelayClient.tick(mc);
        LocalPlayer player = mc.player;
        if (player == null) return;
        RelayClient.IncomingShare share;
        while ((share = RelayClient.pollIncoming()) != null) {
            PENDING.put(share.id(), share);
            Waypoint wp = share.waypoint();
            MutableComponent message = Component.literal("[Wegpunkte] ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(share.from()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" teilt ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wp.displayName()).withStyle(s -> s.withColor(wp.color)))
                    .append(Component.literal(" (" + wp.x + " / " + wp.y + " / " + wp.z + ", "
                            + WaypointStore.dimensionLabel(wp.dimension) + ")  ").withStyle(ChatFormatting.GRAY))
                    .append(button("[Annehmen]", ChatFormatting.GREEN, "accept", share.id(), "Wegpunkt übernehmen"))
                    .append(Component.literal(" "))
                    .append(button("[Ablehnen]", ChatFormatting.RED, "decline", share.id(), "Wegpunkt verwerfen"));
            player.sendSystemMessage(message);
        }
    }

    private static Component button(String label, ChatFormatting color, String action, String id, String hover) {
        return Component.literal(label).withStyle(s -> s.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/" + COMMAND + " " + action + " " + id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    /** Ergebnis als Text für den Chat. */
    public static Component accept(String id) {
        RelayClient.IncomingShare share = PENDING.remove(id);
        if (share == null) return Component.literal("Dieser Wegpunkt wurde bereits bearbeitet.").withStyle(ChatFormatting.GRAY);
        WaypointStore.add(share.waypoint());
        return Component.literal("Wegpunkt \"" + share.waypoint().displayName() + "\" von " + share.from() + " übernommen.")
                .withStyle(ChatFormatting.GREEN);
    }

    public static Component decline(String id) {
        RelayClient.IncomingShare share = PENDING.remove(id);
        if (share == null) return Component.literal("Dieser Wegpunkt wurde bereits bearbeitet.").withStyle(ChatFormatting.GRAY);
        return Component.literal("Wegpunkt von " + share.from() + " abgelehnt.").withStyle(ChatFormatting.GRAY);
    }
}
