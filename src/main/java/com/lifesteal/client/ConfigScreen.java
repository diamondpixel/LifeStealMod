package com.lifesteal.client;

import com.lifesteal.Main;
import com.lifesteal.configs.ServerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<ConfigEntry<?>> configEntries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 24;
    private static final int START_Y = 50;
    private static final int BUTTON_AREA_HEIGHT = 70; // Space reserved for buttons at bottom
    private int entriesPerPage;

    public ConfigScreen(Screen parent) {
        super(Component.literal("LifeSteal Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Calculate how many entries can fit based on screen height
        int availableHeight = this.height - START_Y - BUTTON_AREA_HEIGHT;
        entriesPerPage = Math.max(1, availableHeight / ENTRY_HEIGHT);
        
        configEntries.clear();

        // Basic Lifesteal Settings
        addDoubleEntry("Lifesteal Percent", ServerConfig.LIFESTEAL_PERCENT, 0.0, 100.0, "%");
        addDoubleEntry("Damage Increase Percent", ServerConfig.DAMAGE_INCREASE_PERCENT, 0.0, 100.0, "%");
        addDoubleEntry("Cleave Percent", ServerConfig.CLEAVE_PERCENT, 0.0, 100.0, "%");
        addIntEntry("Cleave Max Stacks", ServerConfig.CLEAVE_MAX_STACKS, 1, 6);

        // Moonrise Settings
        addIntEntry("Moonrise Hit Window (ticks)", ServerConfig.MOONRISE_HIT_WINDOW_TICKS, 1, 200);
        addIntEntry("Moonrise Cooldown (ticks)", ServerConfig.MOONRISE_COOLDOWN_TICKS, 0, 6000);
        addDoubleEntry("Moonrise Damage Percent", ServerConfig.MOONRISE_DAMAGE_PERCENT, 0.0, 1.0, "%");
        addIntEntry("Moonrise Effect Duration (ticks)", ServerConfig.MOONRISE_EFFECT_DURATION_TICKS, 1, 200);

        // Death's Dance Settings
        addDoubleEntry("Death's Dance Reduction/Level", ServerConfig.DEATHS_DANCE_REDUCTION_PER_LEVEL, 0.0, 1.0, "%");
        addDoubleEntry("Death's Dance Heal Percent", ServerConfig.DEATHS_DANCE_HEAL_PERCENT, 0.0, 10.0, "%");
        addIntEntry("Death's Dance Damage Interval (ticks)", ServerConfig.DEATHS_DANCE_DAMAGE_INTERVAL_TICKS, 1, 100);

        // Lifeline Settings
        addDoubleEntry("Lifeline Health Threshold", ServerConfig.LIFELINE_HEALTH_THRESHOLD, 0.0, 1.0, "%");
        addIntEntry("Lifeline Cooldown (ticks)", ServerConfig.LIFELINE_COOLDOWN_TICKS, 0, 12000);
        addDoubleEntry("Lifeline Shield Multiplier", ServerConfig.LIFELINE_SHIELD_MULTIPLIER, 0.0, 5.0, "x");
        addIntEntry("Lifeline Shield Duration (ticks)", ServerConfig.LIFELINE_SHIELD_DURATION_TICKS, 1, 600);

        // Nightstalker Settings
        addDoubleEntry("Nightstalker Damage/Level", ServerConfig.NIGHTSTALKER_DAMAGE_PER_LEVEL, 0.0, 1.0, "%");
        addIntEntry("Nightstalker Invulnerability Duration (ticks)", ServerConfig.NIGHTSTALKER_INVISIBILITY_DURATION, 0, 200);
        addIntEntry("Nightstalker Kill Credit Window (ticks)", ServerConfig.NIGHTSTALKER_KILL_CREDIT_WINDOW, 1, 600);

        int yPos = START_Y;
        int visibleEntries = Math.min(configEntries.size() - scrollOffset, entriesPerPage);

        for (int i = 0; i < visibleEntries; i++) {
            int index = i + scrollOffset;
            if (index < configEntries.size()) {
                configEntries.get(index).createWidget(this, 20, yPos, width - 40);
                yPos += ENTRY_HEIGHT;
            }
        }

        // Done button
        this.addRenderableWidget(Button.builder(Component.literal("Done"), (button) -> {
            saveConfig();
            minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());

        // Reset button
        this.addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), (button) -> {
            resetToDefaults();
            minecraft.setScreen(new ConfigScreen(parent));
        }).bounds(this.width / 2 - 100, this.height - 55, 200, 20).build());
    }

    private void addDoubleEntry(String name, ForgeConfigSpec.DoubleValue config, double min, double max, String suffix) {
        configEntries.add(new DoubleConfigEntry(name, config, min, max, suffix));
    }

    private void addIntEntry(String name, ForgeConfigSpec.IntValue config, int min, int max) {
        configEntries.add(new IntConfigEntry(name, config, min, max));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // Draw labels for visible entries
        int yPos = START_Y;
        int visibleEntries = Math.min(configEntries.size() - scrollOffset, entriesPerPage);
        for (int i = 0; i < visibleEntries; i++) {
            int index = i + scrollOffset;
            if (index < configEntries.size()) {
                ConfigEntry<?> entry = configEntries.get(index);
                graphics.drawString(this.font, entry.name, 20, yPos + 5, 0xFFFFFF);
                yPos += ENTRY_HEIGHT;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (configEntries.size() > entriesPerPage) {
            scrollOffset = Math.max(0, Math.min(configEntries.size() - entriesPerPage, 
                scrollOffset - (int) delta));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void saveConfig() {
        for (ConfigEntry<?> entry : configEntries) {
            entry.save();
        }
        ServerConfig.CONFIG.save();
    }

    private void resetToDefaults() {
        for (ConfigEntry<?> entry : configEntries) {
            entry.reset();
        }
        ServerConfig.CONFIG.save();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // Public helper method for inner classes to access protected method
    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addConfigWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    // Abstract base class for config entries
    private abstract static class ConfigEntry<T> {
        protected final String name;
        protected EditBox textField;

        public ConfigEntry(String name) {
            this.name = name;
        }

        public abstract void createWidget(ConfigScreen screen, int x, int y, int width);
        public abstract void save();
        public abstract void reset();
    }

    private static class DoubleConfigEntry extends ConfigEntry<Double> {
        private final ForgeConfigSpec.DoubleValue config;
        private final double min;
        private final double max;
        private final String suffix;

        public DoubleConfigEntry(String name, ForgeConfigSpec.DoubleValue config, double min, double max, String suffix) {
            super(name);
            this.config = config;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
        }

        @Override
        public void createWidget(ConfigScreen screen, int x, int y, int width) {
            textField = new EditBox(screen.minecraft.font, x + width - 100, y, 80, 18, Component.literal(name));
            double displayValue = config.get();
            if (suffix.equals("%")) {
                displayValue *= 100; // Convert to percentage for display
            }
            textField.setValue(String.format("%.2f", displayValue));
            // Only allow numeric input (digits, minus sign, decimal point)
            textField.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
            // Display range in the same format as the value (e.g., percentage)
            double displayMin = suffix.equals("%") ? min * 100 : min;
            double displayMax = suffix.equals("%") ? max * 100 : max;
            textField.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal(String.format("Range: %.2f - %.2f%s", displayMin, displayMax, suffix))));
            screen.addConfigWidget(textField);
        }

        @Override
        public void save() {
            if (textField == null) return; // Skip if widget was never created
            try {
                String input = textField.getValue();
                if (input.isEmpty() || input.equals("-") || input.equals(".") || input.equals("-.")) {
                    // Reset to current value if input is incomplete
                    return;
                }
                double value = Double.parseDouble(input);
                
                // For percentage fields, validate against display range (0-100), then convert
                if (suffix.equals("%")) {
                    value = Math.max(min * 100, Math.min(max * 100, value)); // Clamp in display range
                    value /= 100.0; // Convert to decimal
                } else if (suffix.equals("x")) {
                    // Direct value, clamp normally
                    value = Math.max(min, Math.min(max, value));
                } else {
                    // Clamp value to min/max range
                    value = Math.max(min, Math.min(max, value));
                }
                config.set(value);
            } catch (NumberFormatException e) {
                Main.LOGGER.warn("Invalid value for {}: {}", name, textField.getValue());
                // Keep the current config value
            }
        }

        @Override
        public void reset() {
            Object defaultValue = config.getDefault();
            if (defaultValue instanceof Double) {
                config.set((Double) defaultValue);
            }
        }
    }

    private static class IntConfigEntry extends ConfigEntry<Integer> {
        private final ForgeConfigSpec.IntValue config;
        private final int min;
        private final int max;

        public IntConfigEntry(String name, ForgeConfigSpec.IntValue config, int min, int max) {
            super(name);
            this.config = config;
            this.min = min;
            this.max = max;
        }

        @Override
        public void createWidget(ConfigScreen screen, int x, int y, int width) {
            textField = new EditBox(screen.minecraft.font, x + width - 100, y, 80, 18, Component.literal(name));
            textField.setValue(String.valueOf(config.get()));
            // Only allow numeric input (digits and minus sign)
            textField.setFilter(s -> s.matches("-?\\d*"));
            textField.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal(String.format("Range: %d - %d", min, max))));
            screen.addConfigWidget(textField);
        }

        @Override
        public void save() {
            if (textField == null) return; // Skip if widget was never created
            try {
                String input = textField.getValue().trim();
                if (input.isEmpty() || input.equals("-")) {
                    // Reset to current value if input is incomplete
                    return;
                }
                int value = Integer.parseInt(input);
                // Clamp value to min/max range
                value = Math.max(min, Math.min(max, value));
                config.set(value);
            } catch (NumberFormatException e) {
                Main.LOGGER.warn("Invalid value for {}: {}", name, textField.getValue());
                // Keep the current config value
            }
        }

        @Override
        public void reset() {
            Object defaultValue = config.getDefault();
            if (defaultValue instanceof Integer) {
                config.set((Integer) defaultValue);
            }
        }
    }
}
