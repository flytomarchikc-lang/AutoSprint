package org.flytomarchik.autosprint.gui;
import net.minecraft.client.gl.ShaderProgramKeys;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RenderUtils {
    private static final List<Particle> particles = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    public static float easeOutElastic(float x) {
        float c4 = (2 * (float)Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 :
                (float)Math.pow(2, -10 * x) * (float)Math.sin((x * 10 - 0.75) * c4) + 1;
    }

    // ==================== ИСПРАВЛЕННАЯ GLass PANEL ====================
    public static void drawGlassPanel(MatrixStack matrices, int x, int y, int width, int height, float radius, int tintColor, int accentColor) {
        // 1. ПОДМЕШИВАЕМ ЦВЕТ ТЕМЫ В ФОН
        // Берем 10% от цвета темы и смешиваем с черным тинтом.
        // Это сделает стекло слегка "цветным" (золотоватым, зеленоватым и т.д.)
        int mixedBackground = lerpColor(tintColor, accentColor, 0.10f);

        int darkTint = setAlpha(mixedBackground, 210);
        int lightTint = setAlpha(addBrightness(mixedBackground, 40), 180);

        // Рисуем фон
        drawGradientRoundedRect(matrices, x, y, width, height, radius, darkTint, lightTint);

        // Блик сверху (оставляем белым для эффекта стекла)
        drawGradientRect(matrices, x, y, width, height / 4, radius, setAlpha(0xFFFFFF, 30), setAlpha(0xFFFFFF, 0));

        // 2. ОБВОДКА ТЕПЕРЬ ЯРКАЯ И ЦВЕТНАЯ
        // Используем accentColor на 100% (alpha 255)
        drawGlowBorder(matrices, x, y, width, height, radius, 2.0f, setAlpha(accentColor, 255));

        // Внутреннее свечение тоже берет немного цвета темы
        drawGlowBorder(matrices, x + 2, y + 2, width - 4, height - 4, radius - 2, 1.0f, setAlpha(accentColor, 50));
    }

    public static void drawGradientRoundedRect(MatrixStack matrices, int x, int y, int width, int height, float radius, int colorTop, int colorBottom) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        float[] rgbaTop = getRGBA(colorTop);
        float[] rgbaBot = getRGBA(colorBottom);

        float centerY = y + height / 2f;
        float t = 0.5f;
        buffer.vertex(matrix, x + width / 2f, centerY, 0)
                .color(lerp(rgbaTop[0], rgbaBot[0], t), lerp(rgbaTop[1], rgbaBot[1], t),
                        lerp(rgbaTop[2], rgbaBot[2], t), lerp(rgbaTop[3], rgbaBot[3], t));

        for (int i = 0; i <= 360; i += 8) {
            float rad = (float) Math.toRadians(i);
            float px = (float) (x + width / 2f + Math.signum(Math.cos(rad)) * (width / 2f - radius));
            float py = (float) (y + height / 2f + Math.signum(Math.sin(rad)) * (height / 2f - radius));
            float vertY = py + (float) Math.sin(rad) * radius;
            float ratio = MathHelper.clamp((vertY - y) / height, 0, 1);

            buffer.vertex(matrix, px + (float) Math.cos(rad) * radius, vertY, 0)
                    .color(lerp(rgbaTop[0], rgbaBot[0], ratio), lerp(rgbaTop[1], rgbaBot[1], ratio),
                            lerp(rgbaTop[2], rgbaBot[2], ratio), lerp(rgbaTop[3], rgbaBot[3], ratio));
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRect(MatrixStack matrices, int x, int y, int width, int height, float radius, int color) {
        drawGradientRoundedRect(matrices, x, y, width, height, radius, color, color);
    }

    public static void drawGradientRect(MatrixStack matrices, int x, int y, int width, int height, float radius, int color1, int color2) {
        drawGradientRoundedRect(matrices, x, y, width, height, radius, color1, color2);
    }

    public static void drawGlowBorder(MatrixStack matrices, int x, int y, int width, int height, float radius, float thickness, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(thickness);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float[] rgba = getRGBA(color);

        // Рисуем контур
        for (int i = 0; i <= 360; i += 4) {
            float rad = (float) Math.toRadians(i);
            // Немного увеличиваем радиус для обводки, чтобы она была снаружи
            float offset = 0.5f;
            float px = (float) (x + width / 2f + Math.signum(Math.cos(rad)) * (width / 2f - radius));
            float py = (float) (y + height / 2f + Math.signum(Math.sin(rad)) * (height / 2f - radius));

            buffer.vertex(matrix, px + (float) Math.cos(rad) * radius, py + (float) Math.sin(rad) * radius, 0)
                    .color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        // Замыкаем контур
        float rad = 0;
        float px = (float) (x + width / 2f + Math.signum(Math.cos(rad)) * (width / 2f - radius));
        float py = (float) (y + height / 2f + Math.signum(Math.sin(rad)) * (height / 2f - radius));
        buffer.vertex(matrix, px + (float) Math.cos(rad) * radius, py + (float) Math.sin(rad) * radius, 0)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public static void createRipple(int x, int y, int color) {
        particles.add(new RippleParticle(x, y, color));
    }

    public static void renderRipples(MatrixStack matrices) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) {
                it.remove();
            } else {
                p.render(matrices);
            }
        }
    }

    public static void spawnParticles(int x, int y, int width, int height, int color, int count) {
        for (int i = 0; i < count; i++) {
            float px = x + RANDOM.nextFloat() * width;
            float py = y + RANDOM.nextFloat() * height;
            float vx = (RANDOM.nextFloat() - 0.5f) * 2;
            float vy = (RANDOM.nextFloat() - 0.5f) * 2 - 1;
            particles.add(new FloatingParticle(px, py, vx, vy, color));
        }
    }

    public static int setAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int addBrightness(int color, int amount) {
        int r = Math.min(255, (color >> 16 & 255) + amount);
        int g = Math.min(255, (color >> 8 & 255) + amount);
        int b = Math.min(255, (color & 255) + amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static float[] getRGBA(int color) {
        return new float[]{
                (color >> 16 & 255) / 255f,
                (color >> 8 & 255) / 255f,
                (color & 255) / 255f,
                (color >> 24 & 255) / 255f
        };
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static int lerpColor(int color1, int color2, float t) {
        int a1 = color1 >> 24 & 255;
        int r1 = color1 >> 16 & 255;
        int g1 = color1 >> 8 & 255;
        int b1 = color1 & 255;

        int a2 = color2 >> 24 & 255;
        int r2 = color2 >> 16 & 255;
        int g2 = color2 >> 8 & 255;
        int b2 = color2 & 255;

        int a = (int) MathHelper.lerp(t, a1, a2);
        int r = (int) MathHelper.lerp(t, r1, r2);
        int g = (int) MathHelper.lerp(t, g1, g2);
        int b = (int) MathHelper.lerp(t, b1, b2);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int addColors(int color1, int color2) {
        int a = MathHelper.clamp((color1 >> 24 & 255) + (color2 >> 24 & 255), 0, 255);
        int r = MathHelper.clamp((color1 >> 16 & 255) + (color2 >> 16 & 255), 0, 255);
        int g = MathHelper.clamp((color1 >> 8 & 255) + (color2 >> 8 & 255), 0, 255);
        int b = MathHelper.clamp((color1 & 255) + (color2 & 255), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static abstract class Particle {
        protected float x, y;
        protected int age;
        protected int maxAge;

        public abstract void update();
        public abstract void render(MatrixStack matrices);
        public boolean isDead() { return age >= maxAge; }
    }

    private static class RippleParticle extends Particle {
        private final int color;
        private float radius;

        public RippleParticle(int x, int y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.radius = 0;
            this.maxAge = 20;
            this.age = 0;
        }

        @Override
        public void update() {
            age++;
            radius += 3f;
        }

        @Override
        public void render(MatrixStack matrices) {
            float alpha = 1f - (age / (float)maxAge);
            int rippleColor = setAlpha(color, (int)(alpha * 100));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(2f);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            float[] rgba = getRGBA(rippleColor);

            for (int i = 0; i <= 360; i += 10) {
                float rad = (float) Math.toRadians(i);
                float px = x + (float)Math.cos(rad) * radius;
                float py = y + (float)Math.sin(rad) * radius;
                buffer.vertex(matrix, px, py, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
        }
    }

    private static class FloatingParticle extends Particle {
        private float vx, vy;
        private final int color;
        private final float size;

        public FloatingParticle(float x, float y, float vx, float vy, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = 1 + RANDOM.nextFloat() * 2;
            this.maxAge = 30 + RANDOM.nextInt(20);
            this.age = 0;
        }

        @Override
        public void update() {
            age++;
            x += vx;
            y += vy;
            vy += 0.1f;
            vx *= 0.98f;
        }

        @Override
        public void render(MatrixStack matrices) {
            float alpha = 1f - (age / (float)maxAge);
            int particleColor = setAlpha(color, (int)(alpha * 255));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float[] rgba = getRGBA(particleColor);

            buffer.vertex(matrix, x - size, y - size, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buffer.vertex(matrix, x - size, y + size, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buffer.vertex(matrix, x + size, y + size, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buffer.vertex(matrix, x + size, y - size, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
        }
    }
}
