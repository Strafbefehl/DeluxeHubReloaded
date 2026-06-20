package dev.strafbefehl.deluxehubreloaded.module.modules.visual.tablist;

import com.google.common.base.Strings;
import dev.strafbefehl.deluxehubreloaded.utility.TextUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Objects;

public class TablistHelper {

	public static void sendTabList(Player player, String header, String footer) {

		Objects.requireNonNull(player, "Cannot update tab for null player");
		String displayName = LegacyComponentSerializer.legacySection().serialize(player.displayName());
		header = Strings.isNullOrEmpty(header) ?
				"" : TextUtil.color(header).replace("%player%", displayName);
		footer = Strings.isNullOrEmpty(footer) ?
				"" : TextUtil.color(footer).replace("%player%", displayName);

		player.sendPlayerListHeaderAndFooter(
				LegacyComponentSerializer.legacySection().deserialize(header),
				LegacyComponentSerializer.legacySection().deserialize(footer)
		);
	}
}
