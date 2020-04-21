package com.ugleh.autocraftchest.nms.v1_12_R1;
import com.ugleh.autocraftchest.nms.CraftingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public class NMS implements CraftingUtil {
    private Object iinventoryObject;

    private Constructor<?> inventorCraftingConstructor;
    private final Class craftInventoryPlayerClass = getBukkitClass("inventory.CraftInventoryPlayer");
    private final Method getHandlePlayerInventory = craftInventoryPlayerClass.getMethod("getInventory");
    private final Class craftWorldClass = getBukkitClass("CraftWorld");
    private final Class playerInventoryClass = getNMSClass("PlayerInventory");
    private final Class blockPositionClass = getNMSClass("BlockPosition");
    private final Class<?> worldClass = getNMSClass("World");
    private final Class<?> craftItemStackClass = getBukkitClass("inventory.CraftItemStack");
    private final Class<?> inventoryCraftingClass = getNMSClass("InventoryCrafting");
    private final Class<?> itemStackClass = getNMSClass("ItemStack");

    private final Method getHandleWorldServer = craftWorldClass.getMethod("getHandle");
    private final Method asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
    private final Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", itemStackClass);
    private final Method setItemMethod = inventoryCraftingClass.getMethod("setItem", int.class, itemStackClass);
    private Method craftMethod;

    public NMS()throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException {
        final Class<?> containerClass = getNMSClass("Container");
        final Class<?> inventoryCraftingClass = getNMSClass("InventoryCrafting");
        inventorCraftingConstructor = inventoryCraftingClass.getConstructor(containerClass, int.class, int.class);

        setItemMethod.setAccessible(true);
        craftMethod = getNMSClass("CraftingManager").getMethod("craft", this.inventoryCraftingClass, worldClass);
    }

    @Override
    public void setContainer(Player player) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchMethodException {
        Object craftInventoryPlayer = craftInventoryPlayerClass.cast(player.getInventory());
        Object playerInventory = getHandlePlayerInventory.invoke(craftInventoryPlayer);
        Object craftWorld = craftWorldClass.cast(player.getWorld());
        Object worldServer = getHandleWorldServer.invoke(craftWorld);
        Constructor containerWorkbench = getNMSClass("ContainerWorkbench").getConstructor(playerInventoryClass, worldClass, blockPositionClass);
        Object container = containerWorkbench.newInstance(playerInventoryClass.cast(playerInventory), worldServer, null);
        iinventoryObject = inventorCraftingConstructor.newInstance(container, 3, 3);
    }

    @Override
    public ItemStack getResult(org.bukkit.inventory.ItemStack[] contents) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {

        for (int i = 0; i < contents.length; i++) {

            setItemMethod.invoke(iinventoryObject, i, asNMSCopyMethod.invoke(null, contents[i]));
        }
        return tryCraft(iinventoryObject);
    }

    protected ItemStack tryCraft(Object inventoryCrafting) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        Object nmsItemStack = craftMethod.invoke(null, inventoryCrafting, null);

        return (ItemStack) asBukkitCopyMethod.invoke(null, nmsItemStack);
    }

    protected Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "net.minecraft.server." + version + nmsClassString;
        return Class.forName(name);
    }

    protected Class<?> getBukkitClass(String bukkitClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "org.bukkit.craftbukkit." + version + bukkitClassString;
        return Class.forName(name);
    }
}
