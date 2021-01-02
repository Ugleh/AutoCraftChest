package com.ugleh.autocraftchest.util;

import com.ugleh.autocraftchest.AutoCraftChest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GUIManagement {

    private Inventory craftMenu;
    private Integer[] craftMenuIgnoreSlots = new Integer[]{10,11,12,19,20,21,24,28,29,30,44,8};
    private Inventory mainMenu;
    private ItemStack accContentsChest;
    private ItemStack filler;
    private ItemStack confirm;
    private ItemStack clear;
    private ItemStack craftingTable;

    public GUIManagement() {
        createButtons();
        createMainMenu();
        createCraftMenu();
    }

    private void createButtons() {
        //Filler for non-used area of the GUI
        filler = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(ChatColor.RED + "#");
        filler.setItemMeta(fillerMeta);

        //Confirm Recipe Button
        confirm = XMaterial.GREEN_DYE.parseItem();
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(AutoCraftChest.getLanguageNode("button.confirm-title"));
        confirmMeta.setLore(AutoCraftChest.getLanguage().wordWrapLore(AutoCraftChest.getLanguageNode("button.confirm-lore")));
        confirm.setItemMeta(confirmMeta);

        //Clear Recipe Button
        clear = XMaterial.ORANGE_DYE.parseItem();
        ItemMeta clearMeta = confirm.getItemMeta();
        clearMeta.setDisplayName(ChatColor.GOLD + "Clear Recipe & Result");
        clearMeta.setDisplayName(AutoCraftChest.getLanguageNode("button.reset-title"));
        clearMeta.setLore(AutoCraftChest.getLanguage().wordWrapLore(AutoCraftChest.getLanguageNode("button.reset-lore")));
        clear.setItemMeta(clearMeta);

        //ACC Contents Button
        accContentsChest = XMaterial.CHEST.parseItem();
        ItemMeta chestMeta = accContentsChest.getItemMeta();
        chestMeta.setDisplayName(AutoCraftChest.getLanguageNode("button.contents-title"));
        chestMeta.setLore(AutoCraftChest.getLanguage().wordWrapLore(AutoCraftChest.getLanguageNode("button.contents-lore")));
        accContentsChest.setItemMeta(chestMeta);

        //ACC Settings Button
        craftingTable = XMaterial.CRAFTING_TABLE.parseItem();
        ItemMeta craftingTableMeta = craftingTable.getItemMeta();
        craftingTableMeta.setDisplayName(AutoCraftChest.getLanguageNode("button.settings-title"));
        craftingTableMeta.setLore(AutoCraftChest.getLanguage().wordWrapLore(AutoCraftChest.getLanguageNode("button.settings-lore")));
        craftingTable.setItemMeta(craftingTableMeta);

    }
    private void createMainMenu() {
        mainMenu = Bukkit.createInventory(null, 9, AutoCraftChest.getLanguageNode("menu.main-title"));
        mainMenu.setItem(3, accContentsChest);
        mainMenu.setItem(0, filler.clone());
        mainMenu.setItem(1, filler.clone());
        mainMenu.setItem(2, filler.clone());
        mainMenu.setItem(4, filler.clone());
        mainMenu.setItem(6, filler.clone());
        mainMenu.setItem(7, filler.clone());
        mainMenu.setItem(8, filler.clone());
        mainMenu.setItem(5, craftingTable);
    }

    private void createCraftMenu() {
        craftMenu = Bukkit.createInventory(null, 45, AutoCraftChest.getLanguageNode("menu.settings-title"));
        for (int i = 0; i < 45; i++) {
            if(Arrays.asList(craftMenuIgnoreSlots).contains(i)) continue;
            craftMenu.setItem(i, filler.clone());
        }
        craftMenu.setItem(44, accContentsChest);
        craftMenu.setItem(8, confirm);
    }

    public Inventory getCraftMenu() {
        return craftMenu;
    }

    public Integer[] getCraftMenuIgnoreSlots() {
        return craftMenuIgnoreSlots;
    }

    public Inventory getMainMenu() {
        return mainMenu;
    }

    public ItemStack getAccContentsChest() {
        return accContentsChest;
    }

    public ItemStack getFiller() {
        return filler;
    }

    public ItemStack getConfirm() {
        return confirm;
    }

    public ItemStack getClear() {
        return clear;
    }

    public ItemStack getCraftingTable() {
        return craftingTable;
    }

}
