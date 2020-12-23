package com.ugleh.autocraftchest.util;

import com.ugleh.autocraftchest.AutoCraftChest;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;

public class ACC {

    private UUID uuid;
    private Location location;
    private Inventory craftSettingsInventory;
    private Chest chest;
    private Map<Material, Integer> ingredients;
    private ItemStack result;
    private boolean isRunning;
    private boolean inCrafting;
    private String recipeFormat;

    public ACC(Location location, Inventory craftSettingsInventory, Chest chest) {
        this.location = location;
        this.craftSettingsInventory = craftSettingsInventory;
        this.chest = chest;
        //TODO: Check for collision
        this.uuid = generateNonCollidedUUID();
    }

    public ACC(Location location, Inventory craftSettingsInventory, Chest chest, UUID uuid) {
        setLocation(location);
        setCraftSettingsInventory(craftSettingsInventory);
        setChest(chest);
        this.uuid = uuid;
    }

    public void setContainer(Player player) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        AutoCraftChest.getInstance().getCraftingUtil().setContainer(player);
    }
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Inventory getCraftSettingsInventory() {
        return craftSettingsInventory;
    }

    public void setCraftSettingsInventory(Inventory craftSettingsInventory) {
        this.craftSettingsInventory = craftSettingsInventory;
    }

    public ItemStack getResult() {
        return result;
    }

    public void setResult(ItemStack result, boolean fromConfig) {
        this.result = result;
        if(!fromConfig) AutoCraftChest.getStorage().setACCResult(this, result);
    }

    public boolean getRunning() {
        return isRunning;
    }

    public void setRunning(boolean running, boolean fromConfig) {
        isRunning = running;
        if(!fromConfig) AutoCraftChest.getStorage().setACCRunning(this, running);
    }

    public Map<Material, Integer> getIngredients() {
        return ingredients;
    }

    public void setIngredients(Map<Material, Integer> ingredients, boolean fromConfig) {
        this.ingredients = ingredients;
        if(!fromConfig) AutoCraftChest.getStorage().setACCIngredients(this, ingredients);

    }
    public Chest getChest() {
        return chest;
    }

    public void setChest(Chest chest) {
        this.chest = chest;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRecipeFormat() {
        return recipeFormat;
    }

    public void setRecipeFormat(String recipeFormat, boolean fromConfig) {
        this.recipeFormat = recipeFormat;
        if(!fromConfig) AutoCraftChest.getStorage().setRecipeFormat(this, recipeFormat);
    }

    public boolean isInCrafting() {
        return inCrafting;
    }

    public void setInCrafting(boolean inCrafting) {
        this.inCrafting = inCrafting;
    }

    private UUID generateNonCollidedUUID() {
        UUID tempUUID = UUID.randomUUID();
        for (Map.Entry<Location, ACC> entrySet : AutoCraftChest.getInstance().getListener().getAutoCraftChests().entrySet()) {
            if(entrySet.getValue().uuid.equals(tempUUID)) {
                return generateNonCollidedUUID();
            }
        }
        return tempUUID;
    }

    public void updateInventoryStuffs() {
        String[] firstSplit = this.recipeFormat.split(";");
        Integer[] craftSlots = new Integer[]{10,11,12,19,20,21,28,29,30};
        int i = 0;
        for (String materialRaw : firstSplit) {
            String[] secondSplit = materialRaw.split(":");
            Material material = Material.matchMaterial(secondSplit[0]);
            int amount = Integer.parseInt(secondSplit[1]);
            if(material == null) {
                material = Material.AIR;
                amount = 1;
            }
            ItemStack itemStack = new ItemStack(material, amount);
            if(this.isRunning) {
                makeUnattainable(itemStack);
            }
            this.craftSettingsInventory.setItem(craftSlots[i], itemStack);
            i++;
        }

        ItemStack resultPreview = result.clone();
        makeUnattainable(resultPreview);
        this.craftSettingsInventory.setItem(24, resultPreview);

        if(this.isRunning) {
            this.craftSettingsInventory.setItem(8, AutoCraftChest.getInstance().getGuiManagement().getClear());
        }
    }

    private void makeUnattainable(ItemStack itemStack) {
        if(itemStack.getType().equals(Material.matchMaterial("AIR"))) return;
        ItemMeta modMeta = itemStack.getItemMeta();
        if(modMeta == null) {
            modMeta = Bukkit.getServer().getItemFactory().getItemMeta(itemStack.getType());
        }
        if(modMeta.hasDisplayName()) {
            modMeta.setDisplayName(ChatColor.RED + AutoCraftChest.getLanguageNode("button.crafting.item-prefix") + ChatColor.RESET + modMeta.getDisplayName());
        }else {
            modMeta.setDisplayName(ChatColor.RED + AutoCraftChest.getLanguageNode("button.crafting.item-prefix") + ChatColor.RESET + WordUtils.capitalizeFully(itemStack.getType().name().toLowerCase().replace('_', ' ')));
        }
        itemStack.setItemMeta(modMeta);
    }

    @Override
    public String toString() {
        return "ACC{"+"uuid=" + uuid.toString() + ",location=" + location.toString() + ",running=" + getRunning() + '}';
    }
}
