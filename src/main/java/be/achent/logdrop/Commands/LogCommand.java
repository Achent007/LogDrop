package be.achent.logdrop.Commands;

import be.achent.logdrop.LogDrop;
import be.achent.logdrop.Managers.DropLogger;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LogCommand implements CommandExecutor, TabCompleter {

    private final LogDrop plugin;

    public LogCommand(LogDrop plugin) {
        this.plugin = plugin;
    }

    private int getLogsPerPage() {
        return plugin.getConfig().getInt("logs-per-page", 10);
    }

    private void sendHoverLog(CommandSender sender, String raw) {
        try {
            String cleaned = raw.contains("|") ? raw.substring(raw.indexOf("|") + 1) : raw;

            String[] parts = cleaned.split(";", 2);
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

    private void sendPaginatedLogs(CommandSender sender, List<String> logs, int page) {
        int logsPerPage = getLogsPerPage();
        int totalPages = (int) Math.ceil((double) logs.size() / logsPerPage);

        if (totalPages == 0) {
            sender.sendMessage(plugin.getLanguageManager().getString("no-logs"));
            return;
        }

        if (page < 1 || page > totalPages) {
            sender.sendMessage(plugin.getLanguageManager().getString("invalid-page"));
            return;
        }

        String header = plugin.getLanguageManager().getString("log-header")
                .replace("%page%", String.valueOf(page))
                .replace("%max%", String.valueOf(totalPages));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', header));

        int start = (page - 1) * logsPerPage;
        int end = Math.min(start + logsPerPage, logs.size());
        List<String> pagedLogs = logs.subList(start, end);

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
            sendPaginatedLogs(sender, logs, page);
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
            sendPaginatedLogs(sender, logs, page);
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

            sendPaginatedLogs(sender, logs, page);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("logdrop.use")) {
            return Collections.emptyList();
        }

        List<String> subcommands = List.of("reload", "history", "rayon");
        List<String> players = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        int logsPerPage = plugin.getConfig().getInt("logs-per-page", 10);

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(prefix)) {
                    completions.add(sub);
                }
            }
            for (String playerName : players) {
                if (playerName.toLowerCase().startsWith(prefix)) {
                    completions.add(playerName);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("rayon")) {
                List<String> radii = List.of("5", "10", "20", "30", "40", "50");
                String prefix = args[1];
                return radii.stream().filter(r -> r.startsWith(prefix)).collect(Collectors.toList());
            } else if (firstArg.equals("history")) {
                List<String> allLogs = DropLogger.getAllLogs();
                int maxPages = (int) Math.ceil((double) allLogs.size() / logsPerPage);
                return suggestPages(args[1], maxPages);
            } else {
                List<String> logs = DropLogger.getLogs(firstArg);
                int maxPages = (int) Math.ceil((double) logs.size() / logsPerPage);
                return suggestPages(args[1], maxPages);
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("rayon")) {
                List<String> logs = DropLogger.getLogsInRadius(((Player) sender).getLocation(), Integer.parseInt(args[1]));
                int maxPages = (int) Math.ceil((double) logs.size() / logsPerPage);
                return suggestPages(args[2], maxPages);
            }
        }

        return Collections.emptyList();
    }

    private List<String> suggestPages(String prefix, int maxPage) {
        maxPage = Math.min(maxPage, 50);
        List<String> pages = new ArrayList<>();
        for (int i = 1; i <= maxPage; i++) {
            String p = String.valueOf(i);
            if (p.startsWith(prefix)) {
                pages.add(p);
            }
        }
        return pages;
    }
}
