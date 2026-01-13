package org.flytomarchik.autosprint.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.flytomarchik.autosprint.AutoSprintClient;
import org.flytomarchik.autosprint.config.ConfigManager;
import org.flytomarchik.autosprint.gui.AutoSprintScreen;
import org.flytomarchik.autosprint.gui.RenderUtils;
import org.flytomarchik.autosprint.theme.ThemeManager;
import org.flytomarchik.autosprint.translation.TranslationManager;

import java.util.Map;

public class LanguageComponent implements UIComponent {

    private final TranslationManager translationManager;
    private final ConfigManager.Config config;
    private final ConfigManager configManager;
    private final AutoSprintScreen screen;

    private boolean expanded = false;

    public LanguageComponent(
            TranslationManager translationManager,
            ConfigManager.Config config,
            ConfigManager configManager,
            AutoSprintScreen screen
    ) {
        this.translationManager = translationManager;
        this.config = config;
        this.configManager = configManager;
        this.screen = screen;
    }

    @Override
    public int getHeight() {
        return expanded
                ? 50 + translationManager.getAvailableLanguages().size() * 35
                : 50;
    }

    @Override
    public void render(
            MatrixStack matrices,
            DrawContext context,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            float delta,
            float pulse,
            ThemeManager.Theme theme,
            TranslationManager translationManager
    ) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, 50);
        int bgColor = hovered
                ? RenderUtils.setAlpha(theme.getAccent(), 30)
                : RenderUtils.setAlpha(0x000000, 40);

        RenderUtils.drawRoundedRect(matrices, x, y, width, 50, 10, bgColor);

        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                "🌐",
                x + 15,
                y + 15,
                theme.getAccent(),
                false
        );

        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                translationManager.get("gui.language"),
                x + 35,
                y + 10,
                0xFFFFFFFF,
                false
        );

        String currentLangName =
                translationManager.getAvailableLanguages()
                        .get(translationManager.getCurrentLanguage());

        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                currentLangName,
                x + 35,
                y + 28,
                theme.getAccent(),
                false
        );

        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                expanded ? "▼" : "▶",
                x + width - 20,
                y + 20,
                0xFF999999,
                false
        );

        if (!expanded) return;

        int listY = y + 55;
        for (Map.Entry<String, String> entry :
                translationManager.getAvailableLanguages().entrySet()) {

            boolean selected =
                    entry.getKey().equals(translationManager.getCurrentLanguage());

            boolean langHovered =
                    isHovered(mouseX, mouseY, x + 10, listY, width - 20, 30);

            int langBg =
                    selected
                            ? RenderUtils.setAlpha(theme.getAccent(), 50)
                            : langHovered
                            ? RenderUtils.setAlpha(0xFFFFFF, 20)
                            : RenderUtils.setAlpha(0x000000, 30);

            RenderUtils.drawRoundedRect(
                    matrices,
                    x + 10,
                    listY,
                    width - 20,
                    30,
                    8,
                    langBg
            );

            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    entry.getValue(),
                    x + 25,
                    listY + 10,
                    selected ? 0xFFFFFFFF : 0xFFCCCCCC,
                    false
            );

            listY += 35;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width) {

        if (isHovered(mouseX, mouseY, x, y, width, 50)) {
            expanded = !expanded;
            click();
            return true;
        }

        if (!expanded) return false;

        int listY = y + 55;
        for (Map.Entry<String, String> entry :
                translationManager.getAvailableLanguages().entrySet()) {

            if (isHovered(mouseX, mouseY, x + 10, listY, width - 20, 30)) {
                String langCode = entry.getKey();

                translationManager.setLanguage(langCode);
                config.language = langCode;
                configManager.save();
                expanded = false;

                click();

                MinecraftClient.getInstance().setScreen(
                        new AutoSprintScreen(
                                null,
                                AutoSprintClient.getConfigManager(),
                                AutoSprintClient.getThemeManager(),
                                AutoSprintClient.getTranslationManager()
                        )
                );
                return true;
            }
            listY += 35;
        }
        return false;
    }

    private void click() {
        MinecraftClient.getInstance().getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK,
                        1.0F
                )
        );
    }
}
