package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Liste aller Wegpunkte der aktuellen Welt: bearbeiten, ein-/ausblenden, löschen. */
public class WaypointListScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 32;

    private final Screen parent;
    private int page;
    private int rowsPerPage;
    private int rowLeft;

    public WaypointListScreen(Screen parent) {
        super(Component.literal("Waypointliste"));
        this.parent = parent;
    }

    private List<Waypoint> waypoints() {
        return WaypointStore.forCurrentWorld();
    }

    private int pageCount() {
        return Math.max(1, (waypoints().size() + rowsPerPage - 1) / rowsPerPage);
    }

    @Override
    protected void init() {
        rowsPerPage = Math.max(1, (height - LIST_TOP - 40) / ROW_HEIGHT);
        page = Math.min(page, pageCount() - 1);
        rowLeft = width / 2 - 200;

        List<Waypoint> list = waypoints();
        int start = page * rowsPerPage;
        for (int i = start; i < Math.min(list.size(), start + rowsPerPage); i++) {
            Waypoint wp = list.get(i);
            int y = LIST_TOP + (i - start) * ROW_HEIGHT;
            int bx = rowLeft + 400;
            addRenderableWidget(Button.builder(Component.literal("Löschen"), b -> confirmDelete(wp))
                    .bounds(bx - 50, y + 1, 50, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Bearbeiten"), b -> Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(this, wp)))
                    .bounds(bx - 116, y + 1, 64, 20).build());
            addRenderableWidget(Button.builder(Component.literal(wp.visible ? "An" : "Aus"), b -> {
                        wp.visible = !wp.visible;
                        WaypointStore.save();
                        rebuildWidgets();
                    })
                    .bounds(bx - 148, y + 1, 30, 20).build());
        }

        int bottom = height - 28;
        Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(width / 2 - 170, bottom, 20, 20).build());
        prev.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(width / 2 - 146, bottom, 20, 20).build());
        next.active = page < pageCount() - 1;

        addRenderableWidget(Button.builder(Component.literal("Neuer Wegpunkt"), b -> Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(this, null)))
                .bounds(width / 2 - 100, bottom, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(width / 2 + 40, bottom, 130, 20).build());
    }

    private void confirmDelete(Waypoint wp) {
        Minecraft.getInstance().gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) WaypointStore.remove(wp);
            Minecraft.getInstance().gui.setScreen(this);
        }, Component.literal("Wegpunkt löschen?"), Component.literal("\"" + displayName(wp) + "\" wird endgültig gelöscht.")));
    }

    private static String displayName(Waypoint wp) {
        return wp.name.isEmpty() ? wp.getIcon().label : wp.name;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);

        List<Waypoint> list = waypoints();
        if (list.isEmpty()) {
            g.centeredText(font, "Noch keine Wegpunkte. Erstelle einen mit \"B\".", width / 2, LIST_TOP + 20, 0xFFA0A0A0);
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        String currentDim = WaypointStore.currentDimension();
        int start = page * rowsPerPage;
        for (int i = start; i < Math.min(list.size(), start + rowsPerPage); i++) {
            Waypoint wp = list.get(i);
            int y = LIST_TOP + (i - start) * ROW_HEIGHT;
            int argb = wp.argb();

            g.fill(rowLeft, y, rowLeft + 400, y + ROW_HEIGHT - 2, 0x60000000);
            g.fill(rowLeft + 2, y + 2, rowLeft + 20, y + 20, argb);
            g.fill(rowLeft + 3, y + 3, rowLeft + 19, y + 19, 0xFF101010);
            wp.getIcon().draw(g, rowLeft + 3, y + 3, 2, argb);

            g.text(font, displayName(wp), rowLeft + 26, y + 3, wp.visible ? argb : 0xFF707070, true);

            String info = wp.x + " / " + wp.y + " / " + wp.z + "  " + WaypointStore.dimensionLabel(wp.dimension);
            if (player != null && wp.dimension.equals(currentDim)) {
                double dx = wp.x + 0.5 - player.getX();
                double dy = wp.y - player.getY();
                double dz = wp.z + 0.5 - player.getZ();
                info += "  " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
            }
            g.text(font, info, rowLeft + 26, y + 13, 0xFFA0A0A0, false);
        }

        g.text(font, (page + 1) + "/" + pageCount(), width / 2 - 122, height - 22, 0xFFFFFFFF, false);
    }
}
