package org.flytomarchik.autosprint.sprint;

import net.minecraft.client.MinecraftClient;
import org.flytomarchik.autosprint.config.ConfigManager;

public class FlySprint extends BaseSprint {
    @Override
    public boolean canSprint(MinecraftClient client, ConfigManager.Config config) {
        // Работает только если модуль включен
        if (!config.flySprint) return false;

        var player = client.player;
        if (player == null) return false;

        // Работает только в креативе, только в полете, и только при движении вперед
        // В Minecraft 1.21+ input.forward - это boolean
        return player.getAbilities().creativeMode && player.getAbilities().flying && player.input.hasForwardMovement();
    }
}