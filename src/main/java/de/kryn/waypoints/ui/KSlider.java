package de.kryn.waypoints.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Schiebeleiste mit festen Stufen 0..steps. */
public class KSlider extends AbstractWidget {
    private final int steps;
    private final IntFunction<String> label;
    private final IntConsumer onChange;
    private int step;

    public KSlider(int x, int y, int width, int height, int steps, int step, IntFunction<String> label, IntConsumer onChange) {
        super(x, y, width, height, Component.empty());
        this.steps = steps;
        this.step = Math.max(0, Math.min(steps, step));
        this.label = label;
        this.onChange = onChange;
    }

    private void setFromMouse(double mouseX) {
        double t = (mouseX - getX() - 4) / (width - 8);
        int value = (int) Math.round(Math.max(0, Math.min(1, t)) * steps);
        if (value != step) {
            step = value;
            onChange.accept(step);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Theme.box(g, getX(), getY(), width, height, Theme.FIELD, isHoveredOrFocused() ? Theme.ACCENT : Theme.PANEL_BORDER);
        int knob = getX() + 4 + (int) Math.round((width - 8) * (step / (double) steps));
        g.fill(getX() + 1, getY() + 1, knob, getY() + height - 1, 0x405DADE2);
        g.fill(knob - 2, getY() + 2, knob + 2, getY() + height - 2, Theme.ACCENT);
        g.centeredText(Minecraft.getInstance().font, label.apply(step), getX() + width / 2, getY() + (height - 8) / 2, Theme.TEXT);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setFromMouse(event.x());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        setFromMouse(event.x());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
