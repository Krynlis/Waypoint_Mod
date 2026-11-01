package de.kryn.waypoints.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Checkbox mit Beschriftung. */
public class KCheckbox extends AbstractWidget {
    private final Consumer<Boolean> onChange;
    private boolean checked;

    public KCheckbox(int x, int y, int width, String label, boolean checked, Consumer<Boolean> onChange) {
        super(x, y, width, 14, Component.literal(label));
        this.checked = checked;
        this.onChange = onChange;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Theme.box(g, getX(), getY(), 14, 14, Theme.FIELD, isHoveredOrFocused() ? Theme.ACCENT : Theme.PANEL_BORDER);
        if (checked) g.fill(getX() + 3, getY() + 3, getX() + 11, getY() + 11, Theme.ACCENT);
        g.text(Minecraft.getInstance().font, getMessage(), getX() + 20, getY() + 3, Theme.TEXT, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        checked = !checked;
        onChange.accept(checked);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
