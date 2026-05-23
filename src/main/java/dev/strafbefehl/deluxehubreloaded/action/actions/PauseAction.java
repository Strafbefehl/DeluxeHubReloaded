package dev.strafbefehl.deluxehubreloaded.action.actions;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.action.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalTime; // FOR TESTING

public class PauseAction implements Action {
    @Override
    public String getIdentifier() {
        return "PAUSE";
    }

    @Override
    public void execute(DeluxeHubPlugin plugin, Player player, String data) {
        // Extract delay and remaining actions
        String[] parts = data.split(";", 2);
        long delay = Long.parseLong(parts[0]);
        // Extract separate remaining actions
        String remainingRaw = parts.length > 1 ? parts[1] : "";
        List<String> remainingActions = remainingRaw.isEmpty()
                ? new ArrayList<>()
                : Arrays.asList(remainingRaw.split("\\|\\|"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // FOR TESTING
            plugin.getLogger().warning("PAUSE executed after " + delay + " ticks"+LocalTime.now());

            // Execute remaining actions
            plugin.getActionManager().executeActions(player, remainingActions);
        }, delay);
    }
}
