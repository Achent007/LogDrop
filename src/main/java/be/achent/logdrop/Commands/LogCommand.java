package be.achent.logdrop.Commands;

import be.achent.logdrop.LogDrop;
import be.achent.logdrop.Managers.DropLogger;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class LogCommand implements CommandExecutor {

    private final LogDrop plugin;

    public LogCommand(LogDrop plugin) {
        this.plugin = plugin;
    }

    private int getLogsPerPage() {
        return plugin.getConfig().getInt("logs-per-page", 10);
    }

    private void sendHoverLog(CommandSender sender, String raw) {
        try {
            String[] parts = raw.split(";", 2);
            String message = parts[0];
            String hover = parts.length > 1 ? parts[1] : null;

            if (sender instanceof Player) {
                TextComponent component = new TextComponent(ChatColor.GRAY + message);
                if (hover != null && !hover.isEmpty()) {
                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(hover.replace("&", "§")).create()));
                }
                ((Player) sender).spigot().sendMessage(component);
            } else {
                sender.sendMessage(message);
            }
        } catch (Exception e) {
            sender.sendMessage(plugin.getLanguageManager().getString("log-error"));
        }
    }

    private void sendPaginatedLogs(CommandSender sender, int page) {
        int logsPerPage = getLogsPerPage();
        List<String> allLogs = DropLogger.getAllLogs();
        int totalPages = (int) Math.ceil((double) allLogs.size() / logsPerPage);

        if (page < 1 || page > totalPages) {
            sender.sendMessage(plugin.getLanguageManager().getString("invalid-page"));
            return;
        }

        String header = plugin.getLanguageManager().getString("log-header")
                .replace("%page%", String.valueOf(page))
                .replace("%max%", String.valueOf(totalPages));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', header));

        List<String> pagedLogs = DropLogger.getLogsPaged(page, logsPerPage);
        for (String log : pagedLogs) {
            sendHoverLog(sender, log);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("logdrop.use")) {
            sender.sendMessage(plugin.getLanguageManager().getString("no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getLanguageManager().reload();
            sender.sendMessage(plugin.getLanguageManager().getString("reload"));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("history")) {
            int page = args.length == 2 ? parsePage(args[1]) : 1;
            List<String> logs = DropLogger.getAllLogs();
            if (logs.isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getString("no-logs"));
                return true;
            }
            sendPaginatedLogs(sender, page);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("rayon")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getString("invalid-radius"));
                return true;
            }

            Player p = (Player) sender;
            int radius = parsePage(args[1]);
            int page = args.length == 3 ? parsePage(args[2]) : 1;

            List<String> logs = DropLogger.getLogsInRadius(p.getLocation(), radius);
            if (logs.isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getString("no-logs"));
                return true;
            }
            sendPaginatedLogs(sender, page);
            return true;
        }

        if (args.length >= 1) {
            String target = args[0];
            int page = args.length == 2 ? parsePage(args[1]) : 1;

            List<String> logs = DropLogger.getLogs(target);
            if (logs.isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getString("no-logs-player").replace("%player%", target));
                return true;
            }
            sendPaginatedLogs(sender, page);
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().getString("invalid-usage"));
        return true;
    }

    private int parsePage(String input) {
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
