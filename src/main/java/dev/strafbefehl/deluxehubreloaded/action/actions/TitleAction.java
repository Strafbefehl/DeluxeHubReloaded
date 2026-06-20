package dev.strafbefehl.deluxehubreloaded.action.actions;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.action.Action;
import dev.strafbefehl.deluxehubreloaded.utility.TextUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TitleAction implements Action {

	@Override
	public String getIdentifier() {
		return "TITLE";
	}

	@Override
	public void execute(DeluxeHubPlugin plugin, Player player, String data) {
		String[] args = data.split(";");

		String mainTitle = args.length > 0 ? TextUtil.color(args[0]) : "";
		String subTitle = args.length > 1 ? TextUtil.color(args[1]) : "";

		int fadeIn;
		int stay;
		int fadeOut;
		try {
			fadeIn = Integer.parseInt(args[2]);
			stay = Integer.parseInt(args[3]);
			fadeOut = Integer.parseInt(args[4]);
		} catch (NumberFormatException ex) {
			fadeIn = 1;
			stay = 3;
			fadeOut = 1;
		}

//		if (XMaterial.supports(10)) {
			player.showTitle(Title.title(
				LegacyComponentSerializer.legacySection().deserialize(mainTitle),
				LegacyComponentSerializer.legacySection().deserialize(subTitle),
				Title.Times.times(
						Duration.ofMillis(fadeIn * 20 * 50L),
						Duration.ofMillis(stay * 20 * 50L),
						Duration.ofMillis(fadeOut * 20 * 50L)
				)
		));
//		} else {
//			Titles.sendTitle(player, fadeIn * 20, stay * 20, fadeOut * 20, mainTitle, subTitle);
//		}
	}
}
