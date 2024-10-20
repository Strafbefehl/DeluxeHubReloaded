package fun.lewisdev.deluxehub.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fun.lewisdev.deluxehub.Permissions;
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
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String localPluginVersion;
	private final Gson gson = new Gson();
    private static final Permission UPDATE_PERM = new Permission(Permissions.UPDATE_NOTIFICATION.getPermission(), PermissionDefault.FALSE);
    private static final long CHECK_INTERVAL = 12_000;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.localPluginVersion = "v"+plugin.getDescription().getVersion();
    }

    public void checkForUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String raw;
					try {
                        final HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.github.com/repos/Strafbefehl/DeluxeHubReloaded/releases").openConnection();
                        connection.setRequestMethod("GET");
						raw = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
                    } catch (IOException e) {
                        cancel();
                        return;
                    }
					JsonArray ghData = gson.fromJson(raw, JsonArray.class);
					JsonObject latest = ghData.get(0).getAsJsonObject();

					final String version = latest.get("tag_name").getAsString();
					final String URL = latest.get("html_url").getAsString();

					if (localPluginVersion.equals(version)) return;

                    plugin.getLogger().warning("An update for DeluxeHubReloaded (%VERSION%) is available at:".replace("%VERSION%", version));
                    plugin.getLogger().warning(URL);

                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler(priority = EventPriority.MONITOR)
                        public void onPlayerJoin(final PlayerJoinEvent event) {
                            final Player player = event.getPlayer();
                            if (!player.hasPermission(UPDATE_PERM)) return;

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage(TextUtil.color("&7An update (%VERSION%) for DeluxeHubReloaded is available at:".replace("%VERSION%", version)));
                                    player.sendMessage(TextUtil.color(URL));
                                }
                            }.runTaskLater(plugin, 60L); // 60 ticks = 3 seconds
                        }
                    }, plugin));

                    cancel();
                });
            }
        }.runTaskTimer(plugin, 0, CHECK_INTERVAL);
    }
}
