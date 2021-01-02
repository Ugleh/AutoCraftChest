package com.ugleh.autocraftchest.listener;

import com.ugleh.autocraftchest.AutoCraftChest;
import com.ugleh.autocraftchest.util.ACC;
import com.ugleh.autocraftchest.util.GUIManagement;
import com.ugleh.autocraftchest.util.XMaterial;
import com.ugleh.autocraftchest.util.XSound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ListenerAutoCraftChest implements Listener {
    private HashMap<UUID, Block> lastOpenedACC = new HashMap<>();
    private HashMap<Location, ACC> autoCraftChests = new HashMap<>();
    private Integer[] craftSlotsOnly = new Integer[]{10,11,12,19,20,21,28,29,30};

    @EventHandler
    private void onHopperMoveItem(InventoryMoveItemEvent e) {
        if(getInventoryLocation(e.getSource()) == null) return;
        if(getInventoryLocation(e.getDestination()) == null) return;
        //Doesn't involve ACC at all
        if(!(autoCraftChests.containsKey(getInventoryLocation(e.getSource())) || autoCraftChests.containsKey(getInventoryLocation(e.getDestination())))) return;
        if(autoCraftChests.containsKey(getInventoryLocation(e.getSource())) && autoCraftChests.get(getInventoryLocation(e.getSource())).getIngredients() != null && autoCraftChests.get(getInventoryLocation(e.getSource())).getIngredients().containsKey(e.getItem().getType())) {
            //Involves ACC, item going out of acc, item IS in the ingredient list
            e.setCancelled(true);
        }else if(autoCraftChests.containsKey(getInventoryLocation(e.getDestination())) && autoCraftChests.get(getInventoryLocation(e.getDestination())).getRunning()) {
            //^Item going in the ACC and Running
            Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> checkCanCraft(autoCraftChests.get(getInventoryLocation(e.getDestination())), false), 1L);
        }
    }

    private void checkCanCraft(ACC acc, boolean fromSelf) {
        if((!fromSelf) && acc.isInCrafting()){
            return;
        }
        if(!acc.isInCrafting()) acc.setInCrafting(true);

        Inventory accInv = acc.getChest().getBlockInventory();
        List<ItemStack> clonedContents = new ArrayList<>();
        for (ItemStack item : accInv.getContents()) {
            if(item != null && (!item.isSimilar(acc.getResult()))) {
                clonedContents.add(item);
            }
        }
        if(acc.getIngredients() == null) {
            acc.setInCrafting(false);
            return;
        }
        Map<Material, Integer> ingredientCopy = new EnumMap<>(Material.class);
        ingredientCopy.putAll(acc.getIngredients());
        for (Map.Entry<Material, Integer> ingredient : ingredientCopy.entrySet()) {
            if(!(listContainsMaterial(clonedContents, ingredient.getKey(), ingredient.getValue()))) {
                acc.setInCrafting(false);
                return;
            }
        }
        //All ingredients are in chest, remove them.
        for (Map.Entry<Material, Integer> ingredient : acc.getIngredients().entrySet()) {
            acc.getChest().getBlockInventory().removeItem(new ItemStack(ingredient.getKey(), ingredient.getValue()));
        }
        //Add result to chest.
        acc.getChest().getBlockInventory().addItem(acc.getResult().clone());

        //Recursive check to see if more items can craft with a cooldown of 6 ticks
        if(acc.getRunning()) {
            Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> checkCanCraft(acc, true), AutoCraftChest.getACCConfig().getCraftCooldownTicks());
        }else {
            acc.setInCrafting(false);
        }
    }

    private boolean listContainsMaterial(List<ItemStack> contents, Material material, int amount) {
        if (amount <= 0) {
            return true;
        } else {
            ItemStack[] itemStacks = contents.toArray(new ItemStack[0]);
            for (ItemStack item : itemStacks) {
                if (item != null && item.getType() == material && (amount -= item.getAmount()) <= 0) {
                    return true;
                }
            }
            return false;
        }
    }

    @EventHandler
    private void onCraft(CraftItemEvent e) {
        if(e.getCurrentItem() == null) return;
        if(e.getCurrentItem().equals(AutoCraftChest.getInstance().getAccItemStack())
                && (!e.getWhoClicked().hasPermission("autocraftchest.craft"))) {
                e.setResult(Event.Result.DENY);
                e.getWhoClicked().sendMessage(AutoCraftChest.getLanguageNode("chat.no-permission-craft"));
            }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent e) {
        if(!e.getBlock().getType().equals(Material.CHEST)) return; //Faster check then next line
        Chest chest = (Chest) e.getBlock().getState();
        boolean placingDownAutoCraft = (e.getItemInHand().isSimilar(getInstance().getAccItemStack()));
        boolean isAutoCraft = (!e.getPlayer().isSneaking()) && checkIfAutoCraftNextdoor(chest, placingDownAutoCraft);
        if(isAutoCraft) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(AutoCraftChest.getLanguageNode("chat.invalid-place"));
            playNotificationSound(e.getPlayer(), false);
            return;
        }

        if(placingDownAutoCraft) {
            if(!e.getPlayer().hasPermission("autocraftchest.place")) {
                e.getPlayer().sendMessage(AutoCraftChest.getLanguageNode("chat.no-permission-place"));
                playNotificationSound(e.getPlayer(), false);
                return;
            }
            Inventory autoCraftSettingsInventory = dupeInventory(getGUI().getCraftMenu(), AutoCraftChest.getLanguageNode("menu.settings-title"));
            ACC savedACC = new ACC(e.getBlock().getLocation(), autoCraftSettingsInventory, chest);
            autoCraftChests.put(e.getBlock().getLocation(), savedACC);
            AutoCraftChest.getStorage().addACC(savedACC);
        }
    }

    private boolean checkIfAutoCraftNextdoor(Chest chest, boolean placingAutoCraft){
        BlockFace blockFace = null;
        try {
            Directional directional = (Directional) chest.getBlockData();
            blockFace = directional.getFacing();
        }catch(NoSuchMethodError e) {
            if(placingAutoCraft && chestNearby(chest)) return true;
            return (autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.NORTH).getLocation()) || autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.EAST).getLocation()) || autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.WEST).getLocation()) || autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.SOUTH).getLocation()));
        }

        if(blockFace == BlockFace.NORTH || blockFace == BlockFace.SOUTH) {
            return (autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.EAST).getLocation()) || autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.WEST).getLocation()));
        }else if(blockFace == BlockFace.WEST || blockFace == BlockFace.EAST) {
            return (autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.NORTH).getLocation()) || autoCraftChests.containsKey(chest.getBlock().getRelative(BlockFace.SOUTH).getLocation()));
        }
        return false;
    }

    private boolean chestNearby(Chest chest) {

        Location l = chest.getLocation();
        if (l.clone().add(0, 0, 1).getBlock().getType() == Material.CHEST) return true;
        if (l.clone().add(0, 0, -1).getBlock().getType() == Material.CHEST) return true;
        if (l.clone().add(1, 0, 0).getBlock().getType() == Material.CHEST) return true;
        if (l.clone().add(-1, 0, 0).getBlock().getType() == Material.CHEST) return true;
        return false;
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        if(autoCraftChests.containsKey(e.getBlock().getLocation())) {
            ACC acc = autoCraftChests.get(e.getBlock().getLocation());
            if(!acc.getRunning()) {
                for (Integer slot : craftSlotsOnly) {
                    ItemStack item = acc.getCraftSettingsInventory().getItem(slot);
                    if(item != null) {
                        e.getPlayer().getWorld().dropItemNaturally(e.getBlock().getLocation(), item);
                    }
                }
            }
            autoCraftChests.remove(e.getBlock().getLocation());
            AutoCraftChest.getStorage().removeACC(acc);
            try {
                e.setDropItems(false);
            } catch (NoSuchMethodError ex) {
                e.getBlock().setType(Objects.requireNonNull(XMaterial.AIR.parseMaterial(), "Material AIR not found?"));
                e.setCancelled(true);
            }
            if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), AutoCraftChest.getInstance().getAccItemStack());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onAutoCraftChestOpen(PlayerInteractEvent e) throws InvocationTargetException, IllegalAccessException {
        if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if(e.getPlayer().isSneaking() && e.isBlockInHand()) return;
        if(e.getClickedBlock() == null) return;
        if(!autoCraftChests.containsKey(e.getClickedBlock().getLocation())) return;
        if(!AutoCraftChest.getInstance().getPluginSupport().canInteract(autoCraftChests.get(e.getClickedBlock().getLocation()), e.getPlayer())) return;
        e.setCancelled(true);
        lastOpenedACC.put(e.getPlayer().getUniqueId(), e.getClickedBlock());
        if(!e.getPlayer().hasPermission("autocraftchest.use")) {
            e.getPlayer().sendMessage(AutoCraftChest.getLanguageNode("chat.no-permission-use"));
            playNotificationSound(e.getPlayer(), false);
        }else {
            e.getPlayer().openInventory(getGUI().getMainMenu());
        }
    }

    //START - Memory Leak Prevention Events
    @EventHandler
    private void onInventoryClose(InventoryCloseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Inventory inventory = e.getInventory();
        if(lastOpenedACC.containsKey(uuid)) {
            Block block = lastOpenedACC.get(uuid);
            Location location = block.getLocation();
            if ((!inventory.equals(getGUI().getMainMenu()) || !autoCraftChests.containsKey(location)) && (!autoCraftChests.containsKey(location) || !inventory.equals(autoCraftChests.get(location).getCraftSettingsInventory()))) {
                lastOpenedACC.remove(uuid);
            }
        }
    }

    @EventHandler
    private void onInventoryOpen(InventoryOpenEvent e) {
        Block block = lastOpenedACC.get(e.getPlayer().getUniqueId());
        if((!e.getInventory().equals(getGUI().getMainMenu())) && block != null && autoCraftChests.containsKey(block.getLocation()) && (!e.getInventory().equals(autoCraftChests.get(block.getLocation()).getCraftSettingsInventory()))) {
            lastOpenedACC.remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    private void onPlayerLeft(PlayerQuitEvent e) {
        Block block = lastOpenedACC.get(e.getPlayer().getUniqueId());
        if(block != null) {
            lastOpenedACC.remove(e.getPlayer().getUniqueId());
        }
    }
    //END - Memory Leak Prevention Events


    @EventHandler
    private void onMenuDrag(InventoryDragEvent e) {
        Block block = lastOpenedACC.get(e.getWhoClicked().getUniqueId());
        if(block != null && autoCraftChests.containsKey(block.getLocation()) && autoCraftChests.get(block.getLocation()).getRunning()) {
            e.setCancelled(true);
        }
        if(block != null && autoCraftChests.containsKey(block.getLocation()) && e.getView().getTopInventory().equals(autoCraftChests.get(block.getLocation()).getCraftSettingsInventory())) {

            for (Map.Entry<Integer, ItemStack> stackEntry : e.getNewItems().entrySet()) {
                if(Arrays.asList(craftSlotsOnly).contains(stackEntry.getKey())) {
                    Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> modifyRecipe(autoCraftChests.get(block.getLocation())), 1L);
                    break;
                }
            }
        }else if(autoCraftChests.containsKey(getInventoryLocation(e.getInventory())) && autoCraftChests.get(getInventoryLocation(e.getInventory())).getRunning()) {
            Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> checkCanCraft(autoCraftChests.get(getInventoryLocation(e.getInventory())), false), 1L);
        }
    }

    @EventHandler
    private void onMenuClick(InventoryClickEvent e) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        boolean cancel = false;
        Block block = lastOpenedACC.get(e.getWhoClicked().getUniqueId());
        if(e.getView().getTopInventory().equals(getGUI().getMainMenu())) {
            cancel = onMainMenuClick(e);
        }else if(block != null && autoCraftChests.containsKey(block.getLocation()) && e.getView().getTopInventory().equals(autoCraftChests.get(block.getLocation()).getCraftSettingsInventory())) {

            cancel = onCraftMenuClick(e.getClickedInventory(), e.getInventory(), e.getSlot(), e.getCurrentItem(), e.getWhoClicked(), block.getLocation());
        }else if(e.getClickedInventory() != null && autoCraftChests.containsKey(getInventoryLocation(e.getClickedInventory())) && autoCraftChests.get(getInventoryLocation(e.getClickedInventory())).getRunning()) {
            Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> checkCanCraft(autoCraftChests.get(getInventoryLocation(e.getClickedInventory())), false), 1L);
        }else if(e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) && e.isShiftClick() && autoCraftChests.containsKey(getInventoryLocation(e.getInventory()))) {
            Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> checkCanCraft(autoCraftChests.get(getInventoryLocation(e.getInventory())), false), 1L);
        }
        if(cancel) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }
    }

    private boolean onCraftMenuClick(Inventory clickedInventory, Inventory inventory, Integer slot, ItemStack clickedItem, HumanEntity whoClicked, Location location) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        ACC acc = autoCraftChests.get(location);
        if(acc == null) return false;
        if(!Objects.equals(clickedInventory, acc.getCraftSettingsInventory())) return false;
        if(acc.getRunning() && (!(slot == 8 || slot == 44))) return true;
        if(clickedItem != null && clickedItem.getType().equals(XMaterial.RED_STAINED_GLASS.parseMaterial())) return true;
        if(clickedItem != null && clickedItem.equals(getGUI().getAccContentsChest())) {
            Chest chest = (Chest) lastOpenedACC.get(whoClicked.getUniqueId()).getState();
            whoClicked.openInventory(chest.getBlockInventory());
            return true;
        }
        if(slot == 8 && clickedItem != null && clickedItem.equals(AutoCraftChest.getInstance().getGuiManagement().getConfirm())) {
            ItemStack result = inventory.getItem(24);
            if(result == null || result.getType().equals(XMaterial.AIR.parseMaterial())) {
                whoClicked.sendMessage(AutoCraftChest.getLanguageNode("chat.invalid-recipe"));
                playNotificationSound((Player) whoClicked, false);
                return true;
            }
            inventory.setItem(8, getGUI().getClear().clone());
            confirmRecipe(acc, clickedInventory, whoClicked);
            playNotificationSound((Player) whoClicked, true);
        }else if(slot == 8 && clickedItem != null && clickedItem.equals(AutoCraftChest.getInstance().getGuiManagement().getClear())) {
            inventory.setItem(8, getGUI().getConfirm().clone());
            clearRecipeAndResult(acc);
            playNotificationSound((Player) whoClicked, true);
        }else if(acc.getRunning()) {
            return true;
        }
        if(Arrays.asList(craftSlotsOnly).contains(slot)) {
            modifyRecipe(acc);
            return false;
        }
        return true;
    }

    private void clearRecipeAndResult(ACC acc) {
        for (Integer slot : craftSlotsOnly) {
            acc.getCraftSettingsInventory().setItem(slot, null);
        }
        acc.getCraftSettingsInventory().setItem(24, null);
        acc.setRunning(false, false);
        acc.setResult(null, false);
        acc.setIngredients(null, false);
        AutoCraftChest.getStorage().clearRecipeAndResult(acc);
    }

    private void confirmRecipe(ACC acc, Inventory craftSettingInv, HumanEntity whoClicked) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        EnumMap<Material, Integer> ingredients = new EnumMap<>(Material.class);
        StringBuilder recipeFormat = new StringBuilder();
        for (Integer slot : craftSlotsOnly) {
            ItemStack item = craftSettingInv.getItem(slot);
            if(item == null) {
                recipeFormat.append("AIR:1;");
            }else {
                recipeFormat.append(item.getType().name()).append(":").append(item.getAmount()).append(";");
            }
            if (item == null) continue;
            //Add items in craft slots into ingredients map
            if (ingredients.containsKey(item.getType())) {
                ingredients.put(item.getType(), ingredients.get(item.getType()) + 1);  //item.getAmount() should always be 1 in a real recipe.
            } else {
                ingredients.put(item.getType(), 1); //item.getAmount() should always be 1 in a real recipe.
            }

            //Return item back to player
            whoClicked.getInventory().addItem(craftSettingInv.getItem(slot));

            //Turn item in craft inventory into unobtainable item.
            ItemStack modItem = new ItemStack(item.getType(), item.getAmount());
            ItemMeta modMeta = modItem.getItemMeta();
            assert modMeta != null;
            if(modMeta.hasDisplayName()) {
                modMeta.setDisplayName(ChatColor.RED + AutoCraftChest.getLanguageNode("button.crafting.item-prefix") + ChatColor.RESET + modMeta.getDisplayName());
            }else {
                modMeta.setDisplayName(ChatColor.RED + AutoCraftChest.getLanguageNode("button.crafting.item-prefix") + ChatColor.RESET + WordUtils.capitalizeFully(modItem.getType().name().toLowerCase().replace('_', ' ')));
            }
            modItem.setItemMeta(modMeta);
            modItem.setAmount(1);
            craftSettingInv.setItem(slot, modItem);
        }
        //Set ACC ingredients, result, and setRunning
        acc.setIngredients(ingredients, false);
        ItemStack[] contents = new ItemStack[9];
        for (int i = 0; i < craftSlotsOnly.length; i++) {
            contents[i] = craftSettingInv.getItem(craftSlotsOnly[i]);
        }
        acc.setResult( AutoCraftChest.getInstance().getCraftingUtil().getResult(contents).clone(), false);
        acc.setRecipeFormat(recipeFormat.toString(), false);
        acc.setRunning(true, false);
        Bukkit.getScheduler().runTaskLater(AutoCraftChest.getInstance(), () -> checkCanCraft(acc, false), 1L);
    }

    private void modifyRecipe(ACC acc) {
        Inventory inventory = acc.getCraftSettingsInventory();
        Bukkit.getScheduler().runTask(AutoCraftChest.getInstance(), () -> {
            ItemStack[] contents = new ItemStack[9];
            for (int i = 0; i < craftSlotsOnly.length; i++) {
                contents[i] = inventory.getItem(craftSlotsOnly[i]);
            }
            ItemStack itemStack = null;
            try {
                itemStack = AutoCraftChest.getInstance().getCraftingUtil().getResult(contents);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            if(itemStack != null && !itemStack.getType().equals(XMaterial.AIR.parseMaterial())) {
                ItemMeta itemMeta = itemStack.getItemMeta();
                assert itemMeta != null;
                if(itemMeta.hasDisplayName()) {
                    itemMeta.setDisplayName(ChatColor.RED + AutoCraftChest.getLanguageNode("button.crafting.item-prefix") + ChatColor.RESET + itemMeta.getDisplayName());
                }else {
                    itemMeta.setDisplayName(ChatColor.RED + AutoCraftChest.getLanguageNode("button.crafting.item-prefix") + ChatColor.RESET + WordUtils.capitalizeFully(itemStack.getType().name().toLowerCase().replace('_', ' ')));
                }
                itemMeta.setLore(AutoCraftChest.getLanguage().wordWrapLore(AutoCraftChest.getLanguageNode("button.crafting.item-lore")));
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(24, itemStack);
            }else {
                inventory.setItem(24, null);
            }
        });
    }

    private boolean onMainMenuClick(InventoryClickEvent e) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
        if(!Objects.equals(e.getClickedInventory(), getGUI().getMainMenu())) return false;
        if(e.getCursor() == null) return true;
        if(!e.getCursor().getType().equals(XMaterial.AIR.parseMaterial())) return true; //Still cancel the event, but don't trigger a button press.
        ItemStack itemStack = e.getCurrentItem();
        if(itemStack == null) return true;
        if(itemStack.getType().equals(AutoCraftChest.getInstance().getGuiManagement().getFiller().getType())) return true; // No need to process the non-button fillers.
        Block accBlock = lastOpenedACC.get(e.getWhoClicked().getUniqueId());
        Chest chest = (Chest) (accBlock).getState();
        if(itemStack.getType().equals(XMaterial.CHEST.parseMaterial())) {
                e.getWhoClicked().openInventory(chest.getBlockInventory());
            return true;
        }else if(itemStack.equals(AutoCraftChest.getInstance().getGuiManagement().getCraftingTable())) {
            if(autoCraftChests.containsKey(accBlock.getLocation())) {
                autoCraftChests.get(accBlock.getLocation()).setContainer(((Player) e.getWhoClicked()));
                e.getWhoClicked().openInventory(autoCraftChests.get(accBlock.getLocation()).getCraftSettingsInventory());
            }else {
                Inventory craftSettingsInv = dupeInventory(getGUI().getCraftMenu(), AutoCraftChest.getLanguageNode("menu.settings-title"));
                ACC savedACC  = new ACC(accBlock.getLocation(), craftSettingsInv, chest);
                autoCraftChests.put(accBlock.getLocation(), savedACC);
                AutoCraftChest.getStorage().addACC(savedACC);
                savedACC.setContainer(((Player) e.getWhoClicked()));
                e.getWhoClicked().openInventory(craftSettingsInv);
            }
            return true;
        }
        return true;
    }

    private AutoCraftChest getInstance() {
        return AutoCraftChest.getInstance();
    }

    public Map<Location, ACC> getAutoCraftChests() {
        return autoCraftChests;
    }

    public Inventory dupeInventory(Inventory inventory, String name) {
        int size = inventory.getSize();
        Inventory newInventory = Bukkit.createInventory(null, size, name);
        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack item = inventory.getItem(i);
            if(item != null)
                newInventory.setItem(i, item.clone());
        }
        return newInventory;
    }

    private Location getInventoryLocation(Inventory inventory) {
        if(inventory.getHolder() instanceof BlockState) {
            BlockState blockState = (BlockState) inventory.getHolder();
            return blockState.getLocation();
        }
        return null;
    }
    
    private GUIManagement getGUI() {
        return AutoCraftChest.getInstance().getGuiManagement();
    }

    private void playNotificationSound(Player player, boolean positive) {
        if(!AutoCraftChest.getACCConfig().isSoundsEnabled()) return;
        if(positive) {
            player.playSound(player.getLocation(), Objects.requireNonNull(XSound.ENTITY_EXPERIENCE_ORB_PICKUP.parseSound(), "ENTITY_EXPERIENCE_ORB_PICKUP sound not found."), 1.0f, 1.0f);
        }else {
            player.playSound(player.getLocation(), Objects.requireNonNull(XSound.BLOCK_ANVIL_LAND.parseSound(), "BLOCK_ANVIL_LAND sound not found."), 0.2f, 1f);
        }
    }
}
