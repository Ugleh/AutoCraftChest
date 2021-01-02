package com.ugleh.autocraftchest.config;

import com.ugleh.autocraftchest.AutoCraftChest;
import com.ugleh.autocraftchest.util.ACC;
import com.ugleh.autocraftchest.util.XMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ACCStorage extends YamlConfiguration {
    private JavaPlugin plugin;
    private File file;
    private String fileName;


    public ACCStorage(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
                plugin.getLogger().severe("Error while creating file " + file.getName());
            }
        }
        try {
            load(file);
            if (defaults != null) {
                InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(plugin.getResource(fileName), "Plugin resource " + fileName + " cannot be found."));
                FileConfiguration defaultsConfig = YamlConfiguration.loadConfiguration(reader);
                setDefaults(defaultsConfig);
                options().copyDefaults(true);
                reader.close();
                save();
            }
            loadACCS();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
            plugin.getLogger().severe("Error while loading file " + file.getName());
        }
    }

    private void save() {
        try {
            options().indent(2);
            save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
            plugin.getLogger().severe("Error while saving file " + file.getName());
        }
    }

    private void loadACCS() {
        if(!this.isConfigurationSection("acc")) return;
        for (String accID : this.getConfigurationSection("acc").getKeys(false)) {
            String nodePrefix = "acc." + accID;
            Location location = (Location) this.get(nodePrefix + ".location");
            Block block = location.getBlock();
            if(block.getState() instanceof Chest) {
                Chest chest = (Chest) block.getState();
                UUID uuid = UUID.fromString(accID);
                Inventory autoCraftSettingsInventory = AutoCraftChest.getInstance().getListener().dupeInventory(AutoCraftChest.getInstance().getGuiManagement().getCraftMenu(), AutoCraftChest.getLanguageNode("menu.settings-title"));
                ACC acc = new ACC(location, autoCraftSettingsInventory, chest, uuid);
                AutoCraftChest.getInstance().getListener().getAutoCraftChests().put(location, acc);
                if(this.isBoolean(nodePrefix + ".running")) {
                    acc.setRunning(this.getBoolean(nodePrefix + ".running"), true);
                    if(this.isConfigurationSection(nodePrefix + ".ingredients")) {
                        EnumMap<Material, Integer> ingredients = new EnumMap<>(Material.class);
                        for (String ingredient : this.getConfigurationSection(nodePrefix + ".ingredients").getKeys(false)) {
                            Material material = Material.matchMaterial(ingredient);
                            //Material material = XMaterial.matchXMaterial(ingredient).parseMaterial();
                            if(material != null)
                                ingredients.put(material, this.getInt(nodePrefix + ".ingredients." + ingredient));
                        }
                        acc.setIngredients(ingredients, true);
                    }
                    if(this.isItemStack(nodePrefix + ".result")) {
                        acc.setResult(this.getItemStack(nodePrefix + ".result"), true);
                    }
                    if(this.isString(nodePrefix + ".recipeformat")) {
                        acc.setRecipeFormat(this.getString(nodePrefix + ".recipeformat"), true);
                        acc.updateInventoryStuffs();
                    }
                }
                //TODO: Modify autoCraftSettingsInventory to match how it should look. Add Recipe String to reform recipe section.
            }else {
                this.set(nodePrefix, null);
                this.save();
            }
            //ACC acc = new ACC()
        }
    }

    public void addACC(ACC acc) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        this.set(nodePrefix + ".location", acc.getLocation());
        this.save();
    }

    public void removeACC(ACC acc) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        this.set(nodePrefix, null);
        this.save();
    }

    public void setACCRunning(ACC acc, boolean running) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        this.set(nodePrefix + ".running", running);
        this.save();

    }

    public void setACCResult(ACC acc, ItemStack result) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        if(result == null) {
            this.set(nodePrefix + ".result", null);
            return;
        }
        this.set(nodePrefix + ".result", result);
        this.save();
    }

    public void setACCIngredients(ACC acc, Map<Material, Integer> ingredients) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        if(ingredients == null) {
            this.set(nodePrefix + ".ingredients", null);
            return;
        }
        HashMap<String, Integer> serializedIngredients = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
            serializedIngredients.put(entry.getKey().name(), entry.getValue());
        }
        this.set(nodePrefix + ".ingredients", serializedIngredients);
        this.save();
    }

    public void setRecipeFormat(ACC acc, String recipeFormat) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        if(recipeFormat == null) {
            this.set(nodePrefix + ".recipeformat", null);
            return;
        }
        this.set(nodePrefix + ".recipeformat", recipeFormat);
        this.save();

    }

    public void clearRecipeAndResult(ACC acc) {
        String nodePrefix = "acc." + acc.getUuid().toString();
        this.set(nodePrefix + ".recipeformat", null);
        this.set(nodePrefix + ".ingredients", null);
        this.set(nodePrefix + ".result", null);
        this.set(nodePrefix + ".running", false);
        this.save();
    }
}
