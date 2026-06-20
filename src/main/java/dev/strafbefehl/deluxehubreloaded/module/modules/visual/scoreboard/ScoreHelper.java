package dev.strafbefehl.deluxehubreloaded.module.modules.visual.scoreboard;

import dev.strafbefehl.deluxehubreloaded.utility.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

import static dev.strafbefehl.deluxehubreloaded.utility.color.patterns.HexUtils.colorize;
import static dev.strafbefehl.deluxehubreloaded.utility.color.patterns.HexUtils.translateHexColorCodes;

/**
 * @author crisdev333
 */
public class ScoreHelper {

	private static final String[] ENTRIES = {
		"§0","§1","§2","§3","§4","§5","§6","§7",
		"§8","§9","§a","§b","§c","§d","§e","§f"
	};

	private final Scoreboard scoreboard;
	private final Objective objective;
	private final Player player;

	public ScoreHelper(Player player) {
		this.player = player;
		scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, Component.empty());
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		for (int i = 0; i < ENTRIES.length; i++) {
			Team team = scoreboard.registerNewTeam("SLOT_" + (i + 1));
			team.addEntry(genEntry(i));
		}
		player.setScoreboard(scoreboard);
	}

	public void setTitle(String title) {
		title = setPlaceholders(title);
		String truncated = title.length() > 400 ? title.substring(0, 400) : title;
		objective.displayName(LegacyComponentSerializer.legacySection().deserialize(truncated));
	}

	@SuppressWarnings("deprecation")
	public void setSlot(int slot, String text) {
		Team team = scoreboard.getTeam("SLOT_" + slot);
		String entry = genEntry(slot - 1);

		if (team != null && !scoreboard.getEntries().contains(entry)) {
			objective.getScore(entry).setScore(slot);
		}

		text = setPlaceholders(text);
		String pre = getFirstSplit(text);
		String suf = getFirstSplit(org.bukkit.ChatColor.getLastColors(pre) + getSecondSplit(text));
		team.prefix(LegacyComponentSerializer.legacySection().deserialize(pre));
		team.suffix(LegacyComponentSerializer.legacySection().deserialize(suf));
	}

	public void removeSlot(int slot) {
		String entry = genEntry(slot - 1);
		if (scoreboard.getEntries().contains(entry)) {
			scoreboard.resetScores(entry);
		}
	}

	public String setPlaceholders(String text) {
		return colorize(translateHexColorCodes(PlaceholderUtil.setPlaceholders(text, this.player)));
	}

	public void setSlotsFromList(List<String> list) {
		while (list.size() > 199) {
			list.remove(list.size() - 1);
		}

		int slot = list.size();

		if (slot < 199) {
			for (int i = (slot + 1); i <= 199; i++) {
				removeSlot(i);
			}
		}

		for (String line : list) {
			setSlot(slot, line);
			slot--;
		}
	}

	private String genEntry(int slot) {
		if (slot < 0 || slot >= ENTRIES.length) {
			return "";
		}
		return ENTRIES[slot];
	}

	private String getFirstSplit(String s) {
		return s.length() > 200 ? s.substring(0, 200) : s;
	}

	private String getSecondSplit(String s) {
		if (s.length() > 245) {
			s = s.substring(0, 245);
		}
		return s.length() > 200 ? s.substring(200) : "";
	}
}
