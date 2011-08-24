package com.bukkit.gemo.FlyModProtection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.alta189.sqlLibrary.SQLite.sqlCore;
import com.bukkit.gemo.NoGrief.NoGriefCore;
import com.gemo.utils.UtilPermissions;

public class FMProtectionCore extends JavaPlugin {
	private static String pluginName = "FMProtection";
	private static Logger log = Logger.getLogger("Minecraft");
	public static Server server = null;

	/** LISTENER */
	private static NoGriefCore core;
	public static FMProtectionPL pListener;

	/** SQLITE */
	private sqlCore SQLite;

	public static HashMap<String, FMChunkArea> selections = new HashMap<String, FMChunkArea>();

	// AUSGABE IN DER CONSOLE
	public static void log(String str) {
		log.info("[ " + pluginName + " ]: " + str);
	}

	// ON DISABLE
	@Override
	public void onDisable() {
		if (searchNoGrief()) {
			log("disabled");
			if (SQLite.checkConnection()) {
				SQLite.close();
			}
		}
	}

	// ON ENABLE
	@Override
	public void onEnable() {
		server = getServer();

		if (searchNoGrief()) {
			// ////////////////////////
			//
			// SQL
			//
			// ////////////////////////
			// Declare SQLite handler
			this.SQLite = new sqlCore(Logger.getLogger("Minecraft"),
					pluginName, "FlyZones", "plugins/FlyModProtection");
			// Initialize SQLite handler
			this.SQLite.initialize();
			// Check if the table exists, if it doesn't create it
			if (!this.SQLite.checkTable("FlyZones")) {
				log("Creating table FlyZones");
				String query = "CREATE TABLE FlyZones ("
						+ "PlayerName VARCHAR(255), "
						+ "WorldName VARCHAR(255), "
						+ "Chunk1_X INT, Chunk1_Z INT, Chunk2_X INT, Chunk2_Z INT,"
						+ "saveTime VARCHAR(255)" + "); ";
				this.SQLite.createTable(query);
			}

			// REGISTER EVENT
			pListener = new FMProtectionPL();
			PluginManager pm = server.getPluginManager();

			pm.registerEvent(Event.Type.PLAYER_MOVE, pListener,
					Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_RESPAWN, pListener,
					Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_TELEPORT, pListener,
					Event.Priority.Normal, this);

			loadZones();

			log("enabled");

		} else {
			log("NoGrief was not found! AntiFlyMod is disabled!");
		}
	}

	// REMOVE SINGLE ZONE
	public void removeZone(String playerName) {
		try {
			String query = "DELETE FROM FlyZones" + " WHERE PlayerName='"
					+ playerName + "';";
			SQLite.deleteQuery(query);
			FMProtectionPL.flyAreas.remove(playerName);
		} catch (Exception e) {
		}
	}

	// ADD SINGLE ZONE
	public void addZone(String playerName, FMChunkArea zone) {
		if (ZoneExistsInDB(playerName))
			removeZone(playerName);

		String query = "INSERT INTO FlyZones " + " VALUES ('" + playerName
				+ "', " + "'" + zone.worldName + "'," + zone.chunk1_x + ", "
				+ zone.chunk1_z + ", " + zone.chunk2_x + ", " + zone.chunk2_z
				+ ", " + "'" + zone.lastSavedTime + "');";
		SQLite.insertQuery(query);
	}

