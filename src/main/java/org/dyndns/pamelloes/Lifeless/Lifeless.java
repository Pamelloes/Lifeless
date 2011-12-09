package org.dyndns.pamelloes.Lifeless;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.GameModeCommand;
import org.bukkit.command.defaults.VanillaCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Event.Priority;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lifeless extends JavaPlugin {
	
	public Logger log = Logger.getLogger("Minecraft");

	protected List<String> names = new ArrayList<String>();
	
	private Object lock = new Object();
	private List<Player> hardcore = new ArrayList<Player>();
	
	private  UpdateThread async = new UpdateThread(this);
	
	private YamlConfiguration players = new YamlConfiguration();
	
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
	
	@SuppressWarnings({ "serial", "unchecked" })
	private boolean registerCmd(SimplePluginManager pm) {
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
	
	@SuppressWarnings("unchecked")
	private void loadData() {
		//TODO Load block data from flatfile.
		File file = new File(getDataFolder(),"players.yml");
		if(file.exists()) try {
			players.load(file);
			names = players.getList("players");
			Player[] ps = getServer().getOnlinePlayers();
			for(Player p : ps) {
				if(names.contains(p.getName())) hardcore.add(p);
			}
		} catch(Exception e) {
		}
	}

	private void registerFallback() {
		this.getCommand("hardcore").setExecutor(new HardcoreCommandExecutor(this));
	}
	
	private void registerEvents(PluginManager pm) {
		LifelessPlayerListener lpl = new LifelessPlayerListener(this);
		pm.registerEvent(Type.PLAYER_JOIN, lpl, Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_QUIT, lpl, Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_KICK, lpl, Priority.Monitor, this);
		
		LifelessBlockListener lbl = new LifelessBlockListener(async);
		pm.registerEvent(Type.BLOCK_BREAK, lbl, Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_BURN, lbl, Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_PLACE, lbl, Priority.Monitor, this);
	}
	
	public boolean hardcore(Player player) {
		return hardcore(player,true);
	}
	
	public boolean hardcore(Player player, boolean save) {
		synchronized(lock) {
			if(hardcore.contains(player)) return false;
			hardcore.add(player);
			if(save) names.add(player.getName());
			log.info("[Lifeless] " + player.getName() + " has entered Hardcore.");
			return true;
		}
	}
	
	public boolean unHardcore(Player player) {
		return unHardcore(player,true);
	}
	
	public boolean unHardcore(Player player, boolean save) {
		synchronized(lock) {
			if(!hardcore.contains(player)) return false;
			hardcore.remove(player);
			if(save) names.remove(player.getName());
			log.info("[Lifeless] " + player.getName() + " has left Hardcore.");
			return true;
		}
	}
	
	public boolean isHardcore(Player player) {
		synchronized(lock) {
			return hardcore.contains(player);
		}
	}

	public void onDisable() {
		// TODO Save block data to flatfile.
		players.set("players",names);
		try {
			players.save(new File(this.getDataFolder(),"players.yml"));
		} catch (IOException e) {
			log.warning("[Lifeless] Could not save active players");
		}
		async.stop();
		log.info("[Lifeless] Lifeless disabled");
	}

}
