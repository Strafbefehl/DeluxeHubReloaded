package dev.strafbefehl.deluxehubreloaded.action.actions;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.action.Action;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectAction implements Action {

    @Override
    public String getIdentifier() {
        return "EFFECT";
    }

    @Override
    public void execute(DeluxeHubPlugin plugin, Player player, String data) {
        String[] args = data.split(";");
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(args[0].toLowerCase()));
        if (type == null || args.length < 2) return;
        int amplifier = Integer.parseInt(args[1]) - 1;
        boolean showIcon = args.length > 2 && Boolean.parseBoolean(args[2]);
        player.addPotionEffect(new PotionEffect(type, -1, amplifier, false, false, showIcon));
    }
}