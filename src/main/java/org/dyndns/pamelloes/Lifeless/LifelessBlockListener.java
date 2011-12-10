package org.dyndns.pamelloes.Lifeless;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

public class LifelessBlockListener extends BlockListener {
	private final UpdateThread queue;
	
	public LifelessBlockListener(final UpdateThread queue) {
		this.queue=queue;
	}
	
	@Override
	public void onBlockBreak(final BlockBreakEvent e) {
		queue.queueEvent(e,e.getBlock().getTypeId());
	}
	
	@Override
	public void onBlockPlace(final BlockPlaceEvent e) {
		queue.queueEvent(e,e.getBlock().getTypeId());
	}
	
	@Override
	public void onBlockBurn(final BlockBurnEvent e) {
		queue.queueEvent(e);
	}
}
