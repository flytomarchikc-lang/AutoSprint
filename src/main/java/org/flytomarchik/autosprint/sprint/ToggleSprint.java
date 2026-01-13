package org.flytomarchik.autosprint.sprint;

import net.minecraft.client.MinecraftClient;
import org.flytomarchik.autosprint.config.ConfigManager;

public class ToggleSprint extends BaseSprint {

    @Override
    public boolean canSprint(MinecraftClient client, ConfigManager.Config config) {
        // canSprintBasic и другие проверки остаются
        if (!canSprintBasic(client, config)) return false;

        var player = client.player;
        if (player == null) return false;

        // ИСПРАВЛЕНИЕ БАГА С ВОДОЙ:
        // Если игрок в воде, обычный спринт отключается. Управление переходит к WaterSprint (если он включен).
        if (player.isTouchingWater() || player.isSubmergedInWater()) return false;

        // Отключаем обычный спринт, если игрок в полете (за это отвечает FlySprint)
        if (player.getAbilities().flying) return false;

        // В Minecraft 1.21+ input.forward - это boolean
        return player.input.hasForwardMovement();
    }
}