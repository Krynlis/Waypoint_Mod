package de.kryn.waypoints.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Flacher Button im Mod-Stil. */
public class KButton extends AbstractWidget {
    private final Runnable onPress;
    private boolean danger;
    /** Hervorgehoben, z.B. für den aktiven Reiter. */
    public boolean selected;

    public KButton(int x, int y, int width, int height, String label, Runnable onPress) {
        super(x, y, width, height, Component.literal(label));
        this.onPress = onPress;
    }

    public KButton danger() {
        this.danger = true;
        return this;
    }

    public void setLabel(String label) {
        setMessage(Component.literal(label));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int fill;
        if (!active) fill = Theme.BUTTON_DISABLED;
        else if (danger) fill = isHovered() ? Theme.DANGER_HOVER : Theme.DANGER;
        else if (selected) fill = 0xFF2F5D7C;
        else fill = isHovered() ? Theme.BUTTON_HOVER : Theme.BUTTON;
        int border = active && (selected || isHoveredOrFocused()) ? Theme.ACCENT : Theme.PANEL_BORDER;
        Theme.box(g, getX(), getY(), width, height, fill, border);
        var font = Minecraft.getInstance().font;
        g.centeredText(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, active ? Theme.TEXT : Theme.DISABLED_TEXT);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
