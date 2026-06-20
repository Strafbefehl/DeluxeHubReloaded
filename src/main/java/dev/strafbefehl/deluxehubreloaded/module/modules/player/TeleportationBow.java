package dev.strafbefehl.deluxehubreloaded.module.modules.player;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.cooldown.CooldownType;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.utility.ItemStackBuilder;
import dev.strafbefehl.deluxehubreloaded.utility.NamespacedKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class TeleportationBow extends Module {
    private short _slot;
	private short _arrow_slot;
	private int _max_range = 0;
    private short _cooldown;
    private EntityType _entityType;
    private ItemStack _bow;
	private ItemStack _arrow;
	private final Map<UUID, UUID> _entityShot = new HashMap<>();

    public TeleportationBow(DeluxeHubPlugin plugin) {
        super(plugin, ModuleType.TELEPORTATION_BOW);
    }

    @Override
    public void onEnable() {
        ConfigurationSection config = getPlugin().getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getConfigurationSection("teleportation_bow");
        _slot = (short) Objects.requireNonNull(config).getInt("slot");
        _arrow_slot = (short) config.getInt("arrow_slot");;
		_max_range = config.getInt("max_range");
		_cooldown = (short) config.getInt("cooldown");
        try {
            _entityType = EntityType.valueOf(config.getString("entity"));
            if (!Projectile.class.isAssignableFrom(Objects.requireNonNull(_entityType.getEntityClass()))) {
                getPlugin().getLogger().warning("Invalid entity type for teleportation bow. Defaulting to ARROW.");
                throw new IllegalArgumentException("Invalid entity type for teleportation bow.");
            }
			if (Set.of(EntityType.BREEZE_WIND_CHARGE, EntityType.DRAGON_FIREBALL, EntityType.FIREBALL,
					EntityType.SMALL_FIREBALL, EntityType.WITHER_SKULL, EntityType.FISHING_BOBBER)
					.contains(_entityType)) {
				throw new IllegalArgumentException("[BREEZE_WIND_CHARGE, DRAGON_FIREBALL, FIREBALL, SMALL_FIREBALL, WITHER_SKULL, FISHING_BOBBER] are not allowed (for now) for teleportation bow. Defaulting to ARROW.");
			}
        }catch(IllegalArgumentException e) {
            _entityType = EntityType.ARROW;
            getPlugin().getLogger().warning("Invalid entity type for teleportation bow. Defaulting to ARROW.");
        }
        ConfigurationSection bowSection = config.getConfigurationSection("item");
		ConfigurationSection arrowSection = config.getConfigurationSection("arrow");
        ItemStack bowItem = new ItemStack(Material.BOW, 1);
		ItemStack arrowItem = new ItemStack(Material.ARROW, 1);
        if(bowSection == null){
            bowSection = config.createSection("item");
        }
        _bow = ItemStackBuilder.
                getItemStack(bowItem,
                        bowSection,
                        null)
				.setUnbreakable(true)
				.withEnchantment(Enchantment.INFINITY)
                .addNamespacedKey(NamespacedKeys.Keys.TELEPORTATION_BOW_ITEM.get(), PersistentDataType.BOOLEAN, true)
                .build();
		if(arrowSection == null){
			arrowSection = config.createSection("arrow");
		}
		_arrow = ItemStackBuilder.
				getItemStack(arrowItem,
						arrowSection,
						null)
				.addNamespacedKey(NamespacedKeys.Keys.TELEPORTATION_BOW_ITEM.get(), PersistentDataType.BOOLEAN, true)
				.build();
        if(config.getBoolean("disable_inventory_movement")){
            getPlugin().getServer().getPluginManager().registerEvents(
				new Listener() {
					@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
					public void onItemMove(InventoryClickEvent event) {
						if(!(event.getWhoClicked() instanceof Player)) return;
						if(!List.of(_slot, _arrow_slot).contains((short) event.getSlot())) return;
						Inventory inv = event.getClickedInventory();
						if(!(inv instanceof PlayerInventory)) return;
						ItemStack item = event.getCurrentItem();
						if(item == null) return;
						if(!item.hasItemMeta()) return;
						if(item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(NamespacedKeys.Keys.TELEPORTATION_BOW_ITEM.get())){
							event.setCancelled(true);
						}
					}
				},
			getPlugin());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent ev){
        if(!(ev.getEntity() instanceof Player)) return;
        Player player = (Player) ev.getEntity();
        if(ev.getBow() == null || !ev.getBow().isSimilar(_bow)) return;
        if(!tryCooldown(player.getUniqueId(), CooldownType.TELEPORTATION_BOW, _cooldown)){
            Messages.TELEPORTATION_BOW_IN_COOLDOWN.send(player, "%time%", getCooldown(player.getUniqueId(), CooldownType.TELEPORTATION_BOW));
            ev.setCancelled(true);
            return;
        }
        Projectile launchedProjectile = (Projectile) ev.getProjectile();
        if(launchedProjectile.getType() != _entityType){
            launchedProjectile.remove();
			if(_entityType.getEntityClass() == null) return;
			ev.setCancelled(true);
			@SuppressWarnings("unchecked")
			Class<? extends Projectile> projectileClass = (Class<? extends Projectile>) _entityType.getEntityClass();
			launchedProjectile = player.launchProjectile(projectileClass, ev.getProjectile().getVelocity());
		}
		if(launchedProjectile instanceof AbstractArrow) ((AbstractArrow) launchedProjectile).setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		_entityShot.remove(player.getUniqueId());
		_entityShot.put(player.getUniqueId(), launchedProjectile.getUniqueId());
		giveArrow(player);
    }

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent ev){
		if(!_entityShot.containsValue(ev.getEntity().getUniqueId())) return;
		if(ev.getEntity().getShooter() instanceof Player){
			Player player = (Player) ev.getEntity().getShooter();
			Location hitLocation = ev.getEntity().getLocation();
            Location playerLocation = player.getLocation();
            double distance = playerLocation.distance(hitLocation);
            if (distance > _max_range) {
                org.bukkit.util.Vector direction = hitLocation.toVector().subtract(playerLocation.toVector()).normalize();
                hitLocation = playerLocation.add(direction.multiply(_max_range));
				Messages.TELEPORTATION_BOW_MAX_DISTANCE_REACHED.send(player);
            }
			hitLocation.setDirection(playerLocation.getDirection());
            player.teleport(hitLocation);
			ev.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDeath(org.bukkit.event.entity.PlayerDeathEvent ev){
		_entityShot.remove(ev.getEntity().getUniqueId());
	}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        giveBow(player);
    }

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent ev){
		giveBow(ev.getPlayer());
	}

    @Override
    public void onDisable() {}

    public void giveBow(Player player){
        Inventory inv = player.getInventory();
        inv.setItem(_slot, _bow);
		giveArrow(player);
    }

	private void giveArrow(Player player){
		PlayerInventory inv = player.getInventory();
		inv.setItem(_arrow_slot, _arrow);
	}
}
