package dev.strafbefehl.deluxehubreloaded.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.strafbefehl.deluxehubreloaded.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public class UpdateChecker {

	private static final Permission UPDATE_PERM = new Permission(Permissions.UPDATE_NOTIFICATION.getPermission(), PermissionDefault.FALSE);
	private static final long CHECK_INTERVAL = 12_000;
	private final JavaPlugin plugin;
	private final String localPluginVersion;
	private final Gson gson = new Gson();

	public UpdateChecker(JavaPlugin plugin) {
		this.plugin = plugin;
		this.localPluginVersion = "v" + plugin.getPluginMeta().getVersion();
	}

	private int compareVersions(String v1, String v2) {
		String s1 = v1.replaceFirst("^v", "");
		String s2 = v2.replaceFirst("^v", "");

		String[] split1 = s1.split("-", 2);
		String[] split2 = s2.split("-", 2);
		boolean preRelease1 = split1.length > 1;
		boolean preRelease2 = split2.length > 1;

		String[] parts1 = split1[0].split("\\.");
		String[] parts2 = split2[0].split("\\.");
		for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
			int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
			int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
			if (p1 != p2) return Integer.compare(p1, p2);
		}

		// Parties numériques égales : pre-release < release (semver)
		if (preRelease1 && !preRelease2) return -1;
		if (!preRelease1 && preRelease2) return 1;
		return 0;
	}

	private static Component legacy(String text) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
	}

	public void checkForUpdate() {
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					String raw;
					try {
						final HttpsURLConnection connection = (HttpsURLConnection) URI.create("https://api.github.com/repos/Strafbefehl/DeluxeHubReloaded/releases").toURL().openConnection();
						connection.setRequestMethod("GET");
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
							raw = reader.readLine();
						}
					} catch (IOException e) {
						cancel();
						return;
					}
					JsonArray ghData = gson.fromJson(raw, JsonArray.class);
					JsonObject latest = ghData.get(0).getAsJsonObject();

					final String version = latest.get("tag_name").getAsString();
					final String url = latest.get("html_url").getAsString();

					if (compareVersions(localPluginVersion, version) >= 0) return;

					if (localPluginVersion.contains("-dev")) {
						plugin.getLogger().warning("This is DEV version of DeluxeHubReloaded plugin.");
						plugin.getLogger().warning("You can download stable version at:");
						plugin.getLogger().warning(url);
					} else {
						plugin.getLogger().warning("An update for DeluxeHubReloaded (%VERSION%) is available at:".replace("%VERSION%", version));
						plugin.getLogger().warning(url);
					}

					Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().registerEvents(new Listener() {
						@EventHandler(priority = EventPriority.MONITOR)
						public void onPlayerJoin(final PlayerJoinEvent event) {
							final Player player = event.getPlayer();
							if (!player.hasPermission(UPDATE_PERM)) return;

							new BukkitRunnable() {
								@Override
								public void run() {
									if (localPluginVersion.contains("-dev")) {
										player.sendMessage(legacy("&7This is DEV version of DeluxeHubReloaded plugin."));
										player.sendMessage(legacy("&a> You can download stable version here! <")
												.clickEvent(ClickEvent.openUrl(url)));
									} else {
										player.sendMessage(legacy("&7An update (" + version + ") for DeluxeHubReloaded is available."));
										player.sendMessage(Component.text()
												.append(legacy("&a[Download]").clickEvent(ClickEvent.openUrl(url)))
												.append(legacy(" &7| "))
												.append(legacy("&6[Changelog]").clickEvent(ClickEvent.openUrl("https://discord.gg/uQkg8ZeHzK")))
												.build());
									}
								}
							}.runTaskLater(plugin, 60L);
						}
					}, plugin));

					cancel();
				});
			}
		}.runTaskTimer(plugin, 0, CHECK_INTERVAL);
	}
}
