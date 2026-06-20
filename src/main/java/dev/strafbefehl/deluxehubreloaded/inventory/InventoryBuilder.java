package dev.strafbefehl.deluxehubreloaded.inventory;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryBuilder implements InventoryHolder {

	private final Map<Integer, List<InventoryItem>> icons;
	private int size;
	private final String title;

	public InventoryBuilder(int size, String title) {
		this.icons = new HashMap<>();
		this.size = size;
		this.title = title;
	}

	public void setItem(int slot, InventoryItem item) {
		icons.computeIfAbsent(slot, k -> new ArrayList<>()).add(item);
	}

	public InventoryItem getIcon(final int slot) {
		List<InventoryItem> items = icons.get(slot);
		return (items != null && !items.isEmpty()) ? items.get(0) : null;
	}

	public InventoryItem getIcon(final int slot, Player player) {
		List<InventoryItem> items = icons.get(slot);
		if (items == null) return null;
		return items.stream()
				.filter(item -> item.hasPermission(player))
				.findFirst()
				.orElse(null);
	}

	public Map<Integer, List<InventoryItem>> getIcons() {
		return icons;
	}

	public Inventory getInventory() {
		if (size > 54) size = 54;
		else if (size < 9) size = 9;

		Inventory inventory = Bukkit.createInventory(this, size,
				LegacyComponentSerializer.legacyAmpersand().deserialize(title));
		for (Map.Entry<Integer, List<InventoryItem>> entry : icons.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				inventory.setItem(entry.getKey(), entry.getValue().get(0).getItemStack());
			}
		}
		return inventory;
	}
}
