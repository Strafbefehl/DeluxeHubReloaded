package dev.strafbefehl.deluxehubreloaded.module.modules.hotbar;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.module.modules.world.BuildMode;
import dev.strafbefehl.deluxehubreloaded.utility.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public abstract class HotbarItem implements Listener {

	private final HotbarManager hotbarManager;
	private final ItemStack item;
	private final String key;
	private final int slot;
	private ConfigurationSection configurationSection;
	private String permission = null;
	private boolean allowMovement;

	public HotbarItem(HotbarManager hotbarManager, ItemStack item, int slot, String key) {
		this.hotbarManager = hotbarManager;
		this.key = key;
		this.slot = slot;

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.getPersistentDataContainer().set(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING, key);
			item.setItemMeta(meta);
		}
		this.item = item;
	}

	public DeluxeHubPlugin getPlugin() {
		return hotbarManager.getPlugin();
	}

	public HotbarManager getHotbarManager() {
		return hotbarManager;
	}

	public ItemStack getItem() {
		return item;
	}

	protected abstract void onInteract(Player player);

	public String getKey() {
		return key;
	}

	public int getSlot() {
		return slot;
	}

	public void setAllowMovement(boolean allowMovement) {
		this.allowMovement = allowMovement;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public ConfigurationSection getConfigurationSection() {
		return configurationSection;
	}

	public void setConfigurationSection(ConfigurationSection configurationSection) {
		this.configurationSection = configurationSection;
	}

	public void giveItem(Player player) {
		if (permission != null && !player.hasPermission(permission)) return;

		ItemStack newItem = item.clone();
		if (getConfigurationSection() != null && getConfigurationSection().contains("username")) {
			newItem = new ItemStackBuilder(newItem).setSkullOwner(player.getName()).build();
		}

		player.getInventory().setItem(slot, newItem);
	}

	public void removeItem(Player player) {
		PlayerInventory inventory = player.getInventory();
		ItemStack item = inventory.getItem(slot);
		if (item == null) return;

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			String hotbarItem = meta.getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING);
			if (hotbarItem != null && hotbarItem.equals(key)) {
				inventory.remove(Objects.requireNonNull(inventory.getItem(slot)));
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!allowMovement) return;
		if (BuildMode.getInstance().isPresent(event.getWhoClicked().getUniqueId())) return;

		Player player = (Player) event.getWhoClicked();
		if (getHotbarManager().inDisabledWorld(player.getLocation())) return;

		ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getType() == Material.AIR) return;

		ItemMeta meta = clicked.getItemMeta();
		if (meta != null) {
			String hotbarItem = meta.getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING);
			if (hotbarItem != null && event.getSlot() == slot && hotbarItem.equals(key))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void hotbarItemInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getHand() != EquipmentSlot.HAND) return;

		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() == Material.AIR) return;

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			String hotbarItem = meta.getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING);
			if (hotbarItem != null && getHotbarManager().inDisabledWorld(player.getLocation()) && hotbarItem.equals(key)) return;
			else if (hotbarItem != null && hotbarItem.equals(key)) {
				onInteract(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void hotbarPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!getHotbarManager().inDisabledWorld(player.getLocation())) giveItem(player);
	}

	@EventHandler
	public void hotbarPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (!getHotbarManager().inDisabledWorld(player.getLocation())) removeItem(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void hotbarWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
			if (getHotbarManager().inDisabledWorld(player.getLocation())) {
				removeItem(player);
			} else {
				giveItem(player);
			}
		}, 5L);
	}

	@EventHandler
	public void hotbarPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
		if (!getHotbarManager().inDisabledWorld(player.getLocation())) giveItem(player);
		if (getPlugin().getModuleManager().isEnabled(ModuleType.DOUBLE_JUMP)) {
			player.setAllowFlight(true);
		}
	}

}
