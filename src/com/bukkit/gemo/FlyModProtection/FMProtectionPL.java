package com.bukkit.gemo.FlyModProtection;

import java.util.HashMap;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.command.ColouredConsoleSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.bukkit.gemo.utils.UtilPermissions;

public class FMProtectionPL implements Listener {
    /************************/
    /** VARS */
    /************************/
    public HashMap<String, FMChunkArea> flyAreas;
    private HashMap<String, FMChunk> currentChunks;
    private HashMap<String, Boolean> inZone;
    private static ColouredConsoleSender console;

    private static TreeMap<String, Long> timeMap = new TreeMap<String, Long>();

    // ///////////////////////////////////
    //
    // CONSTRUCTOR
    //
    // ///////////////////////////////////
    public FMProtectionPL() {
        flyAreas = new HashMap<String, FMChunkArea>();
        currentChunks = new HashMap<String, FMChunk>();
        inZone = new HashMap<String, Boolean>();

        try {
            console = (ColouredConsoleSender) FMProtectionCore.server.getConsoleSender();
        } catch (Exception e) {
        }

        long time = System.currentTimeMillis();
        for (Player p : FMProtectionCore.server.getOnlinePlayers()) {
            long extTime = time;
            extTime += (Math.random() * (3000 - 1000));
            timeMap.put(p.getName(), extTime);
        }
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
        Player[] pList = Bukkit.getOnlinePlayers();
        for (int i = 0; i < pList.length; i++) {
            if (UtilPermissions.getGroupName(pList[i]).equalsIgnoreCase("admins") || UtilPermissions.playerCanUseCommand(pList[i], "nogrief.flymod.showmessage")) {
                pList[i].sendMessage(message);
            }
        }
    }

