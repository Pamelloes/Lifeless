package org.dyndns.pamelloes.Lifeless;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * This class is used to track changes performed to an associated Location so that they can be undone.
 * 
 * @author Pamelloes
 */
public class TrackedBlock {
	private Lifeless life;
	private Location position;
	private int id;
	private boolean remove;
	
	private List<Integer> changesid = new ArrayList<Integer>();
	private transient List<OfflinePlayer> changesperson = new ArrayList<OfflinePlayer>();
	
	/**
	 * Creates a new TrackedBlock for the given block.
	 * 
	 * @param block The Block to be tracked.
	 * @param lifeless The current plugin instance.
	 */
	public TrackedBlock(final Block block, final Lifeless lifeless) {
		this(block.getTypeId(),block.getLocation(),lifeless);
	}
	
	/**
	 * Creates a new TrackedBlock from a position and an id.
	 * @param id
	 * @param location
	 * @param lifeless
	 */
	public TrackedBlock(final int id, final Location location, final Lifeless lifeless) {
		life = lifeless;
		position = location;
		this.id = id;
		changesid.add(id);
		changesperson.add(null);
		
	}
	
	/**
	 * Used for loading.
	 */
	private TrackedBlock(Lifeless lifeless) {
		life = lifeless;
	}
	
	/**
	 * This method is called when the block's id changes.
	 * 
	 * @param player The player who changed the block. If null (i.e. a natural occurance, or
	 * changed by another plugin), then this object is marked for deletion.
	 * @param id The block's new id.
	 */
	public void addChange(final OfflinePlayer player, final int id) {
		if(player==null) {
			remove=true;
			return;
		}
		
		if(life.isHardcore(player)) {
			changesperson.add(player);
			changesid.add(id);
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
	 * This is called after the player leaves hardcore.
	 * 
	 * @param player The player who's actions to remove.
	 */
	public synchronized void clearPlayer(final OfflinePlayer player) {
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
		if(changesperson.get(changesperson.size()-1).equals(player)) {
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
	public synchronized void removePlayer(final OfflinePlayer player) {
		for(int i=1;i<changesperson.size()-1;i++) {
			if(changesperson.get(i).equals(player)) {
				changesperson.remove(i);
				changesid.remove(i);
				i--;
			}
		}
		if(changesperson.size()<=1) {
			remove = true;
			int id = changesid.get(changesid.size()-1);
			updateId(id);
			return;
		}
		if(changesperson.get(changesperson.size()-1).equals(player)) {
			changesperson.remove(changesperson.size()-1);
			changesid.remove(changesid.size()-1);
			int id = changesid.get(changesid.size()-1);
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
	
	/**
	 * Saves this block's data to the database.
	 */
	public void save() throws SQLException {
		Connection c = life.getDBConnection();
	    Statement statement = c.createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.
	    ResultSet rs = statement.executeQuery("SELECT DISTINCT main_id FROM blocks");
	    List<Integer> ids = new ArrayList<Integer>();
	    while (rs.next()) {
	    	int id = rs.getInt("main_id");
	    	ids.add(id);
	    }
	    int mainid = ids.isEmpty() ? 0 : Collections.max(ids) + 1;
	    System.out.println("Position: " + position + "\nWorld: " + position.getWorld());
	    statement.executeUpdate("insert into blocks values(" + mainid+", "+position.getBlockX()+", "+position.getBlockY()+", "+position.getBlockZ()+", -1, '"+position.getWorld().getName()+"', -1)");
	    for(int i = 0; i < changesid.size(); i++) {
	    	int blockid = changesid.get(i);
	    	OfflinePlayer op = changesperson.get(i);
	    	String player = op==null ? "" : op.getName();
		    statement.executeUpdate("insert into blocks values(" + mainid+", -1, -1, -1, "+i+", '" + player + "', "+blockid+")");
	    }
	}
	
	/**
	 * Load a trackedBlock with the given id from the database.
	 */
	public static TrackedBlock load(int id, Lifeless lifeless) throws SQLException {
		TrackedBlock tb = new TrackedBlock(lifeless);
		tb.load(id);
		return tb;
	}
	
	private void load(int id) throws SQLException {
		Connection c = life.getDBConnection();
	    Statement statement = c.createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.
	    ResultSet rs = statement.executeQuery("SELECT * FROM blocks WHERE main_id="+id+" ORDER BY sub_id ASC");
	    while(rs.next()) {
	    	int subid = rs.getInt("sub_id");
	    	if(subid==-1) {
	    		int blockx = rs.getInt("x");
	    		int blocky = rs.getInt("y");
	    		int blockz = rs.getInt("z");
	    		String worldname = rs.getString("players");
	    		World world = Bukkit.getWorld(worldname);
	    		position = new Location(world,blockx,blocky,blockz);
	    		System.out.println(position);
	    		continue;
	    	}
	    	String playername = rs.getString("players");
	    	int blockid = rs.getInt("block_id");
	    	OfflinePlayer player = playername.equals("") ? null : Bukkit.getOfflinePlayer(playername);
			changesperson.add(player);
			changesid.add(blockid);
			this.id = blockid;
	    }
	    if(changesperson.size()<2) remove = true;
	}
	
	private synchronized void updateId(final int id) {
		this.id=id;
		life.getServer().getScheduler().scheduleSyncDelayedTask(life, new Runnable() {
			public void run() {
				position.getBlock().setTypeId(id);
			}
		});
	}
}
