package com.ugleh.autocraftchest.nms;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public interface CraftingUtil {

    ItemStack getResult(ItemStack[] contents) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, NoSuchFieldException;
    void setContainer(Player player) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchMethodException;
}
