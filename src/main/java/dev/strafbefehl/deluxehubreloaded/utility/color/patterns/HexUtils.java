package dev.strafbefehl.deluxehubreloaded.utility.color.patterns;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexUtils {

	public static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");


	@SuppressWarnings("deprecation")
	public static String colorize(final String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	@SuppressWarnings("deprecation")
	public static String translateHexColorCodes(final String message) {
		final char colorChar = ChatColor.COLOR_CHAR;

		final Matcher matcher = HEX_PATTERN.matcher(message);
		final StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

		while (matcher.find()) {
			final String group = matcher.group(1);

			matcher.appendReplacement(buffer, colorChar + "x"
					+ colorChar + group.charAt(0) + colorChar + group.charAt(1)
					+ colorChar + group.charAt(2) + colorChar + group.charAt(3)
					+ colorChar + group.charAt(4) + colorChar + group.charAt(5));
		}

		return matcher.appendTail(buffer).toString();
	}

}
