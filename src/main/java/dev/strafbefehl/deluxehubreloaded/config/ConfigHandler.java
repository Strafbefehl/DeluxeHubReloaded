package dev.strafbefehl.deluxehubreloaded.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigHandler {

	private final JavaPlugin plugin;
	private final String name;
	private final File file;
	private FileConfiguration configuration;
	private boolean needsMigration;

	public ConfigHandler(JavaPlugin plugin, String name) {
		this.plugin = plugin;
		this.name = name + ".yml";
		this.file = new File(plugin.getDataFolder(), this.name);
		this.configuration = new YamlConfiguration();
		this.needsMigration = false;
	}

	/**
	 * Saves the default config if it doesn't exist, otherwise loads the existing config
	 * and marks it for migration.
	 */
	public void saveDefaultConfig() {
		boolean fileExists = file.exists();

		if (!fileExists) {
			plugin.saveResource(name, false);
		} else {
			// If file exists, it might need migration when plugin updates
			needsMigration = true;
		}

		try {
			configuration.load(file);
		} catch (InvalidConfigurationException | IOException e) {
			handleConfigError(e);
		}
	}

	/**
	 * Saves the current configuration to file
	 */
	public void save() {
		if (configuration == null || file == null) return;
		try {
			getConfig().save(file);
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to save configuration file: " + name + " — " + e.getMessage());
		}
	}

	/**
	 * Reloads the configuration from file
	 */
	public void reload() {
		try {
			configuration = YamlConfiguration.loadConfiguration(file);
		} catch (Exception e) {
			handleConfigError(e);
		}
	}

	/**
	 * Gets the file configuration
	 */
	public FileConfiguration getConfig() {
		return configuration;
	}

	/**
	 * Gets the file associated with this config handler
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Checks if this configuration needs migration
	 */
	public boolean needsMigration() {
		return needsMigration;
	}

	/**
	 * Marks this configuration as migrated
	 */
	public void setMigrated() {
		this.needsMigration = false;
	}

	/**
	 * Handle configuration errors
	 */
	private void handleConfigError(Exception e) {
		e.printStackTrace();
		plugin.getLogger().severe("============= CONFIGURATION ERROR =============");
		plugin.getLogger().severe("There was an error loading " + name);
		plugin.getLogger().severe("Please check for any obvious configuration mistakes");
		plugin.getLogger().severe("such as using tabs for spaces or forgetting to end quotes");
		plugin.getLogger().severe("before reporting to the developer. The plugin will now disable..");
		plugin.getLogger().severe("============= CONFIGURATION ERROR =============");
		plugin.getServer().getPluginManager().disablePlugin(plugin);
	}
}