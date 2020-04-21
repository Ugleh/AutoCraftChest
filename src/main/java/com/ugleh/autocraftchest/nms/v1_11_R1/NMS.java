package com.ugleh.autocraftchest.nms.v1_11_R1;
import com.ugleh.autocraftchest.nms.CraftingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public class NMS extends com.ugleh.autocraftchest.nms.v1_12_R1.NMS implements CraftingUtil{
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
    private final Class<?> craftingManagerClass = getNMSClass("CraftingManager");

    private final Method getHandleWorldServer = craftWorldClass.getMethod("getHandle");
    private final Method asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
    private final Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", itemStackClass);
    private final Method setItemMethod = inventoryCraftingClass.getMethod("setItem", int.class, itemStackClass);
    private Object craftingManager;
    private Method craftMethod;
    private Method bMethod;
    public NMS()throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException {
        final Class<?> containerClass = getNMSClass("Container");
        final Class<?> inventoryCraftingClass = getNMSClass("InventoryCrafting");
        inventorCraftingConstructor = inventoryCraftingClass.getConstructor(containerClass, int.class, int.class);

        setItemMethod.setAccessible(true);
        craftingManager = craftingManagerClass.getMethod("getInstance").invoke(null);
        craftMethod = craftingManager.getClass().getMethod("craft", this.inventoryCraftingClass, worldClass);
        bMethod = craftingManagerClass.getMethod("b", this.inventoryCraftingClass, worldClass);
    }

    @Override
    protected ItemStack tryCraft(Object inventoryCrafting) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        return this.craft(craftingManager, inventoryCrafting, null);
    }

    private ItemStack craft(Object craftingManager, Object inventorycrafting, Object world) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Object recipes = craftingManager.getClass().getField("recipes").get(craftingManager);
        Iterator iterator = (Iterator) recipes.getClass().getMethod("iterator").invoke(recipes);
        Class iRecipeClass = getNMSClass("IRecipe");
        Object irecipe;

        do {
            if (!iterator.hasNext()) {
                return null;
            }

            irecipe = iRecipeClass.cast(iterator.next());
        } while (!(boolean) iRecipeClass.getMethod("a", inventoryCraftingClass, worldClass).invoke(irecipe, inventorycrafting, world));
        Object itemstack = iRecipeClass.getMethod("craftItem", inventoryCraftingClass).invoke(irecipe, inventorycrafting);
        return (ItemStack) asBukkitCopyMethod.invoke(null, itemstack);
    }
}
