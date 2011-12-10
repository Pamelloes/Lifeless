package org.dyndns.pamelloes.Lifeless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.GameModeCommand;
import org.bukkit.command.defaults.VanillaCommand;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Event.Priority;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lifeless extends JavaPlugin {
	
	public final Logger log = Logger.getLogger("Minecraft");

	private final List<String> names = Collections.synchronizedList(new ArrayList<String>());
	private final List<OfflinePlayer> hardcore = Collections.synchronizedList(new ArrayList<OfflinePlayer>());
	
	private final UpdateThread async = new UpdateThread(this);
	
	private final YamlConfiguration players = new YamlConfiguration();
	
	public void onEnable() {
		loadData();

		PluginManager pm = getServer().getPluginManager();
		boolean hccmd = true;
		if(pm instanceof SimplePluginManager) hccmd=!registerCmd((SimplePluginManager) pm);
		else registerFallback();
		
		registerEvents(pm);
		
		getServer().getScheduler().scheduleAsyncDelayedTask(this, async);
		
		if(hccmd) log.warning("[Lifeless] Lifeless enabled with \"/hardcore\"");
		else log.info("[Lifeless] Lifeless enabled");
	}
	
	/**
	 * Tries to hook into /gamemode command via reflection.
	 * @param pm A SimplePluginManager (to be manipulated via reflection)
	 * @return true if the command was properly registered, false if the
	 * backup command was registered due to an issue.
	 */
	@SuppressWarnings({ "serial", "unchecked" })
	private boolean registerCmd(final SimplePluginManager pm) {
		Field f = null;
		try {
			f = SimplePluginManager.class.getDeclaredField("commandMap");
		} catch (SecurityException e) {
			log.warning("[Lifeless] Security Error O_o");
			registerFallback();
			return false;
		} catch (NoSuchFieldException e) {
			log.warning("[Lifeless] SimplePluginManager's definition has changed.");
			registerFallback();
			return false;
		}
		if(f==null) return false;
		SimpleCommandMap map = null;
		try {
			f.setAccessible(true);
			map = (SimpleCommandMap) f.get(pm);
		} catch (IllegalArgumentException e) {
			log.warning("[Lifeless] SimplePluginManager is not actually a SimplePluginManager. O_o");
			registerFallback();
			return false;
		} catch (IllegalAccessException e) {
			log.warning("[Lifeless] Security Error O_o");
			registerFallback();
			return false;
		}
		if(map==null) return false;
		
		f = null;
		try {
			f = SimpleCommandMap.class.getDeclaredField("fallbackCommands");
		} catch (SecurityException e) {
			log.warning("[Lifeless] Security Error O_o");
			registerFallback();
			return false;
		} catch (NoSuchFieldException e) {
			log.warning("[Lifeless] SimpleCommandMap's definition has changed.");
			registerFallback();
			return false;
		}
		if(f==null) return false;
		Set<VanillaCommand> defaults = null;
		try {
			f.setAccessible(true);
			defaults = (Set<VanillaCommand>) f.get(map);
		} catch (IllegalArgumentException e) {
			log.warning("[Lifeless] SimpleCommandMap is not actually a SimpleCommandMap. O_o");
			registerFallback();
			return false;
		} catch (IllegalAccessException e) {
			log.warning("[Lifeless] Security Error O_o");
			registerFallback();
			return false;
		}
		if(defaults==null) return false;
		
		VanillaCommand vc = new HardcoreGameMode(this);
		vc.setAliases(new ArrayList<String>() {{
			add("hardcore");
		}} );
		for(VanillaCommand cmd : defaults) {
			if(cmd instanceof GameModeCommand) {
				defaults.remove(cmd);
				break;
			}
		}
		defaults.add(vc);
		return true;
	}
	
	/**
	 * Loads saved data.
	 */
	@SuppressWarnings("unchecked")
	private void loadData() {
		File f = new File(this.getDataFolder(),"block.dat");
		if(f.exists()) {
			try {
				async.load(new ObjectInputStream(new FileInputStream(f)));
			} catch (IOException e) {
				log.warning("[Lifeless] Could not load block data");
			} catch (ClassNotFoundException e) {
				log.warning("[Lifeless] Could not resolve block data");
			}
		}
		File file = new File(getDataFolder(),"players.yml");
		if(file.exists()) try {
			players.load(file);
			names.clear();
			names.addAll(players.getList("players"));
			Iterator<String> i = names.iterator();
			while(i.hasNext()) {
				OfflinePlayer player = getServer().getOfflinePlayer(i.next());
				if(player==null) i.remove();
				else hardcore.add(player);
			}
		} catch(Exception e) {
		}
	}

	/**
	 * Registers a fallback command in case the program couldn't hook into /gamemode
	 */
	private void registerFallback() {
		this.getCommand("hardcore").setExecutor(new HardcoreCommandExecutor(this));
	}
	
	/**
	 * Registers the necessary events.
	 * @param pm The PluginManager to register the events through.
	 */
	private void registerEvents(final PluginManager pm) {
		LifelessBlockListener lbl = new LifelessBlockListener(async);
		pm.registerEvent(Type.BLOCK_BREAK, lbl, Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_BURN, lbl, Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_PLACE, lbl, Priority.Monitor, this);
	}
	
	/**
	 * Puts the given player into hardcore.
	 * @param player The Player to put in hardcore.
	 * @return True if the player was put in hardcore, false if they
	 * already were in it.
	 */
	public boolean hardcore(final OfflinePlayer player) {
		if(hardcore.contains(player)) return false;
		hardcore.add(player);
		names.add(player.getName());
		log.info("[Lifeless] " + player.getName() + " has entered Hardcore.");
		return true;
	}

	/**
	 * Tales the given player out of hardcore.
	 * @param player The Player to be taken out of hardcore.
	 * @return True if the player was removed from hardcore, false if they
	 * didn't have it in the first place.
	 */
	public boolean unHardcore(final OfflinePlayer player) {
		if(!hardcore.contains(player)) return false;
		hardcore.remove(player);
		names.remove(player.getName());
		log.info("[Lifeless] " + player.getName() + " has left Hardcore.");
		return true;
	}
	
	/**
	 * Checks if the given player is in hardcore mode.
	 * @param player The player to check if is in hardcore mode.
	 * @return True if the given player is in hardcore mode.
	 */
	public boolean isHardcore(final OfflinePlayer player) {
		return hardcore.contains(player);
	}

	public void onDisable() {
		saveData();
	}
	
	/**
	 * Saves long-term data.
	 */
	private void saveData() {
		players.set("players",names);
		try {
			players.save(new File(this.getDataFolder(),"players.yml"));
		} catch (IOException e) {
			log.warning("[Lifeless] Could not save active players");
		}
		try {
			async.save(new ObjectOutputStream(new FileOutputStream(new File(this.getDataFolder(),"block.dat"))), false);
		} catch (IOException e) {
			log.warning("[Lifeless] Could not save block data");
		}
		log.info("[Lifeless] Lifeless disabled");
	}

}