    // ///////////////////////////////////
    //
    // ON MOVE
    //
    // ///////////////////////////////////
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isOp())
            return;

        // CHECK TIME
        if (System.currentTimeMillis() <= timeMap.get(player.getName())) {
            return;
        }

        // CHECK GROUP
        String groupName = UtilPermissions.getGroupName(player);
        if (groupName.equalsIgnoreCase("vip") || groupName.equalsIgnoreCase("default") || groupName.equalsIgnoreCase("probe")) {
            if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                removePermission(player.getName(), "nogrief.flymod.use", false);
                UtilPermissions.forcePermissionUpdate(player, "nogrief.flymod.use");
            }
            return;
        }

        // CREATE NEW CHECKTIME
        long time = System.currentTimeMillis();
        long extTime = time;
        extTime += (Math.random() * (500 - 1500));
        timeMap.put(player.getName(), extTime);

        // IN SPACEMAP = RETURN
        if (event.getTo().getWorld().getName().equalsIgnoreCase("space"))
            return;

        // GET NEW CHUNK
        Chunk chunk = event.getTo().getBlock().getChunk();
        if (!currentChunks.containsKey(player.getName())) {
            currentChunks.put(player.getName(), new FMChunk(chunk));
        }

        // SAME CHUNK = RETURN
        if (currentChunks.get(player.getName()).isInChunk(chunk)) {
            return;
        }

        // UPDATE CURRENT CHUNK
        currentChunks.put(player.getName(), new FMChunk(chunk));
        if (!flyAreas.containsKey(player.getName())) {
            // NO FLY AREA DEFINED = REMOVE PERMISSION
            if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                removePermission(player.getName(), "nogrief.flymod.use", false);
                UtilPermissions.forcePermissionUpdate(player, "nogrief.flymod.use");
                return;
            }
        } else {
            // AREA DEFINED = HANDLE MOVEMENT
            // NOT IN AREA = REMOVE PERMISSION
            if (!flyAreas.get(player.getName()).isInArea(chunk)) {
                if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                    removePermission(player.getName(), "nogrief.flymod.use", true);
                    UtilPermissions.forcePermissionUpdate(player, "nogrief.flymod.use");
                    return;
                }
            } else {
                // IN AREA = ADD PERMISSION
                if (!UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                    addPermission(player.getName(), "nogrief.flymod.use", true);
                    UtilPermissions.forcePermissionUpdate(player, "nogrief.flymod.use");
                    return;
                }
            }
        }
    }

    // REMOVE PERMISSION
    public void removePermission(String playerName, String node, boolean showMSG) {
        FMProtectionCore.server.dispatchCommand(console, "manselect world");
        FMProtectionCore.server.dispatchCommand(console, "manudelp " + playerName + " nogrief.flymod.use");

        if (showMSG) {
            if (inZone.containsKey(playerName)) {
                if (inZone.get(playerName)) {
                    getPlayer(playerName).sendMessage(ChatColor.AQUA + "[FlyZone] " + ChatColor.RED + "You have left your Flymod-Zone!");
                    inZone.put(playerName, false);
                    return;
                }
            }
            getPlayer(playerName).sendMessage(ChatColor.AQUA + "[FlyZone] " + ChatColor.RED + "You have left your Flymod-Zone!");
            inZone.put(playerName, false);
        }
    }

    // ADD PERMISSION
    public void addPermission(String playerName, String node, boolean showMSG) {
        FMProtectionCore.server.dispatchCommand(console, "manselect world");
        FMProtectionCore.server.dispatchCommand(console, "manuaddp " + playerName + " nogrief.flymod.use");

        if (showMSG)
            getPlayer(playerName).sendMessage(ChatColor.AQUA + "[FlyZone] " + ChatColor.GREEN + "You have entered your Flymod-Zone!");

        inZone.put(playerName, true);
    }

    // ///////////////////////////////////
    //
    // RESET METHODS
    //
    // ///////////////////////////////////
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (UtilPermissions.getGroupName(player).equalsIgnoreCase("admins"))
            return;

        // IN SPACEMAP = RETURN
        if (event.getRespawnLocation().getWorld().getName().equalsIgnoreCase("space"))
            return;

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
            if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                removePermission(player.getName(), "nogrief.flymod.use", false);
                return;
            }
        } else {
            // AREA DEFINED = HANDLE MOVEMENT
            // NOT IN AREA = REMOVE PERMISSION
            if (!flyAreas.get(player.getName()).isInArea(chunk)) {
                if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                    removePermission(player.getName(), "nogrief.flymod.use", false);
                    return;
                }
            } else {
                // IN AREA = ADD PERMISSION
                if (!UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                    addPermission(player.getName(), "nogrief.flymod.use", false);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        long time = System.currentTimeMillis();
        long extTime = time;
        extTime += (Math.random() * (3000 - 1000));
        timeMap.put(event.getPlayer().getName(), extTime);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        timeMap.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // TP TO SPACE
        Player player = event.getPlayer();

        // IN SPACEMAP = RETURN
        if (event.getTo().getWorld().getName().equalsIgnoreCase("space"))
            return;

        if (UtilPermissions.getGroupName(player).equalsIgnoreCase("admins"))
            return;

        if (flyAreas.containsKey(player.getName())) {
            if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                if (!flyAreas.get(player.getName()).isInArea(event.getTo().getBlock().getChunk())) {
                    if (event.getFrom().getWorld().getName().equalsIgnoreCase("space"))
                        removePermission(player.getName(), "nogrief.flymod.use", false);
                    else
                        removePermission(player.getName(), "nogrief.flymod.use", true);

                }
            } else {
                if (flyAreas.get(player.getName()).isInArea(event.getTo().getBlock().getChunk())) {
                    addPermission(player.getName(), "nogrief.flymod.use", true);
                }
            }
        } else {
            if (UtilPermissions.playerCanUseCommand(player, "nogrief.flymod.use")) {
                if (event.getFrom().getWorld().getName().equalsIgnoreCase("space"))
                    removePermission(player.getName(), "nogrief.flymod.use", false);
                else
                    removePermission(player.getName(), "nogrief.flymod.use", true);
            }
        }
    }
}
