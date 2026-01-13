package org.flytomarchik.autosprint.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class SoundUtils {
    public static void playToggle(boolean state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Высокий тон для включения, низкий для выключения
        float pitch = state ? 1.2f : 0.8f;
        // Звук "pling" или "toast"
        mc.world.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER, 0.5f, pitch);
    }

    public static void playClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
        }
    }

    public static void playDelete() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 0.6f);
        }
    }
}