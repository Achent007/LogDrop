package be.achent.logdrop.ConfigManager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final File file;
    private final YamlConfiguration config;
    private final Map<String, String> cache = new HashMap<>();

    public LanguageManager(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "language.yml");
        if (!file.exists()) plugin.saveResource("language.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);
        reload();
    }

    public void reload() {
        try {
            config.load(file);
            cache.clear();
            for (String key : config.getKeys(false)) {
                cache.put(key, config.getString(key).replace("&", "§"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        return cache.getOrDefault(key, "&cMessage manquant: " + key);
    }
}
