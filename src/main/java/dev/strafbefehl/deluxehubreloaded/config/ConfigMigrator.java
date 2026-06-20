package dev.strafbefehl.deluxehubreloaded.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ConfigMigrator {

    private final JavaPlugin plugin;
    private final Version currentVersion;
    private final List<VersionMigration> versionMigrations;
    private final Set<String> excludedPaths;

    /**
     * Creates a new ConfigMigrator with the current plugin version
     */
    public ConfigMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = Version.parse(plugin.getPluginMeta().getVersion());
        this.versionMigrations = new ArrayList<>();
        this.excludedPaths = new HashSet<>();

        // Register paths to exclude from migration
        registerExcludedPaths();

        // Register any version-specific migrations here
        registerMigrations();
    }

    /**
     * Register paths that should be excluded from migration
     */
    private void registerExcludedPaths() {
        // Add the announcements section to excluded paths
        excludedPaths.add("announcements");
        excludedPaths.add("custom_join_items");
        excludedPaths.add("join_events");
        excludedPaths.add("custom_commands");
        excludedPaths.add("commands");
        excludedPaths.add("command_block");
        excludedPaths.add("anti_swear");
        excludedPaths.add("join_settings");
        excludedPaths.add("pvp_mode");



        // Add other paths to exclude if needed
        // excludedPaths.add("another.path.to.exclude");
    }

    private void registerMigrations() {
        // Example: Add migrations for specific version jumps
        // versionMigrations.add(new VersionMigration(
        //     Version.parse("1.0.0"),
        //     Version.parse("1.1.0"),
        //     this::migrateFrom1_0_0To1_1_0
        // ));
    }

    /**
     * Migrates a configuration file, adding missing options from the default config
     * while preserving existing values.
     *
     * @param configType The type of config to migrate
     * @param configHandler The config handler containing the file
     * @return True if changes were made, false otherwise
     */
    public boolean migrateConfig(ConfigType configType, ConfigHandler configHandler) {
        String fileName = getFileNameForType(configType);
        if (fileName == null) {
            plugin.getLogger().warning("No file name found for config type: " + configType);
            return false;
        }

        // Get the resource from jar
        InputStream defaultConfigStream = plugin.getResource(fileName);
        if (defaultConfigStream == null) {
            plugin.getLogger().warning("Could not find default configuration for: " + fileName);
            return false;
        }

        // Load default config from jar
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream));

        // Get current config
        FileConfiguration currentConfig = configHandler.getConfig();

        // Track if we made changes
        boolean changes = false;

        // Check if we need to perform version-specific migrations
        String storedVersion = currentConfig.getString("version", "0.0.0");
        Version configVersion = Version.parse(storedVersion);

        // Run version-specific migrations if config version is older than current version
        if (configVersion.isOlderThan(currentVersion)) {
            for (VersionMigration migration : versionMigrations) {
                if (configVersion.isOlderThan(migration.getTargetVersion()) &&
                        (migration.getSourceVersion().isSameAs(configVersion) ||
                                migration.getSourceVersion().isOlderThan(configVersion))) {

                    plugin.getLogger().info("Applying migration from " +
                            migration.getSourceVersion() + " to " + migration.getTargetVersion() +
                            " for " + fileName);

                    // Apply the migration
                    if (migration.apply(configType, currentConfig)) {
                        changes = true;
                    }
                }
            }

            // Update the version in the config
            currentConfig.set("version", currentVersion.toString());
            changes = true;
        }

        // Add missing sections and values
        if (addMissingEntries(defaultConfig, currentConfig, "")) {
            changes = true;
        }

        // Save if changes were made
        if (changes) {
            try {
                configHandler.save();
                plugin.getLogger().info("Updated configuration file: " + fileName);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save updated configuration for: " + fileName, e);
            }
        }

        return changes;
    }

    /**
     * Recursively adds missing entries from default config to current config,
     * but skips excluded paths
     *
     * @param defaultConfig The default configuration from resources
     * @param currentConfig The current configuration file
     * @param path The current path being examined
     * @return True if changes were made, false otherwise
     */
    private boolean addMissingEntries(FileConfiguration defaultConfig, FileConfiguration currentConfig, String path) {
        boolean changes = false;

        // Skip this path and its children if it's in the excluded paths
        for (String excludedPath : excludedPaths) {
            if (path.equals(excludedPath) || path.startsWith(excludedPath + ".")) {
                return false;
            }
        }

        Set<String> defaultKeys = path.isEmpty()
                ? defaultConfig.getKeys(false)
                : defaultConfig.getConfigurationSection(path).getKeys(false);

        for (String key : defaultKeys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            // Skip excluded paths
            boolean shouldSkip = false;
            for (String excludedPath : excludedPaths) {
                if (fullPath.equals(excludedPath) || fullPath.startsWith(excludedPath + ".")) {
                    shouldSkip = true;
                    break;
                }
            }

            if (shouldSkip) {
                continue;
            }

            if (!currentConfig.contains(fullPath)) {
                // This key doesn't exist in the current config, so add it
                currentConfig.set(fullPath, defaultConfig.get(fullPath));
                changes = true;
                plugin.getLogger().info("Added missing config option: " + fullPath);
            } else if (defaultConfig.isConfigurationSection(fullPath)) {
                // This is a section, so recursively check its children
                changes |= addMissingEntries(defaultConfig, currentConfig, fullPath);
            }
            // If the path exists and isn't a section, keep the user's current value
        }

        return changes;
    }

    /**
     * Gets the file name for the given config type
     */
    private String getFileNameForType(ConfigType type) {
        switch (type) {
            case SETTINGS:
                return "config.yml";
            case MESSAGES:
                return "messages.yml";
            case COMMANDS:
                return "commands.yml";
            case DATA:
                return "data.yml";
            default:
                return null;
        }
    }

    /**
     * Class representing a migration between two specific versions
     */
    private class VersionMigration {
        private final Version sourceVersion;
        private final Version targetVersion;
        private final MigrationFunction migrationFunction;

        @SuppressWarnings("unused")
        public VersionMigration(Version sourceVersion, Version targetVersion,
                                MigrationFunction migrationFunction) {
            this.sourceVersion = sourceVersion;
            this.targetVersion = targetVersion;
            this.migrationFunction = migrationFunction;
        }

        public Version getSourceVersion() {
            return sourceVersion;
        }

        public Version getTargetVersion() {
            return targetVersion;
        }

        public boolean apply(ConfigType configType, FileConfiguration config) {
            return migrationFunction.migrate(configType, config);
        }
    }

    /**
     * Functional interface for migration functions
     */
    @FunctionalInterface
    private interface MigrationFunction {
        boolean migrate(ConfigType configType, FileConfiguration config);
    }

}