package org.flytomarchik.autosprint.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.flytomarchik.autosprint.config.ConfigManager;
import org.flytomarchik.autosprint.gui.RenderUtils;
import org.flytomarchik.autosprint.theme.ThemeManager;
import org.flytomarchik.autosprint.translation.TranslationManager;

public class SprintModeComponent implements UIComponent {
    private final ConfigManager.Config config;
    private final ConfigManager configManager;
    private final TranslationManager translationManager;

    public SprintModeComponent(ConfigManager.Config config, ConfigManager configManager, TranslationManager translationManager) {
        this.config = config;
        this.configManager = configManager;
        this.translationManager = translationManager;
    }

    @Override
    public int getHeight() { return 70; }

    @Override
    public void render(MatrixStack matrices, DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta, float pulse, ThemeManager.Theme theme, TranslationManager translationManager) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, 70);
        int bgColor = hovered ? RenderUtils.setAlpha(theme.getAccent(), 30) : RenderUtils.setAlpha(0x000000, 40);
        RenderUtils.drawRoundedRect(matrices, x, y, width, 70, 10, bgColor);
        RenderUtils.drawRoundedRect(matrices, x + 4, y + 25, 3, 20, 1.5f, theme.getAccent());

        String icon = getModeIcon(config.sprintMode);
        context.drawText(MinecraftClient.getInstance().textRenderer, icon, x + 15, y + 15, theme.getAccent(), false);
        context.drawText(MinecraftClient.getInstance().textRenderer, translationManager.get("gui.sprint_mode"), x + 35, y + 10, 0xFFFFFFFF, false);

        String modeName = translationManager.get(getModeKey(config.sprintMode));
        context.drawText(MinecraftClient.getInstance().textRenderer, modeName, x + 35, y + 28, theme.getAccent(), false);

        String modeDesc = translationManager.get(getModeKey(config.sprintMode) + ".desc");
        context.drawText(MinecraftClient.getInstance().textRenderer, modeDesc, x + 35, y + 45, 0xFF999999, false);

        context.drawText(MinecraftClient.getInstance().textRenderer, "▶", x + width - 20, y + 30, 0xFF999999, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width) {
        if (isHovered(mouseX, mouseY, x, y, width, 70)) {
            ConfigManager.Config.SprintMode[] modes = ConfigManager.Config.SprintMode.values();
            int currentIndex = config.sprintMode.ordinal();
            config.sprintMode = modes[(currentIndex + 1) % modes.length];
            configManager.save();
            MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return false;
    }

    private String getModeIcon(ConfigManager.Config.SprintMode mode) {
        switch (mode) {
            case FORWARD_ONLY: return "→";
            case OMNI_DIRECTIONAL: return "↔";
            case TOGGLE_SPRINT: return "⟲";
            case RAGE_SPRINT: return "⚡";
            default: return "→";
        }
    }

    private String getModeKey(ConfigManager.Config.SprintMode mode) {
        switch (mode) {
            case FORWARD_ONLY: return "mode.forward_only";
            case OMNI_DIRECTIONAL: return "mode.omni";
            case TOGGLE_SPRINT: return "mode.toggle";
            case RAGE_SPRINT: return "mode.rage";
            default: return "mode.toggle";
        }
    }
}