package com.bukkit.gemo.AntiFlyMod;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import com.bukkit.gemo.NoGrief.NoGriefCore;
import com.gemo.utils.UtilPermissions;

public class AntiFlyModPL extends PlayerListener {
	/************************/
	/** VARS */
	/************************/
	public static HashMap<String, Integer> PlayerSprintCount;
	public static HashMap<String, Integer> PlayerFlyCount;
	public static HashMap<String, Location> lastSaveLocations;

	// ///////////////////////////////////
	//
	// CONSTRUCTOR
	//
	// ///////////////////////////////////
	public AntiFlyModPL() {
		PlayerFlyCount = new HashMap<String, Integer>();
		PlayerSprintCount = new HashMap<String, Integer>();
		lastSaveLocations = new HashMap<String, Location>();
	}

	public void sendToAdmins(String message) {
		Player[] pList = NoGriefCore.server.getOnlinePlayers();
		for (int i = 0; i < pList.length; i++) {
			if (UtilPermissions.getGroupName(pList[i]).equalsIgnoreCase(
					"admins")
					|| UtilPermissions.playerCanUseCommand(pList[i],
							"nogrief.flymod.showmessage")) {
				pList[i].sendMessage(message);
			}
		}
	}

	// ///////////////////////////////////
	//
	// ON MOVE
	//
	// ///////////////////////////////////
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		
		// NOT IN SPACEMAP
		if(player.getWorld().getName().equalsIgnoreCase("space") || UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use"))
		{
			
		}
		
		
		/*
		boolean onLadder = block.getTypeId() == Material.LADDER.getId();
		 
		boolean onHalfblock = !block.isEmpty();
		boolean onGround = !block.getRelative(BlockFace.DOWN).isEmpty();

		Block beneath = block.getRelative(BlockFace.DOWN);
		Block[] blocks = { beneath, beneath.getRelative(BlockFace.NORTH),
				beneath.getRelative(BlockFace.NORTH_EAST),
				beneath.getRelative(BlockFace.EAST),
				beneath.getRelative(BlockFace.SOUTH_EAST),
				beneath.getRelative(BlockFace.SOUTH),
				beneath.getRelative(BlockFace.SOUTH_WEST),
				beneath.getRelative(BlockFace.WEST),
				beneath.getRelative(BlockFace.NORTH_WEST), };

		for (Block b : blocks) {
			if (!b.isEmpty()) {
				onGround = true;
				break;
			}
		}

		boolean inLiquid = block.isLiquid()
				|| block.getRelative(BlockFace.DOWN).isLiquid();
		boolean isFalling = player.getFallDistance() > 0;
		boolean isFlying = !(onLadder || inLiquid || onGround || isFalling || onHalfblock);

		String status = "";
		if (onLadder) {
			status = "On ladder";
		} else if (inLiquid) {
			status = "Swimming";
		} else if (onHalfblock) {
			status = "On Halfblock";
		} else if (onGround) {
			status = "On Ground";
		} else if (isFalling) {
			status = "Falling";
		} else if (isFlying)
			status = "fly/jump";

		// System.out.println(status.toUpperCase());

		if (event.getTo().getBlockY() > 127 || event.getTo().getBlockY() < 0) {
			if (lastSaveLocations.containsKey(player.getName())) {
				player.teleport(lastSaveLocations.get(player.getName()));
			}
		}

		if (isFlying) {
			int oldVal = 0;
			if (PlayerFlyCount.containsKey(player.getName())) {
				oldVal = PlayerFlyCount.get(player.getName());
			}
			oldVal++;

			if (oldVal > 10) {
				if (lastSaveLocations.containsKey(player.getName())) {
					player.teleport(lastSaveLocations.get(player.getName()));
				}
				
				this.sendToAdmins(ChatColor.RED + "User '" + player.getName() + "' is using unallowed Flymod!");
				player.sendMessage(ChatColor.RED + "You are not allowed to use Flymod!");
				
				oldVal = 0;
			}

			// System.out.println("oldVal: " + oldVal);

			PlayerFlyCount.put(player.getName(), oldVal);
		} else {
			lastSaveLocations.put(player.getName(), event.getTo());
			PlayerFlyCount.put(player.getName(), 0);
		}	
		*/	
	}

	// ///////////////////////////////////
	//
	// RESET METHODS
	//
	// ///////////////////////////////////

	@Override
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		this.clearLists(event.getPlayer().getName());
	}

	@Override
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		this.clearLists(event.getPlayer().getName());
	}

	@Override
	public void onPlayerPortal(PlayerPortalEvent event) {
		this.clearLists(event.getPlayer().getName());
	}

	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		this.clearLists(event.getPlayer().getName());
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		this.clearLists(event.getPlayer().getName());
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.clearLists(event.getPlayer().getName());
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		PlayerFlyCount.remove(event.getPlayer().getName());
		PlayerSprintCount.remove(event.getPlayer().getName());
	}

	// ///////////////////////////////////
	//
	// CLEAR LIST
	//
	// ///////////////////////////////////
	public void clearLists(String playerName) {
		PlayerFlyCount.put(playerName, 0);
		PlayerSprintCount.put(playerName, 0);
	}
}
