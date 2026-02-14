package org.flytomarchik.autosprint;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.flytomarchik.autosprint.config.ConfigManager;
import org.flytomarchik.autosprint.gui.AutoSprintScreen;
import org.flytomarchik.autosprint.sprint.*;
import org.flytomarchik.autosprint.theme.ThemeManager;
import org.flytomarchik.autosprint.translation.TranslationManager;
import org.lwjgl.glfw.GLFW;

public class AutoSprintClient implements ClientModInitializer {

    public static KeyBinding masterKey;
    public static KeyBinding configKey;
    public static KeyBinding waterKey;
    public static KeyBinding flyKey;
    public static KeyBinding blurKey;
    public static KeyBinding resetGuiKey;

    private static SprintManager sprintManager;
    private static ConfigManager configManager;
    private static ThemeManager themeManager;
    private static TranslationManager translationManager;

    public static ThemeManager getThemeManager() {
        return themeManager;
    }

    public static TranslationManager getTranslationManager() {
        return translationManager;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static SprintManager getSprintManager() {
        return sprintManager;
    }

    @Override
    public void onInitializeClient() {
        translationManager = new TranslationManager();
        configManager = new ConfigManager();
        themeManager = new ThemeManager();
        sprintManager = new SprintManager(configManager);
        configManager.load();

        themeManager.setTheme(configManager.getConfig().theme);

        masterKey = registerKey("key.autosprint.toggle", GLFW.GLFW_KEY_H);
        configKey = registerKey("key.autosprint.config", GLFW.GLFW_KEY_G);
        waterKey = registerKey("gui.water_sprint", GLFW.GLFW_KEY_UNKNOWN);
        flyKey = registerKey("gui.fly_sprint", GLFW.GLFW_KEY_UNKNOWN);
        blurKey = registerKey("key.autosprint.blur", GLFW.GLFW_KEY_UNKNOWN);
        resetGuiKey = registerKey("key.autosprint.reset", GLFW.GLFW_KEY_UNKNOWN);

        ClientTickEvents.END_CLIENT_TICK.register((MinecraftClient client) -> {
            if (masterKey.wasPressed()) {
                sprintManager.toggleMaster();
                playSound(client, configManager.getConfig().masterToggle);
                sendNotification(client, "gui.master_switch", configManager.getConfig().masterToggle);
            }
            if (waterKey.wasPressed()) {
                sprintManager.toggleWater();
                playSound(client, configManager.getConfig().allowInWater);
                sendNotification(client, "gui.water_sprint", configManager.getConfig().allowInWater);
            }
            if (flyKey.wasPressed()) {
                sprintManager.toggleFly();
                playSound(client, configManager.getConfig().flySprint);
                sendNotification(client, "gui.fly_sprint", configManager.getConfig().flySprint);
            }
            if (blurKey.wasPressed()) {
                boolean newState = !configManager.getConfig().useBlur;
                configManager.getConfig().useBlur = newState;
                configManager.save();
                playSound(client, newState);
                sendNotification(client, "Blur Effect", newState);
            }
            if (resetGuiKey.wasPressed()) {
                resetGuiPositions();
                playSound(client, true);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§8[§bAutoSprint§8] §7GUI Positions reset."), true);
                }
            }
            if (configKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new AutoSprintScreen(null, configManager, themeManager, translationManager));
                }
            }
            sprintManager.tick(client);
        });
    }

    private KeyBinding registerKey(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, code, "category.autosprint"));
    }

    private void playSound(MinecraftClient client, boolean enabled) {
        if (client.player == null || client.world == null) return;
        float pitch = enabled ? 1.5f : 0.8f;
        // ИСПРАВЛЕНО: Добавлен аргумент client.player (источник) и убран boolean в конце
        client.world.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0f, pitch);
    }

    private void sendNotification(MinecraftClient client, String keyName, boolean state) {
        if (client.player == null) return;
        String name = translationManager.get(keyName);

        Text status = Text.literal(state ? " ✔ ENABLED" : " ❌ DISABLED")
                .styled(style -> style
                        .withColor(state ? Formatting.GREEN : Formatting.RED)
                        .withBold(true));

        Text prefix = Text.literal("AutoSprint » ").styled(style -> style.withColor(Formatting.AQUA).withBold(true));
        Text module = Text.literal(name).styled(style -> style.withColor(Formatting.GRAY));

        client.player.sendMessage(prefix.copy().append(module).append(status), true);
    }

    private void resetGuiPositions() {
        var cfg = configManager.getConfig();
        cfg.panelThemeX = -1; cfg.panelThemeY = -1;
        cfg.panelMainX = -1; cfg.panelMainY = -1;
        cfg.panelConfigX = -1; cfg.panelConfigY = -1;
        configManager.save();
    }
}
