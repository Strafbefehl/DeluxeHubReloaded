package dev.strafbefehl.deluxehubreloaded;

import cl.bgmp.minecraft.util.commands.exceptions.*;
import dev.strafbefehl.deluxehubreloaded.action.ActionManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.world.BuildMode;
import dev.strafbefehl.deluxehubreloaded.command.CommandManager;
import dev.strafbefehl.deluxehubreloaded.config.ConfigManager;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.config.Version;
import dev.strafbefehl.deluxehubreloaded.cooldown.CooldownManager;
import dev.strafbefehl.deluxehubreloaded.hook.HooksManager;
import dev.strafbefehl.deluxehubreloaded.inventory.InventoryManager;
import dev.strafbefehl.deluxehubreloaded.module.ModuleManager;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.module.modules.hologram.HologramManager;
import dev.strafbefehl.deluxehubreloaded.utility.NamespacedKeys;
import dev.strafbefehl.deluxehubreloaded.utility.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class DeluxeHubPlugin extends JavaPlugin {

	private static final int BSTATS_ID = 23061;

	private ConfigManager configManager;
	private ActionManager actionManager;
	private HooksManager hooksManager;
	private CommandManager commandManager;
	private CooldownManager cooldownManager;
	private ModuleManager moduleManager;
	private InventoryManager inventoryManager;
	private Version currentVersion;

	public void onEnable() {
		long start = System.currentTimeMillis();
		getLogger().log(Level.INFO, "Based on original code from DeluxeHub");
		getLogger().log(Level.INFO, "Modified, and maintained by Athar42 & Strafbefehl, 2025-2026");

		// Check if running on Paper
		try {
			Class.forName("io.papermc.paper.configuration.Configuration");
		} catch (ClassNotFoundException ex) {
			getLogger().severe("============= PAPER NOT DETECTED =============");
			getLogger().severe("DeluxeHubReloaded requires Paper to run.");
			getLogger().severe("Download Paper here: https://papermc.io/downloads/paper");
			getLogger().severe("The plugin will now disable.");
			getLogger().severe("============= PAPER NOT DETECTED =============");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Check server version (requires 1.21+ or 26.x+)
		String rawVersion = Bukkit.getBukkitVersion().split("-")[0];
		if (rawVersion.startsWith("1.")) {
			try {
				int minor = Integer.parseInt(rawVersion.split("\\.")[1]);
				if (minor < 21) {
					getLogger().severe("============= UNSUPPORTED SERVER VERSION =============");
					getLogger().severe("DeluxeHubReloaded requires Paper 1.21 or newer to run.");
					getLogger().severe("The plugin will now disable.");
					getLogger().severe("============= UNSUPPORTED SERVER VERSION =============");
					getServer().getPluginManager().disablePlugin(this);
					return;
				}
			} catch (NumberFormatException ignored) {}
		}

		// Enable bStats metrics
		new MetricsLite(this, BSTATS_ID);

		NamespacedKeys.registerKeys();

		// Check plugin hooks
		hooksManager = new HooksManager(this);

		// Load config files
		configManager = new ConfigManager();
		configManager.loadFiles(this);
		currentVersion = Version.parse(getPluginMeta().getVersion());

		// If there were any configuration errors we should not continue
		if (!getServer().getPluginManager().isPluginEnabled(this)) return;

		// Command manager
		commandManager = new CommandManager(this);
		commandManager.reload();

		// Cooldown manager
		cooldownManager = new CooldownManager();

		// Inventory (GUI) manager
		inventoryManager = new InventoryManager();
		if (!hooksManager.isHookEnabled("HEAD_DATABASE")) inventoryManager.onEnable(this);

		// Core plugin modules
		moduleManager = new ModuleManager();
		moduleManager.loadModules(this);

		// Action system
		actionManager = new ActionManager(this);

		//BuildMode manager
		BuildMode.getInstance();

		// Load update checker (if enabled)
		if (getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getBoolean("check-updates"))
			new UpdateChecker(this).checkForUpdate();

		// Register BungeeCord channels
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		getLogger().log(Level.INFO, "");
		getLogger().log(Level.INFO, "Successfully loaded in " + (System.currentTimeMillis() - start) + "ms");

	}

	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		moduleManager.unloadModules();
		inventoryManager.onDisable();
		//configManager.saveFiles();
	}

	public void reload() {
		Bukkit.getScheduler().cancelTasks(this);
		HandlerList.unregisterAll(this);

		configManager.reloadFiles();

		BuildMode.getInstance().runScheduler();

		inventoryManager.onDisable();
		inventoryManager.onEnable(this);

		getCommandManager().reload();

		moduleManager.loadModules(this);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.Command cmd, @NotNull String commandLabel, String[] args) {
		try {
			getCommandManager().execute(cmd.getName(), args, sender);
		} catch (CommandPermissionsException e) {
			Messages.NO_PERMISSION.send(sender);
		} catch (MissingNestedCommandException e) {
			sender.sendMessage(Component.text(e.getUsage(), NamedTextColor.RED));
		} catch (CommandUsageException e) {
			sender.sendMessage(Component.text("Usage: " + e.getUsage(), NamedTextColor.RED));
		} catch (WrappedCommandException e) {
			if (e.getCause() instanceof NumberFormatException) {
				sender.sendMessage(Component.text("Number expected, string received instead.", NamedTextColor.RED));
			} else {
				sender.sendMessage(Component.text("An internal error has occurred. See console.", NamedTextColor.RED));
				e.printStackTrace();
			}
		} catch (CommandException e) {
			sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
		}
		return true;
	}

	public HologramManager getHologramManager() {
		return (HologramManager) moduleManager.getModule(ModuleType.HOLOGRAMS);
	}

	public HooksManager getHookManager() {
		return hooksManager;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}

	public CommandManager getCommandManager() {
		return commandManager;
	}

	public CooldownManager getCooldownManager() {
		return cooldownManager;
	}

	public InventoryManager getInventoryManager() {
		return inventoryManager;
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public Version getCurrentVersion() {
		return currentVersion;
	}

	public ActionManager getActionManager() {
		return actionManager;
	}
}
