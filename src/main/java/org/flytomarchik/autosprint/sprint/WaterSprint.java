package org.flytomarchik.autosprint.sprint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.flytomarchik.autosprint.config.ConfigManager;

public class WaterSprint extends BaseSprint {

    @Override
    public boolean canSprint(MinecraftClient client, ConfigManager.Config config) {
        // Модуль работает, только если разрешен в конфиге
        if (!config.allowInWater) return false;

        PlayerEntity player = client.player;
        if (player == null) return false;

        // Проверка, находится ли игрок в воде
        return player.isTouchingWater() || player.isSubmergedInWater();
    }

    public boolean isInWater(MinecraftClient client) {
        var player = client.player;
        return player != null && (player.isTouchingWater() || player.isSubmergedInWater());
    }
}