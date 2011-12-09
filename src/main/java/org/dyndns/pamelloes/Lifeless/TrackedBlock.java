package org.dyndns.pamelloes.Lifeless;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.dyndns.pamelloes.Lifeless.serializable.LocationSerializable;


public class TrackedBlock implements Serializable {
	private transient Lifeless life;
	private transient Location position;
	private int id;
	private boolean remove;
	
	private Stack<Integer> changesid = new Stack<Integer>();
	private Stack<Player> changesperson = new Stack<Player>();
	
	/**
	 * Creates a new TrackedBlock for the given block.
	 * 
	 * @param block The Block to be tracked.
	 * @param lifeless The current plugin instance.
	 */
	public TrackedBlock(Block block, Lifeless lifeless) {
		this(block.getTypeId(),block.getLocation(),lifeless);
	}
	
	/**
	 * Creates a new TrackedBlock from a position and an id.
	 * @param id
	 * @param location
	 * @param lifeless
	 */
	public TrackedBlock(int id, Location location, Lifeless lifeless) {
		life = lifeless;
		position = location;
		this.id = id;
		changesid.push(id);
		changesperson.push(null);
		
	}
	
	/**
	 * This method is called when the block's id changes.
	 * 
	 * @param player The player who changed the block. If null (i.e. a natural occurance, or
	 * changed by another plugin), then this object is marked for deletion.
	 * @param id The block's new id.
	 */
	public void addChange(Player player, int id) {
		if(player==null) {
			remove=true;
			return;
		}
		
		if(life.isHardcore(player)) {
			changesperson.push(player);
			changesid.push(id);
			this.id = id;
		} else {
			remove = true;
			return;
		}
	}
	
	/**
	 * Removes all actions a player has performed on this block. If the player's actions were
	 * the most recent to affect this block, then this Object is marked for deletion.
	 * 
	 * This is called after the playerleaves hardcore.
	 * 
	 * @param player The player who's actions to remove.
	 */
	public synchronized void clearPlayer(Player player) {
		for(int i=1;i<changesperson.size()-1;i++) {
			if(changesperson.get(i).equals(player)) {
				changesperson.remove(i);
				changesid.remove(i);
				i--;
			}
		}
		if(changesperson.size()<=1) {
			remove = true;
			return;
		}
		if(changesperson.lastElement().equals(player)) {
			remove=true;
			return;
		}
	}
	
	/**
	 * Removes all actions a player has performed on this block. If the player's actions were
	 * the most recent to affect this block, then the block's id is changed to match its id
	 * before the player's action(s).
	 * 
	 * This is called after the player dies.
	 * 
	 * @param player The player who's actions to remove.
	 */
	public synchronized void removePlayer(Player player) {
		for(int i=1;i<changesperson.size()-1;i++) {
			if(changesperson.get(i).equals(player)) {
				changesperson.remove(i);
				changesid.remove(i);
				i--;
			}
		}
		if(changesperson.size()<=1) {
			remove = true;
			int id = changesid.lastElement();
			updateId(id);
			return;
		}
		if(changesperson.lastElement().equals(player)) {
			changesperson.remove(changesperson.size()-1);
			changesid.remove(changesperson.size()-1);
			int id = changesid.lastElement();
			updateId(id);
			if(changesperson.size()<=1) {
				remove=true;
				return;
			}
		}
	}
	
	/**
	 * Do I really need to say what this does?
	 * @rturn The TrackedBlock's Location
	 */
	public Location getLocation() {
		return position;
	}
	
	/**
	 * When changes occur to a block that are considered irrevocable, then
	 * the block is marked for removal. After any change is made to this object
	 * this method should be called to avoid keeping the object in memory.
	 * 
	 * @return True if the reference to this object can be released.
	 */
	public boolean needsRemoval() {
		return remove;
	}
	
	/**
	 * Gets the TrackedBlock's Id, if this id doesn't match the actual
	 * blocks id, then something is wrong.
	 */
	public synchronized int getId() {
		return id;
	}
	
	private synchronized void updateId(final int id) {
		this.id=id;
		life.getServer().getScheduler().scheduleSyncDelayedTask(life, new Runnable() {
			public void run() {
				position.getBlock().setTypeId(id);
			}
		});
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(new LocationSerializable(position));
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		position = ((LocationSerializable) in.readObject()).getLocation();
	}
}
