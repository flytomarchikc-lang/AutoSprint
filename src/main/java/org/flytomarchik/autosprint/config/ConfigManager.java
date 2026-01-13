package org.flytomarchik.autosprint.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Улучшенный менеджер конфигураций с поддержкой:
 * - Автосохранения
 * - Резервных копий
 * - Валидации конфигов
 * - Метаданных конфигов (скрыты в папке .system)
 */
public class ConfigManager {
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("autosprint").toFile();

    // Новая папка для системных файлов (скрытая)
    private static final File SYSTEM_DIR = new File(CONFIG_DIR, ".system");
    // Путь к файлу метаданных внутри системной папки
    private static final File METADATA_FILE = new File(SYSTEM_DIR, "metadata.json");

    private static final File BACKUP_DIR = new File(CONFIG_DIR, "backups");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BACKUPS = 5; // Максимальное количество резервных копий

    private Config config;
    private String currentConfigName = "default";
    private final Map<String, ConfigMetadata> configMetadata = new HashMap<>();

    public ConfigManager() {
        // 1. Создаем структуру папок
        if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
        if (!BACKUP_DIR.exists()) BACKUP_DIR.mkdirs();
        if (!SYSTEM_DIR.exists()) SYSTEM_DIR.mkdirs();

        // 2. МИГРАЦИЯ: Если старый файл metadata.json лежит в корне, переносим его в .system
        File oldMetadata = new File(CONFIG_DIR, "metadata.json");
        if (oldMetadata.exists()) {
            try {
                System.out.println("Migrating metadata.json to system folder...");
                Files.move(oldMetadata.toPath(), METADATA_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Failed to migrate metadata file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.config = new Config();
        loadAllMetadata();
    }

    /**
     * Загружает конфиг по умолчанию
     */
    public void load() {
        load(currentConfigName);
    }

    /**
     * Загружает конкретный конфиг
     * @param name имя конфига
     */
    public void load(String name) {
        // Сохраняем текущий конфиг перед переключением
        if (!currentConfigName.equals(name)) {
            save();
        }

        this.currentConfigName = name;
        File file = new File(CONFIG_DIR, name + ".json");

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Config loaded = GSON.fromJson(reader, Config.class);
                if (loaded != null && validateConfig(loaded)) {
                    config = loaded;
                    updateMetadata(name);
                } else {
                    System.err.println("Invalid config file: " + name);
                    config = new Config();
                    save(); // Перезаписываем битый файл дефолтным
                }
            } catch (IOException e) {
                e.printStackTrace();
                config = new Config();
                save();
            }
        } else {
            this.config = new Config();
            save();
        }
    }

    /**
     * Сохраняет текущий конфиг
     */
    public void save() {
        File file = new File(CONFIG_DIR, currentConfigName + ".json");

        // Создаем резервную копию перед сохранением
        if (file.exists()) {
            createBackup(currentConfigName);
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(config, writer);
            updateMetadata(currentConfigName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает новый конфиг
     * @param name имя нового конфига
     */
    public void createNewConfig(String name) {
        if (name == null || name.trim().isEmpty()) {
            System.err.println("Config name cannot be empty");
            return;
        }

        // Проверяем, не является ли имя зарезервированным
        if (name.equalsIgnoreCase("metadata") || name.equalsIgnoreCase("backups")) {
            System.err.println("This name is reserved by system");
            return;
        }

        // Проверяем, не существует ли уже конфиг с таким именем
        File file = new File(CONFIG_DIR, name + ".json");
        if (file.exists()) {
            System.err.println("Config already exists: " + name);
            return;
        }

        // Сохраняем текущий конфиг
        save();

        // Создаем новый пустой конфиг
        this.config = new Config();
        this.currentConfigName = name;
        save();

        // Создаем метаданные
        ConfigMetadata metadata = new ConfigMetadata();
        metadata.createdDate = System.currentTimeMillis();
        metadata.lastModified = System.currentTimeMillis();
        metadata.description = "New configuration";
        configMetadata.put(name, metadata);
        saveMetadata();
    }

    /**
     * Дублирует существующий конфиг
     * @param sourceName исходный конфиг
     * @param newName имя нового конфига
     */
    public void duplicateConfig(String sourceName, String newName) {
        File sourceFile = new File(CONFIG_DIR, sourceName + ".json");
        File targetFile = new File(CONFIG_DIR, newName + ".json");

        if (!sourceFile.exists()) {
            System.err.println("Source config does not exist: " + sourceName);
            return;
        }

        if (targetFile.exists()) {
            System.err.println("Target config already exists: " + newName);
            return;
        }

        try {
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);

            // Создаем метаданные для дубликата
            ConfigMetadata metadata = new ConfigMetadata();
            metadata.createdDate = System.currentTimeMillis();
            metadata.lastModified = System.currentTimeMillis();
            metadata.description = "Copy of " + sourceName;
            configMetadata.put(newName, metadata);
            saveMetadata();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаляет конфиг
     * @param name имя конфига для удаления
     */
    public void deleteConfig(String name) {
        // Нельзя удалить дефолтный конфиг
        if (name.equals("default")) {
            System.err.println("Cannot delete default config");
            return;
        }

        File file = new File(CONFIG_DIR, name + ".json");
        if (file.exists()) {
            // Создаем финальную резервную копию перед удалением
            createBackup(name);
            file.delete();
            configMetadata.remove(name);
            saveMetadata();
        }

        // Если удаляем текущий конфиг, переключаемся на default
        if (currentConfigName.equals(name)) {
            load("default");
        }
    }

    /**
     * Создает резервную копию конфига
     * @param name имя конфига
     */
    private void createBackup(String name) {
        File sourceFile = new File(CONFIG_DIR, name + ".json");
        if (!sourceFile.exists()) return;

        // Формат: config_name_YYYYMMDD_HHMMSS.json
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(BACKUP_DIR, name + "_" + timestamp + ".json");

        try {
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cleanupOldBackups(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаляет старые резервные копии, оставляя только последние MAX_BACKUPS
     * @param configName имя конфига
     */
    private void cleanupOldBackups(String configName) {
        File[] backups = BACKUP_DIR.listFiles((dir, name) ->
                name.startsWith(configName + "_") && name.endsWith(".json"));

        if (backups == null || backups.length <= MAX_BACKUPS) return;

        // Сортируем по дате модификации (старые первыми)
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

        // Удаляем старые бэкапы
        for (int i = 0; i < backups.length - MAX_BACKUPS; i++) {
            backups[i].delete();
        }
    }

    /**
     * Восстанавливает конфиг из резервной копии
     * @param backupFileName имя файла резервной копии
     */
    public void restoreFromBackup(String backupFileName) {
        File backupFile = new File(BACKUP_DIR, backupFileName);
        if (!backupFile.exists()) {
            System.err.println("Backup file does not exist: " + backupFileName);
            return;
        }

        // Извлекаем имя конфига из имени файла бэкапа (отсекаем таймштамп)
        // Пример: myconfig_20230101_120000.json -> myconfig
        String configName = backupFileName.substring(0, backupFileName.lastIndexOf('_'));
        // На случай, если в имени были подчеркивания, логика может быть сложнее,
        // но для базового варианта этого достаточно, или можно использовать regex.

        File targetFile = new File(CONFIG_DIR, configName + ".json");

        try {
            Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (currentConfigName.equals(configName)) {
                load(configName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает список всех конфигов.
     * Исключает системные файлы и метаданные.
     * @return список имен конфигов
     */
    public List<String> listConfigs() {
        // listFiles не рекурсивный, он не увидит файлы внутри .system или backups
        File[] files = CONFIG_DIR.listFiles((dir, name) ->
                name.endsWith(".json") &&
                        !name.equals("metadata.json") // На всякий случай фильтруем старый файл, если он остался
        );

        if (files == null) return new ArrayList<>();

        return Arrays.stream(files)
                .map(f -> f.getName().replace(".json", ""))
                .sorted((a, b) -> {
                    // default всегда первый
                    if (a.equals("default")) return -1;
                    if (b.equals("default")) return 1;
                    // Остальные по дате изменения (новые первыми)
                    ConfigMetadata metaA = configMetadata.get(a);
                    ConfigMetadata metaB = configMetadata.get(b);
                    if (metaA != null && metaB != null) {
                        return Long.compare(metaB.lastModified, metaA.lastModified);
                    }
                    return a.compareTo(b);
                })
                .collect(Collectors.toList());
    }

    /**
     * Получает список резервных копий для конфига
     * @param configName имя конфига
     * @return список имен файлов бэкапов
     */
    public List<String> listBackups(String configName) {
        File[] backups = BACKUP_DIR.listFiles((dir, name) ->
                name.startsWith(configName + "_") && name.endsWith(".json"));

        if (backups == null) return new ArrayList<>();

        return Arrays.stream(backups)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    /**
     * Валидирует конфиг
     * @param config конфиг для проверки
     * @return true если конфиг валиден
     */
    private boolean validateConfig(Config config) {
        if (config == null) return false;
        return config.sprintMode != null;
    }

    /**
     * Обновляет метаданные конфига
     * @param name имя конфига
     */
    private void updateMetadata(String name) {
        ConfigMetadata metadata = configMetadata.getOrDefault(name, new ConfigMetadata());
        if (metadata.createdDate == 0) {
            metadata.createdDate = System.currentTimeMillis();
        }
        metadata.lastModified = System.currentTimeMillis();
        configMetadata.put(name, metadata);
        saveMetadata();
    }

    /**
     * Загружает все метаданные из системной папки
     */
    private void loadAllMetadata() {
        if (METADATA_FILE.exists()) {
            try (FileReader reader = new FileReader(METADATA_FILE)) {
                Map<String, ConfigMetadata> loaded = GSON.fromJson(reader,
                        new com.google.gson.reflect.TypeToken<Map<String, ConfigMetadata>>(){}.getType());
                if (loaded != null) {
                    configMetadata.putAll(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Сохраняет метаданные в системную папку
     */
    private void saveMetadata() {
        if (!SYSTEM_DIR.exists()) SYSTEM_DIR.mkdirs();

        try (FileWriter writer = new FileWriter(METADATA_FILE)) {
            GSON.toJson(configMetadata, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Config getConfig() {
        return config;
    }

    public String getCurrentConfigName() {
        return currentConfigName;
    }

    public ConfigMetadata getMetadata(String configName) {
        return configMetadata.get(configName);
    }

    /**
     * Метаданные конфигурации
     */
    public static class ConfigMetadata {
        public long createdDate = 0;
        public long lastModified = 0;
        public String description = "";

        public String getFormattedCreatedDate() {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(createdDate));
        }

        public String getFormattedLastModified() {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(lastModified));
        }
    }

    /**
     * Класс конфигурации
     */
    public static class Config {
        public boolean masterToggle = true;
        public boolean allowInWater = false;
        public boolean flySprint = false;
        public boolean allowWhileSneaking = false;
        public boolean useBlur = true;

        // Позиции панелей
        public int panelThemeX = -1, panelThemeY = -1;
        public int panelMainX = -1, panelMainY = -1;
        public int panelConfigX = -1, panelConfigY = -1;

        public String theme = "VIBRANT_PURPLE";
        public String language = "en";

        public SprintMode sprintMode = SprintMode.TOGGLE_SPRINT;

        public enum SprintMode {
            FORWARD_ONLY,
            OMNI_DIRECTIONAL,
            TOGGLE_SPRINT,
            RAGE_SPRINT
        }
    }
}