	// EXISTS IN DB
	public boolean ZoneExistsInDB(String playerName) {
		String query = "SELECT * FROM FlyZones" + " WHERE PlayerName='"
				+ playerName + "' LIMIT 1;";

		ResultSet result = null;
		result = SQLite.sqlQuery(query);
		try {
			int count = 0;
			while (result != null && result.next()) {
				count++;
			}
			return count > 0;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// CLEAR ALL ZONES
	public void clearZones() {
		try {
			String query = "DROP TABLE FlyZones;";
			SQLite.deleteQuery(query);
						
			if (!this.SQLite.checkTable("FlyZones")) {
				log("Creating table FlyZones");
				query = "CREATE TABLE FlyZones ("
						+ "PlayerName VARCHAR(255), "
						+ "WorldName VARCHAR(255), "
						+ "Chunk1_X INT, Chunk1_Z INT, Chunk2_X INT, Chunk2_Z INT,"
						+ "saveTime VARCHAR(255)" + "); ";
				this.SQLite.createTable(query);
			}
			
			FMProtectionPL.flyAreas.clear();
		} catch (Exception e) {
		}
	}

	// SAVE ALL ZONES
	public void saveZones() {
		try {
			String query = "DELETE FROM FlyZones;";
			SQLite.deleteQuery(query);

			for (Map.Entry<String, FMChunkArea> entry : FMProtectionPL.flyAreas
					.entrySet()) {
				addZone(entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
		}
	}

	// LOAD ZONES
	public void loadZones() {
		String query = "Select * FROM FlyZones;";
		ResultSet result = null;
		result = SQLite.sqlQuery(query);
		int loadedZones = 0;
		try {
			while (result != null && result.next()) {
				String playerName = result.getString("PlayerName");
				String worldName = result.getString("WorldName");
				int x1 = result.getInt("Chunk1_X");
				int z1 = result.getInt("Chunk1_Z");
				int x2 = result.getInt("Chunk2_X");
				int z2 = result.getInt("Chunk2_Z");
				long saved = Long.valueOf(result.getString("saveTime"));

				FMChunkArea zone = new FMChunkArea(worldName, x1, z1, x2, z2);
				zone.lastSavedTime = saved;
				zone.empty = false;
				FMProtectionPL.flyAreas.put(playerName, zone);

				loadedZones++;
			}
			log("" + loadedZones + " Flyzones loaded");

		} catch (SQLException e) {
			log("Error while loading FlyZones: ");
			log("");
			e.printStackTrace();
		}
	}

	// GET PLAYER
	public static Player getPlayer(String name) {
		Player[] pList = server.getOnlinePlayers();
		for (Player player : pList) {
			if (player.getName().equalsIgnoreCase(name))
				return player;
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		if (!(sender instanceof Player))
			return true;

		if (!label.equalsIgnoreCase("fly")) {
			return true;
		}

		Player player = (Player) sender;
		if (!UtilPermissions.playerCanUseCommand(player, "fm.area")) {
			player.sendMessage(ChatColor.RED
					+ "[FlyZone] You are not allowed to use that command!");
			return true;
		}

		// CoK-Map
		if (!player.getWorld().getName().equalsIgnoreCase("world")) {
			player.sendMessage(ChatColor.RED
					+ "[FlyZone] FlyZones are not allowed in this world!");
			return true;
		}

		if (args == null)
			return true;

		if (args.length != 1 && args.length != 2) {
			player.sendMessage(ChatColor.RED + "[FlyZone] Wrong Syntax!");
			player.sendMessage(ChatColor.GRAY + "Use: /fly 1 or /fly 2");
			return true;
		}

		if (args[0].equalsIgnoreCase("clearall")) {
			if (player.isOp()) {
				this.clearZones();
				player.sendMessage(ChatColor.GREEN
						+ "[FlyZone] All FlyZones deleted!");
				return true;
			}
		} else if (args[0].equalsIgnoreCase("clear") && args.length == 2) {
			if (player.isOp()) {
				this.removeZone(args[1]);
				player.sendMessage(ChatColor.GREEN + "[FlyZone] Zone of '"
						+ args[1] + "' deleted!");
				return true;
			}
		}

		try {
			if (Integer.valueOf(args[0]) != 1 && Integer.valueOf(args[0]) != 2) {
				player.sendMessage(ChatColor.RED + "[FlyZone] Wrong Syntax!");
				player.sendMessage(ChatColor.GRAY + "Use: /fly 1 or /fly 2");
				return true;
			}
		} catch (Exception e) {
			player.sendMessage(ChatColor.RED + "[FlyZone] Wrong Syntax!");
			player.sendMessage(ChatColor.GRAY + "Use: /fly 1 or /fly 2");
			return true;
		}

		int type = Integer.valueOf(args[0]);

		if (type == 1) {
			FMChunkArea area = new FMChunkArea();
			area.worldName = player.getWorld().getName();
			area.chunk1_x = player.getLocation().getBlock().getChunk().getX();
			area.chunk1_z = player.getLocation().getBlock().getChunk().getZ();
			selections.put(player.getName(), area);
			player.sendMessage(ChatColor.GREEN + "[FlyZone] Point 1 set!");
		} else {
			if (!selections.containsKey(player.getName())) {
				player.sendMessage(ChatColor.RED
						+ "[FlyZone] Use /fly 1 first!");
				return true;
			}

			long oldSaveTime = 0;
			if (FMProtectionPL.flyAreas.containsKey(player.getName())) {
				oldSaveTime = FMProtectionPL.flyAreas.get(player.getName()).lastSavedTime;
			}

			FMChunkArea area = selections.get(player.getName());
			long thisSaveTime = System.currentTimeMillis();
			int hoursToWait = 48;
			long coolDown = 1000 * 60 * 60 * hoursToWait;
			long elapsedTime = thisSaveTime - oldSaveTime;
			long leftTime = coolDown - elapsedTime;

			if (!area.worldName.equalsIgnoreCase(player.getWorld().getName())) {
				player.sendMessage(ChatColor.RED
						+ "[FlyZone] You have selected different worlds!");
				return true;
			}

			if (leftTime >= 0) {
				Date d = new Date(oldSaveTime + coolDown);
				player.sendMessage(ChatColor.RED
						+ "[FlyZone] You have to wait at least " + hoursToWait
						+ " hours to define a new FlyZone!");
				player.sendMessage(ChatColor.GRAY
						+ "Next change available on this date: " + d);
				return true;
			}

			area.chunk2_x = player.getLocation().getBlock().getChunk().getX();
			area.chunk2_z = player.getLocation().getBlock().getChunk().getZ();
			int maxChunkCount = 8;
			if (Math.abs(area.chunk2_x - area.chunk1_x) >= maxChunkCount
					|| Math.abs(area.chunk2_z - area.chunk1_z) >= maxChunkCount) {
				player.sendMessage(ChatColor.RED
						+ "[FlyZone] The selected area is too large!");
				player.sendMessage(ChatColor.GRAY + "Maximum is "
						+ maxChunkCount + "x" + maxChunkCount + " Chunks");
				return true;
			}

			area.empty = false;
			area.lastSavedTime = System.currentTimeMillis();
			area.worldName = player.getLocation().getWorld().getName();
			area.updatePositions();
			FMProtectionPL.flyAreas.put(player.getName(), area);
			addZone(player.getName(), area);
			pListener.addPermission(player.getName(), "nogrief.flymod.use",
					true);
			player.sendMessage(ChatColor.GREEN + "[FlyZone] Area set!");
		}

		return true;
	}

	// SEARCH NOGRIEF-CORE
	private boolean searchNoGrief() {
		PluginManager pm = getServer().getPluginManager();
		if (pm.getPlugin("NoGrief") != null) {
			if (pm.getPlugin("NoGrief").isEnabled()) {
				setCore((NoGriefCore) pm.getPlugin("NoGrief"));
				return true;
			} else {
				// pm.enablePlugin(pm.getPlugin("NoGrief"));
				setCore((NoGriefCore) pm.getPlugin("NoGrief"));
				return true;
			}
		} else {
			return false;
		}
	}

	public static NoGriefCore getCORE() {
		return core;
	}

	public static void setCore(NoGriefCore CORE) {
		core = CORE;
	}
}
