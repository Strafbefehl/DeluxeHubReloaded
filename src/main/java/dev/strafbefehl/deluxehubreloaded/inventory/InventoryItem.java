package dev.strafbefehl.deluxehubreloaded.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InventoryItem {

	public final ItemStack itemStack;
	public final List<ClickAction> clickActions;
	private String permission;

	public InventoryItem(final ItemStack itemStack) {
		this.clickActions = new ArrayList<>();
		this.itemStack = itemStack;
	}

	public InventoryItem addClickAction(final ClickAction clickAction) {
		this.clickActions.add(clickAction);
		return this;
	}

	public InventoryItem withPermission(String permission) {
		this.permission = permission;
		return this;
	}

	public String getPermission() {
		return permission;
	}

	public boolean hasPermission(Player player) {
		return permission == null || player.hasPermission(permission);
	}

	public List<ClickAction> getClickActions() {
		return this.clickActions;
	}

	public ItemStack getItemStack() {
		return this.itemStack;
	}
}
