package dev.strafbefehl.deluxehubreloaded.action.actions;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.action.Action;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GamemodeAction implements Action {

	@Override
	public String getIdentifier() {
		return "GAMEMODE";
	}

	@Override
	public void execute(DeluxeHubPlugin plugin, Player player, String data) {
		try {
			player.setGameMode(GameMode.valueOf(data.toUpperCase()));
			if ((player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SURVIVAL)
					&& plugin.getModuleManager().isEnabled(ModuleType.DOUBLE_JUMP)) {
				player.setAllowFlight(true);
			}
		} catch (IllegalArgumentException ex) {
			Bukkit.getLogger().warning("[DeluxeHubReloaded Action] Invalid gamemode name: " + data.toUpperCase());
		}
	}
}
