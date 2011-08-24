package com.bukkit.gemo.FlyModProtection;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.bukkit.gemo.NoGrief.NoGriefCore;
import com.gemo.utils.UtilPermissions;

public class FMProtectionCore extends JavaPlugin {
	private static String pluginName = "FMProtection";
	private static Logger log = Logger.getLogger("Minecraft");
	public static Server server = null;

	/** LISTENER */
	private static NoGriefCore core;
	public static FMProtectionPL pListener;
	
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
		}
	}

	// ON ENABLE
	@Override
	public void onEnable() {
		server = getServer();

		if (searchNoGrief()) {
			// REGISTER EVENT
			pListener = new FMProtectionPL();

			PluginManager pm = server.getPluginManager();

			pm.registerEvent(Event.Type.PLAYER_MOVE, pListener,
					Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_RESPAWN, pListener,
					Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_TELEPORT, pListener,
					Event.Priority.Normal, this);

			log("enabled");

		} else {
			log("NoGrief was not found! AntiFlyMod is disabled!");
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
		
		if(!(sender instanceof Player))
			return true;
		
		if(!label.equalsIgnoreCase("fly"))
		{
			return true;
		}
		
		Player player = (Player)sender;
		if(!UtilPermissions.playerCanUseCommand(player, "fm.area"))
		{
			player.sendMessage(ChatColor.RED + "[FlyZone] You are not allowed to use that command!");
			return true;
		}
		
		if(args == null)
			return true;
		
		if(args.length != 1)
		{
			player.sendMessage(ChatColor.RED + "[FlyZone] Wrong Syntax!");
			player.sendMessage(ChatColor.GRAY + "Use: /fly 1 or /fly 2");
			return true;
		}
		
		try
		{
			if(Integer.valueOf(args[0]) != 1 && Integer.valueOf(args[0]) != 2)
			{
				player.sendMessage(ChatColor.RED + "[FlyZone] Wrong Syntax!");
				player.sendMessage(ChatColor.GRAY + "Use: /fly 1 or /fly 2");
				return true;
			}		
		}
		catch(Exception e)
		{
			player.sendMessage(ChatColor.RED + "[FlyZone] Wrong Syntax!");
			player.sendMessage(ChatColor.GRAY + "Use: /fly 1 or /fly 2");
			return true;
		}
		
		int type = Integer.valueOf(args[0]);
		
		if(type == 1)
		{
			FMChunkArea area = new FMChunkArea();
			area.chunk1_x = player.getLocation().getBlock().getChunk().getX();
			area.chunk1_z = player.getLocation().getBlock().getChunk().getZ();
			selections.put(player.getName(), area);		
			player.sendMessage(ChatColor.GREEN + "[FlyZone] Point 1 set!");		
		}
		else
		{
			if(!selections.containsKey(player.getName()))
			{
				player.sendMessage(ChatColor.RED + "[FlyZone] Use /fly 1 first!");
				return true;
			}
			
			long oldSaveTime = 0;						
			if(FMProtectionPL.flyAreas.containsKey(player.getName()))
			{
				oldSaveTime = FMProtectionPL.flyAreas.get(player.getName()).lastSavedTime;
			}
			
			FMChunkArea area = selections.get(player.getName());
			long thisSaveTime = System.currentTimeMillis();			
			int hoursToWait = 48;
			long coolDown = 1000 * 60 * 60 * hoursToWait;			
			long elapsedTime = thisSaveTime - oldSaveTime;
			long leftTime = coolDown - elapsedTime;			
			
			if(leftTime >= 0)
			{	
				int totalSecs = (int) (leftTime / 1000);			
				int leftH = (int) Math.floor(totalSecs / (60 * 60));
				int leftM = (int) ((totalSecs - (leftH * 1000*60)) / 60);
				int leftS = (int) (totalSecs - (leftH * 1000*60)) % 60;
				
				String waitString = leftH + " hours " + leftM + " minutes " + leftS + " seconds";
			
				player.sendMessage(ChatColor.RED + "[FlyZone] You have to wait at least " + hoursToWait + " hours to define a new FlyZone!");
				player.sendMessage(ChatColor.GRAY + waitString + " to wait.");				
				return true;
			}		
				
			
			area.chunk2_x = player.getLocation().getBlock().getChunk().getX();
			area.chunk2_z = player.getLocation().getBlock().getChunk().getZ();
			int maxChunkCount = 5;
			if(Math.abs(area.chunk2_x - area.chunk1_x) >= maxChunkCount || Math.abs(area.chunk2_z - area.chunk1_z) >= maxChunkCount)
			{
				player.sendMessage(ChatColor.RED + "[FlyZone] The selected area is too large!");
				player.sendMessage(ChatColor.GRAY + "Maximum is " + maxChunkCount  + "x" + maxChunkCount  + " Chunks");				
				return true;
			}
			
			area.empty = false;
			area.lastSavedTime = System.currentTimeMillis();			
			area.worldName = player.getLocation().getWorld().getName();
			area.updatePositions();
			FMProtectionPL.flyAreas.put(player.getName(), area);
			player.sendMessage(ChatColor.GREEN + "[FlyZone] Area set!");
			pListener.addPermission(player.getName(), "nogrief.flymod.use", true);
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
