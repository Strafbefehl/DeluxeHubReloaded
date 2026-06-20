package dev.strafbefehl.deluxehubreloaded.module.modules.world;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.Permissions;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.cooldown.CooldownType;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.module.modules.hologram.Hologram;
import dev.strafbefehl.deluxehubreloaded.module.modules.player.PvPMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.List;

public class WorldProtect extends Module {
    private FileConfiguration config;
    private final List<Material> interactables = Arrays.asList(
            Material.ANVIL,
            Material.ARMOR_STAND,
            Material.BARREL,
            Material.BEACON,
            Material.BREWING_STAND,
            Material.CAULDRON,
            Material.CARTOGRAPHY_TABLE,
            Material.CHISELED_BOOKSHELF,
            Material.COMMAND_BLOCK,
            Material.COMPARATOR,
            Material.COMPOSTER,
            Material.CRAFTING_TABLE,
            Material.CRAFTER,
            Material.DAYLIGHT_DETECTOR,
            Material.DECORATED_POT,
            Material.DISPENSER,
            Material.DROPPER,
            Material.ENCHANTING_TABLE,
            Material.FLETCHING_TABLE,
            Material.FLOWER_POT,
            Material.FURNACE,
            Material.GRINDSTONE,
            Material.HOPPER,
            Material.HOPPER_MINECART,
            Material.ITEM_FRAME,
            Material.JUKEBOX,
            Material.LEAD,
            Material.LEVER,
            Material.LOOM,
            Material.MINECART,
            Material.NOTE_BLOCK,
            Material.PAINTING,
            Material.REDSTONE_WIRE,
            Material.REPEATER,
            Material.RESPAWN_ANCHOR,
            Material.SMITHING_TABLE,
            Material.SMOKER,
            Material.SPAWNER,
            Material.STONECUTTER,
            Material.STRUCTURE_BLOCK,
            Material.TRIAL_SPAWNER,
            Material.BLAST_FURNACE,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL
    );

    private boolean hungerLoss;
    private boolean fallDamage;
    private boolean weatherChange;
    private boolean deathMessage;
    private boolean fireSpread;
    private boolean leafDecay;
    private boolean mobSpawning;
    private boolean blockBurn;
    private boolean voidDeath;
    private boolean itemDrop;
    private boolean itemPickup;
    private boolean blockBreak;
    private boolean blockPlace;
    private boolean blockInteract;
    private boolean playerPvP;
    private boolean playerDrowning;
    private boolean fireDamage;
    private boolean hidePotionIcons;

    public WorldProtect(DeluxeHubPlugin plugin) {
        super(plugin, ModuleType.WORLD_PROTECT);
    }

