package org.flytomarchik.autosprint.sprint;

import net.minecraft.client.MinecraftClient;
import org.flytomarchik.autosprint.config.ConfigManager;

public class SprintManager {
    private final ConfigManager configManager;
    private final ToggleSprint toggleSprint;
    private final WaterSprint waterSprint;
    private final FlySprint flySprint;

    // Флаг для отслеживания состояния в прошлом тике
    private boolean wasSprintingLastTick = false;

    public SprintManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.toggleSprint = new ToggleSprint();
        this.waterSprint = new WaterSprint();
        this.flySprint = new FlySprint();
    }

    public void tick(MinecraftClient client) {
        if (client.player == null) return;

        ConfigManager.Config config = configManager.getConfig();

        // 1. Глобальный выключатель
        if (!config.masterToggle) {
            if (wasSprintingLastTick) {
                // ИСПРАВЛЕНИЕ: Если выключили мастер-свитч во время бега, принудительно стопаем
                client.options.sprintKey.setPressed(false);
                client.player.setSprinting(false);
                wasSprintingLastTick = false;
            }
            return;
        }

        boolean shouldSprint = false;

        // 2. Логика приоритетов (Fly -> Water -> Standard)

        // --- FLY SPRINT ---
        if (client.player.getAbilities().creativeMode && client.player.getAbilities().flying) {
            if (flySprint.canSprint(client, config)) {
                shouldSprint = true;
            } else if (wasSprintingLastTick) {
                // БАГ ФИКС: Летим, но FlySprint выключен -> стоп
                client.player.setSprinting(false);
            }
        }
        // --- WATER SPRINT ---
        else if (client.player.isTouchingWater() || client.player.isSubmergedInWater()) {
            if (waterSprint.canSprint(client, config)) {
                shouldSprint = true;
            } else if (wasSprintingLastTick) {
                // БАГ ФИКС: В воде, но WaterSprint выключен -> стоп
                client.player.setSprinting(false);
            }
        }
        // --- STANDARD SPRINT ---
        else {
            BaseSprint currentMode = getCurrentSprintMode();
            if (currentMode != null && currentMode.canSprint(client, config)) {
                shouldSprint = true;
            }
        }

        // Применяем состояние
        client.options.sprintKey.setPressed(shouldSprint);
        wasSprintingLastTick = shouldSprint;
    }

    private BaseSprint getCurrentSprintMode() {
        return toggleSprint;
    }

    public void toggleMaster() {
        ConfigManager.Config config = configManager.getConfig();
        config.masterToggle = !config.masterToggle;
        configManager.save();
    }

    public void toggleWater() {
        ConfigManager.Config config = configManager.getConfig();
        config.allowInWater = !config.allowInWater;
        configManager.save();
    }

    public void toggleFly() {
        ConfigManager.Config config = configManager.getConfig();
        config.flySprint = !config.flySprint;
        configManager.save();
    }

    public boolean isEnabled() {
        return configManager.getConfig().masterToggle;
    }
}