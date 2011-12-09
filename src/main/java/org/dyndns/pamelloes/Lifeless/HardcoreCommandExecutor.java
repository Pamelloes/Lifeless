package org.dyndns.pamelloes.Lifeless;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HardcoreCommandExecutor implements CommandExecutor {
	private Lifeless lifeless;
	
	public HardcoreCommandExecutor(Lifeless plugin) {
		lifeless = plugin;
	}
	
	public boolean onCommand(CommandSender sender, Command paramCommand, String paramString, String[] args) {
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