    @Override
    public void onEnable() {
        config = getConfig(ConfigType.SETTINGS);
        hungerLoss = config.getBoolean("world_settings.disable_hunger_loss");
        fallDamage = config.getBoolean("world_settings.disable_fall_damage");
        playerPvP = config.getBoolean("world_settings.disable_player_pvp");
        voidDeath = config.getBoolean("world_settings.disable_void_death");
        weatherChange = config.getBoolean("world_settings.disable_weather_change");
        deathMessage = config.getBoolean("world_settings.disable_death_message");
        mobSpawning = config.getBoolean("world_settings.disable_mob_spawning");
        itemDrop = config.getBoolean("world_settings.disable_item_drop");
        itemPickup = config.getBoolean("world_settings.disable_item_pickup");
        blockBreak = config.getBoolean("world_settings.disable_block_break");
        blockPlace = config.getBoolean("world_settings.disable_block_place");
        blockInteract = config.getBoolean("world_settings.disable_block_interact");
        blockBurn = config.getBoolean("world_settings.disable_block_burn");
        fireSpread = config.getBoolean("world_settings.disable_block_fire_spread");
        leafDecay = config.getBoolean("world_settings.disable_block_leaf_decay");
        playerDrowning = config.getBoolean("world_settings.disable_drowning");
        fireDamage = config.getBoolean("world_settings.disable_fire_damage");
        hidePotionIcons = config.getBoolean("world_settings.hide_potion_icons");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inDisabledWorld(player.getLocation())) continue;
            for (PotionEffect effect : player.getActivePotionEffects()) {
                boolean needsUpdate = hidePotionIcons
                        ? (effect.hasIcon() || effect.hasParticles())
                        : (!effect.hasIcon() && !effect.hasParticles());
                if (!needsUpdate) continue;
                player.addPotionEffect(new PotionEffect(
                        effect.getType(), effect.getDuration(), effect.getAmplifier(),
                        effect.isAmbient(), !hidePotionIcons, !hidePotionIcons
                ));
            }
        }
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!hidePotionIcons) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (inDisabledWorld(player.getLocation())) return;
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null || (!newEffect.hasIcon() && !newEffect.hasParticles())) return;
        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(
                newEffect.getType(),
                newEffect.getDuration(),
                newEffect.getAmplifier(),
                newEffect.isAmbient(),
                false,
                false
        ));
    }

    // Prevent sign editing
    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) return;
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
            Messages.EVENT_BLOCK_INTERACT.send(player);
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        for (Hologram entry : getPlugin().getHologramManager().getHolograms()) {
            for (ArmorStand stand : entry.getStands()) {
                if (stand.equals(event.getRightClicked())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockBreak || event.isCancelled()) return;

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) return;
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);

        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
            Messages.EVENT_BLOCK_BREAK.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!blockPlace || event.isCancelled()) return;

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) return;
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.AIR) return;
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String hotbarItem = meta.getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "hotbarItem"), PersistentDataType.STRING);
            if (hotbarItem != null) {
                event.setCancelled(true);
                return;
            }
        }

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_PLACE.getPermission())) return;
        }


        event.setCancelled(true);

        if (tryCooldown(event.getPlayer().getUniqueId(), CooldownType.BLOCK_PLACE, 3)) {
            Messages.EVENT_BLOCK_PLACE.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Player player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material type = block.getType();

            // Check generic interactables first
            if (interactables.contains(type)) {
                event.setCancelled(true);
                if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                    Messages.EVENT_BLOCK_INTERACT.send(player);
                }
                return;
            }

            // Check type patterns
            if (type.name().contains("_DOOR") || type.name().contains("_TRAPDOOR") || type.name().contains("_BUTTON") || type.name().contains("_SIGN") || type.name().contains("_GATE") || type.name().contains("CHEST") || type.name().contains("_FENCE_GATE") || type.name().contains("POTTED_") || type.name().contains("_BED") || type.name().contains("_BOAT")) {

                event.setCancelled(true);
                if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                    Messages.EVENT_BLOCK_INTERACT.send(player);
                }
                return;
            }
        } else if (event.getAction() == Action.PHYSICAL && block.getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!blockBurn) return;
        if (inDisabledWorld(event.getBlock().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFireSpread(BlockIgniteEvent event) {
        if (!fireSpread) return;
        if (inDisabledWorld(event.getBlock().getLocation())) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!hungerLoss) return;

        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (inDisabledWorld(player.getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropEvent(PlayerDropItemEvent event) {
        if (!itemDrop) return;

        Player player = event.getPlayer();

        if (inDisabledWorld(player.getLocation())) return;

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_ITEM_DROP.getPermission())) return;
        }


        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);

        if (tryCooldown(player.getUniqueId(), CooldownType.ITEM_DROP, 3)) {
            Messages.EVENT_ITEM_DROP.send(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupEvent(EntityPickupItemEvent event) {
        if (!itemPickup) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (inDisabledWorld(player.getLocation())) return;
            if (config.getBoolean("legacySystems.permissionsEnabled")) {
                if (player.hasPermission(Permissions.EVENT_ITEM_PICKUP.getPermission())) return;
            }
            if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.ITEM_PICKUP, 3)) {
                Messages.EVENT_ITEM_PICKUP.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!leafDecay) return;
        if (inDisabledWorld(event.getBlock().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!mobSpawning) return;
        if (inDisabledWorld(event.getEntity().getLocation())) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!weatherChange || inDisabledWorld(event.getWorld())) return;
        event.setCancelled(event.toWeatherState());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathMessage || inDisabledWorld(event.getEntity().getLocation())) return;
        if (BuildMode.getInstance().isPresent(event.getEntity().getUniqueId())) event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.deathMessage(null);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (inDisabledWorld(player.getLocation())) return;
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        PvPMode pvpMode = (PvPMode) getPlugin().getModuleManager().getModule(ModuleType.PVP_MODE);
        EntityDamageEvent.DamageCause cause = event.getCause();
        switch (cause) {
            case FALL:
                if (fallDamage) event.setCancelled(true);
                break;
            case DROWNING:
                if (playerDrowning) event.setCancelled(true);
                break;
            case FIRE:
            case FIRE_TICK:
                if (!playerPvP) return;
                if (config.getBoolean("pvp_mode.enabled")) {
                    if (pvpMode.isPlayerInPvPMode(player.getUniqueId())) return;
                }
            case LAVA:
                if (fireDamage) event.setCancelled(true);
                break;
            case VOID: {
                if (voidDeath) {
                    player.setFallDistance(0.0F);
                    Location location = ((LobbySpawn) getPlugin().getModuleManager().getModule(ModuleType.LOBBY)).getLocation();
                    if (location == null) return;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> player.teleport(location), 3L);
                    event.setCancelled(true);
                }
                break;
            }
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!playerPvP) return;

        if (!(event.getEntity() instanceof Player victim)) return;
        if (inDisabledWorld(victim.getLocation())) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) return;

        if (config.getBoolean("pvp_mode.enabled")) {
            PvPMode pvpMode = (PvPMode) getPlugin().getModuleManager().getModule(ModuleType.PVP_MODE);
            if (pvpMode.isPlayerInPvPMode(attacker.getUniqueId())) {
                if (!pvpMode.isPlayerInPvPMode(victim.getUniqueId())) {
                    if (tryCooldown(attacker.getUniqueId(), CooldownType.VICTIM_NOT_IN_PVP_MODE, 3)) {
                        Messages.PVP_MODE_VICTIM_NOT_IN_PVP_MODE.send(attacker, "%victim%", victim.getName());
                    }
                    event.setCancelled(true);
                }
                return;
            }
        }

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (attacker.hasPermission(Permissions.EVENT_PLAYER_PVP.getPermission())) return;
        }

        event.setCancelled(true);
        if (tryCooldown(attacker.getUniqueId(), CooldownType.PLAYER_PVP, 3)) {
            Messages.EVENT_PLAYER_PVP.send(attacker);
        }
    }

    // Prevent destroying of item frame/paintings (including via projectiles)
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDestroy(HangingBreakByEntityEvent event) {
        if (!blockBreak || inDisabledWorld(event.getEntity().getLocation())) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Painting) && !(entity instanceof ItemFrame)) return;

        Entity remover = event.getRemover();
        Player player = null;
        if (remover instanceof Player p) {
            player = p;
        } else if (remover instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            player = p;
        }

        if (player == null) return;

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
            Messages.EVENT_BLOCK_BREAK.send(player);
        }
    }

    // Prevent items being rotated in item frame
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getRightClicked().getLocation())) return;
        Entity entity = event.getRightClicked();
        Entity player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (entity instanceof ItemFrame) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    // Prevent items being taken from item frames (including via projectiles)
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getEntity().getLocation())) return;
        if (!(event.getEntity() instanceof ItemFrame)) return;

        Entity damager = event.getDamager();
        Player player = null;
        if (damager instanceof Player p) {
            player = p;
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            player = p;
        }

        if (player == null) return;

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;
        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
            Messages.EVENT_BLOCK_INTERACT.send(player);
        }
    }

    // Prevent books being taken from lecterns
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerTakeLecternBookEvent event) {
        if (!blockInteract || inDisabledWorld(event.getLectern().getLocation())) return;
        Entity player = event.getPlayer();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
            Messages.EVENT_BLOCK_INTERACT.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBoatInteract(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Player player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        // Check for all boat types
        if (event.getItem() != null &&
                (event.getItem().getType().name().contains("_BOAT"))) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler
    public void onPlayerBoatInteraction(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (entity instanceof Boat) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleBreak(VehicleDestroyEvent event) {
        if (!blockBreak || inDisabledWorld(event.getVehicle().getLocation())) return;
        if (!(event.getVehicle() instanceof Boat) && !(event.getVehicle() instanceof Minecart)) return;

        Entity attacker = event.getAttacker();
        Player player = null;
        if (attacker instanceof Player p) {
            player = p;
        } else if (attacker instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            player = p;
        }

        if (player == null) return;

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
            Messages.EVENT_BLOCK_BREAK.send(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (event.getPlayer().hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(event.getPlayer().getUniqueId())) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && (clickedBlock.getType() == Material.CHISELED_BOOKSHELF || clickedBlock.getType() == Material.DECORATED_POT)) {
                event.setCancelled(true);

                if (tryCooldown(event.getPlayer().getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                    Messages.EVENT_BLOCK_INTERACT.send(event.getPlayer());
                }
            }
        }
    }

    // Prevent decorated pots being shattered by projectiles
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHitDecoratedPot(ProjectileHitEvent event) {
        if (!blockBreak || event.getHitBlock() == null) return;
        if (inDisabledWorld(event.getHitBlock().getLocation())) return;
        if (event.getHitBlock().getType() != Material.DECORATED_POT) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onMinecartInteraction(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        if (entity instanceof Minecart ||
                entity instanceof StorageMinecart ||
                entity instanceof HopperMinecart ||
                entity instanceof CommandMinecart ||
                entity instanceof ExplosiveMinecart) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMinecartPlacement(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) return;

        Player player = event.getPlayer();
        if (config.getBoolean("legacySystems.permissionsEnabled")) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) return;
        }
        if (BuildMode.getInstance().isPresent(player.getUniqueId())) return;

        // Check for all minecart types
        if (event.getItem() != null &&
                (event.getItem().getType() == Material.MINECART ||
                        event.getItem().getType() == Material.CHEST_MINECART ||
                        event.getItem().getType() == Material.FURNACE_MINECART ||
                        event.getItem().getType() == Material.HOPPER_MINECART ||
                        event.getItem().getType() == Material.TNT_MINECART ||
                        event.getItem().getType() == Material.COMMAND_BLOCK_MINECART)) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }
}