package be.achent.logdrop;

import be.achent.logdrop.Commands.LogCommand;
import be.achent.logdrop.ConfigManager.ConfigManager;
import be.achent.logdrop.ConfigManager.LanguageManager;
import be.achent.logdrop.Managers.DropListener;
import be.achent.logdrop.Managers.DropLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class LogDrop extends JavaPlugin {

    private static LogDrop instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        DropLogger.init();

        getServer().getPluginManager().registerEvents(new DropListener(), this);
        getCommand("logdrop").setExecutor(new LogCommand(this));

        getLogger().info("LogDrop activé !");
    }

    public static LogDrop getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void reload() {
        reloadConfig();
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
    }
}
