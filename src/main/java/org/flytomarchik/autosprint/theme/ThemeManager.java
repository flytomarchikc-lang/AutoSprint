package org.flytomarchik.autosprint.theme;

import java.util.LinkedHashMap;
import java.util.Map;

public class ThemeManager {

    private Theme currentTheme;
    private final Map<String, Theme> themes = new LinkedHashMap<>();

    public ThemeManager() {
        registerThemes();
        currentTheme = themes.getOrDefault("VIBRANT_PURPLE", themes.values().iterator().next());
    }

    private void registerThemes() {
        // === КЛАССИЧЕСКИЕ ===
        themes.put("DARK", new Theme("🌑 Dark", 0xFF121212, 0xFFFFFFFF)); // Тёмный фон, белый
        themes.put("LIGHT", new Theme("☀️ Light", 0xFFFFFFFF, 0xFF000000)); // Белый фон, чёрный


        // === ЯРКИЕ И ЭНЕРГИЧНЫЕ ===
        themes.put("VIBRANT_PURPLE", new Theme("💜 Vibrant Purple", 0xFFD500F9, 0xFF220029));
        themes.put("CYBER_CYAN", new Theme("🔵 Cyber Cyan", 0xFF00E5FF, 0xFF00151A));
        themes.put("TOXIC_GREEN", new Theme("☢️ Toxic Green", 0xFF76FF03, 0xFF0F2100));
        themes.put("SUNSET_ORANGE", new Theme("🌅 Sunset Orange", 0xFFFF9100, 0xFF261100));
        themes.put("ROYAL_GOLD", new Theme("👑 Royal Gold", 0xFFFFD700, 0xFF262000));
        themes.put("CRIMSON_BLOOD", new Theme("🩸 Crimson Blood", 0xFFFF1744, 0xFF2B0006));

        // === ТЕМНЫЕ И ГЛУБОКИЕ ===
        themes.put("MIDNIGHT_BLUE", new Theme("🌌 Midnight Blue", 0xFF304FFE, 0xFF00031C));
        themes.put("VOID_PURPLE", new Theme("🌀 Void Purple", 0xFFAA00FF, 0xFF12001B));
        themes.put("DEEP_OCEAN", new Theme("🌊 Deep Ocean", 0xFF0091EA, 0xFF001219));
        themes.put("SHADOW_RED", new Theme("🔴 Shadow Red", 0xFFFF5252, 0xFF1A0000));

        // === ПАСТЕЛЬНЫЕ И МЯГКИЕ ===
        themes.put("SOFT_PASTEL", new Theme("🌸 Soft Pastel", 0xFFF8BBD0, 0xFF3E2723));
        themes.put("MINT_DREAM", new Theme("🍃 Mint Dream", 0xFF69F0AE, 0xFF1B3A2F));
        themes.put("LAVENDER_MIST", new Theme("🌺 Lavender Mist", 0xFFE1BEE7, 0xFF2C1B3A));
        themes.put("PEACH_CREAM", new Theme("🍑 Peach Cream", 0xFFFFAB91, 0xFF3A2415));

        // === НЕОНОВЫЕ И ФУТУРИСТИЧНЫЕ ===
        themes.put("NEON_PINK", new Theme("💖 Neon Pink", 0xFFFF10F0, 0xFF1A0018));
        themes.put("ELECTRIC_LIME", new Theme("⚡ Electric Lime", 0xFFC6FF00, 0xFF1C2600));
        themes.put("PLASMA_BLUE", new Theme("🔷 Plasma Blue", 0xFF00B0FF, 0xFF001A26));
        themes.put("LASER_RED", new Theme("🔴 Laser Red", 0xFFFF1744, 0xFF260005));

        // === ПРИРОДНЫЕ ===
        themes.put("FOREST_GREEN", new Theme("🌲 Forest Green", 0xFF00C853, 0xFF002611));
        themes.put("SUNSET_SKY", new Theme("🌇 Sunset Sky", 0xFFFF6E40, 0xFF260F00));
        themes.put("OCEAN_BREEZE", new Theme("🌬️ Ocean Breeze", 0xFF00BCD4, 0xFF00191F));
        themes.put("CHERRY_BLOSSOM", new Theme("🌸 Cherry Blossom", 0xFFFF4081, 0xFF260010));

        // === ТЕМНЫЕ ЭЛЕГАНТНЫЕ ===
        themes.put("OBSIDIAN", new Theme("⬛ Obsidian", 0xFF9E9E9E, 0xFF000000));
        themes.put("RUBY_NIGHT", new Theme("💎 Ruby Night", 0xFFE91E63, 0xFF1F0008));
        themes.put("EMERALD_DARK", new Theme("💚 Emerald Dark", 0xFF00E676, 0xFF002612));
        themes.put("SAPPHIRE_ABYSS", new Theme("💙 Sapphire Abyss", 0xFF2979FF, 0xFF000F26));

        // === СПЕЦИАЛЬНЫЕ ===
        themes.put("MATRIX", new Theme("🖥️ Matrix", 0xFF00FF00, 0xFF001100));
        themes.put("VAPORWAVE", new Theme("🌊 Vaporwave", 0xFFFF71CE, 0xFF1A0026));
        themes.put("CYBERPUNK", new Theme("🤖 Cyberpunk", 0xFFFFEA00, 0xFF1A1600));
        themes.put("GALAXY", new Theme("🌌 Galaxy", 0xFFE040FB, 0xFF1A002D));
        themes.put("AURORA", new Theme("🌈 Aurora", 0xFF00E5FF, 0xFF001826));

        // === RETRO ===
        themes.put("RETRO_WAVE", new Theme("📼 Retro Wave", 0xFFFF006E, 0xFF1A0008));
        themes.put("SYNTHWAVE", new Theme("🎹 Synthwave", 0xFFFF10F0, 0xFF1A0018));
        themes.put("ARCADE", new Theme("🕹️ Arcade", 0xFFFFFF00, 0xFF1A1A00));

        // === МИНИМАЛИСТИЧНЫЕ ===
        themes.put("ICE_BLUE", new Theme("❄️ Ice Blue", 0xFF80D8FF, 0xFF001019));
        themes.put("FIRE_ORANGE", new Theme("🔥 Fire Orange", 0xFFFF9E80, 0xFF260F00));
        themes.put("EARTH_BROWN", new Theme("🏔️ Earth Brown", 0xFFBCAAA4, 0xFF211713));
        themes.put("WIND_WHITE", new Theme("💨 Wind White", 0xFFECEFF1, 0xFF1C1E1F));
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(String id) {
        if (themes.containsKey(id)) {
            currentTheme = themes.get(id);
        }
    }

    public Map<String, Theme> getAllThemes() {
        return themes;
    }

    public static class Theme {
        private final String name;
        private final int accent;
        private final int tint;

        public Theme(String name, int accent, int tint) {
            this.name = name;
            this.accent = accent;
            this.tint = tint;
        }

        public String getName() { return name; }
        public int getAccent() { return accent; }
        public int getTint() { return tint; }
    }
}