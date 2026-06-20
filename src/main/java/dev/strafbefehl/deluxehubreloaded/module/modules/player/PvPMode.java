package dev.strafbefehl.deluxehubreloaded.module.modules.player;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.command.commands.FlyCommand;
import dev.strafbefehl.deluxehubreloaded.module.modules.hotbar.HotbarManager;
import dev.strafbefehl.deluxehubreloaded.utility.ItemStackBuilder;
import dev.strafbefehl.deluxehubreloaded.utility.NamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class PvPMode extends Module {
    private short _slot;
    private short _time_to_toggle;
    private final EnumMap<PvPSwitcherState, ItemStack> _switcher = new EnumMap<>(PvPSwitcherState.class);
    private final EnumMap<PvPItemType, List<ItemStack>> _items = new EnumMap<>(PvPItemType.class);
    private final List<UUID> _players = new ArrayList<>();
    private final Map<UUID, Integer> _tasks = new HashMap<>();
    private boolean playerPvPStatus;

    public enum PvPSwitcherState {
        PVP_ON,
        PVP_OFF
    }

    public enum PvPItemType {
        SWORD(0),
        HELMET(-1),
        CHESTPLATE(-1),
        LEGGINGS(-1),
        BOOTS(-1),
		OTHER(-1);

        private final int _slot;

        PvPItemType(int slot) {
            _slot = slot;
        }

        public int getSlot() {
            return _slot;
        }
    }

    public PvPMode(DeluxeHubPlugin plugin) {
        super(plugin, ModuleType.PVP_MODE);
    }

    @Override
    public void onEnable() {
        ConfigurationSection config = getPlugin().getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getConfigurationSection("pvp_mode");
        playerPvPStatus = getPlugin().getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getBoolean("world_settings.disable_player_pvp");
		_slot = (short) config.getInt("slot");
        _time_to_toggle = (short) config.getInt("time_to_toggle");
        ConfigurationSection switcherSection = config.getConfigurationSection("switcher");
        if(switcherSection == null){
            Messages.PVP_MODE_NO_SWITCHER_SECTION.send(getPlugin().getServer().getConsoleSender());
            return;
        }
        for (PvPSwitcherState state : PvPSwitcherState.values()) {
            ConfigurationSection section = switcherSection.getConfigurationSection(state.name().toLowerCase());
            if(section == null) continue;
			_switcher.put(state,
					ItemStackBuilder.getItemStack(switcherSection)
							.addPartialData(section)
							.addNamespacedKey(NamespacedKeys.Keys.PVP_MODE_SWITCHER.get(), PersistentDataType.BOOLEAN, true)
							.addNamespacedKey(NamespacedKeys.Keys.PVP_MODE_SWITCHER_STATE.get(), PersistentDataType.BOOLEAN, state != PvPSwitcherState.PVP_OFF).build());
        }
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if(itemsSection == null) return;
		for (PvPItemType type : PvPItemType.values()) {
			ConfigurationSection section = itemsSection.getConfigurationSection(type.name().toLowerCase());
			if(section == null){
				if(type == PvPItemType.OTHER){
					@SuppressWarnings("unchecked")
				List<LinkedHashMap<String, ?>> list = (List<LinkedHashMap<String, ?>>) itemsSection.getList("other");
					if (list != null) {
						_items.put(type, new ArrayList<>());
						for (LinkedHashMap<String, ?> map : list) {
							ConfigurationSection otherItemSection = itemsSection.createSection(UUID.randomUUID().toString());
							for (Map.Entry<String, ?> entry : map.entrySet()) {
								otherItemSection.set(entry.getKey(), entry.getValue());
							}
							ItemStack item = ItemStackBuilder.getItemStack(otherItemSection)
									.addNamespacedKey(NamespacedKeys.Keys.PVP_MODE_ITEM.get(), PersistentDataType.BOOLEAN, true).build();
							_items.get(type).add(item);
						}
					}
				}
				continue;
			}
			List<ItemStack> itemList = new ArrayList<>();
			itemList.add(ItemStackBuilder.getItemStack(section)
				.addNamespacedKey(NamespacedKeys.Keys.PVP_MODE_ITEM.get(), PersistentDataType.BOOLEAN, true).build());
			_items.put(type, itemList);
		}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent ev) {
		PlayerInventory inv = ev.getPlayer().getInventory();
		inv.setHelmet(null);
		inv.setChestplate(null);
		inv.setLeggings(null);
		inv.setBoots(null);
		for(ItemStack item : inv.getContents())
			if(item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(NamespacedKeys.Keys.PVP_MODE_ITEM.get(), PersistentDataType.BOOLEAN))
				inv.remove(item);
		giveSwitcher(ev.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChangeItem(PlayerItemHeldEvent ev) {
        Player player = ev.getPlayer();
		UUID pUUID = player.getUniqueId();
        if(_tasks.containsKey(pUUID)){
            BukkitScheduler scheduler = getPlugin().getServer().getScheduler();
			int taskId = _tasks.get(player.getUniqueId());
            scheduler.cancelTask(taskId);
            _tasks.remove(pUUID);
			player.sendActionBar(Component.empty());
            return;
        }
		PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getItem(ev.getNewSlot());
		if(item == null) return;
		if(!item.hasItemMeta()) return;
		PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
		if(container.isEmpty()) return;
		if(container.get(NamespacedKeys.Keys.PVP_MODE_SWITCHER.get(), PersistentDataType.BOOLEAN) == null) return;
		if(container.get(NamespacedKeys.Keys.PVP_MODE_SWITCHER_STATE.get(), PersistentDataType.BOOLEAN) == null) return;
		if(Boolean.FALSE.equals(container.get(NamespacedKeys.Keys.PVP_MODE_SWITCHER_STATE.get(), PersistentDataType.BOOLEAN))){
			int taskId = getPlugin().getServer().getScheduler().runTaskTimer(getPlugin(), new Runnable() {
				int timeLeft = _time_to_toggle;
				@Override
				public void run() {
					if(timeLeft > 0){
						player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
								Messages.PVP_MODE_SWITCH_ON_TIME.toString().replaceAll("&", "§").replace("%time%", ""+timeLeft)));
						timeLeft--;
					}else {
						if (player.isOnline()) {
							_players.add(pUUID);
							inv.clear();
							inv.setItem(_slot, _switcher.get(PvPSwitcherState.PVP_ON));
							for (PvPItemType itemType : PvPItemType.values()) {
								List<ItemStack> is = _items.get(itemType);
								switch (itemType) {
									case SWORD:
										short slot = (short) itemType.getSlot();
										while (inv.getItem(slot) != null) {
											slot = (short) ((slot + 1) % 9);
										}
										inv.setItem(slot, is.get(0));
										inv.setHeldItemSlot(slot);
										continue;
									case HELMET:
										inv.setHelmet(is.get(0));
										break;
									case CHESTPLATE:
										inv.setChestplate(is.get(0));
										break;
									case LEGGINGS:
										inv.setLeggings(is.get(0));
										break;
									case BOOTS:
										inv.setBoots(is.get(0));
										break;
									default:{
										for(ItemStack item : is)
											for(int i = 0; i < 9; i++){
												if(inv.getItem(i) == null){
													inv.setItem(i, item);
													break;
												}
											}
									}
								}
							}
							inv.clear(_slot);
							inv.setItem(8, _switcher.get(PvPSwitcherState.PVP_ON));
							getPlugin().getServer().getScheduler().cancelTask(_tasks.get(pUUID));
							_tasks.remove(pUUID);
							player.setAllowFlight(false);
							player.setFlying(false);
							player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
									Messages.PVP_MODE_LETS_FIGHT.toString().replaceAll("&", "§")));
						}
					}
				}
			}, 0L, 20L).getTaskId();
            _tasks.put(pUUID, taskId);
        }else if(Boolean.TRUE.equals(container.get(NamespacedKeys.Keys.PVP_MODE_SWITCHER_STATE.get(), PersistentDataType.BOOLEAN))){
            int taskId = getPlugin().getServer().getScheduler().runTaskTimer(getPlugin(), new Runnable() {
				int timeLeft = _time_to_toggle;
				@Override
				public void run() {
					if (timeLeft > 0) {
						player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
								Messages.PVP_MODE_SWITCH_OFF_TIME.toString().replaceAll("&", "§").replace("%time%", ""+timeLeft)));
						timeLeft--;
					} else {
						if (player.isOnline()) {
							HotbarManager hotbarManager = (HotbarManager) getPlugin().getModuleManager().getModule(ModuleType.HOTBAR_ITEMS);
							TeleportationBow tpBow = (TeleportationBow) getPlugin().getModuleManager().getModule(ModuleType.TELEPORTATION_BOW);
							_players.remove(pUUID);
							inv.clear();
							hotbarManager.giveItems(player);
							if (tpBow != null) {
								tpBow.giveBow(player);
							}
							inv.setItem(_slot, _switcher.get(PvPSwitcherState.PVP_OFF));
							hotbarManager.changeToJoinSlot(player);
							getPlugin().getServer().getScheduler().cancelTask(_tasks.get(pUUID));
							_tasks.remove(pUUID);
							boolean shouldHaveFlight = getPlugin().getModuleManager().isEnabled(ModuleType.DOUBLE_JUMP)
									|| Boolean.TRUE.equals(FlyCommand.allowPlayerFly.get(pUUID));
							player.setAllowFlight(shouldHaveFlight);
							player.sendActionBar(Component.empty());
						}
					}
				}
			}, 0L, 20L).getTaskId();
            _tasks.put(pUUID, taskId);
        }
    }

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemMove(InventoryClickEvent ev) {
		if (!(ev.getWhoClicked() instanceof Player)) return;
		ItemStack movedItem = ev.getCurrentItem();
		if(movedItem == null) return;
		if(!movedItem.hasItemMeta()) return;
		PersistentDataContainer container = movedItem.getItemMeta().getPersistentDataContainer();
		if(container.isEmpty()) return;
		if(Boolean.TRUE.equals(container.get(NamespacedKeys.Keys.PVP_MODE_ITEM.get(), PersistentDataType.BOOLEAN))) ev.setCancelled(true);
		if(Boolean.TRUE.equals(container.get(NamespacedKeys.Keys.PVP_MODE_SWITCHER.get(), PersistentDataType.BOOLEAN))) ev.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityAttack(EntityDamageByEntityEvent ev) {
		if (!(ev.getDamager() instanceof Player)) return;
		if (!(ev.getEntity() instanceof Player)) return;
        if (!playerPvPStatus) return;
		Player attacker = (Player) ev.getDamager();
		Player target = (Player) ev.getEntity();
		if (!_players.contains(attacker.getUniqueId()) || !_players.contains(target.getUniqueId())) {
			ev.setCancelled(true);
		}
		ItemStack item = attacker.getInventory().getItemInMainHand();
		if(item.getType() == Material.AIR) return;
		if(!item.hasItemMeta()) return;
		PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
		if(container.isEmpty()) return;
		if(Boolean.TRUE.equals(container.get(NamespacedKeys.Keys.PVP_MODE_SWITCHER.get(), PersistentDataType.BOOLEAN))) ev.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
            if (!playerPvPStatus) return;
			EntityDamageEvent.DamageCause cause = event.getCause();
			Player damageTarget = (Player) event.getEntity();
			if(!_players.contains(damageTarget.getUniqueId())) return;
			switch(cause){
				case FIRE:
				case FIRE_TICK:
				case ENTITY_ATTACK:
				case ENTITY_EXPLOSION:
				case PROJECTILE:
				case ENTITY_SWEEP_ATTACK:
					return;
				default:
					event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if(!_players.contains(event.getEntity().getUniqueId())) return;
		final Player p = event.getEntity();
		p.getInventory().clear();
		removePlayer(p.getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRespawn(PlayerRespawnEvent event) {
		giveSwitcher(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player p = event.getPlayer();
		p.getInventory().clear();
		removePlayer(p.getUniqueId());
	}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeave(PlayerQuitEvent ev) {
		final UUID pUUID = ev.getPlayer().getUniqueId();
		ev.getPlayer().getInventory().clear();
		removePlayer(pUUID);
    }

	public final boolean isPlayerInPvPMode(UUID uuid){
		return _players.contains(uuid);
	}

	private void removePlayer(final UUID pUUID){
		BukkitScheduler scheduler = getPlugin().getServer().getScheduler();
		_players.remove(pUUID);
		_tasks.keySet().stream().filter(pUUID::equals).forEach(uuid->scheduler.cancelTask(_tasks.get(uuid)));
		_tasks.remove(pUUID);
	}

	public void giveSwitcher(Player player){
		Inventory inv = player.getInventory();
		boolean found = false;
		int slot = -1;
		for (int i = 0; i < 9; i++) {
			slot = (_slot + i) % 9;
			ItemStack item = inv.getItem(slot);
			if (item == null ||
				(item.hasItemMeta() && (!item.getItemMeta().getPersistentDataContainer().isEmpty() && Boolean.FALSE.equals(item.getItemMeta().getPersistentDataContainer().get(NamespacedKeys.Keys.PVP_MODE_SWITCHER_STATE.get(), PersistentDataType.BOOLEAN))))) {
				found = true;
				break;
			}
		}
		if (!found) {
			Messages.PVP_MODE_NO_EMPTY_SLOT_FOUND.send(player);
			getPlugin().getLogger().warning("No empty slot found for PvPMode switcher for player " + player.getName());
			return;
		}
		inv.setItem(slot, _switcher.get(PvPSwitcherState.PVP_OFF));
	}

    @Override
    public void onDisable() {
    }
}
