package de.kryn.waypoints.render;

import de.kryn.waypoints.data.Settings;
import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointStore;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Zeichnet Wegpunkte als HUD-Overlay: bis zur eingestellten Entfernung als Symbol + Name in der Welt
 * (auf den Bildschirm projiziert, also auch durch Wände sichtbar), weiter weg als Symbol auf der XP-Leiste.
 */
public final class WaypointHud {
    private static final int XP_BAR_HALF_WIDTH = 91;

    private WaypointHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        Vec3 eye = player.getEyePosition(partialTick);
        float yaw = player.getViewYRot(partialTick);
        float pitch = player.getViewXRot(partialTick);

        int width = g.guiWidth();
        int height = g.guiHeight();
        double fov = mc.options.fov().get();
        Settings settings = Settings.get();

        for (Waypoint wp : WaypointStore.forCurrentDimension()) {
            if (!wp.visible) continue;
            double dx = wp.x + 0.5 - eye.x();
            double dy = wp.y + 1.0 - eye.y();
            double dz = wp.z + 0.5 - eye.z();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance <= settings.switchDistance) {
                renderInWorld(g, mc.font, wp, dx, dy, dz, distance, yaw, pitch, fov, width, height, settings);
            } else {
                renderOnXpBar(g, wp, dx, dz, yaw, width, height, settings.alpha());
            }
        }
    }

    private static void renderInWorld(GuiGraphicsExtractor g, Font font, Waypoint wp, double dx, double dy, double dz,
                                      double distance, float yaw, float pitch, double fov, int width, int height,
                                      Settings settings) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double sinYaw = Math.sin(yawRad), cosYaw = Math.cos(yawRad);
        double sinPitch = Math.sin(pitchRad), cosPitch = Math.cos(pitchRad);

        // Kamerabasis (Minecraft: Yaw 0 = Blick nach +Z)
        double fx = -sinYaw * cosPitch, fy = -sinPitch, fz = cosYaw * cosPitch;
        double rx = -cosYaw, rz = -sinYaw;
        // up = right x forward
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;

        double camZ = dx * fx + dy * fy + dz * fz;
        if (camZ <= 0.05) return;
        double camX = dx * rx + dz * rz;
        double camY = dx * ux + dy * uy + dz * uz;

        double focal = (height / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);
        int sx = (int) Math.round(width / 2.0 + camX / camZ * focal);
        int sy = (int) Math.round(height / 2.0 - camY / camZ * focal);
        if (sx < -50 || sx > width + 50 || sy < -50 || sy > height + 50) return;

        int iconSize = settings.markerPixels();
        int alpha = settings.alpha();
        int left = sx - iconSize / 2;
        int top = sy - iconSize / 2;
        g.fill(left - 2, top - 2, left + iconSize + 2, top + iconSize + 2, withAlpha(wp.color, alpha));
        g.fill(left - 1, top - 1, left + iconSize + 1, top + iconSize + 1, withAlpha(0x101010, alpha * 3 / 4));
        wp.drawIcon(g, left, top, iconSize, alpha);

        String name = wp.displayName();
        int nameWidth = font.width(name);
        int nameY = top - 14;
        g.fill(sx - nameWidth / 2 - 2, nameY - 2, sx + nameWidth / 2 + 2, nameY + 9, withAlpha(0, alpha / 2));
        g.centeredText(font, name, sx, nameY, withAlpha(wp.color, alpha));

        String dist = (int) distance + "m";
        g.centeredText(font, dist, sx, top + iconSize + 4, withAlpha(0xE0E0E0, alpha));
    }

    private static void renderOnXpBar(GuiGraphicsExtractor g, Waypoint wp, double dx, double dz, float yaw, int width, int height,
                                      int alpha) {
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = wrapDegrees(targetYaw - yaw);
        double clamped = Math.max(-90.0, Math.min(90.0, relative));

        int centerX = width / 2 + (int) Math.round(clamped / 90.0 * XP_BAR_HALF_WIDTH);
        int left = centerX - 4;
        int top = height - 31;
        boolean behind = Math.abs(relative) > 90.0;

        g.fill(left - 1, top - 1, left + 9, top + 9, behind ? withAlpha(0, alpha * 3 / 8) : withAlpha(wp.color, alpha));
        g.fill(left, top, left + 8, top + 8, withAlpha(0x101010, alpha * 3 / 4));
        wp.drawIcon(g, left, top, 8, alpha);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static double wrapDegrees(double degrees) {
        double d = degrees % 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }
}
