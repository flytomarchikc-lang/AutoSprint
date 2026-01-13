package org.flytomarchik.autosprint.gui.components;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.flytomarchik.autosprint.theme.ThemeManager;
import org.flytomarchik.autosprint.translation.TranslationManager;
public interface UIComponent {
    void render(MatrixStack matrices, DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta, float pulse, ThemeManager.Theme theme, TranslationManager translationManager);
    boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width);
    int getHeight();
    default boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    default int setAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}