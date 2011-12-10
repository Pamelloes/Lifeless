package org.dyndns.pamelloes.Lifeless;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

public class LifelessEntityListener extends EntityListener {
	private final UpdateThread update;
	private final Lifeless life;
	
	public LifelessEntityListener(final Lifeless life, final UpdateThread update) {
		this.life=life;
		this.update=update;
	}
	
	@Override
	public void onEntityDeath(final EntityDeathEvent e) {
		if(!(e.getEntity() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		if(life.isHardcore(p)) {
			e.getDrops().clear();
			update.queueEvent(e);
		}
	}
}
