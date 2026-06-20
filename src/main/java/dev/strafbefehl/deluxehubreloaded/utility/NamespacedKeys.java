package dev.strafbefehl.deluxehubreloaded.utility;

import org.bukkit.NamespacedKey;

public class NamespacedKeys{
	public enum Keys {
		PLAYER_HEAD("dhub.cgui.playerhead"),
		PVP_MODE_ITEM("dhub.pvpmode.item"),
		PVP_MODE_SWITCHER("dhub.pvpmode.switcher"),
		PVP_MODE_SWITCHER_STATE("dhub.pvpmode.switcher.state"),
		TELEPORTATION_BOW_ITEM("dhub.teleportationbow");

		private final String key;

		Keys(String key) {
			this.key = key;
		}
		public String getKey() {
			return key;
		}
		public NamespacedKey get() {
			return NamespacedKey.fromString(key);
		}
	}

	public static void registerKeys(){}
}


