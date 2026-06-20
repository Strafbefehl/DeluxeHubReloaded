package dev.strafbefehl.deluxehubreloaded.inventory;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.utility.ItemStackBuilder;
import dev.strafbefehl.deluxehubreloaded.utility.NamespacedKeys;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractInventory implements Listener {

	private final DeluxeHubPlugin plugin;
	private final List<UUID> openInventories;
	private boolean refreshEnabled = false;

	public AbstractInventory(DeluxeHubPlugin plugin) {
		this.plugin = plugin;
		openInventories = new ArrayList<>();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void setInventoryRefresh(long value) {
		if (value <= 0) return;
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new InventoryTask(this), 0L, value);
		refreshEnabled = true;
	}

	public abstract void onEnable();

	protected abstract Inventory getInventory();

	protected DeluxeHubPlugin getPlugin() {
		return plugin;
	}

	public Inventory refreshInventory(Player player, Inventory inventory) {
		if (!(inventory.getHolder() instanceof InventoryBuilder holder)) return inventory;

		for (Map.Entry<Integer, List<InventoryItem>> entry : holder.getIcons().entrySet()) {
			int slot = entry.getKey();
			InventoryItem inventoryItem = holder.getIcon(slot, player);

			if (inventoryItem == null) {
				inventory.setItem(slot, null);
				continue;
			}

			ItemStack item = inventoryItem.getItemStack().clone();
			if (item.getType() == Material.AIR || !item.hasItemMeta()) {
				inventory.setItem(slot, item);
				continue;
			}

			if (item.getType() == Material.PLAYER_HEAD) {
				ItemMeta itemMeta = item.getItemMeta();
				if (itemMeta != null) {
					PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
					if (dataContainer.get(NamespacedKeys.Keys.PLAYER_HEAD.get(), PersistentDataType.BOOLEAN) != null) {
						SkullMeta meta = (SkullMeta) item.getItemMeta();
						if (meta != null) {
							meta.setPlayerProfile(player.getPlayerProfile());
							item.setItemMeta(meta);
						}
					}
				}
			}

			ItemStackBuilder newItem = new ItemStackBuilder(item);
			if (item.getItemMeta().hasDisplayName()) newItem.withName(
					LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName()), player);
			if (item.getItemMeta().hasLore()) newItem.withLore(
					item.getItemMeta().lore().stream()
							.map(c -> LegacyComponentSerializer.legacySection().serialize(c))
							.toList(), player);
			inventory.setItem(slot, newItem.build());
		}
		return inventory;
	}

	public void openInventory(Player player) {
		if (getInventory() == null) return;

		player.openInventory(refreshInventory(player, getInventory()));
		if (refreshEnabled && !openInventories.contains(player.getUniqueId())) {
			openInventories.add(player.getUniqueId());
		}
	}

	public List<UUID> getOpenInventories() {
		return openInventories;
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getView().getTopInventory().getHolder() instanceof InventoryBuilder && refreshEnabled) {
			openInventories.remove(event.getPlayer().getUniqueId());
			//System.out.println("removed " + event.getPlayer().getName());
		}
	}

}
