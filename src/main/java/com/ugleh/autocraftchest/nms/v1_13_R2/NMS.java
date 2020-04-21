package com.ugleh.autocraftchest.nms.v1_13_R2;
import com.ugleh.autocraftchest.nms.CraftingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NMS implements CraftingUtil {
    private Object iinventoryObject;

    private Method asNMSCopyMethod;
    private Method asBukkitCopyMethod;
    private Method setItemMethod;
    private Method craftMethod;
    private Object craftManager;
    private Constructor<?> inventorCraftingConstructor;
    private final Class craftInventoryPlayerClass = getBukkitClass("inventory.CraftInventoryPlayer");
    private final Method getHandlePlayerInventory = craftInventoryPlayerClass.getMethod("getInventory");
    private final Class craftWorldClass = getBukkitClass("CraftWorld");
    private final Class worldServerClass = getNMSClass("WorldServer");
    private final Class playerInventoryClass = getNMSClass("PlayerInventory");
    private final Class blockPositionClass = getNMSClass("BlockPosition");
    private final Method getHandleWorldServer = craftWorldClass.getMethod("getHandle");
    private final Method getMinecraftWorld = worldServerClass.getMethod("getMinecraftWorld");
    private final Class<?> worldClass = getNMSClass("World");

    public NMS()throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        Class<?> iiInventoryClass = getNMSClass("IInventory");
        final Class<?> itemStackClass = getNMSClass("ItemStack");
        final Class<?> craftItemStackClass = getBukkitClass("inventory.CraftItemStack");
        final Class<?> containerClass = getNMSClass("Container");
        final Class<?> inventoryCraftingClass = getNMSClass("InventoryCrafting");
        inventorCraftingConstructor = inventoryCraftingClass.getConstructor(containerClass, int.class, int.class);

        setItemMethod = iiInventoryClass.getMethod("setItem", int.class, itemStackClass);
        setItemMethod.setAccessible(true);
        asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", itemStackClass);


        final Method getHandle = Bukkit.getServer().getClass().getMethod("getHandle");
        Object craftServer = getHandle.invoke(Bukkit.getServer());

        final Method getHandle2 = craftServer.getClass().getMethod("getServer");
        Object dedicatedServer = getHandle2.invoke(craftServer);

        final Method getHandle3 = dedicatedServer.getClass().getMethod("getCraftingManager");
        craftManager = getHandle3.invoke(dedicatedServer);
        craftMethod = craftManager.getClass().getMethod("craft", iiInventoryClass, worldClass);
    }

    @Override
    public void setContainer(Player player) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchMethodException {
        Object craftInventoryPlayer = craftInventoryPlayerClass.cast(player.getInventory());
        Object playerInventory = getHandlePlayerInventory.invoke(craftInventoryPlayer);
        Object craftWorld = craftWorldClass.cast(player.getWorld());
        Object worldServer = getHandleWorldServer.invoke(craftWorld);
        Object world = getMinecraftWorld.invoke(worldServer);
        Constructor containerWorkbench = getNMSClass("ContainerWorkbench").getConstructor(playerInventoryClass, worldClass, blockPositionClass);
        Object container = containerWorkbench.newInstance(playerInventoryClass.cast(playerInventory), world, null);
        iinventoryObject = inventorCraftingConstructor.newInstance(container, 3, 3);
    }

    @Override
    public ItemStack getResult(org.bukkit.inventory.ItemStack[] contents) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        for (int i = 0; i < contents.length; i++) {

            setItemMethod.invoke(iinventoryObject, i, asNMSCopyMethod.invoke(null, contents[i]));
        }
        return tryCraft(iinventoryObject);
    }

    private ItemStack tryCraft(Object inventoryCrafting) throws InvocationTargetException, IllegalAccessException {
        Object nmsITemStack = craftMethod.invoke(craftManager, inventoryCrafting, null);
        return (ItemStack) asBukkitCopyMethod.invoke(null, nmsITemStack);
    }

    private Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "net.minecraft.server." + version + nmsClassString;
        return Class.forName(name);
    }

    private Class<?> getBukkitClass(String bukkitClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "org.bukkit.craftbukkit." + version + bukkitClassString;
        return Class.forName(name);
    }

}
