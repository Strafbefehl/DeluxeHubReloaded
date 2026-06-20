package dev.strafbefehl.deluxehubreloaded.hook.hooks.head;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.hook.PluginHook;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaseHead implements PluginHook, HeadHook {

	private Map<String, ItemStack> cache;

	@Override
	public void onEnable(DeluxeHubPlugin plugin) {
		cache = new HashMap<>();
	}

	@Override
	public ItemStack getHead(String data) {
		if (cache.containsKey(data)) return cache.get(data);

		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) head.getItemMeta();
		PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
		profile.setProperty(new ProfileProperty("textures", data));
		meta.setPlayerProfile(profile);
		head.setItemMeta(meta);
		cache.put(data, head);
		return head;
	}
}
