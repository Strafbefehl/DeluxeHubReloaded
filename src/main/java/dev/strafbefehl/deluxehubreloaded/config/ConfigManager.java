package dev.strafbefehl.deluxehubreloaded.config;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Map<ConfigType, ConfigHandler> configurations;
    private ConfigMigrator migrator;

    public ConfigManager() {
        configurations = new HashMap<>();
    }

    /**
     * Loads all config files and initializes the migrator.
     */
    public void loadFiles(DeluxeHubPlugin plugin) {
        // Initialize the config migrator
        migrator = new ConfigMigrator(plugin);

        // Register all config files
        registerFile(ConfigType.SETTINGS, new ConfigHandler(plugin, "config"));
        registerFile(ConfigType.MESSAGES, new ConfigHandler(plugin, "messages"));
        registerFile(ConfigType.DATA, new ConfigHandler(plugin, "data"));
        registerFile(ConfigType.COMMANDS, new ConfigHandler(plugin, "commands"));

        // Load all configs safely
        configurations.values().forEach(config -> {
            try {
                config.saveDefaultConfig();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load config: " + config.getFile().getName());
                e.printStackTrace();
            }
        });

        // Perform migrations if needed
        migrateConfigs();

        // Set the Messages configuration safely
        ConfigHandler messages = getFile(ConfigType.MESSAGES);
        if (messages != null) {
            Messages.setConfiguration(messages.getConfig());
        } else {
            plugin.getLogger().warning("Messages config not found!");
        }
    }

    /**
     * Migrates all configurations that need updating
     */
    private void migrateConfigs() {
        for (Map.Entry<ConfigType, ConfigHandler> entry : configurations.entrySet()) {
            ConfigHandler handler = entry.getValue();
            if (handler != null && handler.needsMigration()) {
                if (migrator.migrateConfig(entry.getKey(), handler)) {
                    handler.setMigrated();
                }
            }
        }
    }

    /**
     * Get a registered ConfigHandler by type
     */
    public ConfigHandler getFile(ConfigType type) {
        return configurations.get(type);
    }

    /**
     * Reload all configs safely
     */
    public void reloadFiles() {
        configurations.values().forEach(config -> {
            try {
                if (config != null) config.reload();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Perform migrations after reload
        migrateConfigs();

        // Update Messages configuration safely
        ConfigHandler messages = getFile(ConfigType.MESSAGES);
        if (messages != null) {
            Messages.setConfiguration(messages.getConfig());
        }
    }

    /**
     * Save all configs safely
     */
    public void saveFiles() {
        configurations.values().forEach(config -> {
            if (config != null) {
                try {
                    config.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Register a config handler
     */
    public void registerFile(ConfigType type, ConfigHandler config) {
        if (type != null && config != null) {
            configurations.put(type, config);
        }
    }

    /**
     * Load a standalone FileConfiguration safely
     */
    public FileConfiguration getFileConfiguration(File file) {
        if (file == null || !file.exists()) return new YamlConfiguration();
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            e.printStackTrace();
            return new YamlConfiguration();
        }
    }
}
