package com.ugleh.autocraftchest.config;

import com.ugleh.autocraftchest.AutoCraftChest;
import com.ugleh.autocraftchest.util.Updater;
import com.ugleh.autocraftchest.util.XMaterial;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

public class ACCConfig extends YamlConfiguration {
    private JavaPlugin plugin;
    private File file;
    private String fileName;

    private boolean soundsEnabled;
    private boolean metricsEnabled;
    private boolean updateChecker;
    private long craftCooldownTicks;
    private boolean isShapedCraftable;
    private boolean isShapelessCraftable;

    public ACCConfig(JavaPlugin plugin, String fileName) {
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
            if (fileName != null) {
                InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(plugin.getResource(fileName), "Plugin resource " + fileName + " cannot be found."));
                FileConfiguration defaultsConfig = YamlConfiguration.loadConfiguration(reader);
                setDefaults(defaultsConfig);
                options().copyDefaults(true);
                reader.close();
                save();
            }
            loadSettings();
        } catch (Exception exception) {
            exception.printStackTrace();
            plugin.getLogger().severe("Error while loading file " + file.getName());
        }
    }

    private void loadSettings() {
        soundsEnabled = this.getBoolean("acc-settings." + "enable-sounds");
        metricsEnabled = this.getBoolean("acc-settings." + "enable-metrics");
        updateChecker = this.getBoolean("acc-settings." + "enable-update-checker");
        craftCooldownTicks = this.getInt("acc-settings." + "craft-cooldown-ticks");
        isShapedCraftable = this.getBoolean("craft-settings." + "enable-shaped-craftable");
        isShapelessCraftable = this.getBoolean("craft-settings." + "enable-shapeless-craftable");
        determineRecipes();
        if(isMetricsEnabled())
            loadMetrics();
        if(isUpdateChecker())
            loadUpdater();
    }

    private void loadUpdater() {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            Updater updater = new Updater(plugin,77144);
            try {
                if (updater.checkForUpdates()) {
                    //new update avaible
                    Bukkit.getConsoleSender().sendMessage("[AutoCraftChest] " + ChatColor.RED + "New Update is available!");
                    Bukkit.getConsoleSender().sendMessage("[AutoCraftChest] " + ChatColor.RED + "https://www.spigotmc.org/resources/autocraftchest.77144");

                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates! Stacktrace:");
                e.printStackTrace();
            }
        }, 20L);
    }

    private void loadMetrics() {
        new Metrics(AutoCraftChest.getInstance());
    }

    private void determineRecipes() {
        if(isShapelessCraftable) {
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(AutoCraftChest.getInstance().getAccItemStack());
            for (String key : this.getConfigurationSection("craft-settings.shapeless-recipe").getKeys(true)) {
                shapelessRecipe.addIngredient(this.getInt("craft-settings.shapeless-recipe." + key), Objects.requireNonNull(XMaterial.matchXMaterial(key).parseMaterial(), "Material in Shapeless Recipe can not be null."));
            }
            Bukkit.getServer().addRecipe(shapelessRecipe);
        }

        if(isShapedCraftable) {
            ShapedRecipe shapedRecipe = new ShapedRecipe(AutoCraftChest.getInstance().getAccItemStack());
            shapedRecipe.shape("abc", "def", "ghi");
            List<String> list = (List<String>) this.getList("craft-settings.shaped-recipe");
            char letterIncrement = 'a';
            for (String line : list) {
                String[] materialStrings = line.split(",");
                for (String materialString : materialStrings) {
                    Material material = XMaterial.matchXMaterial(materialString).parseMaterial();
                    if(material != null && (!material.equals(Material.matchMaterial("AIR")))) {
                        shapedRecipe.setIngredient(letterIncrement, material);
                    }
                    letterIncrement++;
                }
            }
            Bukkit.getServer().addRecipe(shapedRecipe);
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


    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public boolean isUpdateChecker() {
        return updateChecker;
    }

    public long getCraftCooldownTicks() {
        return craftCooldownTicks;
    }

}
