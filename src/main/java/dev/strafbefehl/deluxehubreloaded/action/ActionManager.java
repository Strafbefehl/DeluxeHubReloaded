package dev.strafbefehl.deluxehubreloaded.action;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.action.actions.*;
import dev.strafbefehl.deluxehubreloaded.utility.PlaceholderUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionManager {

	private final DeluxeHubPlugin plugin;
	private final Map<String, Action> actions;

	public ActionManager(DeluxeHubPlugin plugin) {
		this.plugin = plugin;
		actions = new HashMap<>();
		load();
	}

	private void load() {
		registerAction(
				new MessageAction(),
				new BroadcastMessageAction(),
				new CommandAction(),
				new ConsoleCommandAction(),
				new SoundAction(),
				new PotionEffectAction(),
				new GamemodeAction(),
				new ProxyAction(),
				new CloseInventoryAction(),
				new ActionbarAction(),
				new TitleAction(),
				new MenuAction(),
				new PauseAction()
		);
	}

	public void registerAction(Action... actions) {
		Arrays.asList(actions).forEach(action -> this.actions.put(action.getIdentifier(), action));
	}

	public void executeActions(Player player, List<String> items) {
		for (String item : items) { // Changed the Lambda to a loop to be able to break out on action
			String actionName = StringUtils.substringBetween(item, "[", "]");
			Action action = actionName == null ? null : actions.get(actionName.toUpperCase());

			if ("pause".equalsIgnoreCase(actionName)) {
				String value = item.contains(" ") ? item.split(" ", 2)[1] : "0";
				int pause = Integer.parseInt(value);

				// Get remaining actions after this one
				int currentIndex = items.indexOf(item);
				List<String> remaining = items.subList(currentIndex + 1, items.size());

				// Join them into a single string, separated by `||`
				String remainingJoined = String.join("||", remaining);

				// Execute pause and send remaining actions to PauseAction
				String[] data = { String.valueOf(pause), remainingJoined };
				String joined = String.join(";", data);
				action.execute(plugin, player, joined);

				break;
			} else if (action != null) {
				item = item.contains(" ") ? item.split(" ", 2)[1] : "";
				item = PlaceholderUtil.setPlaceholders(item, player);
				action.execute(plugin, player, item);
			} else {
				plugin.getLogger().warning("There was a problem attempting to process action: '" + item + "'");
			}
	}
}
}