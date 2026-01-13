package org.flytomarchik.autosprint.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.flytomarchik.autosprint.AutoSprintClient;
import org.flytomarchik.autosprint.config.ConfigManager;
import org.flytomarchik.autosprint.gui.components.*;
import org.flytomarchik.autosprint.theme.ThemeManager;
import org.flytomarchik.autosprint.translation.TranslationManager;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoSprintScreen extends Screen {
    private final Screen parent;
    private final ConfigManager configManager;
    private final ThemeManager themeManager;
    private final TranslationManager translationManager;
    private final List<UIComponent> modules = new ArrayList<>();

    private float openAnim = 0;
    private float scrollY = 0, targetScrollY = 0;
    private float themeScrollY = 0, targetThemeScrollY = 0;
    private float configScrollY = 0, targetConfigScrollY = 0;

    private int draggingPanel = -1;
    private double dragOffsetX, dragOffsetY;

    private String inputConfigName = "";
    private boolean isTypingName = false;

    private String pendingDeleteConfig = null;
    private float deleteConfirmAnim = 0f;

    private int themeX, themeY, mainX, mainY, confX, confY;
    private final int themeW = 180, mainW = 320, confW = 200; // Немного расширил панель конфигов для дат
    private final int panelH = 350;

    // Высота одного элемента в списке конфигов
    private final int CONFIG_ITEM_HEIGHT = 40;

    public AutoSprintScreen(Screen parent, ConfigManager configManager, ThemeManager themeManager, TranslationManager translationManager) {
        super(Text.literal("AutoSprint"));
        this.parent = parent;
        this.configManager = configManager;
        this.themeManager = themeManager;
        this.translationManager = translationManager;
    }

    @Override
    protected void init() {
        ConfigManager.Config config = configManager.getConfig();

        // === ИСПРАВЛЕНИЕ: Применяем тему из конфига при инициализации ===
        if (config.theme != null && !config.theme.isEmpty()) {
            themeManager.setTheme(config.theme);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int totalW = themeW + 10 + mainW + 10 + confW;
        int startX = centerX - totalW / 2;
        int startY = centerY - panelH / 2;

        themeX = (config.panelThemeX == -1) ? startX : config.panelThemeX;
        themeY = (config.panelThemeY == -1) ? startY : config.panelThemeY;
        mainX = (config.panelMainX == -1) ? startX + themeW + 10 : config.panelMainX;
        mainY = (config.panelMainY == -1) ? startY : config.panelMainY;
        confX = (config.panelConfigX == -1) ? startX + themeW + 10 + mainW + 10 : config.panelConfigX;
        confY = (config.panelConfigY == -1) ? startY : config.panelConfigY;

        initModules();

        if (config.useBlur) {
            applyBlur();
        } else {
            removeBlur();
        }
    }

    private void initModules() {
        modules.clear();
        ConfigManager.Config config = configManager.getConfig();

        modules.add(new ToggleComponent(translationManager.get("gui.master_switch"), translationManager.get("gui.master_switch.desc"), "⚡",
                () -> config.masterToggle, v -> { config.masterToggle = v; configManager.save(); }, AutoSprintClient.masterKey));

        modules.add(new ToggleComponent(translationManager.get("gui.water_sprint"), translationManager.get("gui.water_sprint.desc"), "🌊",
                () -> config.allowInWater, v -> { config.allowInWater = v; configManager.save(); }, AutoSprintClient.waterKey));

        modules.add(new ToggleComponent(translationManager.get("gui.fly_sprint"), translationManager.get("gui.fly_sprint.desc"), "🕊",
                () -> config.flySprint, v -> { config.flySprint = v; configManager.save(); }, AutoSprintClient.flyKey));

        modules.add(new ToggleComponent("Blur Effect", "Toggle background blur", "🌫",
                () -> config.useBlur, v -> {
            config.useBlur = v;
            configManager.save();
            if(v) applyBlur(); else removeBlur();
        }, AutoSprintClient.blurKey));

        modules.add(new LanguageComponent(translationManager, config, configManager, this));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!configManager.getConfig().useBlur) {
            this.renderBackground(context, mouseX, mouseY, delta);
        }

        openAnim = MathHelper.lerp(delta * 0.15f, openAnim, 1.0f);

        float c1 = 1.70158f;
        float c3 = c1 + 1;
        float scaleAnim = 1 + c3 * (float)Math.pow(openAnim - 1, 3) + c1 * (float)Math.pow(openAnim - 1, 2);

        MatrixStack matrices = context.getMatrices();
        ThemeManager.Theme theme = themeManager.getCurrentTheme();

        matrices.push();
        matrices.translate(width/2f, height/2f, 0);
        matrices.scale(scaleAnim, scaleAnim, 1f);
        matrices.translate(-width/2f, -height/2f, 0);

        if (openAnim > 0.05f) {
            renderThemePanel(matrices, context, mouseX, mouseY, delta, theme);
            renderMainPanel(matrices, context, mouseX, mouseY, delta, theme);
            renderConfigPanel(matrices, context, mouseX, mouseY, delta, theme);
        }

        matrices.pop();

        if (pendingDeleteConfig != null) {
            deleteConfirmAnim = MathHelper.lerp(delta * 0.2f, deleteConfirmAnim, 1.0f);
            renderDeleteConfirmation(matrices, context, mouseX, mouseY, theme);
        } else {
            deleteConfirmAnim = 0f;
        }
    }

    private void renderDeleteConfirmation(MatrixStack matrices, DrawContext context, int mouseX, int mouseY, ThemeManager.Theme theme) {
        int overlayColor = RenderUtils.setAlpha(0x000000, (int)(deleteConfirmAnim * 180));
        context.fill(0, 0, width, height, overlayColor);

        int dialogW = 300;
        int dialogH = 120;
        int dialogX = width / 2 - dialogW / 2;
        int dialogY = height / 2 - dialogH / 2;

        float dialogScale = RenderUtils.easeOutBack(deleteConfirmAnim);
        matrices.push();
        matrices.translate(width / 2f, height / 2f, 0);
        matrices.scale(dialogScale, dialogScale, 1f);
        matrices.translate(-width / 2f, -height / 2f, 0);

        RenderUtils.drawGlassPanel(matrices, dialogX, dialogY, dialogW, dialogH, 12, theme.getTint(), theme.getAccent());

        String title = "Delete Config?";
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, dialogY + 15, 0xFFFFFFFF);

        String msg = "Delete '" + pendingDeleteConfig + "'?";
        context.drawCenteredTextWithShadow(textRenderer, msg, width / 2, dialogY + 35, 0xFFCCCCCC);
        context.drawCenteredTextWithShadow(textRenderer, "This action cannot be undone!", width / 2, dialogY + 50, 0xFFFF5555);

        int btnW = 120;
        int btnH = 30;
        int btnY = dialogY + dialogH - btnH - 15;
        int cancelX = dialogX + 20;
        int confirmX = dialogX + dialogW - btnW - 20;

        boolean cancelHover = isHovered(mouseX, mouseY, cancelX, btnY, btnW, btnH);
        int cancelBg = cancelHover ? RenderUtils.setAlpha(theme.getAccent(), 100) : RenderUtils.setAlpha(0xFFFFFF, 30);
        RenderUtils.drawRoundedRect(matrices, cancelX, btnY, btnW, btnH, 8, cancelBg);
        if (cancelHover) {
            RenderUtils.drawGlowBorder(matrices, cancelX, btnY, btnW, btnH, 8, 2.0f, RenderUtils.setAlpha(theme.getAccent(), 240));
        }
        context.drawCenteredTextWithShadow(textRenderer, "Cancel", cancelX + btnW / 2, btnY + 10, 0xFFFFFFFF);

        boolean confirmHover = isHovered(mouseX, mouseY, confirmX, btnY, btnW, btnH);
        int confirmBg = confirmHover ? RenderUtils.setAlpha(0xFF0000, 180) : RenderUtils.setAlpha(0xFF0000, 120);
        RenderUtils.drawRoundedRect(matrices, confirmX, btnY, btnW, btnH, 8, confirmBg);
        if (confirmHover) {
            RenderUtils.drawGlowBorder(matrices, confirmX, btnY, btnW, btnH, 8, 2.0f, RenderUtils.setAlpha(0xFF0000, 240));
        }
        context.drawCenteredTextWithShadow(textRenderer, "Delete", confirmX + btnW / 2, btnY + 10, 0xFFFFFFFF);

        matrices.pop();
    }

    private void renderThemePanel(MatrixStack matrices, DrawContext context, int mouseX, int mouseY, float delta, ThemeManager.Theme theme) {
        RenderUtils.drawGlassPanel(matrices, themeX, themeY, themeW, panelH, 12, theme.getTint(), theme.getAccent());

        RenderUtils.drawRoundedRect(matrices, themeX, themeY, themeW, 30, 12, RenderUtils.setAlpha(theme.getAccent(), 50));
        context.drawText(textRenderer, translationManager.get("gui.theme"), themeX + 15, themeY + 10, 0xFFFFFFFF, false);

        context.enableScissor(themeX, themeY + 35, themeX + themeW, themeY + panelH - 10);
        themeScrollY += (targetThemeScrollY - themeScrollY) * delta * 0.3f;
        int y = themeY + 40 - (int)themeScrollY;

        for (Map.Entry<String, ThemeManager.Theme> entry : themeManager.getAllThemes().entrySet()) {
            boolean selected = entry.getValue().equals(theme);
            boolean hovered = isHovered(mouseX, mouseY, themeX + 10, y, themeW - 20, 28);
            int bg = selected ? RenderUtils.setAlpha(theme.getAccent(), 100) : (hovered ? RenderUtils.setAlpha(0xFFFFFF, 30) : RenderUtils.setAlpha(0x000000, 30));
            RenderUtils.drawRoundedRect(matrices, themeX + 10, y, themeW - 20, 28, 6, bg);

            if (selected) {
                RenderUtils.drawGlowBorder(matrices, themeX + 10, y, themeW - 20, 28, 6, 2.5f, RenderUtils.setAlpha(theme.getAccent(), 240));
            }

            context.drawText(textRenderer, entry.getValue().getName(), themeX + 20, y + 10, selected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            RenderUtils.drawRoundedRect(matrices, themeX + themeW - 35, y + 7, 14, 14, 4, entry.getValue().getAccent());
            y += 32;
        }

        context.disableScissor();
    }

    private void renderMainPanel(MatrixStack matrices, DrawContext context, int mouseX, int mouseY, float delta, ThemeManager.Theme theme) {
        RenderUtils.drawGlassPanel(matrices, mainX, mainY, mainW, panelH, 12, theme.getTint(), theme.getAccent());

        RenderUtils.drawRoundedRect(matrices, mainX, mainY, mainW, 30, 12, RenderUtils.setAlpha(theme.getAccent(), 50));
        context.drawText(textRenderer, translationManager.get("gui.title"), mainX + 15, mainY + 10, 0xFFFFFFFF, false);

        if (isHovered(mouseX, mouseY, mainX + mainW - 25, mainY + 5, 20, 20)) {
            context.drawText(textRenderer, "⟲", mainX + mainW - 20, mainY + 10, 0xFFFFFFFF, false);
        } else {
            context.drawText(textRenderer, "⟲", mainX + mainW - 20, mainY + 10, 0xFFAAAAAA, false);
        }

        context.enableScissor(mainX, mainY + 35, mainX + mainW, mainY + panelH - 10);
        scrollY += (targetScrollY - scrollY) * delta * 0.3f;
        int y = mainY + 40 - (int)scrollY;

        for (UIComponent comp : modules) {
            comp.render(matrices, context, mainX + 10, y, mainW - 20, mouseX, mouseY, delta, 0, theme, translationManager);
            y += comp.getHeight() + 5;
        }

        context.disableScissor();
    }

    private void renderConfigPanel(MatrixStack matrices, DrawContext context, int mouseX, int mouseY, float delta, ThemeManager.Theme theme) {
        RenderUtils.drawGlassPanel(matrices, confX, confY, confW, panelH, 12, theme.getTint(), theme.getAccent());

        RenderUtils.drawRoundedRect(matrices, confX, confY, confW, 30, 12, RenderUtils.setAlpha(theme.getAccent(), 50));
        context.drawText(textRenderer, "Configs", confX + 15, confY + 10, 0xFFFFFFFF, false);

        int contentY = confY + 40;

        int inputW = confW - 45;
        int plusBtnX = confX + 10 + inputW + 5;

        boolean inputHover = isHovered(mouseX, mouseY, confX + 10, contentY, inputW, 20);
        int inputColor = isTypingName ? RenderUtils.setAlpha(theme.getAccent(), 50) : (inputHover ? RenderUtils.setAlpha(0xFFFFFF, 30) : RenderUtils.setAlpha(0x000000, 50));
        RenderUtils.drawRoundedRect(matrices, confX + 10, contentY, inputW, 20, 5, inputColor);

        if (isTypingName) {
            RenderUtils.drawGlowBorder(matrices, confX + 10, contentY, inputW, 20, 5, 2.5f, RenderUtils.setAlpha(theme.getAccent(), 240));
        }

        String displayText = inputConfigName.isEmpty() && !isTypingName ? "Name..." : inputConfigName + (isTypingName && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");
        if (textRenderer.getWidth(displayText) > inputW - 10) {
            displayText = textRenderer.trimToWidth(displayText, inputW - 10, true);
        }
        context.drawText(textRenderer, displayText, confX + 15, contentY + 6, 0xFFDDDDDD, false);

        boolean plusHover = isHovered(mouseX, mouseY, plusBtnX, contentY, 20, 20);
        int plusColor = plusHover ? theme.getAccent() : RenderUtils.setAlpha(theme.getAccent(), 150);
        RenderUtils.drawRoundedRect(matrices, plusBtnX, contentY, 20, 20, 5, plusColor);
        context.drawCenteredTextWithShadow(textRenderer, "+", plusBtnX + 10, contentY + 6, 0xFFFFFFFF);

        contentY += 30;
        context.drawText(textRenderer, "Files:", confX + 15, contentY, 0xFFAAAAAA, false);
        contentY += 15;

        context.enableScissor(confX, contentY, confX + confW, confY + panelH - 10);
        configScrollY += (targetConfigScrollY - configScrollY) * delta * 0.3f;
        int listY = contentY - (int)configScrollY;

        List<String> configs = configManager.listConfigs();

        for (String cfgName : configs) {
            boolean isCurrent = cfgName.equals(configManager.getCurrentConfigName());
            boolean itemHover = isHovered(mouseX, mouseY, confX + 10, listY, confW - 20, CONFIG_ITEM_HEIGHT - 5);

            // Получаем метаданные для даты
            ConfigManager.ConfigMetadata meta = configManager.getMetadata(cfgName);
            String dateStr = (meta != null) ? meta.getFormattedLastModified() : "Unknown";

            int bg = isCurrent ? RenderUtils.setAlpha(theme.getAccent(), 70) : (itemHover ? RenderUtils.setAlpha(0xFFFFFF, 20) : RenderUtils.setAlpha(0x000000, 30));
            RenderUtils.drawRoundedRect(matrices, confX + 10, listY, confW - 20, CONFIG_ITEM_HEIGHT - 5, 5, bg);

            if (isCurrent) {
                RenderUtils.drawGlowBorder(matrices, confX + 10, listY, confW - 20, CONFIG_ITEM_HEIGHT - 5, 5, 2.5f, RenderUtils.setAlpha(theme.getAccent(), 240));
            }

            // Имя конфига
            context.drawText(textRenderer, cfgName, confX + 15, listY + 5, isCurrent ? theme.getAccent() : 0xFFFFFFFF, false);

            // === НОВОЕ: Дата изменения (серый, поменьше) ===
            matrices.push();
            matrices.scale(0.8f, 0.8f, 1f);
            context.drawText(textRenderer, dateStr, (int)((confX + 15) / 0.8f), (int)((listY + 18) / 0.8f), 0xFFAAAAAA, false);
            matrices.pop();

            // === НОВОЕ: Надпись ACTIVE ===
            if (isCurrent) {
                String activeText = "[ACTIVE]";
                int activeWidth = textRenderer.getWidth(activeText);
                // Рисуем зеленым справа
                matrices.push();
                matrices.scale(0.8f, 0.8f, 1f);
                context.drawText(textRenderer, activeText,
                        (int)((confX + confW - 25) / 0.8f) - activeWidth,
                        (int)((listY + 6) / 0.8f),
                        0xFF55FF55, false); // Ярко-зеленый
                matrices.pop();
            }

            // Кнопка удаления (только если не активный)
            if (!isCurrent) {
                int delX = confX + confW - 30;
                boolean delHover = isHovered(mouseX, mouseY, delX, listY + 5, 20, 20);
                context.drawText(textRenderer, "x", delX + 6, listY + 10, delHover ? 0xFFFF0000 : 0xFF777777, false);
            }
            listY += CONFIG_ITEM_HEIGHT;
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingDeleteConfig != null) {
            int dialogW = 300;
            int dialogH = 120;
            int dialogX = width / 2 - dialogW / 2;
            int dialogY = height / 2 - dialogH / 2;
            int btnW = 120;
            int btnH = 30;
            int btnY = dialogY + dialogH - btnH - 15;
            int cancelX = dialogX + 20;
            int confirmX = dialogX + dialogW - btnW - 20;

            if (isHovered(mouseX, mouseY, cancelX, btnY, btnW, btnH)) {
                pendingDeleteConfig = null;
                return true;
            }

            if (isHovered(mouseX, mouseY, confirmX, btnY, btnW, btnH)) {
                configManager.deleteConfig(pendingDeleteConfig);
                pendingDeleteConfig = null;
                targetConfigScrollY = Math.max(0, targetConfigScrollY - CONFIG_ITEM_HEIGHT);
                return true;
            }

            if (!isHovered(mouseX, mouseY, dialogX, dialogY, dialogW, dialogH)) {
                pendingDeleteConfig = null;
                return true;
            }

            return true;
        }

        int contentY = confY + 40;
        int inputW = confW - 45;

        isTypingName = isHovered(mouseX, mouseY, confX + 10, contentY, inputW, 20);

        int plusBtnX = confX + 10 + inputW + 5;
        if (isHovered(mouseX, mouseY, plusBtnX, contentY, 20, 20)) {
            createConfigAction();
            return true;
        }

        if (isHovered(mouseX, mouseY, themeX, themeY, themeW, 30)) {
            draggingPanel = 0;
            dragOffsetX = mouseX - themeX;
            dragOffsetY = mouseY - themeY;
            return true;
        }

        if (isHovered(mouseX, mouseY, mainX, mainY, mainW, 30)) {
            if (isHovered(mouseX, mouseY, mainX + mainW - 25, mainY + 5, 20, 20)) {
                resetPositions();
                return true;
            }
            draggingPanel = 1;
            dragOffsetX = mouseX - mainX;
            dragOffsetY = mouseY - mainY;
            return true;
        }

        if (isHovered(mouseX, mouseY, confX, confY, confW, 30)) {
            draggingPanel = 2;
            dragOffsetX = mouseX - confX;
            dragOffsetY = mouseY - confY;
            return true;
        }

        int listStartY = confY + 85;
        if (isHovered(mouseX, mouseY, confX, listStartY, confW, panelH - 95)) {
            int currentY = listStartY - (int)configScrollY;
            List<String> configs = configManager.listConfigs();

            for (String cfgName : configs) {
                // Используем CONFIG_ITEM_HEIGHT
                if (mouseY >= currentY && mouseY <= currentY + CONFIG_ITEM_HEIGHT - 5) {
                    boolean isCurrent = cfgName.equals(configManager.getCurrentConfigName());

                    // Зона удаления
                    if (!isCurrent && mouseX >= confX + confW - 30 && mouseX <= confX + confW - 10) {
                        pendingDeleteConfig = cfgName;
                        return true;
                    }

                    // Зона выбора конфига
                    if (mouseX >= confX + 10 && mouseX <= confX + confW - 35) {
                        configManager.load(cfgName);

                        // === ВАЖНО: Обновляем тему сразу после загрузки конфига ===
                        String loadedTheme = configManager.getConfig().theme;
                        if(loadedTheme != null) {
                            themeManager.setTheme(loadedTheme);
                        }

                        initModules();
                        return true;
                    }
                }
                currentY += CONFIG_ITEM_HEIGHT;
            }
        }

        if (isHovered(mouseX, mouseY, mainX, mainY + 35, mainW, panelH - 35)) {
            int y = mainY + 40 - (int)scrollY;
            for (UIComponent comp : modules) {
                if (comp.mouseClicked(mouseX, mouseY, mainX + 10, y, mainW - 20)) return true;
                y += comp.getHeight() + 5;
            }
        }

        if (isHovered(mouseX, mouseY, themeX, themeY + 35, themeW, panelH - 35)) {
            int y = themeY + 40 - (int)themeScrollY;
            for (Map.Entry<String, ThemeManager.Theme> entry : themeManager.getAllThemes().entrySet()) {
                if (mouseY >= y && mouseY <= y + 28) {
                    themeManager.setTheme(entry.getKey());
                    configManager.getConfig().theme = entry.getKey();
                    configManager.save();
                    return true;
                }
                y += 32;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void createConfigAction() {
        if (!inputConfigName.isEmpty()) {
            configManager.createNewConfig(inputConfigName);
            inputConfigName = "";
            initModules();
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPanel == 0) {
            themeX = (int)(mouseX - dragOffsetX);
            themeY = (int)(mouseY - dragOffsetY);
        }
        if (draggingPanel == 1) {
            mainX = (int)(mouseX - dragOffsetX);
            mainY = (int)(mouseY - dragOffsetY);
        }
        if (draggingPanel == 2) {
            confX = (int)(mouseX - dragOffsetX);
            confY = (int)(mouseY - dragOffsetY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanel = -1;
        ConfigManager.Config cfg = configManager.getConfig();
        cfg.panelThemeX = themeX;
        cfg.panelThemeY = themeY;
        cfg.panelMainX = mainX;
        cfg.panelMainY = mainY;
        cfg.panelConfigX = confX;
        cfg.panelConfigY = confY;
        configManager.save();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isTypingName) {
            if (chr >= 32 && chr <= 126 && inputConfigName.length() < 16) {
                inputConfigName += chr;
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pendingDeleteConfig != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                pendingDeleteConfig = null;
                return true;
            }
            return false;
        }

        if (isTypingName) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputConfigName.isEmpty()) {
                inputConfigName = inputConfigName.substring(0, inputConfigName.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                createConfigAction();
                isTypingName = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isTypingName = false;
                return true;
            }
            return false;
        }

        for(UIComponent c : modules) {
            if(c instanceof ToggleComponent) {
                if(((ToggleComponent)c).keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (isHovered(mouseX, mouseY, themeX, themeY, themeW, panelH)) {
            int max = Math.max(0, themeManager.getAllThemes().size() * 32 - (panelH - 50));
            targetThemeScrollY = MathHelper.clamp(targetThemeScrollY - (float)vertical * 20, 0, max);
        } else if (isHovered(mouseX, mouseY, mainX, mainY, mainW, panelH)) {
            int contentH = modules.stream().mapToInt(c -> c.getHeight() + 5).sum();
            int max = Math.max(0, contentH - (panelH - 50));
            targetScrollY = MathHelper.clamp(targetScrollY - (float)vertical * 20, 0, max);
        } else if (isHovered(mouseX, mouseY, confX, confY, confW, panelH)) {
            // Исправлен расчет высоты контента с новым размером элементов
            int contentH = configManager.listConfigs().size() * CONFIG_ITEM_HEIGHT;
            int max = Math.max(0, contentH - (panelH - 95));
            targetConfigScrollY = MathHelper.clamp(targetConfigScrollY - (float)vertical * 20, 0, max);
        }
        return true;
    }

    private void resetPositions() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int totalW = themeW + 10 + mainW + 10 + confW;
        int startX = centerX - totalW / 2;
        int startY = centerY - panelH / 2;

        themeX = startX;
        themeY = startY;
        mainX = startX + themeW + 10;
        mainY = startY;
        confX = startX + themeW + 10 + mainW + 10;
        confY = startY;

        mouseReleased(0, 0, 0);
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    protected void applyBlur() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && mc.gameRenderer != null) {
            try {
                Identifier blurId = Identifier.of("minecraft", "blur");
                for (Method m : net.minecraft.client.render.GameRenderer.class.getDeclaredMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Identifier.class && m.getReturnType() == void.class) {
                        m.setAccessible(true);
                        try {
                            m.invoke(mc.gameRenderer, blurId);
                            return;
                        } catch (Exception e) {
                            blurId = Identifier.of("minecraft", "shaders/post/blur.json");
                            m.invoke(mc.gameRenderer, blurId);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private void removeBlur() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer != null) {
            try {
                for (Method m : net.minecraft.client.render.GameRenderer.class.getDeclaredMethods()) {
                    if ((m.getName().contains("disable") || m.getName().contains("clear"))
                            && m.getParameterCount() == 0
                            && m.getReturnType() == void.class) {
                        m.setAccessible(true);
                        try {
                            m.invoke(mc.gameRenderer);
                            return;
                        } catch (Exception ignored) {
                        }
                    }
                }

                for (Method m : net.minecraft.client.render.GameRenderer.class.getDeclaredMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Identifier.class) {
                        m.setAccessible(true);
                        try {
                            m.invoke(mc.gameRenderer, (Identifier) null);
                            return;
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void close() {
        removeBlur();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        removeBlur();
        super.removed();
    }
}