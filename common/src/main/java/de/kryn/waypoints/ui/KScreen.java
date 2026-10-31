package de.kryn.waypoints.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Basis für alle Menüs der Mod: abgedunkelter Hintergrund, Panel mit Titelleiste, gestylte Textfelder. */
public abstract class KScreen extends Screen {
    protected final Screen parent;
    protected int panelX;
    protected int panelY;
    protected int panelW;
    protected int panelH;
    private final List<Field> fields = new ArrayList<>();

    private record Field(EditBox box, int x, int y, int w, int h) {}

    protected KScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    @Override
    protected final void init() {
        fields.clear();
        layout();
    }

    /** Baut die Widgets auf und setzt panelX/Y/W/H. */
    protected abstract void layout();

    protected void setPanel(int x, int y, int w, int h) {
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;
    }

    /** Textfeld ohne Vanilla-Rahmen; Rahmen und Hintergrund zeichnet das Menü selbst. */
    protected EditBox field(int x, int y, int w, int h, String value, int maxLength, String hint) {
        int textY = y + (h - 8) / 2;
        EditBox box = new EditBox(font, x + 5, textY, w - 10, y + h - textY, Component.literal(hint));
        box.setBordered(false);
        box.setMaxLength(maxLength);
        box.setValue(value);
        if (!hint.isEmpty()) box.setHint(Component.literal(hint));
        fields.add(new Field(box, x, y, w, h));
        return addRenderableWidget(box);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, Theme.DIM);
        Theme.panel(g, panelX, panelY, panelW, panelH);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 20, Theme.HEADER);
        g.fill(panelX + 1, panelY + 20, panelX + panelW - 1, panelY + 21, Theme.PANEL_BORDER);
        for (Field f : fields) {
            Theme.box(g, f.x(), f.y(), f.w(), f.h(), Theme.FIELD, f.box().isFocused() ? Theme.ACCENT : Theme.PANEL_BORDER);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, panelX + panelW / 2, panelY + 6, Theme.TEXT);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
