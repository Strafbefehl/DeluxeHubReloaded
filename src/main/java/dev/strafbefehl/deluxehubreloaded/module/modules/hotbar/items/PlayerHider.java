package dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.items;

import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.cooldown.CooldownType;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.HotbarItem;
import dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.HotbarManager;
import dev.strafbefehl.deluxehubreloaded.module.modules.player.PlayerVanish;
import dev.strafbefehl.deluxehubreloaded.utility.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerHider extends HotbarItem {

	private final int cooldown;
	private final ItemStack hiddenItem;
	private final List<UUID> hidden;

	public PlayerHider(HotbarManager hotbarManager, ItemStack item, int slot, String key) {
		super(hotbarManager, item, slot, key);
		hidden = new ArrayList<>();
		FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);
		ItemStackBuilder builder = ItemStackBuilder.getItemStack(config.getConfigurationSection("player_hider.hidden"));
		ItemMeta meta = builder.build().getItemMeta();
		if (meta != null) {
			meta.getPersistentDataContainer().set(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING, key);
			hiddenItem = builder.build();
			hiddenItem.setItemMeta(meta);
		} else {
			hiddenItem = null;
		}
		cooldown = config.getInt("player_hider.cooldown");
	}

	public boolean isHiding(Player player) {
		return hidden.contains(player.getUniqueId());
	}

	private boolean isVanished(Player player) {
		PlayerVanish vanishModule = (PlayerVanish) getPlugin().getModuleManager().getModule(ModuleType.VANISH);
		return vanishModule != null && vanishModule.isVanished(player);
	}

	@Override
	protected void onInteract(Player player) {

		if (!getHotbarManager().tryCooldown(player.getUniqueId(), CooldownType.PLAYER_HIDER, cooldown)) {
			Messages.COOLDOWN_ACTIVE.send(player, "%time%", getHotbarManager().getCooldown(player.getUniqueId(), CooldownType.PLAYER_HIDER));
			return;
		}

		if (!hidden.contains(player.getUniqueId())) {
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				player.hidePlayer(getPlugin(), pl);
			}
			hidden.add(player.getUniqueId());
			Messages.PLAYER_HIDER_HIDDEN.send(player);

			player.getInventory().setItem(getSlot(), hiddenItem);
		} else {
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				player.showPlayer(getPlugin(), pl);
				if (isVanished(pl)) player.hidePlayer(getPlugin(), pl);
			}
			hidden.remove(player.getUniqueId());
			Messages.PLAYER_HIDER_SHOWN.send(player);

			player.getInventory().setItem(getSlot(), getItem());
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		if (hidden.contains(player.getUniqueId())) {
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				player.showPlayer(getPlugin(), pl);
				if (isVanished(pl)) player.hidePlayer(getPlugin(), pl);
			}
		}
		hidden.remove(player.getUniqueId());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		hidden.forEach(uuid -> {
			Bukkit.getPlayer(uuid).hidePlayer(getPlugin(), player);
		});
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		if (getHotbarManager().inDisabledWorld(player.getLocation()) && hidden.contains(player.getUniqueId())) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				player.showPlayer(getPlugin(), p);
				if (isVanished(p)) player.hidePlayer(getPlugin(), p);
			}
			hidden.remove(player.getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (hidden.contains(player.getUniqueId())) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				player.showPlayer(getPlugin(), p);
				if (isVanished(p)) player.hidePlayer(getPlugin(), p);
			}
			hidden.remove(player.getUniqueId());
		}
	}
}