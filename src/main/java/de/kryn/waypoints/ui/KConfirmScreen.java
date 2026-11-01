package de.kryn.waypoints.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Ja/Nein-Abfrage im Mod-Stil. */
public class KConfirmScreen extends KScreen {
    private final String message;
    private final String confirmLabel;
    private final Runnable onConfirm;

    public KConfirmScreen(Screen parent, String title, String message, String confirmLabel, Runnable onConfirm) {
        super(title, parent);
        this.message = message;
        this.confirmLabel = confirmLabel;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void layout() {
        int w = Math.max(220, font.width(message) + 30);
        int x = width / 2 - w / 2;
        int y = height / 2 - 40;
        setPanel(x, y, w, 80);
        addRenderableWidget(new KButton(width / 2 - 102, y + 52, 100, 20, confirmLabel, () -> {
            onConfirm.run();
            onClose();
        }).danger());
        addRenderableWidget(new KButton(width / 2 + 2, y + 52, 100, 20, "Abbrechen", this::onClose));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, message, width / 2, panelY + 32, Theme.TEXT);
    }
}
