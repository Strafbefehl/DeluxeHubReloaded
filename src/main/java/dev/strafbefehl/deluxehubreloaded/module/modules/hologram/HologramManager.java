package dev.strafbefehl.deluxehubreloaded.module.modules.hologram;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class HologramManager extends Module {

	private Set<Hologram> holograms;
	private NamespacedKey hologramKey;

	public HologramManager(DeluxeHubPlugin plugin) {
		super(plugin, ModuleType.HOLOGRAMS);
	}

	@Override
	public void onEnable() {
		hologramKey = new NamespacedKey(getPlugin(), "hologram_id");
		holograms = new HashSet<>();
		loadHolograms();
	}

	@Override
	public void onDisable() {
		saveHolograms();
	}

	public void loadHolograms() {
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {

			FileConfiguration config = getConfig(ConfigType.DATA);

			if (config.contains("holograms")) {
				for (String key : config.getConfigurationSection("holograms").getKeys(false)) {
					List<String> lines = config.getStringList("holograms." + key + ".lines");

					Location loc = (Location) config.get("holograms." + key + ".location");
					if (loc == null) continue;
					cleanupHologramEntities(key, loc);

					Hologram holo = createHologram(key, loc);
					holo.setLines(lines);
					saveHologram(holo);
				}
			}
		}, 40L);
	}

	public void saveHolograms() {
		holograms.forEach(this::serializeHologram);
		getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
		deleteAllHolograms();
	}

	// Persists a single hologram to data.yml immediately.
	public void saveHologram(Hologram holo) {
		serializeHologram(holo);
		getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
	}

	private void serializeHologram(Hologram holo) {
		FileConfiguration config = getConfig(ConfigType.DATA);
		config.set("holograms." + holo.getName() + ".location", holo.getLocation());

		List<String> lines = new ArrayList<>();
		for (ArmorStand stand : holo.getStands()) {
			Component comp = stand.customName();
			lines.add(comp != null ? LegacyComponentSerializer.legacySection().serialize(comp) : "");
		}
		config.set("holograms." + holo.getName() + ".lines", lines);

		List<String> entityUuids = holo.getStands().stream()
				.map(s -> s.getUniqueId().toString())
				.collect(Collectors.toList());
		config.set("holograms." + holo.getName() + ".entities", entityUuids);
	}

	public Set<Hologram> getHolograms() {
		return holograms;
	}

	public boolean hasHologram(String name) {
		return getHolograms().stream().anyMatch(hologram -> hologram.getName().equalsIgnoreCase(name));
	}

	public Hologram getHologram(String name) {
		return getHolograms().stream().filter(hologram -> hologram.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	public Hologram createHologram(String name, Location location) {
		Hologram holo = new Hologram(hologramKey, name, location);
		holograms.add(holo);
		return holo;
	}

	public void deleteHologram(String name) {
		Hologram holo = getHologram(name);
		holo.remove();
		cleanupHologramEntities(name, holo.getLocation());

		holograms.remove(holo);
		getConfig(ConfigType.DATA).set("holograms." + name, null);
		getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
	}

	public void deleteAllHolograms() {
		holograms.forEach(Hologram::remove);
		holograms.clear();
	}

	// Removes all armor stand entities associated with a hologram before (re)creating it.
	// Three layers: UUID (deterministic), PDC scan (orphan recovery), radius (pre-PDC migration).
	private void cleanupHologramEntities(String name, Location loc) {
		loc.getChunk().load();

		FileConfiguration config = getConfig(ConfigType.DATA);
		List<String> uuids = config.getStringList("holograms." + name + ".entities");

		if (uuids.isEmpty()) {
			// No UUID data yet: first run after upgrade — fall back to radius search.
			deleteNearbyHolograms(loc);
			return;
		}

		for (String uuidStr : uuids) {
			try {
				Entity e = Bukkit.getEntity(UUID.fromString(uuidStr));
				if (e != null) e.remove();
			} catch (IllegalArgumentException ignored) {}
		}

		// PDC fallback: catch orphans that survived a crash and were not in the saved UUID list.
		World world = loc.getWorld();
		if (world == null) return;
		world.getNearbyEntities(loc, 2, 50, 2).stream()
				.filter(e -> e instanceof ArmorStand)
				.filter(e -> name.equals(e.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING)))
				.forEach(Entity::remove);
	}

	private void deleteNearbyHolograms(Location location) {
		World world = location.getWorld();
		if (world == null) return;
		world.getNearbyEntities(location, 1, 20, 1).stream()
				.filter(entity -> entity instanceof ArmorStand)
				.forEach(Entity::remove);
	}

}
