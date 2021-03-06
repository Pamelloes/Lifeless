package org.dyndns.pamelloes.Lifeless;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.GameModeCommand;
import org.bukkit.entity.Player;

/**
 * This class extends the vanilla GameModeCommand to allow the use
 * to enter hardcore mode via /gamemode <user> 2
 * 
 * @author Pamelloes
 */
public class HardcoreGameMode extends GameModeCommand {
		private final Lifeless life;
		
		public HardcoreGameMode(final Lifeless life) {
			this.life=life;
		}
		
		@Override
		public boolean execute(final CommandSender sender, final String currentAlias, final String[] args) {
			Player player = null;
			if(!currentAlias.equalsIgnoreCase("hardcore")) {
				if(args.length!=2) return super.execute(sender, currentAlias, args);
	            int value = -1;

	            try {
	                value = Integer.parseInt(args[1]);
	            } catch (NumberFormatException ex) {}

	            player = Bukkit.getPlayerExact(args[0]);

	            if(value!=2) {
					if(player!=null) life.unHardcore(player);
	            	return super.execute(sender, currentAlias, args);
	            }
			} else {
				if(args.length!=1)  sender.sendMessage(ChatColor.RED + "Usage: " + "/hardcore <player>");
				player = Bukkit.getPlayerExact(args[0]);
			}

			if (!(testPermission(sender))) return true;
			
            if (life.hardcore(player)) {
            	player.setGameMode(GameMode.SURVIVAL);
                Command.broadcastCommandMessage(sender, "Setting " + player.getName() + " to game mode 2");
            } else {
                sender.sendMessage(player.getName() + " already has game mode 2");
            }
			return true;
		}
}
