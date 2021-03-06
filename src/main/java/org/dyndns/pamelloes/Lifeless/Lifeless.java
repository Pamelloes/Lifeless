package org.dyndns.pamelloes.Lifeless;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

	private final List<String> hardcore = Collections.synchronizedList(new ArrayList<String>());
	
	private final UpdateThread async = new UpdateThread(this);
	
	private final YamlConfiguration players = new YamlConfiguration();
	
	private Connection connection;
	
	public void onEnable() {
		try {
			downloadSQLite();
		} catch (IOException e) {
			log.severe("Could not download SQLite!");
			e.printStackTrace();
		}
		
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

	public void onDisable() {
		saveData();
		log.info("[Lifeless] Lifeless disabled");
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
	 * Registers a fallback command in case the program couldn't hook into /gamemode
	 */
	private void registerFallback() {
		this.getCommand("hardcore").setExecutor(new HardcoreCommandExecutor(this));
	}
	
	/**
	 * Loads saved data.
	 */
	@SuppressWarnings("unchecked")
	private void loadData() {
		File file = new File(getDataFolder(),"players.yml");
		if(file.exists()) try {
			players.load(file);
			hardcore.clear();
			hardcore.addAll(players.getList("players"));
			Iterator<String> i = hardcore.iterator();
			while(i.hasNext()) {
				OfflinePlayer player = getServer().getOfflinePlayer(i.next());
				if(player==null) i.remove();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		try {
			Class.forName("org.sqlite.JDBC");
		    connection = DriverManager.getConnection("jdbc:sqlite:" + new File(getDataFolder(),"data.db").getAbsolutePath());
		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
		    statement.executeUpdate("create table if not exists blocks (main_id integer, x integer, y integer, z integer, sub_id integer, players string, block_id integer)");
		    async.load();
		} catch (ClassNotFoundException e) {
			log.info("[Lifeless] Could not load SQLite.");
		} catch (SQLException e) {
			log.info("[Lifeless] Could not load/create database.");
		}
		log.info("[Lifeless] Loaded data.");
	}
	
	/**
	 * Saves long-term data.
	 */
	private void saveData() {
		players.set("players",hardcore);
		try {
			players.save(new File(this.getDataFolder(),"players.yml"));
		} catch (IOException e) {
			log.warning("[Lifeless] Could not save active players");
		}
		 try {
			async.save(true);
		} catch (SQLException e) {
			log.warning("[Lifeles] Could not save block data.");
		}
		log.info("[Lifeless] Saved data.");
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
		
		LifelessEntityListener lel = new LifelessEntityListener(this,async);
		pm.registerEvent(Type.ENTITY_DEATH, lel, Priority.Monitor, this);
	}
	
	/**
	 * Puts the given player into hardcore.
	 * @param player The Player to put in hardcore.
	 * @return True if the player was put in hardcore, false if they
	 * already were in it.
	 */
	public boolean hardcore(final OfflinePlayer player) {
		if(hardcore.contains(player.getName())) return false;
		hardcore.add(player.getName());
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
		if(!hardcore.contains(player.getName())) return false;
		hardcore.remove(player.getName());
		async.queueEvent(null,player);
		log.info("[Lifeless] " + player.getName() + " has left Hardcore.");
		return true;
	}
	
	/**
	 * Checks if the given player is in hardcore mode.
	 * @param player The player to check if is in hardcore mode.
	 * @return True if the given player is in hardcore mode.
	 */
	public boolean isHardcore(final OfflinePlayer player) {
		return hardcore.contains(player.getName());
	}
	
	/**
	 * Gets the database connection.
	 */
	public Connection getDBConnection() {
		return connection;
	}

	private void downloadSQLite() throws IOException {
		File dir = new File("lib");
		if (!dir.exists()) dir.mkdir();
		File file = new File(dir, "sqlite-jdbc.jar");
		if (file.exists()) return;
		log.info("[Lifeless] Downloading dependencies.");
		URL google = new URL("http://www.xerial.org/maven/repository/artifact/org/xerial/sqlite-jdbc/3.7.2/sqlite-jdbc-3.7.2.jar");
		ReadableByteChannel rbc = Channels.newChannel(google.openStream());
		FileOutputStream fos = new FileOutputStream(file);
		fos.getChannel().transferFrom(rbc, 0, 1 << 24);
		log.info("[Lifeless] Downloaded.");
	}
}
