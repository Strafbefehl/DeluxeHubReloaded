package dev.strafbefehl.deluxehubreloaded.utility;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.hook.hooks.head.HeadHook;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemStackBuilder {

	private static final DeluxeHubPlugin PLUGIN = JavaPlugin.getPlugin(DeluxeHubPlugin.class);
	private final ItemStack ITEM_STACK;

	public ItemStackBuilder(ItemStack item) {
		this.ITEM_STACK = item;
	}

	public static ItemStackBuilder getItemStack(ItemStack item, ConfigurationSection section, Player player) {
		ItemStackBuilder builder = new ItemStackBuilder(item);

		if (section.contains("amount")) {
			builder.withAmount(section.getInt("amount"));
		}

		if (section.contains("username") && player != null) {
			builder.setSkullOwner(section.getString("username").replace("%player%", player.getName()));
		} else if (section.contains("username") && player == null) {
			if (section.getString("username").equalsIgnoreCase("%player%")) {
				ItemMeta meta = item.getItemMeta();
				if (meta != null) {
					PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
					dataContainer.set(NamespacedKeys.Keys.PLAYER_HEAD.get(), PersistentDataType.BOOLEAN, true);
					item.setItemMeta(meta);
				}
			}
		}

		if (section.contains("display_name")) {
			if (player != null) builder.withName(section.getString("display_name"), player);
			else builder.withName(section.getString("display_name"));
		}

		if (section.contains("lore")) {
			if (player != null) builder.withLore(section.getStringList("lore"), player);
			else builder.withLore(section.getStringList("lore"));
		}

		if (section.contains("glow") && section.getBoolean("glow")) {
			builder.withGlow();
		}

		if (section.contains("custom_model_data")) {
			builder.withCustomModelData(section.getInt("custom_model_data"));
		}

		if (section.contains("item_flags")) {
			List<ItemFlag> flags = new ArrayList<>();
			section.getStringList("item_flags").forEach(text -> {
				try {
					ItemFlag flag = ItemFlag.valueOf(text);
					flags.add(flag);
				} catch (IllegalArgumentException ignored) {
				}
			});
			builder.withFlags(flags.toArray(new ItemFlag[0]));
		}

		if(section.contains("custom_model_data")) {
			builder.setCustomModelData(section.getInt("custom_model_data"));
		}

		if(section.contains("enchantments")) {
			@SuppressWarnings("unchecked")
			List<LinkedHashMap<String, Integer>> enchantments = (List<LinkedHashMap<String, Integer>>) section.getList("enchantments");
			for (LinkedHashMap<String, Integer> enchantmentMap : enchantments) {
				for (Map.Entry<String, Integer> entry : enchantmentMap.entrySet()) {
					String enchantmentName = entry.getKey();
					int level = entry.getValue();
					Enchantment enchantment = RegistryAccess.registryAccess()
							.getRegistry(RegistryKey.ENCHANTMENT)
							.get(NamespacedKey.minecraft(enchantmentName.toLowerCase()));
					if (enchantment != null) {
						builder.withEnchantment(enchantment, level);
					} else {
						PLUGIN.getLogger().warning("Enchantment " + enchantmentName + " not found!");
					}
				}
			}
		}

		return builder;
	}

	public static ItemStackBuilder getItemStack(ConfigurationSection section, Player player) {
		Material material;
		try {
			material = Material.valueOf(section.getString("material").toUpperCase());
		} catch (IllegalArgumentException e) {
			// Fallback to a default material if the configured one is invalid
			material = Material.STONE;
			// You might want to log this error
		}

		ItemStack item = new ItemStack(material);

		if (material == Material.PLAYER_HEAD) {
			if (section.contains("base64")) {
				item = ((HeadHook) PLUGIN.getHookManager().getPluginHook("BASE64")).getHead(section.getString("base64")).clone();
			} else if (section.contains("hdb") && PLUGIN.getHookManager().isHookEnabled("HEAD_DATABASE")) {
				item = ((HeadHook) PLUGIN.getHookManager().getPluginHook("HEAD_DATABASE")).getHead(section.getString("hdb"));
			}
		}

		return getItemStack(item, section, player);
	}

	public static ItemStackBuilder getItemStack(ConfigurationSection section) {
		return getItemStack(section, null);
	}

	public ItemStackBuilder withAmount(int amount) {
		ITEM_STACK.setAmount(amount);
		return this;
	}

	public ItemStackBuilder withFlags(ItemFlag... flags) {
		ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.addItemFlags(flags);
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	public ItemStackBuilder withName(String name) {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.displayName(LegacyComponentSerializer.legacySection().deserialize(TextUtil.color(name)));
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	public ItemStackBuilder withName(String name, Player player) {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.displayName(LegacyComponentSerializer.legacySection().deserialize(
				TextUtil.color(PlaceholderUtil.setPlaceholders(name, player))));
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	@SuppressWarnings("deprecation")
	public ItemStackBuilder setCustomModelData(int data) {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.setCustomModelData(data);
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	public ItemStackBuilder setSkullOwner(String owner) {
		try {
			SkullMeta im = (SkullMeta) ITEM_STACK.getItemMeta();
			if (im != null) {
				if (Bukkit.getPlayer(owner) != null) {
					im.setPlayerProfile(Bukkit.getPlayer(owner).getPlayerProfile());
				}
			}
			ITEM_STACK.setItemMeta(im);
		} catch (ClassCastException ignored) {
		}
		return this;
	}

	public ItemStackBuilder withLore(List<String> lore, Player player) {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.lore(lore.stream()
				.map(s -> LegacyComponentSerializer.legacySection().deserialize(
						TextUtil.color(PlaceholderUtil.setPlaceholders(s, player))))
				.toList());
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	public ItemStackBuilder withLore(List<String> lore) {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.lore(lore.stream()
				.map(s -> LegacyComponentSerializer.legacySection().deserialize(TextUtil.color(s)))
				.toList());
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	@SuppressWarnings("deprecation")
	public ItemStackBuilder withCustomModelData(int data) {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.setCustomModelData(data);
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	@SuppressWarnings("deprecation")
	public ItemStackBuilder withDurability(int durability) {
		ITEM_STACK.setDurability((short) durability);
		return this;
	}

	@SuppressWarnings("deprecation")
	public ItemStackBuilder withData(int data) {
		ITEM_STACK.setDurability((short) data);
		return this;
	}

	public ItemStackBuilder withEnchantment(Enchantment enchantment, final int level) {
		final ItemMeta im = ITEM_STACK.getItemMeta();
		if(im == null) return this;
		im.addEnchant(enchantment, level, true);
		ITEM_STACK.setItemMeta(im);
		return this;
	}

	public ItemStackBuilder withEnchantment(Enchantment enchantment) {
		return withEnchantment(enchantment, 1);
	}

	public ItemStackBuilder withGlow() {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		ITEM_STACK.setItemMeta(meta);
		return withEnchantment(Enchantment.INFINITY, 1);
	}

	@SuppressWarnings("deprecation")
	public ItemStackBuilder withType(Material material) {
		ITEM_STACK.setType(material);
		return this;
	}

	public ItemStackBuilder clearLore() {
		final ItemMeta meta = ITEM_STACK.getItemMeta();
		meta.lore(new ArrayList<>());
		ITEM_STACK.setItemMeta(meta);
		return this;
	}

	public ItemStackBuilder clearEnchantments() {
		for (Enchantment enchantment : ITEM_STACK.getEnchantments().keySet()) {
			ITEM_STACK.removeEnchantment(enchantment);
		}
		return this;
	}

	public ItemStackBuilder withColor(Color color) {
		Material type = ITEM_STACK.getType();
		if (type == Material.LEATHER_BOOTS || type == Material.LEATHER_CHESTPLATE || type == Material.LEATHER_HELMET || type == Material.LEATHER_LEGGINGS) {
			LeatherArmorMeta meta = (LeatherArmorMeta) ITEM_STACK.getItemMeta();
			meta.setColor(color);
			ITEM_STACK.setItemMeta(meta);
			return this;
		} else {
			throw new IllegalArgumentException("withColor is only applicable for leather armor!");
		}
	}

	public ItemStackBuilder addPartialData(ConfigurationSection section){
		return getItemStack(ITEM_STACK, section, null);
	}

	public ItemStackBuilder addPartialData(ConfigurationSection section, Player player){
		return getItemStack(ITEM_STACK, section, player);
	}

	public <P, C> ItemStackBuilder addNamespacedKey(NamespacedKey key, PersistentDataType<P, C> type, C value) {
		ItemMeta meta = ITEM_STACK.getItemMeta();
		if (meta != null) {
			PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
			dataContainer.set(key, type, value);
			ITEM_STACK.setItemMeta(meta);
		}
		return this;
	}

	public ItemStackBuilder setUnbreakable(boolean unbreakable) {
		ItemMeta meta = ITEM_STACK.getItemMeta();
		if (meta != null) {
			meta.setUnbreakable(unbreakable);
			ITEM_STACK.setItemMeta(meta);
		}
		return this;
	}

	public ItemStack build() {
		return ITEM_STACK;
	}
}

