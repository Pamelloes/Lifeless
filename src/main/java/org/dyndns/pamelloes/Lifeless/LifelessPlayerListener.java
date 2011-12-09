package org.dyndns.pamelloes.Lifeless;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LifelessPlayerListener extends PlayerListener {
	private Lifeless life;
	
	public LifelessPlayerListener(Lifeless life) {
		this.life=life;
	}
	
	@Override
	public void onPlayerJoin(PlayerJoinEvent e) {
		if(life.names.contains(e.getPlayer().getName())) life.hardcore(e.getPlayer(),false);
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent e) {
		if(life.isHardcore(e.getPlayer())) life.unHardcore(e.getPlayer(),false);
	}
	
	@Override
	public void onPlayerKick(PlayerKickEvent e) {
		if(life.isHardcore(e.getPlayer())) life.unHardcore(e.getPlayer(),false);
	}
}
