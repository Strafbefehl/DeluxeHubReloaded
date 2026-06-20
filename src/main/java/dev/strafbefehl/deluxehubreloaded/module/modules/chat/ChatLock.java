package dev.strafbefehl.deluxehubreloaded.module.modules.chat;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.Permissions;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.module.Module;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class ChatLock extends Module {

	private boolean isChatLocked;

	public ChatLock(DeluxeHubPlugin plugin) {
		super(plugin, ModuleType.CHAT_LOCK);
	}

	@Override
	public void onEnable() {
		isChatLocked = getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().getBoolean("chat_locked");
	}

	@Override
	public void onDisable() {
		getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().set("chat_locked", isChatLocked);
		getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
	}

	@EventHandler
	public void onPlayerChat(AsyncChatEvent event) {
		Player player = event.getPlayer();

		if (!isChatLocked || player.hasPermission(Permissions.LOCK_CHAT_BYPASS.getPermission())) return;

		event.setCancelled(true);
		Messages.CHAT_LOCKED.send(player);
	}

	public boolean isChatLocked() {
		return isChatLocked;
	}

	public void setChatLocked(boolean chatLocked) {
		isChatLocked = chatLocked;
		getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().set("chat_locked", isChatLocked);
		getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
	}
}
