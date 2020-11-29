package com.ugleh.autocraftchest;

import com.ugleh.autocraftchest.command.CommandAutoCraftChest;
import com.ugleh.autocraftchest.command.TabCompleterAutoCraftChest;
import com.ugleh.autocraftchest.config.ACCStorage;
import com.ugleh.autocraftchest.config.LanguageConfig;
import com.ugleh.autocraftchest.config.ACCConfig;
import com.ugleh.autocraftchest.listener.ListenerAutoCraftChest;
import com.ugleh.autocraftchest.nms.CraftingUtil;
import com.ugleh.autocraftchest.util.GUIManagement;
import com.ugleh.autocraftchest.util.PluginSupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

public class AutoCraftChest extends JavaPlugin {
    private static AutoCraftChest instance;
    private static LanguageConfig language;
    private static ACCStorage accStorage;
    private static ACCConfig config;
    private ListenerAutoCraftChest listener;
    private ItemStack accItemStack;
    private GUIManagement guiManagement;
    private CraftingUtil craftingUtil;

    private PluginSupport pluginSupport;
    private static String failSafeVersion = "v_16_R3";
    @Override
    public void onEnable() {
        setInstance(this);
        // If it can't find the right version it will try the latest fail safe version. This is to ensure that even past the latest version there is a chance it will work.
        // Crafting hasn't changed in the last year or so so when it comes to nms packages you only need to rely on the last version most times.
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
            final Class<?> clazz = Class.forName("com.ugleh.autocraftchest.nms." + version + "NMS");
            craftingUtil = (CraftingUtil) clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            final Class<?> clazz;
            try {
                clazz = Class.forName("com.ugleh.autocraftchest.nms." + failSafeVersion + "NMS");
                craftingUtil = (CraftingUtil) clazz.getConstructor().newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
        setLanguageInstance(new LanguageConfig(this, "language.yml"));
        //Why in this order? GUIManagement & Listener uses Language, and Config uses Listener
        guiManagement = new GUIManagement();
        this.getServer().getPluginManager().registerEvents(listener = new ListenerAutoCraftChest(), this);
        //ACCConfig and Command uses ItemStack
        createItemStack();

        setAccConfig(new ACCConfig(this, "config.yml"));
        PluginCommand commandAutoCraftChest = this.getCommand("autocraftchest");
        assert commandAutoCraftChest != null;
        commandAutoCraftChest.setExecutor(new CommandAutoCraftChest());
        commandAutoCraftChest.setTabCompleter(new TabCompleterAutoCraftChest());

        //load existing ACC's
        setAccStorage(new ACCStorage(this, "acc.yml"));

        //Load supporting plugins
        pluginSupport = new PluginSupport();
    }

    private void createItemStack() {
        ItemStack itemStack = new ItemStack(Material.CHEST, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(getLanguageNode("item.title"));
        itemMeta.setLore(getLanguage().wordWrapLore(getLanguageNode("item.lore")));
        itemStack.setItemMeta(itemMeta);
        setAccItemStack(itemStack);
    }

    public static AutoCraftChest getInstance() {
        return instance;
    }
    public static LanguageConfig getLanguage() {
        return language;
    }
    public static ACCConfig getACCConfig() {
        return config;
    }
    public static ACCStorage getStorage() {
        return accStorage;
    }

    public PluginSupport getPluginSupport() {
        return pluginSupport;
    }

    private static void setAccStorage(ACCStorage accStorage) {
        AutoCraftChest.accStorage = accStorage;
    }
    public static String getLanguageNode(String string) {
        return language.getLanguageNodes().get(string);
    }

    private static void setLanguageInstance(LanguageConfig languageConfig) {
        language = languageConfig;
    }

    private static void setAccConfig(ACCConfig accConfig) {
        config = accConfig;
    }

    public ListenerAutoCraftChest getListener() {
        return this.listener;
    }

    public GUIManagement getGuiManagement() {
        return guiManagement;
    }

    private static void setInstance(AutoCraftChest instance) {
        AutoCraftChest.instance = instance;
    }

    public ItemStack getAccItemStack() {
        return accItemStack;
    }

    public CraftingUtil getCraftingUtil() {
        return craftingUtil;
    }
    private void setAccItemStack(ItemStack accItemStack) {
        this.accItemStack = accItemStack;
    }
}
