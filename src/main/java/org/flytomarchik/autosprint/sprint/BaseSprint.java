package org.flytomarchik.autosprint.sprint;

import net.minecraft.client.MinecraftClient;
import org.flytomarchik.autosprint.config.ConfigManager;

public abstract class BaseSprint {
    protected boolean canSprintBasic(MinecraftClient client, ConfigManager.Config config) {
        var player = client.player;
        if (player == null) return false;

        if (player.getHungerManager().getFoodLevel() <= 6 && !player.getAbilities().allowFlying) {
            return false;
        }

        if (player.horizontalCollision && !player.getAbilities().allowFlying) {
            return false;
        }

        if (player.isSneaking() && !config.allowWhileSneaking) {
            return false;
        }

        // ИСПРАВЛЕНИЕ БАГА: Проверяем воду только если персонаж действительно плывет
        // А не просто находится в воде (например, прыгает над водой)
        if (player.isSwimming() && !config.allowInWater) {
            return false;
        }

        return true;
    }

    public abstract boolean canSprint(MinecraftClient client, ConfigManager.Config config);
}