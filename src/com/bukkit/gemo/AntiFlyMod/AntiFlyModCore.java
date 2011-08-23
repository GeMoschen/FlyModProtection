package com.bukkit.gemo.AntiFlyMod;

import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.bukkit.gemo.NoGrief.NoGriefCore;

public class AntiFlyModCore extends JavaPlugin
{
	private static String pluginName = "AntiFlyMod";
	private static Logger log = Logger.getLogger("Minecraft");
	public static Server server = null;
	

	/** LISTENER */
	private static NoGriefCore core;
	public static AntiFlyModPL pListener;	
	public static AntiFlyModEL eListener;
	
	// AUSGABE IN DER CONSOLE
	public static void log(String str)
	{
		log.info("[ " + pluginName + " ]: " + str);
	}
	
	// ON DISABLE
	@Override
	public void onDisable()
	{
		if(searchNoGrief())
		{
			log("disabled");
		}
	}

	// ON ENABLE
	@Override
	public void onEnable()
	{		
		server = getServer();
		
		if(searchNoGrief())
		{		
			// REGISTER EVENT
			eListener = new AntiFlyModEL();
			pListener = new AntiFlyModPL();
			
			PluginManager pm = server.getPluginManager();
			pm.registerEvent(Event.Type.ENTITY_DEATH, eListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.ENTITY_DAMAGE, eListener, Event.Priority.Normal, this);
			
			pm.registerEvent(Event.Type.PLAYER_BED_ENTER, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_BED_LEAVE, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_JOIN, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_MOVE, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_PORTAL, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_QUIT, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_RESPAWN, pListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_TELEPORT, pListener, Event.Priority.Normal, this);
			
			log("enabled");
		  
		}
		else
		{
			log("NoGrief was not found! AntiFlyMod is disabled!");
		}
	}
		
	// GET PLAYER
	public static Player getPlayer(String name)
	{
		Player[] pList = server.getOnlinePlayers();
		for(Player player : pList)
		{
			if(player.getName().equalsIgnoreCase(name))
				return player;
		}		
		return null;
	}

	
	// SEARCH NOGRIEF-CORE
	private boolean searchNoGrief()
	{
		PluginManager pm = getServer().getPluginManager();		
		if(pm.getPlugin("NoGrief") != null)
		{
			if(pm.getPlugin("NoGrief").isEnabled())
			{
				setCore((NoGriefCore)pm.getPlugin("NoGrief")); 
				return true;
			}
			else
			{
				//pm.enablePlugin(pm.getPlugin("NoGrief"));
				setCore((NoGriefCore)pm.getPlugin("NoGrief"));
				return true;
			}
		}
		else
		{
			return false;
		}		
	}

	public static NoGriefCore getCORE()
	{
		return core;
	}

	public static void setCore(NoGriefCore CORE)
	{
		core = CORE;
	}
	
	
	
}
