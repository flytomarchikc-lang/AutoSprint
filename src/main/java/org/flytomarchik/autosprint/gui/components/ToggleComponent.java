package org.flytomarchik.autosprint.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.flytomarchik.autosprint.gui.RenderUtils;
import org.flytomarchik.autosprint.theme.ThemeManager;
import org.flytomarchik.autosprint.translation.TranslationManager;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ToggleComponent implements UIComponent {
    private final String name;
    private final String description;
    private final String icon;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final KeyBinding keyBinding;

    private float animation = 0f;
    private float hoverAnimation = 0f;
    private float clickAnim = 0f;
    private float bounceAnim = 0f;
    private long lastToggleTime = 0L;

    private boolean isBinding = false;
    private float bindHoverAnim = 0f;

    public ToggleComponent(String name, String description, String icon, BooleanSupplier getter, Consumer<Boolean> setter, KeyBinding keyBinding) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.getter = getter;
        this.setter = setter;
        this.keyBinding = keyBinding;
        this.animation = getter.getAsBoolean() ? 1f : 0f;
    }

    @Override
    public int getHeight() { return 50; }

    @Override
    public void render(MatrixStack matrices, DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta, float pulse, ThemeManager.Theme theme, TranslationManager translationManager) {
        boolean enabled = getter.getAsBoolean();
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, 50);

        float target = enabled ? 1f : 0f;
        animation = MathHelper.clamp(MathHelper.lerp(delta * 0.25f, animation, target), 0f, 1f);
        hoverAnimation = MathHelper.clamp(MathHelper.lerp(delta * 0.3f, hoverAnimation, hovered ? 1f : 0f), 0f, 1f);

        if (clickAnim > 0f) {
            clickAnim = Math.max(0f, clickAnim - delta * 0.05f);
        }

        long timeSinceToggle = System.currentTimeMillis() - lastToggleTime;
        if (timeSinceToggle < 500) {
            float progress = timeSinceToggle / 500f;
            bounceAnim = (float)Math.sin(progress * Math.PI * 3) * (1 - progress) * 0.15f;
        } else {
            bounceAnim = 0f;
        }

        // Фон модуля
        int bgColor = RenderUtils.lerpColor(
                RenderUtils.setAlpha(theme.getTint(), 60), // Чуть темнее фон
                RenderUtils.setAlpha(theme.getAccent(), 40), // Включенный фон
                animation
        );

        if (hoverAnimation > 0f) {
            bgColor = RenderUtils.addColors(bgColor, RenderUtils.setAlpha(theme.getAccent(), (int)(hoverAnimation * 30)));
        }

        float scaleX = 1.0f + bounceAnim + clickAnim * 0.05f;
        float scaleY = 1.0f + bounceAnim + clickAnim * 0.05f;

        matrices.push();
        matrices.translate(x + width / 2f, y + 25, 0);
        matrices.scale(scaleX, scaleY, 1f);
        matrices.translate(-(x + width / 2f), -(y + 25), 0);

        RenderUtils.drawRoundedRect(matrices, x, y, width, 50, 10, bgColor);

        // Полоска слева (индикатор)
        if (animation > 0.01f) {
            float barHeight = 26 * RenderUtils.easeOutBack(animation);
            barHeight = MathHelper.clamp(barHeight, 0, 40);
            int barColor = RenderUtils.setAlpha(theme.getAccent(), (int)(animation * 255));
            RenderUtils.drawRoundedRect(matrices, x + 4, (int)(y + 25 - barHeight / 2), 3, (int)barHeight, 1, barColor);
        }

        // === ГЛАВНОЕ ИЗМЕНЕНИЕ: ОБВОДКА МОДУЛЯ ===
        // Рисуем обводку ВСЕГДА цветом темы
        int borderAlpha = 60 + (int)(animation * 195); // От 60 (выкл) до 255 (вкл)
        float borderThick = 1.5f + (animation * 1.5f); // От 1.5 (выкл) до 3.0 (вкл)

        // Если наведен курсор, делаем обводку чуть ярче даже если выключен
        if (hoverAnimation > 0) borderAlpha += (int)(hoverAnimation * 50);
        borderAlpha = MathHelper.clamp(borderAlpha, 0, 255);

        RenderUtils.drawGlowBorder(matrices, x, y, width, 50, 10, borderThick, RenderUtils.setAlpha(theme.getAccent(), borderAlpha));

        // Верхний градиент при наведении
        if (hoverAnimation > 0.1f) {
            int overlayTop = RenderUtils.setAlpha(theme.getAccent(), (int)(hoverAnimation * 20));
            int overlayBot = RenderUtils.setAlpha(theme.getAccent(), 0);
            RenderUtils.drawGradientRect(matrices, x, y, width, 25, 10, overlayTop, overlayBot);
        }

        matrices.pop();

        // Иконка и текст
        float iconScale = 1.0f;
        int iconColor = RenderUtils.lerpColor(0xFFAAAAAA, theme.getAccent(), animation);
        if (enabled) {
            float pulseVal = 0.9f + 0.1f * (float)Math.sin(System.currentTimeMillis() / 300.0);
            iconScale = pulseVal;
        }

        matrices.push();
        matrices.translate(x + 15, y + 15, 0);
        matrices.scale(iconScale, iconScale, 1f);
        matrices.translate(-(x + 15), -(y + 15), 0);
        context.drawText(MinecraftClient.getInstance().textRenderer, icon, x + 10, y + 10, iconColor, false);
        matrices.pop();

        context.drawText(MinecraftClient.getInstance().textRenderer, name, x + 35, y + 8, 0xFFFFFFFF, false);

        int rightSpace = 85;
        int maxTextWidth = width - 50 - rightSpace;
        String trimmedDesc = MinecraftClient.getInstance().textRenderer.trimToWidth(description, maxTextWidth);
        context.drawText(MinecraftClient.getInstance().textRenderer, trimmedDesc, x + 35, y + 26, 0xFF999999, false);

        renderBindButton(matrices, context, x, y, width, mouseX, mouseY, delta, theme);
        renderSwitch(matrices, context, x, y, width, theme, pulse);
    }

    private void renderSwitch(MatrixStack matrices, DrawContext context, int x, int y, int width, ThemeManager.Theme theme, float pulse) {
        int switchWidth = 32;
        int switchX = x + width - 40;
        int switchY = y + 16;

        // Фон свитча тоже берет цвет темы
        int trackColor = RenderUtils.lerpColor(
                RenderUtils.setAlpha(theme.getTint(), 200),
                theme.getAccent(),
                animation
        );
        RenderUtils.drawRoundedRect(matrices, switchX, switchY, switchWidth, 18, 9, RenderUtils.setAlpha(trackColor, 150));

        // Обводка свитча всегда цветом темы
        RenderUtils.drawGlowBorder(matrices, switchX, switchY, switchWidth, 18, 9, 1.5f, RenderUtils.setAlpha(theme.getAccent(), 150));

        float springPos = RenderUtils.easeOutBack(animation);
        float clampedSpring = MathHelper.clamp(springPos, -0.2f, 1.2f);
        float knobX = MathHelper.lerp(MathHelper.clamp(animation, 0, 1), switchX + 2, switchX + switchWidth - 16);
        float springOffset = (clampedSpring - animation) * 10;

        float knobScale = 1.0f + bounceAnim * 2 + clickAnim * 0.05f;
        matrices.push();
        matrices.translate(knobX + springOffset, switchY + 9, 0);
        matrices.scale(knobScale, knobScale, 1f);
        matrices.translate(-(knobX + springOffset), -(switchY + 9), 0);
        RenderUtils.drawRoundedRect(matrices, (int)(knobX + springOffset), switchY + 2, 14, 14, 7, 0xFFFFFFFF);
        matrices.pop();

        if (Math.abs(animation - (getter.getAsBoolean() ? 1f : 0f)) > 0.05f || animation > 0.1f) {
            int glowAlpha = (int)(animation * 240 + (pulse * 50 * animation));
            RenderUtils.drawGlowBorder(matrices, switchX - 2, switchY - 2, switchWidth + 4, 22, 10, 3.0f,
                    RenderUtils.setAlpha(theme.getAccent(), MathHelper.clamp(glowAlpha, 0, 255)));
        }
    }

    private void renderBindButton(MatrixStack matrices, DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta, ThemeManager.Theme theme) {
        if (keyBinding == null) return;

        String bindText = isBinding ? "..." : (keyBinding.isUnbound() ? "NONE" : keyBinding.getBoundKeyLocalizedText().getString().toUpperCase());
        if (bindText.length() > 5 && !isBinding) bindText = bindText.substring(0, 4) + ".";

        int bindWidth = MinecraftClient.getInstance().textRenderer.getWidth(bindText) + 8;
        if (bindWidth < 30) bindWidth = 30;

        int bindX = x + width - 45 - bindWidth - 5;
        int bindY = y + 16;
        int bindHeight = 18;

        boolean bindHover = isHovered(mouseX, mouseY, bindX, bindY, bindWidth, bindHeight);
        bindHoverAnim = MathHelper.lerp(delta * 0.3f, bindHoverAnim, bindHover ? 1f : 0f);

        int bgColor;
        if (isBinding) {
            bgColor = RenderUtils.setAlpha(theme.getAccent(), 120);
        } else {
            bgColor = RenderUtils.lerpColor(
                    RenderUtils.setAlpha(theme.getTint(), 40),
                    RenderUtils.setAlpha(theme.getAccent(), 60),
                    bindHoverAnim
            );
        }
        RenderUtils.drawRoundedRect(matrices, bindX, bindY, bindWidth, bindHeight, 5, bgColor);

        // Всегда рисуем обводку бинда цветом темы
        int glowAlpha = isBinding ? 255 : (int)(100 + bindHoverAnim * 155);
        RenderUtils.drawGlowBorder(matrices, bindX, bindY, bindWidth, bindHeight, 5, 2.0f,
                RenderUtils.setAlpha(theme.getAccent(), glowAlpha));

        if (bindHoverAnim > 0f) {
            float scale = 1.0f + bindHoverAnim * 0.05f;
            matrices.push();
            matrices.translate(bindX + bindWidth / 2f, bindY + bindHeight / 2f, 0);
            matrices.scale(scale, scale, 1f);
            matrices.translate(-(bindX + bindWidth / 2f), -(bindY + bindHeight / 2f), 0);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, bindText, bindX + bindWidth / 2, bindY + 5, 0xFFFFFFFF);
            matrices.pop();
        } else {
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, bindText, bindX + bindWidth / 2, bindY + 5, 0xFFFFFFFF);
        }
    }

    // ... mouseClicked, keyPressed методы остаются такими же ...
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width) {
        if (keyBinding != null) {
            String bindText = keyBinding.isUnbound() ? "NONE" : keyBinding.getBoundKeyLocalizedText().getString().toUpperCase();
            if (bindText.length() > 5) bindText = bindText.substring(0, 4) + ".";
            int bindWidth = MinecraftClient.getInstance().textRenderer.getWidth(bindText) + 8;
            if (bindWidth < 30) bindWidth = 30;
            int bindX = x + width - 45 - bindWidth - 5;

            if (isHovered(mouseX, mouseY, bindX, y + 16, bindWidth, 18)) {
                isBinding = !isBinding;
                MinecraftClient.getInstance().getSoundManager().play(
                        net.minecraft.client.sound.PositionedSoundInstance.master(
                                net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        if (isHovered(mouseX, mouseY, x, y, width, 50) && !isBinding) {
            boolean newState = !getter.getAsBoolean();
            setter.accept(newState);
            lastToggleTime = System.currentTimeMillis();
            clickAnim = 1.0f;

            int particleColor = newState ? 0x00FF00 : 0xFF0000;
            RenderUtils.spawnParticles(x, y, width, 50, particleColor, 20);
            RenderUtils.createRipple((int)mouseX, (int)mouseY, particleColor);

            MinecraftClient.getInstance().getSoundManager().play(
                    net.minecraft.client.sound.PositionedSoundInstance.master(
                            net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        isBinding = false;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isBinding && keyBinding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                keyBinding.setBoundKey(InputUtil.UNKNOWN_KEY);
            } else {
                keyBinding.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            }
            MinecraftClient.getInstance().options.write();
            KeyBinding.updateKeysByCode();
            isBinding = false;
            return true;
        }

        if (keyBinding != null && keyBinding.matchesKey(keyCode, scanCode)) {
            boolean newState = !getter.getAsBoolean();
            setter.accept(newState);
            lastToggleTime = System.currentTimeMillis();
            clickAnim = 1.0f;

            int color = newState ? 0x00FF00 : 0xFF0000;
            RenderUtils.spawnParticles(0, 0, 0, 50, color, 20);
            return true;
        }

        return false;
    }

    public boolean getState() { return getter.getAsBoolean(); }

    public boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}