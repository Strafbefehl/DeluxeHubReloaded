package dev.strafbefehl.deluxehubreloaded.module.modules.chat;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.Permissions;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import org.bukkit.Bukkit;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;

public class AntiSwear extends Module {

	private List<String> blockedWords;

	public AntiSwear(DeluxeHubPlugin plugin) {
		super(plugin, ModuleType.ANTI_SWEAR);
	}

	@Override
	public void onEnable() {
		blockedWords = getConfig(ConfigType.SETTINGS).getStringList("anti_swear.blocked_words");
	}

	@Override
	public void onDisable() {
	}

	@EventHandler
	public void onPlayerChat(AsyncChatEvent event) {

		Player player = event.getPlayer();
		if (player.hasPermission(Permissions.ANTI_SWEAR_BYPASS.getPermission())) return;

		String message = PlainTextComponentSerializer.plainText().serialize(event.message());

		for (String word : blockedWords) {
			if (message.toLowerCase().contains(word.toLowerCase())) {

				event.setCancelled(true);
				Messages.ANTI_SWEAR_WORD_BLOCKED.send(player);

				Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission(Permissions.ANTI_SWEAR_NOTIFY.getPermission())).forEach(p -> {
					Messages.ANTI_SWEAR_ADMIN_NOTIFY.send(p, "%player%", player.getName(), "%word%", message);
				});

				return;
			}
		}
	}
}
