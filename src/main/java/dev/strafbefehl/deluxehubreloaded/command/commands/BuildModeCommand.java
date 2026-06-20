package dev.strafbefehl.deluxehubreloaded.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.Permissions;
import dev.strafbefehl.deluxehubreloaded.command.commands.FlyCommand;
import dev.strafbefehl.deluxehubreloaded.module.modules.world.BuildMode;
import dev.strafbefehl.deluxehubreloaded.config.ConfigType;
import dev.strafbefehl.deluxehubreloaded.config.Messages;
import dev.strafbefehl.deluxehubreloaded.module.ModuleType;
import dev.strafbefehl.deluxehubreloaded.module.modules.player.PvPMode;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused") // FlyCommand IS used at line 91 via FlyCommand.allowPlayerFly — IDE false positive
public class BuildModeCommand {
	private final DeluxeHubPlugin plugin;
	private static final DeluxeHubPlugin PLUGIN = JavaPlugin.getPlugin(DeluxeHubPlugin.class);

	public BuildModeCommand(DeluxeHubPlugin plugin) {
		this.plugin = plugin;
	}

	@Command(
			aliases = {"buildmode"},
			desc = "Toggle build mode."
	)
	public void buildMode(final CommandContext args, final CommandSender sender) throws CommandException {
		FileConfiguration config = PLUGIN.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
		final BuildMode bm = BuildMode.getInstance();
		Player target;
		if(args.argsLength() >= 1){
			if (!(sender.hasPermission(Permissions.COMMAND_BUILD_MODE_OTHERS.getPermission()))) {
				Messages.NO_PERMISSION.send(sender);
				return;
			}
			target = Bukkit.getPlayer(args.getString(0));
			if(target == null){
				Messages.BUILD_MODE_COMMAND_TARGET_NOT_FOUND.send(sender, "%target%", args.getString(0));
				return;
			}

			if (config.getBoolean("pvp_mode.enabled") && !config.getBoolean("multiple_worlds")) {
				PvPMode pvpMode = (PvPMode) plugin.getModuleManager().getModule(ModuleType.PVP_MODE);
				if (pvpMode.isPlayerInPvPMode(target.getUniqueId())) {
					Messages.BUILD_MODE_COMMAND_TARGET_IN_PVP_MODE.send(sender, "%target%", LegacyComponentSerializer.legacySection().serialize(target.displayName()));
					return;
				}
			}



		}else{
			if (!(sender instanceof Player)) throw new CommandException("Console cannot use build mode");
			target = (Player) sender;

			if (config.getBoolean("pvp_mode.enabled") && !config.getBoolean("multiple_worlds")) {
				if(((PvPMode) plugin.getModuleManager().getModule(ModuleType.PVP_MODE)).isPlayerInPvPMode(target.getUniqueId())){
					Messages.BUILD_MODE_COMMAND_IN_PVP_MODE.send(sender);
					return;
				}
			}

			if (!(sender.hasPermission(Permissions.COMMAND_BUILD_MODE.getPermission())) || !(sender.hasPermission(Permissions.COMMAND_BUILD_MODE_OTHERS.getPermission()))) {
				if(BuildMode.getInstance().isPresent(((Player) sender).getUniqueId())){
					remove(bm, target);
					return;
				}
				Messages.NO_PERMISSION.send(sender);
				return;
			}
		}

		if (bm.isPresent(target.getUniqueId())) {
			remove(bm, target);
			return;
		}
		bm.addPlayer(target);
		Messages.BUILD_MODE_ENABLED.send(target);
		if(args.argsLength() >= 1) Messages.BUILD_MODE_COMMAND_ENABLED_FOR_TARGET.send(sender, "%target%", LegacyComponentSerializer.legacySection().serialize(target.displayName()));
	}

	private void remove(BuildMode instance, Player target){
		instance.removePlayer(target.getUniqueId());
		boolean shouldHaveFlight = PLUGIN.getModuleManager().isEnabled(ModuleType.DOUBLE_JUMP)
				|| Boolean.TRUE.equals(FlyCommand.allowPlayerFly.get(target.getUniqueId()));
		target.setAllowFlight(shouldHaveFlight);
		Messages.BUILD_MODE_DISABLED.send(target);
	}
}
