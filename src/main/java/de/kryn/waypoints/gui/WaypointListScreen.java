package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.data.WaypointStore;
import de.kryn.waypoints.share.RelayClient;
import de.kryn.waypoints.ui.KButton;
import de.kryn.waypoints.ui.KConfirmScreen;
import de.kryn.waypoints.ui.KScreen;
import de.kryn.waypoints.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/** Liste aller Wegpunkte der aktuellen Welt: bearbeiten, ein-/ausblenden, löschen. */
public class WaypointListScreen extends KScreen {
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDTH = 400;

    private int page;
    private int rowsPerPage;
    private int rowLeft;
    private int listTop;

    public WaypointListScreen(Screen parent) {
        super("Waypointliste", parent);
    }

    private List<Waypoint> waypoints() {
        return WaypointStore.forCurrentWorld();
    }

    private int pageCount() {
        return Math.max(1, (waypoints().size() + rowsPerPage - 1) / rowsPerPage);
    }

    @Override
    protected void layout() {
        int panelTop = 6;
        int panelHeight = height - 12;
        rowLeft = width / 2 - ROW_WIDTH / 2;
        listTop = panelTop + 28;
        rowsPerPage = Math.max(1, (panelHeight - 28 - 34) / ROW_HEIGHT);
        page = Math.max(0, Math.min(page, pageCount() - 1));
        setPanel(rowLeft - 8, panelTop, ROW_WIDTH + 16, panelHeight);

        List<Waypoint> list = waypoints();
        int start = page * rowsPerPage;
        for (int i = start; i < Math.min(list.size(), start + rowsPerPage); i++) {
            Waypoint wp = list.get(i);
            int y = listTop + (i - start) * ROW_HEIGHT;
            int bx = rowLeft + ROW_WIDTH - 3;
            addRenderableWidget(new KButton(bx - 50, y + 3, 50, 16, "Löschen", () -> confirmDelete(wp)).danger());
            addRenderableWidget(new KButton(bx - 116, y + 3, 64, 16, "Bearbeiten",
                    () -> Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(this, wp))));
            KButton share = addRenderableWidget(new KButton(bx - 164, y + 3, 46, 16, "Teilen",
                    () -> Minecraft.getInstance().gui.setScreen(new ShareScreen(this, wp))));
            share.active = RelayClient.isConnected();
            addRenderableWidget(new KButton(bx - 196, y + 3, 30, 16, wp.visible ? "An" : "Aus", () -> {
                wp.visible = !wp.visible;
                WaypointStore.save();
                rebuildWidgets();
            }));
        }

        int bottom = panelTop + panelHeight - 26;
        KButton prev = addRenderableWidget(new KButton(rowLeft, bottom, 20, 20, "<", () -> changePage(-1)));
        prev.active = page > 0;
        KButton next = addRenderableWidget(new KButton(rowLeft + 60, bottom, 20, 20, ">", () -> changePage(1)));
        next.active = page < pageCount() - 1;

        addRenderableWidget(new KButton(width / 2 - 70, bottom, 130, 20, "Neuer Wegpunkt",
                () -> Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(this, null))));
        addRenderableWidget(new KButton(rowLeft + ROW_WIDTH - 130, bottom, 130, 20, "Fertig", this::onClose));
    }

    private void changePage(int delta) {
        int target = Math.max(0, Math.min(pageCount() - 1, page + delta));
        if (target != page) {
            page = target;
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) changePage(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void confirmDelete(Waypoint wp) {
        Minecraft.getInstance().gui.setScreen(new KConfirmScreen(this, "Wegpunkt löschen?",
                "\"" + wp.displayName() + "\" wird endgültig gelöscht.", "Löschen", () -> WaypointStore.remove(wp)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        List<Waypoint> list = waypoints();
        int start = page * rowsPerPage;
        for (int i = start; i < Math.min(list.size(), start + rowsPerPage); i++) {
            int y = listTop + (i - start) * ROW_HEIGHT;
            Theme.box(g, rowLeft, y, ROW_WIDTH, ROW_HEIGHT - 2, Theme.ROW, Theme.PANEL_BORDER);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        List<Waypoint> list = waypoints();
        if (list.isEmpty()) {
            g.centeredText(font, "Noch keine Wegpunkte. Erstelle einen mit \"B\".", width / 2, listTop + 20, Theme.MUTED);
        }

        LocalPlayer player = Minecraft.getInstance().player;
        String currentDim = WaypointStore.currentDimension();
        int start = page * rowsPerPage;
        for (int i = start; i < Math.min(list.size(), start + rowsPerPage); i++) {
            Waypoint wp = list.get(i);
            int y = listTop + (i - start) * ROW_HEIGHT;

            Theme.box(g, rowLeft + 3, y + 2, 18, 18, Theme.FIELD, wp.argb());
            wp.drawIcon(g, rowLeft + 4, y + 3, 16, 0xFF);

            g.text(font, wp.displayName(), rowLeft + 26, y + 3, wp.visible ? wp.argb() : Theme.DISABLED_TEXT, true);
            String info = wp.x + " / " + wp.y + " / " + wp.z + "  " + WaypointStore.dimensionLabel(wp.dimension);
            if (player != null && wp.dimension.equals(currentDim)) {
                double dx = wp.x + 0.5 - player.getX();
                double dy = wp.y - player.getY();
                double dz = wp.z + 0.5 - player.getZ();
                info += "  " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
            }
            g.text(font, info, rowLeft + 26, y + 13, Theme.MUTED, false);
        }

        g.centeredText(font, (page + 1) + "/" + pageCount(), rowLeft + 40, panelY + panelH - 20, Theme.TEXT);
    }
}
