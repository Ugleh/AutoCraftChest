package com.ugleh.autocraftchest.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class LanguageConfig extends YamlConfiguration {
    private JavaPlugin plugin;
    private File file;
    private String fileName;
    private char colorChar;
    private HashMap<String, String> languageNodes = new HashMap<>();
    private HashMap<String, String> placeholderNodes = new HashMap<>();


    public LanguageConfig(JavaPlugin plugin, String fileName) {
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
            loadLanguageSettings();
            loadPlaceholderNodes();
            loadLanguageNodes();
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

    private void loadLanguageSettings() {
        this.colorChar = Objects.requireNonNull(this.getString("color-character"), "color-character missing from language.yml").charAt(0);
    }

    private void loadPlaceholderNodes() {
        for (String languageID : this.getKeys(true)) {
            String langString = this.getString(languageID);
            if(!this.isConfigurationSection(languageID) && (!languageID.startsWith("config.")))
                placeholderNodes.put("{" + languageID.replace("placeholder.", "") + "}", langString);
        }
    }

    private void loadLanguageNodes() {
        for (String languageID : this.getKeys(true)) {
            String langString = this.getString(languageID);
            if(!this.isConfigurationSection(languageID) && (!languageID.startsWith("config.")))
                languageNodes.put(languageID.replace("language.", ""), coloredString(placeholderReplaced(langString)));
        }
    }

    private String placeholderReplaced(String langString) {
        for (Map.Entry<String, String> placeholder : placeholderNodes.entrySet()) {
            if(langString.contains(placeholder.getKey())) {
                langString = langString.replace(placeholder.getKey(), placeholder.getValue());
            }
        }
        return langString;
    }

    private String coloredString(String langString) {
        return ChatColor.translateAlternateColorCodes(colorChar, langString);
    }

    public List<String> wordWrapLore(String rawString) {
        if(rawString == null || rawString.equals("")) return Collections.singletonList("");
        rawString = ChatColor.translateAlternateColorCodes(colorChar, rawString);
        if(rawString.equals("")) return Collections.singletonList("");
        StringBuilder newString = new StringBuilder();
        String[] lines;
        lines = rawString.split("\\\\n");
        for(String string : lines) {
            StringBuilder sb = new StringBuilder(string);
            int i = 0;
            while (i + 35 < sb.length() && (i = sb.lastIndexOf(" ", i + 35)) != -1) {
                sb.replace(i, i + 1, "\\n");
            }
            newString.append(sb).append("\\n");
        }
        return Arrays.asList(newString.toString().split("\\\\n"));
    }
    public Map<String, String> getLanguageNodes() {
        return languageNodes;
    }

}
