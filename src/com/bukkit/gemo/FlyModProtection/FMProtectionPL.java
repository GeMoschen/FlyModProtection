package com.bukkit.gemo.FlyModProtection;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import com.bukkit.gemo.NoGrief.NoGriefCore;
import com.gemo.utils.BlockUtils;
import com.gemo.utils.UtilPermissions;

public class FMProtectionPL extends PlayerListener {
	/************************/
	/** VARS */
	/************************/
	public static HashMap<String, FMChunkArea> flyAreas;
	public static HashMap<String, FMChunk> currentChunks;

	// ///////////////////////////////////
	//
	// CONSTRUCTOR
	//
	// ///////////////////////////////////
	public FMProtectionPL() {
		flyAreas = new HashMap<String, FMChunkArea>();
		currentChunks = new HashMap<String, FMChunk>();
	}

	// GET PLAYER
	public static Player getPlayer(String name) {
		Player[] pList = FMProtectionCore.server.getOnlinePlayers();
		for (Player player : pList) {
			if (player.getName().equalsIgnoreCase(name))
				return player;
		}
		return null;
	}

	// SEND TO ADMINS
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

		if (UtilPermissions.getGroupName(player).equalsIgnoreCase("admins"))
			return;

		// IN SPACEMAP = ADD PERMISSION
		if (event.getTo().getWorld().getName().equalsIgnoreCase("space")
				&& !UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
			addPermission(player.getName(), "nogrief.flymod.use", false);
			return;
		}

		// SAME BLOCK = RETURN
		if (BlockUtils.LocationEquals(event.getFrom(), event.getTo())) {
			return;
		}

		// NOT A PAYUSER = REMOVE PERMISSION
		if (!UtilPermissions.playerCanUseCommand(player, "fm.area")) {
			if (UtilPermissions.playerCanUseCommand(player,
					"nogrief.flymod.use")) {
				removePermission(player.getName(), "nogrief.flymod.use", false);
				return;
			}
		}

		// GET NEW CHUNK
		Chunk chunk = event.getTo().getBlock().getChunk();

		if (!currentChunks.containsKey(player.getName()))
			currentChunks.put(player.getName(), new FMChunk(chunk));

		// SAME CHUNK = RETURN
		if (currentChunks.get(player.getName()).isInChunk(chunk))
			return;

		// UPDATE CURRENT CHUNK
		currentChunks.put(player.getName(), new FMChunk(chunk));

