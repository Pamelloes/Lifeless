package org.dyndns.pamelloes.Lifeless;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * This Thread is the Heart of the program, it processes all of the data. If this Thread gets overloaded,
 * however, delays can grow exponential.
 * 
 * @author Pamelloes
 *
 */
public class UpdateThread implements Runnable {
	private final Lifeless life;
	private boolean run = true;
	private boolean critical = true;
	private boolean running = false;
	private final List<Event> events = Collections.synchronizedList(new ArrayList<Event>());
	private final List<Object> data = Collections.synchronizedList(new ArrayList<Object>());
	
	private List<TrackedBlock> blocks = new ArrayList<TrackedBlock>();
	
	/**
	 * UpdateThread handles updating objects in an async manner so that tick processing doesn't go into
	 * these intensive calculations.
	 * 
	 * @param life Current plugin.
	 */
	public UpdateThread(final Lifeless life) {
		this.life=life;
	}
	
	/**
	 * Starts the run-loop, should <strong>NOT</strong>  be called directly.
	 */
	public void run() {
		Event process = null;
		Object dat = null;
		boolean execute = false,canrun=false;;
		synchronized(this) {
			execute = run;
			canrun = critical;
			running=true;
		}
		while(canrun && (events.size()>0 || execute)) {
			process = null;
			dat = null;
			if(events.size()>0) {
				process = events.remove(0);
				dat = data.remove(0);
			} else try {
				Thread.sleep(50);
			} catch(Exception e) {}
			
			if(process!=null || dat!=null) processEvent(process,dat);
			
			synchronized(this) {
				execute = run;
				canrun = critical;
			}
		}
		synchronized(this) {
			running=false;
		}
	}
	
	/**
	 * Stops the execution. When this is called, the thread will finish its
	 * event queue, and then terminate, if you need immediate termination,
	 * use stopNow(), which will stop after the current event is processed.
	 * @param block Whether or not this method should block until the Thread
	 * terminates.
	 */
	public void stop(final boolean block) {
		synchronized(this) {
			run = false;
		}
		if(block) {
			while(isRunning()) {
				try {
					Thread.sleep(100);
				} catch(Exception e) {}
			}
		}
	}
	
	/**
	 * Stops the execution. When this is called, the thread will finish
	 * the event it is currently processing and then terminate.
	 * <br />
	 * <br />
	 * <strong>WARNING:</strong> May result in data loss.
	 * @param block Whether or not this method should block until the Thread
	 * terminates.
	 */
	public void stopNow(final boolean block) {
		synchronized(this) {
			critical=false;
		}
		if(block) {
			while(isRunning()) {
				try {
					Thread.sleep(100);
				} catch(Exception e) {}
			}
		}
	}
	
	/**
	 * Adds an event to the event-process queue.
	 * @param e The event to be processed.
	 */
	public void queueEvent(final Event e) {
		queueEvent(e,null);
	}
	
	/**
	 * Adds an event to event-process queue with an argument.
	 * @param e The event to be processed (in time :P)
	 * @param data The data to be passed with the event. This is
	 * typically something important that will might not
	 * be the same when the event is processed.
	 */
	public void queueEvent(final Event e, final Object data) {
		if(!run) return;
		events.add(e);
		this.data.add(data);
	}
	
	/**
	 * Loads the object block data from the database.
	 * @throws SQLException If the data couldn't be read.
	 */
	public void load() throws SQLException {
		Statement statement = life.getDBConnection().createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.
	    ResultSet res = statement.executeQuery("SELECT DISTINCT main_id FROM blocks");
	    while(res.next()) {
	    	int id = res.getInt("main_id");
	    	TrackedBlock tb = TrackedBlock.load(id, life);
	    	blocks.add(tb);
	    }
	}
	
	/**
	 * Saves the UpdateThread's data to a file. This method stops the Thread, and
	 * then waits until the Thread finishes stopping before saving.
	 * @param asap Whether or not to stop the Thread via stopNow()[true] or stop()[false];
	 * @throws SQLException if an error occurs saving the data.
	 */
	public void save(final boolean asap) throws SQLException {
		if(asap) stopNow(true);
		else stop(true);
		life.log.info("[Lifeless] Async thread stopped");
		Statement statement = life.getDBConnection().createStatement();
		statement.executeUpdate("DELETE FROM blocks");
		for(TrackedBlock tb : blocks) tb.save();
	}
	
	/**
	 * @return True if this Object is currently running.
	 */
	public synchronized boolean isRunning() {
		return running;
	}
	
	private void processEvent(final Event e, final Object data) {
		if(e == null) {
			if(data instanceof OfflinePlayer) {
				handleLeaveHardcore((OfflinePlayer) data);
				return;
			}
			return;
		}
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
		if(e instanceof EntityDeathEvent) {
			handleDeath((EntityDeathEvent) e, data);
			return;
		}
	}
	
	private void handleLeaveHardcore(OfflinePlayer player) {
		Iterator<TrackedBlock> blockz = blocks.iterator();
		while(blockz.hasNext()) {
			TrackedBlock b = blockz.next();
			b.clearPlayer(player);
		}
	}
	
	private void handleBreak(final BlockBreakEvent e, final Object data) {
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
	
	private void handleBurn(final BlockBurnEvent e, final Object data) {
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
	
	private void handlePlace(final BlockPlaceEvent e, final Object data) {
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
	
	private void handleDeath(final EntityDeathEvent e, final Object data) {
		Player p = (Player) e.getEntity();

		Iterator<TrackedBlock> blockz = blocks.iterator();
		while(blockz.hasNext()) {
			TrackedBlock b = blockz.next();
			b.removePlayer(p);
			if(b.needsRemoval()) blockz.remove();
		}
	}
}
