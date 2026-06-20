package dev.strafbefehl.deluxehubreloaded.inventory;

import dev.strafbefehl.deluxehubreloaded.module.modules.world.BuildMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (BuildMode.getInstance().isPresent(event.getWhoClicked().getUniqueId())) return;
		if (event.getView().getTopInventory().getHolder() instanceof InventoryBuilder) {

			event.setCancelled(true);

			if (event.getWhoClicked() instanceof Player) {

				Player player = (Player) event.getWhoClicked();
				ItemStack itemStack = event.getCurrentItem();

				if (itemStack == null || itemStack.getType() == Material.AIR) return;

				InventoryBuilder customHolder = (InventoryBuilder) event.getView().getTopInventory().getHolder();
				InventoryItem item = customHolder.getIcon(event.getRawSlot(), player);

				if (item == null) return;

				for (final ClickAction clickAction : item.getClickActions()) {
					clickAction.execute(player);
				}
			}
		}
	}

}
