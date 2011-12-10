package org.dyndns.pamelloes.Lifeless;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This is a backup method for entering hardcore mode. Should the program be unable
 * to hook into the /gamemode command, this command will be registered instead.
 * @author Pamelloes
 */
public class HardcoreCommandExecutor implements CommandExecutor {
	private final Lifeless lifeless;
	
	public HardcoreCommandExecutor(final Lifeless plugin) {
		lifeless = plugin;
	}
	
	public boolean onCommand(final CommandSender sender, final Command paramCommand, final String paramString, final String[] args) {
		if(!paramCommand.testPermission(sender)) return true;

        Player player = Bukkit.getPlayerExact(args[0]);

        if (lifeless.hardcore(player)) {
        	player.setGameMode(GameMode.SURVIVAL);
            Command.broadcastCommandMessage(sender, "Setting " + player.getName() + " to game mode Hardcore");
            return true;
        } else {
        	lifeless.unHardcore(player);
        	player.setGameMode(GameMode.SURVIVAL);
            Command.broadcastCommandMessage(sender, "Setting " + player.getName() + " to game mode Survival");
            return true;
        }
	}

}
