package dev.strafbefehl.deluxehubreloaded.module.modules.player;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.HotbarManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.items.PlayerHider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerVanish extends Module {

	private List<UUID> vanished;

	public PlayerVanish(DeluxeHubPlugin plugin) {
		super(plugin, ModuleType.VANISH);
	}

	@Override
	public void onEnable() {
		vanished = new ArrayList<>();
	}

	@Override
	public void onDisable() {
		vanished.clear();
	}

	public void toggleVanish(Player player) {
		if (isVanished(player)) {
			vanished.remove(player.getUniqueId());
			Bukkit.getOnlinePlayers().forEach(pl -> {
				if (!isPlayerHiding(pl)) pl.showPlayer(getPlugin(), player);
			});

			Messages.VANISH_DISABLE.send(player);
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);

		} else {
			vanished.add(player.getUniqueId());
			Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(getPlugin(), player));

			Messages.VANISH_ENABLE.send(player);
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 1));
		}
	}

	public boolean isVanished(Player player) {
		return vanished.contains(player.getUniqueId());
	}

	private boolean isPlayerHiding(Player observer) {
		HotbarManager hotbarManager = (HotbarManager) getPlugin().getModuleManager().getModule(ModuleType.HOTBAR_ITEMS);
		if (hotbarManager == null) return false;
		return hotbarManager.getHotbarItems().stream()
				.filter(item -> item instanceof PlayerHider)
				.map(item -> (PlayerHider) item)
				.findFirst()
				.map(hider -> hider.isHiding(observer))
				.orElse(false);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		vanished.forEach(hidden -> event.getPlayer().hidePlayer(getPlugin(), Bukkit.getPlayer(hidden)));
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		player.removePotionEffect(PotionEffectType.NIGHT_VISION);
		vanished.remove(player.getUniqueId());
	}
}
