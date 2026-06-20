package dev.strafbefehl.deluxehubreloaded.utility.reflection;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.ArmorStand;

public class ArmorStandName {

	public static String getName(ArmorStand stand) {
		Component name = stand.customName();
		return name != null ? LegacyComponentSerializer.legacySection().serialize(name) : "";
	}
}
