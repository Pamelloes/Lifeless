package org.dyndns.pamelloes.Lifeless;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

public class LifelessBlockListener extends BlockListener {
	private UpdateThread queue;
	
	public LifelessBlockListener(UpdateThread queue) {
		this.queue=queue;
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent e) {
		queue.queueEvent(e,e.getBlock().getTypeId());
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent e) {
		queue.queueEvent(e,e.getBlock().getTypeId());
	}
	
	@Override
	public void onBlockBurn(BlockBurnEvent e) {
		queue.queueEvent(e);
	}
}
