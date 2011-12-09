package org.dyndns.pamelloes.Lifeless;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class UpdateThread implements Runnable {
	private Lifeless life;
	private Object lock = new Object();
	private boolean run = true;
	private List<Event> events = new ArrayList<Event>();
	private List<Object> data = new ArrayList<Object>();
	
	private List<TrackedBlock> blocks = new ArrayList<TrackedBlock>();
	
	/**
	 * UpdateThread handles updating objects in an async manner so that tick processing doesn't go into
	 * these intensive calculations.
	 * 
	 * @param life Current plugin.
	 */
	public UpdateThread(Lifeless life) {
		this.life=life;
	}
	
	public void run() {
		Event process = null;
		Object dat = null;
		boolean execute = false;
		synchronized(lock) {
			execute = run;
		}
		while(execute) {
			process = null;
			dat = null;
			synchronized(lock) {
				if(events.size()>0) {
					process = events.remove(0);
					dat = data.remove(0);
				}
			}
			
			if(process!=null) processEvent(process,dat);
			else try {
				Thread.sleep(50);
			} catch(Exception e) {}
			
			synchronized(lock) {
				execute = run;
			}
		}
	}
	
	public void stop() {
		synchronized(lock) {
			run = false;
		}
	}
	
	public void queueEvent(Event e) {
		queueEvent(e,null);
	}
	
	public void queueEvent(Event e, Object data) {
		synchronized(lock) {
			events.add(e);
			this.data.add(data);
		}
	}
	
	private void processEvent(Event e, Object data) {
		if(e instanceof BlockBreakEvent) {
			handleBreak((BlockBreakEvent)e, data);
			return;
		}
		if(e instanceof BlockBurnEvent) {
			handleBurn((BlockBurnEvent) e, data);
			return;
		}
		if(e instanceof BlockPlaceEvent) {
			handlePlace((BlockPlaceEvent) e, data);
			return;
		}
	}
	
	private void handleBreak(BlockBreakEvent e, Object data) {
		if(e.isCancelled()) return;
		Iterator<TrackedBlock> blockz = blocks.iterator();
		while(blockz.hasNext()) {
			TrackedBlock b = blockz.next();
			if(b.getLocation().equals(e.getBlock().getLocation())) {
				b.addChange(e.getPlayer(), Material.AIR.getId());
				if(b.needsRemoval()) blockz.remove();
				return;
			}
		}
		if(!life.isHardcore(e.getPlayer())) return;
		int id = 0;
		if(data instanceof Integer) id = (Integer) data;
		TrackedBlock b = new TrackedBlock(id,e.getBlock().getLocation(),life);
		b.addChange(e.getPlayer(), 0);
		blocks.add(b);
	}
	
	private void handleBurn(BlockBurnEvent e, Object data) {
		if(e.isCancelled()) return;
		Iterator<TrackedBlock> blockz = blocks.iterator();
		while(blockz.hasNext()) {
			TrackedBlock b = blockz.next();
			if(b.getLocation().equals(e.getBlock().getLocation())) {
				b.addChange(null, Material.AIR.getId());
				if(b.needsRemoval()) blockz.remove();
				return;
			}
		}
	}
	
	private void handlePlace(BlockPlaceEvent e, Object data) {
		if(e.isCancelled()) return;
		int id = e.getBlock().getTypeId();
		if(data instanceof Integer) id = (Integer) data;
		
		Iterator<TrackedBlock> blockz = blocks.iterator();
		while(blockz.hasNext()) {
			TrackedBlock b = blockz.next();
			if(b.getLocation().equals(e.getBlock().getLocation())) {
				b.addChange(e.getPlayer(), id);
				if(b.needsRemoval()) blockz.remove();
				return;
			}
		}
		if(!life.isHardcore(e.getPlayer())) return;
		TrackedBlock b = new TrackedBlock(0,e.getBlock().getLocation(),life);
		b.addChange(e.getPlayer(), id);
		blocks.add(b);
	}
}
