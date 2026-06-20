package dev.strafbefehl.deluxehubreloaded.module.modules.player;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.command.commands.FlyCommand;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class DoubleJump extends Module {

	private long cooldownDelay;
	private double launch;
	private double launchY;
	private List<String> actions;

	public DoubleJump(DeluxeHubPlugin plugin) {
		super(plugin, ModuleType.DOUBLE_JUMP);
	}

	@Override
	public void onEnable() {
		FileConfiguration config = getConfig(ConfigType.SETTINGS);
		cooldownDelay = config.getLong("double_jump.cooldown", 0);
		launch = config.getDouble("double_jump.launch_power", 1.3);
		launchY = config.getDouble("double_jump.launch_power_y", 1.2);
		actions = config.getStringList("double_jump.actions");

		if (launch > 4.0) launch = 4.0;
		if (launchY > 4.0) launchY = 4.0;

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
			if (inDisabledWorld(player.getLocation())) continue;
			player.setAllowFlight(true);
		}
	}

	@Override
	public void onDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
			if (Boolean.TRUE.equals(FlyCommand.allowPlayerFly.get(player.getUniqueId()))) continue;
			player.setAllowFlight(false);
			player.setFlying(false);
		}
	}

	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {

		Player player = event.getPlayer();

		// Perform checks
		//if (player.hasPermission(new Permission(Permissions.DOUBLE_JUMP_BYPASS.getPermission(), PermissionDefault.FALSE))) return; Useless lol
		if (FlyCommand.allowPlayerFly.putIfAbsent(player.getUniqueId(), false) == null) ;
		if (FlyCommand.allowPlayerFly.get(player.getUniqueId())) return;
		else if (inDisabledWorld(player.getLocation())) return;
		else if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
		else if (!event.isFlying()) return;

		PvPMode pvpMode = (PvPMode) getPlugin().getModuleManager().getModule(ModuleType.PVP_MODE);
		if (pvpMode != null && pvpMode.isPlayerInPvPMode(player.getUniqueId())) {
			event.setCancelled(true);
			return;
		}


		// Check for cooldown
//        UUID uuid = player.getUniqueId();
//        if (!tryCooldown(uuid, CooldownType.DOUBLE_JUMP, cooldownDelay)) {
//            Messages.DOUBLE_JUMP_COOLDOWN.send(player, "%time%", getCooldown(uuid, CooldownType.DOUBLE_JUMP));
//            return;
//        }

		// Execute double jump
		player.setVelocity(player.getLocation().getDirection().multiply(launch).setY(launchY));
		executeActions(player, actions);
		event.setCancelled(true);
		player.setAllowFlight(false);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (pvpMode != null && pvpMode.isPlayerInPvPMode(player.getUniqueId())) return;
				player.setAllowFlight(true);
				event.setCancelled(true);
			}
		}.runTaskLater(getPlugin(), 20 * cooldownDelay);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR && !inDisabledWorld(player.getLocation())) {
			player.getPlayer().setAllowFlight(true);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR
				&& !inDisabledWorld(player.getLocation()))
			player.getPlayer().setAllowFlight(true);
	}
}