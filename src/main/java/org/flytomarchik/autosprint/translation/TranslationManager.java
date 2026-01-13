package org.flytomarchik.autosprint.translation;

import java.util.HashMap;
import java.util.Map;

public class TranslationManager {

    private Map<String, String> availableLanguages = new HashMap<>();
    private String currentLanguage = "en";
    private Map<String, Map<String, String>> translations = new HashMap<>();

    public TranslationManager() {
        availableLanguages.put("en", "English");
        availableLanguages.put("ru", "Русский");

        // --- ENGLISH ---
        Map<String, String> enTrans = new HashMap<>();
        enTrans.put("gui.language", "Language");
        enTrans.put("gui.master_switch", "Master Switch");
        enTrans.put("gui.master_switch.desc", "Enables auto sprint globally");
        enTrans.put("gui.water_sprint", "Water Sprint");
        enTrans.put("gui.water_sprint.desc", "Sprint while swimming");
        enTrans.put("gui.fly_sprint", "Fly Sprint");
        enTrans.put("gui.fly_sprint.desc", "Faster flight in creative mode");
        enTrans.put("gui.sprint_mode", "Sprint Mode");
        enTrans.put("gui.theme", "Theme");
        enTrans.put("gui.title", "Modules");

        // Новые переводы
        enTrans.put("gui.blur", "Blur Effect");
        enTrans.put("gui.blur.desc", "Toggle background blur");
        enTrans.put("gui.configs", "Profiles");
        enTrans.put("gui.create", "Create & Load");
        enTrans.put("gui.reset", "Reset Positions");
        enTrans.put("gui.name_placeholder", "Name...");

        // Режимы
        enTrans.put("mode.forward_only", "Forward");
        enTrans.put("mode.forward_only.desc", "Sprint only when moving W");
        enTrans.put("mode.omni", "Omni");
        enTrans.put("mode.omni.desc", "Sprint in all directions");
        enTrans.put("mode.toggle", "Toggle");
        enTrans.put("mode.toggle.desc", "Standard toggle behavior");
        enTrans.put("mode.rage", "Rage");
        enTrans.put("mode.rage.desc", "Always sprint (Legit unsafe)");

        translations.put("en", enTrans);

        // --- RUSSIAN ---
        Map<String, String> ruTrans = new HashMap<>();
        ruTrans.put("gui.language", "Язык");
        ruTrans.put("gui.master_switch", "Главный модуль");
        ruTrans.put("gui.master_switch.desc", "Включает автоспринт");
        ruTrans.put("gui.water_sprint", "Спринт в воде");
        ruTrans.put("gui.water_sprint.desc", "Быстрое плавание");
        ruTrans.put("gui.fly_sprint", "Спринт в полете");
        ruTrans.put("gui.fly_sprint.desc", "Ускоренный полет (Creative)");
        ruTrans.put("gui.sprint_mode", "Режим бега");
        ruTrans.put("gui.theme", "Темы");
        ruTrans.put("gui.title", "Модули");

        // Новые переводы
        ruTrans.put("gui.blur", "Эффект размытия");
        ruTrans.put("gui.blur.desc", "Размытие фона в меню");
        ruTrans.put("gui.configs", "Профили");
        ruTrans.put("gui.create", "Создать");
        ruTrans.put("gui.reset", "Сброс позиций");
        ruTrans.put("gui.name_placeholder", "Название...");

        // Режимы
        ruTrans.put("mode.forward_only", "Вперед");
        ruTrans.put("mode.forward_only.desc", "Бег только на W");
        ruTrans.put("mode.omni", "Омни");
        ruTrans.put("mode.omni.desc", "Бег во все стороны");
        ruTrans.put("mode.toggle", "Переключение");
        ruTrans.put("mode.toggle.desc", "Стандартный режим");
        ruTrans.put("mode.rage", "Rage");
        ruTrans.put("mode.rage.desc", "Вечный бег");

        translations.put("ru", ruTrans);
    }

    public String get(String key) {
        return translations.getOrDefault(currentLanguage, translations.get("en")).getOrDefault(key, key);
    }

    public Map<String, String> getAvailableLanguages() { return availableLanguages; }
    public String getCurrentLanguage() { return currentLanguage; }
    public void setLanguage(String lang) { if (availableLanguages.containsKey(lang)) currentLanguage = lang; }
}