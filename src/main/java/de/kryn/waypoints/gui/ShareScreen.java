package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Waypoint;
import de.kryn.waypoints.head.FaceRenderer;
import de.kryn.waypoints.head.PlayerHeads;
import de.kryn.waypoints.share.RelayClient;
import de.kryn.waypoints.ui.KButton;
import de.kryn.waypoints.ui.KScreen;
import de.kryn.waypoints.ui.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Auswahl, an wen ein Wegpunkt geteilt wird: alle Spieler online, die die Mod nutzen. Links Kopf, rechts Name. */
public class ShareScreen extends KScreen {
    private static final int ROW_HEIGHT = 22;
    private static final int WIDTH = 220;

    private final Waypoint waypoint;
    private int listTop;
    private int rows;
    private int scroll;
    private List<String> shownTargets = List.of();

    public ShareScreen(Screen parent, Waypoint waypoint) {
        super("Wegpunkt teilen", parent);
        this.waypoint = waypoint;
    }

    private static List<String> targets() {
        String self = Minecraft.getInstance().getUser().getName();
        List<String> result = new ArrayList<>();
        for (String name : PlayerHeadGrid.onlinePlayers()) {
            if (!name.equalsIgnoreCase(self) && RelayClient.hasMod(name)) result.add(name);
        }
        return result;
    }

    @Override
    protected void layout() {
        int panelHeight = Math.min(height - 12, 260);
        int panelTop = (height - panelHeight) / 2;
        int left = width / 2 - WIDTH / 2;
        setPanel(left - 8, panelTop, WIDTH + 16, panelHeight);
        listTop = panelTop + 40;
        rows = Math.max(1, (panelHeight - 40 - 32) / ROW_HEIGHT);

        shownTargets = targets();
        scroll = Math.max(0, Math.min(scroll, shownTargets.size() - rows));
        for (int i = scroll; i < Math.min(shownTargets.size(), scroll + rows); i++) {
            addRenderableWidget(new TargetRow(left, listTop + (i - scroll) * ROW_HEIGHT, WIDTH, ROW_HEIGHT - 2, shownTargets.get(i)));
        }
        addRenderableWidget(new KButton(width / 2 - 60, panelTop + panelHeight - 26, 120, 20, "Abbrechen", this::onClose));
    }

    @Override
    public void tick() {
        super.tick();
        // Liste aktualisieren, wenn Spieler kommen/gehen oder die Mod-Erkennung nachzieht
        if (!targets().equals(shownTargets)) rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, shownTargets.size() - rows);
        int target = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        if (target != scroll) {
            scroll = target;
            rebuildWidgets();
        }
        return true;
    }

    private void shareWith(String name) {
        Minecraft mc = Minecraft.getInstance();
        RelayClient.share(name, waypoint, error -> {
            if (mc.player == null) return;
            if (error == null) {
                mc.player.sendSystemMessage(Component.literal("[Wegpunkte] \"" + waypoint.displayName() + "\" an " + name + " geteilt.")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                mc.player.sendSystemMessage(Component.literal("[Wegpunkte] " + error).withStyle(ChatFormatting.RED));
            }
        });
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, "\"" + waypoint.displayName() + "\" teilen mit:", width / 2, panelY + 26, Theme.MUTED);
        if (shownTargets.isEmpty()) {
            String message = RelayClient.isConnected()
                    ? "Niemand online nutzt gerade die Mod."
                    : RelayClient.status();
            g.centeredText(font, message, width / 2, listTop + 20, Theme.MUTED);
        }
    }

    /** Eine Zeile: links der Kopf, rechts der Name. */
    private class TargetRow extends AbstractWidget {
        private final String name;

        TargetRow(int x, int y, int width, int height, String name) {
            super(x, y, width, height, Component.literal(name));
            this.name = name;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            Theme.box(g, getX(), getY(), width, height, isHovered() ? Theme.BUTTON_HOVER : Theme.ROW,
                    isHoveredOrFocused() ? Theme.ACCENT : Theme.PANEL_BORDER);
            int[] face = PlayerHeads.request(name);
            if (face != null) {
                FaceRenderer.draw(g, face, getX() + 3, getY() + 2, 16, 0xFF);
            } else {
                g.fill(getX() + 3, getY() + 2, getX() + 19, getY() + 18, 0xFF2A2E36);
            }
            g.text(font, name, getX() + 26, getY() + (height - 8) / 2, Theme.TEXT, false);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            playButtonClickSound(Minecraft.getInstance().getSoundManager());
            shareWith(name);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
