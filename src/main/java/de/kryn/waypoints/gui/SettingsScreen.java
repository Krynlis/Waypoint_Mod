package de.kryn.waypoints.gui;

import de.kryn.waypoints.data.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Kryn´s Waypoints – Optionen"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Settings settings = Settings.get();
        int x = width / 2 - 110;
        int y = height / 2 - 50;

        addRenderableWidget(new DistanceSlider(x, y, 220, 20));
        addRenderableWidget(new SizeSlider(x, y + 26, 220, 20));
        addRenderableWidget(Checkbox.builder(Component.literal("Marker 50% transparent"), font)
                .pos(x, y + 54)
                .selected(settings.transparent)
                .onValueChange((checkbox, value) -> settings.transparent = value)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(width / 2 - 75, y + 90, 150, 20).build());
    }

    @Override
    public void onClose() {
        Settings.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, height / 2 - 75, 0xFFFFFFFF);
    }

    private static class DistanceSlider extends AbstractSliderButton {
        private static final int STEPS = (Settings.MAX_DISTANCE - Settings.MIN_DISTANCE) / Settings.DISTANCE_STEP;

        DistanceSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (Settings.get().switchDistance - Settings.MIN_DISTANCE) / (double) (Settings.MAX_DISTANCE - Settings.MIN_DISTANCE));
            updateMessage();
        }

        private int distance() {
            return Settings.MIN_DISTANCE + (int) Math.round(value * STEPS) * Settings.DISTANCE_STEP;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("XP-Leiste ab: " + distance() + " Blöcke"));
        }

        @Override
        protected void applyValue() {
            Settings.get().switchDistance = distance();
        }
    }

    private static class SizeSlider extends AbstractSliderButton {
        private static final int MAX = Settings.MARKER_SIZES.length - 1;

        SizeSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), Settings.get().markerSize / (double) MAX);
            updateMessage();
        }

        private int index() {
            return (int) Math.round(value * MAX);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Markergröße: " + Settings.MARKER_SIZE_NAMES[index()]));
        }

        @Override
        protected void applyValue() {
            Settings.get().markerSize = index();
        }
    }
}
