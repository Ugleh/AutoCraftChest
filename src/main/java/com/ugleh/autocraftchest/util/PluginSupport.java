package com.ugleh.autocraftchest.util;

import nl.rutgerkok.blocklocker.BlockLockerAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PluginSupport {
    private HashMap<Plugin, Method> supportMap = new HashMap<>();

    public PluginSupport() {
        //BlockLocker
        if(Bukkit.getPluginManager().getPlugin("BlockLocker") != null) {
            try {
                supportMap.put(Bukkit.getPluginManager().getPlugin("BlockLocker"), this.getClass().getMethod("blockLockerIsAllowedToInteract", ACC.class, OfflinePlayer.class));
            } catch (NoSuchMethodException ignored) {

            }
        }
    }

    public boolean blockLockerIsAllowedToInteract(ACC acc, OfflinePlayer player) {
        return BlockLockerAPI.isAllowed(player.getPlayer(), acc.getChest().getBlock(), true);
    }

    public boolean canInteract(ACC acc, OfflinePlayer player) throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<Plugin, Method> pluginMethodEntry : supportMap.entrySet()) {
            boolean aBoolean = (boolean) pluginMethodEntry.getValue().invoke(this, acc, player);
            if(!aBoolean) { //As long as 1 dependable plugin returns false the method will return false, otherwise return true.
                return false;
            }
        }
        return true;
    }
}