		if (!flyAreas.containsKey(player.getName())) {
			// NO FLY AREA DEFINED = REMOVE PERMISSION
			if (UtilPermissions.playerCanUseCommand(player,
					"nogrief.flymod.use")) {
				removePermission(player.getName(), "nogrief.flymod.use", false);
				return;
			}
		} else {
			// AREA DEFINED = HANDLE MOVEMENT
			// NOT IN AREA = REMOVE PERMISSION
			if (!flyAreas.get(player.getName()).isInArea(chunk)) {
				if (UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
					removePermission(player.getName(), "nogrief.flymod.use",
							true);
					return;
				}
			} else {
				// IN AREA = ADD PERMISSION
				if (!UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
					addPermission(player.getName(), "nogrief.flymod.use", true);
					return;
				}
			}
		}
	}

	// REMOVE PERMISSION
	public void removePermission(String playerName, String node, boolean showMSG) {
		ConsoleCommandSender sender = new ConsoleCommandSender(
				FMProtectionCore.server);
		FMProtectionCore.server.dispatchCommand(sender, "manselect world");

		FMProtectionCore.server.dispatchCommand(sender, "manudelp "
				+ playerName + " nogrief.flymod.use");

		if (showMSG)
			getPlayer(playerName).sendMessage(
					ChatColor.AQUA + "[FlyZone] " + ChatColor.RED
							+ "You have left your Flymod-Zone!");
	}

	// ADD PERMISSION
	public void addPermission(String playerName, String node, boolean showMSG) {
		ConsoleCommandSender sender = new ConsoleCommandSender(
				FMProtectionCore.server);
		FMProtectionCore.server.dispatchCommand(sender, "manselect world");

		FMProtectionCore.server.dispatchCommand(sender, "manuaddp "
				+ playerName + " nogrief.flymod.use");

		if (showMSG)
			getPlayer(playerName).sendMessage(
					ChatColor.AQUA + "[FlyZone] " + ChatColor.GREEN
							+ "You have entered your Flymod-Zone!");
	}

	// ///////////////////////////////////
	//
	// RESET METHODS
	//
	// ///////////////////////////////////
	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		if (UtilPermissions.getGroupName(player).equalsIgnoreCase("admins"))
			return;
		
		// IN SPACEMAP = ADD PERMISSION
		if (event.getRespawnLocation().getWorld().getName()
				.equalsIgnoreCase("space")
				&& !UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
			addPermission(player.getName(), "nogrief.flymod.use", false);
			return;
		}

		// GET NEW CHUNK
		Chunk chunk = event.getRespawnLocation().getBlock().getChunk();
		if (!currentChunks.containsKey(player.getName()))
			currentChunks.put(player.getName(), new FMChunk(chunk));

		// SAME CHUNK = RETURN
		if (currentChunks.get(player.getName()).isInChunk(chunk))
			return;

		currentChunks.put(player.getName(), new FMChunk(chunk));

		if (!flyAreas.containsKey(player.getName())) {
			// NO FLY AREA DEFINED = REMOVE PERMISSION
			if (UtilPermissions.playerCanUseCommand(player,
					"nogrief.flymod.use")) {
				removePermission(player.getName(), "nogrief.flymod.use", false);
				return;
			}
		} else {
			// AREA DEFINED = HANDLE MOVEMENT
			// NOT IN AREA = REMOVE PERMISSION
			if (!flyAreas.get(player.getName()).isInArea(chunk)) {
				if (UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
					removePermission(player.getName(), "nogrief.flymod.use",
							false);
					return;
				}
			} else {
				// IN AREA = ADD PERMISSION
				if (!UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
					addPermission(player.getName(), "nogrief.flymod.use", false);
					return;
				}
			}
		}
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		// TP TO SPACE
		Player player = event.getPlayer();
		if (UtilPermissions.getGroupName(player).equalsIgnoreCase("admins"))
			return;
		
		if (event.getTo().getWorld().getName().equalsIgnoreCase("space")
				&& !event.getFrom().getWorld().getName()
						.equalsIgnoreCase("space")) {
			if (!UtilPermissions.playerCanUseCommand(player,
					"nogrief.flymod.use")) {
				addPermission(player.getName(), "nogrief.flymod.use", false);
			}
		} else if (!event.getTo().getWorld().getName()
				.equalsIgnoreCase("space"))
		// TP TO SOMEWHERE ELSE
		{
			if (flyAreas.containsKey(player.getName())) {
				if (UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
					if (!flyAreas.get(player.getName()).isInArea(
							event.getTo().getBlock().getChunk())) {
						if (event.getFrom().getWorld().getName()
								.equalsIgnoreCase("space"))
							removePermission(player.getName(),
									"nogrief.flymod.use", false);
						else
							removePermission(player.getName(),
									"nogrief.flymod.use", true);

					}
				} else {
					if (flyAreas.get(player.getName()).isInArea(
							event.getTo().getBlock().getChunk())) {
						addPermission(player.getName(), "nogrief.flymod.use",
								true);
					}
				}
			} else {
				if (UtilPermissions.playerCanUseCommand(player,
						"nogrief.flymod.use")) {
					if (event.getFrom().getWorld().getName()
							.equalsIgnoreCase("space"))
						removePermission(player.getName(),
								"nogrief.flymod.use", false);
					else
						removePermission(player.getName(),
								"nogrief.flymod.use", true);
				}
			}
		}
	}
}
