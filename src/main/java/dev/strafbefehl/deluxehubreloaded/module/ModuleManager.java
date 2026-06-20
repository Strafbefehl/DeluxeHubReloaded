package dev.strafbefehl.deluxehubreloaded.module;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.command.CustomCommandHandler;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.module.modules.chat.AntiSwear;
import dev.strafbefehl.deluxehubreloaded.module.modules.chat.AutoBroadcast;
import dev.strafbefehl.deluxehubreloaded.module.modules.chat.ChatCommandBlock;
import dev.strafbefehl.deluxehubreloaded.module.modules.chat.ChatLock;
import dev.strafbefehl.deluxehubreloaded.module.modules.hologram.HologramManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.HotbarManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.player.*;
import dev.strafbefehl.deluxehubreloaded.module.modules.visual.scoreboard.ScoreboardManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.visual.tablist.TablistManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.world.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ModuleManager {

	private DeluxeHubPlugin plugin;
	private List<String> disabledWorlds;
	private final Map<ModuleType, Module> modules = new HashMap<>();

	public void loadModules(DeluxeHubPlugin plugin) {
		this.plugin = plugin;

		if (!modules.isEmpty()) unloadModules();

		FileConfiguration config = plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
		disabledWorlds = config.getStringList("disabled-worlds.worlds");

		if (config.getBoolean("disabled-worlds.invert")) {
			disabledWorlds = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
			for (String world : config.getStringList("disabled-worlds.worlds")) disabledWorlds.remove(world);
		}

		if (!config.getBoolean("multiple_worlds")) {
			registerModule(new TeleportationBow(plugin), "teleportation_bow.enabled");
			registerModule(new PvPMode(plugin), "pvp_mode.enabled");
		} else {
			Bukkit.getLogger().log(Level.WARNING, "Multiple worlds is enabled (This deactivates the teleportation bow and pvp mode modules)");
		}
		registerModule(new StaticTime(plugin), "static_time.enabled");
		registerModule(new AntiWorldDownloader(plugin), "anti_wdl.enabled");
		registerModule(new DoubleJump(plugin), "double_jump.enabled");
		registerModule(new Launchpad(plugin), "launchpad.enabled");
		registerModule(new ScoreboardManager(plugin), "scoreboard.enabled");
		registerModule(new TablistManager(plugin), "tablist.enabled");
		registerModule(new AutoBroadcast(plugin), "announcements.enabled");
		registerModule(new AntiSwear(plugin), "anti_swear.enabled");
		registerModule(new ChatCommandBlock(plugin), "command_block.enabled");
		registerModule(new ChatLock(plugin));
		registerModule(new CustomCommandHandler(plugin));
		registerModule(new PlayerListener(plugin));
		registerModule(new HotbarManager(plugin));
		registerModule(new WorldProtect(plugin));
		registerModule(new LobbySpawn(plugin));
		registerModule(new PlayerVanish(plugin));
		registerModule(new HologramManager(plugin));

		// Requires 1.9+
//		if (XMaterial.supports(9)) {
//			registerModule(new PlayerOffHandSwap(plugin), "world_settings.disable_off_hand_swap");
//		}

		for (Module module : modules.values()) {
			try {
				module.setDisabledWorlds(disabledWorlds);
				module.onEnable();
			} catch (Exception e) {
				e.printStackTrace();
				plugin.getLogger().severe("============= DELUXEHUB MODULE LOAD ERROR =============");
				plugin.getLogger().severe("There was an error loading the " + module.getModuleType() + " module");
				plugin.getLogger().severe("The plugin will now disable..");
				plugin.getLogger().severe("============= DELUXEHUB MODULE LOAD ERROR =============");
				plugin.getServer().getPluginManager().disablePlugin(plugin);
				break;
			}
		}

		plugin.getLogger().info("Loaded " + modules.size() + " plugin modules.");
	}

	public void unloadModules() {
		for (Module module : modules.values()) {
			try {
				HandlerList.unregisterAll(module);
				module.onDisable();
			} catch (Exception e) {
				e.printStackTrace();
				plugin.getLogger().severe("There was an error unloading the " + module.getModuleType().toString() + " module.");
			}
		}
		modules.clear();
	}

	public Module getModule(ModuleType type) {
		return modules.get(type);
	}

	public void registerModule(Module module) {
		registerModule(module, null);
	}

	public void registerModule(Module module, String isEnabledPath) {
		DeluxeHubPlugin plugin = module.getPlugin();
		if (isEnabledPath != null && !plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getBoolean(isEnabledPath, false))
			return;

		plugin.getServer().getPluginManager().registerEvents(module, plugin);
		modules.put(module.getModuleType(), module);
	}

	public boolean isEnabled(ModuleType type) {
		return modules.containsKey(type);
	}

	public List<String> getDisabledWorlds() {
		return disabledWorlds;
	}
}
