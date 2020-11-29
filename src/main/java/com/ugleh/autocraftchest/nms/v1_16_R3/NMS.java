package com.ugleh.autocraftchest.nms.v1_16_R3;
import com.ugleh.autocraftchest.nms.CraftingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class NMS implements CraftingUtil {
    private Object inventoryCrafting;
    private Class<?> iiInventoryClass;
    private Class<?> recipeCraftingClass;

    private Method asNMSCopyMethod;
    private Method asBukkitCopyMethod;
    private Method setItemMethod;
    private Method craftMethod;
    private Object craftManager;
    private Object recipeType;

    public NMS()throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        iiInventoryClass = getNMSClass("IInventory");
        final Class<?> itemStackClass = getNMSClass("ItemStack");
        final Class<?> craftItemStackClass = getBukkitClass("inventory.CraftItemStack");
        recipeCraftingClass = getNMSClass("RecipeCrafting");
        final Class<?> worldClass = getNMSClass("World");
        final Class<?> recipeClass = getNMSClass("Recipes");

        final Class<?> playerInventoryClass = getNMSClass("PlayerInventory");
        final Class<?> containerWorkbenchClass = getNMSClass("ContainerWorkbench");
        final Constructor<?> containerWorkbenchConstructor = containerWorkbenchClass.getConstructor(int.class, playerInventoryClass);
        final Class<?> entityHumanClass = getNMSClass("EntityHuman");
        final Constructor<?> playerInventoryConstructor = playerInventoryClass.getConstructor(entityHumanClass);
        Object containerWorkbench = containerWorkbenchConstructor.newInstance(-1, playerInventoryConstructor.newInstance(entityHumanClass.cast(null)));

        final Class<?> containerClass = getNMSClass("Container");
        final Class<?> inventoryCraftingClass = getNMSClass("InventoryCrafting");
        final Constructor<?> inventorCraftingConstructor = inventoryCraftingClass.getConstructor(containerClass, int.class, int.class, entityHumanClass);
        inventoryCrafting = inventorCraftingConstructor.newInstance(containerWorkbench, 3, 3, null);
        setItemMethod = inventoryCrafting.getClass().getMethod("setItem", int.class, itemStackClass);
        asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", itemStackClass);


        final Method getHandle = Bukkit.getServer().getClass().getMethod("getHandle");
        Object craftServer = getHandle.invoke(Bukkit.getServer());

        final Method getHandle2 = craftServer.getClass().getMethod("getServer");
        Object dedicatedServer = getHandle2.invoke(craftServer);

        final Method getHandle3 = dedicatedServer.getClass().getMethod("getCraftingManager");
        craftManager = getHandle3.invoke(dedicatedServer);
        craftMethod = craftManager.getClass().getMethod("craft", recipeClass, iiInventoryClass, worldClass);
        recipeType = recipeClass.getField("CRAFTING").get(null);

    }

    @Override
    public ItemStack getResult(ItemStack[] contents) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        for (int i = 0; i < contents.length; i++) {

            setItemMethod.invoke(inventoryCrafting, i, asNMSCopyMethod.invoke(null, contents[i]));
        }
        return tryCraft(inventoryCrafting);
    }

    @Override
    public void setContainer(Player player) {
        //Not needed in 1.14
    }

    private ItemStack tryCraft(Object inventoryCrafting) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object optionalRecipe = craftMethod.invoke(craftManager, recipeType, inventoryCrafting, null);
        Optional optional = (Optional) optionalRecipe;
        if (optional.isPresent()) {
            return (ItemStack) asBukkitCopyMethod.invoke(null, recipeCraftingClass.getMethod("a", iiInventoryClass).invoke(optional.get(), inventoryCrafting));
        }
        return null;
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
