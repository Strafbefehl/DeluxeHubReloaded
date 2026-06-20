package dev.strafbefehl.deluxehubreloaded.inventory.inventories;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.inventory.AbstractInventory;
import dev.strafbefehl.deluxehubreloaded.inventory.InventoryBuilder;
import dev.strafbefehl.deluxehubreloaded.inventory.InventoryItem;
import dev.strafbefehl.deluxehubreloaded.utility.ItemStackBuilder;
import dev.strafbefehl.deluxehubreloaded.utility.TextUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class CustomGUI extends AbstractInventory {

	private InventoryBuilder inventory;
	private final FileConfiguration config;

	public CustomGUI(DeluxeHubPlugin plugin, FileConfiguration config) {
		super(plugin);
		this.config = config;
	}

	@Override
	public void onEnable() {

		InventoryBuilder inventoryBuilder = new InventoryBuilder(config.getInt("slots"), TextUtil.color(config.getString("title")));

		if (config.contains("refresh") && config.getBoolean("refresh.enabled")) {
			setInventoryRefresh(config.getLong("refresh.rate"));
		}

		List<InventoryItem> fillerItems = new ArrayList<>();

		for (String entry : config.getConfigurationSection("items").getKeys(false)) {

			try {
				ItemStackBuilder builder = ItemStackBuilder.getItemStack(config.getConfigurationSection("items." + entry));

				InventoryItem inventoryItem;
				if (!config.contains("items." + entry + ".actions")) {
					inventoryItem = new InventoryItem(builder.build());
				} else {
					inventoryItem = new InventoryItem(builder.build()).addClickAction(p -> getPlugin().getActionManager().executeActions(p, config.getStringList("items." + entry + ".actions")));
				}

				if (config.contains("items." + entry + ".permission")) {
					inventoryItem.withPermission(config.getString("items." + entry + ".permission"));
				}

				if (config.contains("items." + entry + ".slots")) {
					for (String slot : config.getStringList("items." + entry + ".slots")) {
						inventoryBuilder.setItem(Integer.parseInt(slot), inventoryItem);
					}
				} else if (config.contains("items." + entry + ".slot")) {
					int slot = config.getInt("items." + entry + ".slot");
					if (slot == -1) {
						fillerItems.add(inventoryItem);
					} else {
						inventoryBuilder.setItem(slot, inventoryItem);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				getPlugin().getLogger().warning("There was an error loading GUI item ID '" + entry + "', skipping..");
			}
		}

		// Fill remaining empty slots with filler items after all specific-slot items are loaded.
		if (!fillerItems.isEmpty()) {
			int totalSlots = Math.max(9, Math.min(54, config.getInt("slots")));
			for (InventoryItem fillerItem : fillerItems) {
				for (int s = 0; s < totalSlots; s++) {
					if (!inventoryBuilder.getIcons().containsKey(s)) {
						inventoryBuilder.setItem(s, fillerItem);
					}
				}
			}
		}

		inventory = inventoryBuilder;

	}

	@Override
	protected Inventory getInventory() {
		return inventory.getInventory();
	}
}
