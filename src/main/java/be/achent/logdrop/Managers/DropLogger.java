package be.achent.logdrop.Managers;

import be.achent.logdrop.LogDrop;
import be.achent.logdrop.Utils.ItemHoverUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DropLogger {

    private static final File file = new File(LogDrop.getInstance().getDataFolder(), "droplogs.yml");
    private static final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy à HH'h'mm'min'ss's'");

    private static final Map<String, List<String>> dropLogs = new HashMap<>();

    public static void init() {
        purgeOldLogs();
    }

    public static void log(Player player, ItemStack stack, Location location) {
        String name = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                ? stack.getItemMeta().getDisplayName()
                : stack.getType().name().toLowerCase().replace("_", " ");

        String hover = ItemHoverUtil.getHover(stack);
        String time = fullDateFormat.format(new Date());
        int amount = stack.getAmount();

        String format = LogDrop.getInstance().getLanguageManager().getString("log-format");
        String line = format
                .replace("%time%", time)
                .replace("%player%", player.getName())
                .replace("%item%", "x" + amount + " " + name)
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()))
                .replace("%world%", location.getWorld().getName());

        String entry = player.getName() + "|" + line + ";" + hover;
        List<String> logs = config.getStringList("logs");
        logs.add(entry);
        config.set("logs", logs);
        save();
    }

    public static List<String> getAllLogs() {
        List<String> logs = config.getStringList("logs");
        Collections.reverse(logs);
        return logs;
    }

    public static List<String> getLogs(String playerName) {
        loadDropLogs();
        String lower = playerName.toLowerCase();

        List<String> logs = dropLogs.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(lower))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());

        Collections.reverse(logs);

        return logs;
    }

    public static List<String> getLogsInRadius(Location center, int radius) {
        List<String> allLogs = getAllLogs();
        List<String> filtered = new ArrayList<>();

        for (String log : allLogs) {
            try {
                String messagePart = log.split(";", 2)[0];
                String coordPart = messagePart.split("aux coordonnées")[1].split("dans le monde")[0].trim();
                String[] coords = coordPart.split(" ");
                if (coords.length == 3) {
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);

                    String worldName = messagePart.split("dans le monde")[1].trim();

                    if (!center.getWorld().getName().equals(worldName)) continue;

                    Location logLoc = new Location(center.getWorld(), x, y, z);

                    if (logLoc.distance(center) <= radius) {
                        filtered.add(log);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return filtered;
    }

    public static void loadDropLogs() {
        dropLogs.clear();
        List<String> allLogs = config.getStringList("logs");
        for (String log : allLogs) {
            try {
                String[] parts = log.split("\\|", 2);
                if (parts.length < 2) continue;
                String pseudo = parts[0].toLowerCase();

                dropLogs.putIfAbsent(pseudo, new ArrayList<>());
                dropLogs.get(pseudo).add(log);
            } catch (Exception ignored) {}
        }
    }

    private static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void purgeOldLogs() {
        String durationString = LogDrop.getInstance().getConfig().getString("logs-expire-after", "7d");
        long expireMillis = parseDurationToMillis(durationString);
        long now = System.currentTimeMillis();

        List<String> current = config.getStringList("logs");
        List<String> filtered = new ArrayList<>();

        for (String log : current) {
            try {
                long timestamp = getTimestamp(log, now);
                if (now - timestamp <= expireMillis) {
                    filtered.add(log);
                }
            } catch (Exception e) {
                filtered.add(log);
            }
        }

        config.set("logs", filtered);
        save();
    }

    private static long getTimestamp(String log, long now) throws Exception {
        String[] parts = log.split("\\|", 2);
        if (parts.length < 2) {
            throw new Exception("Format log invalide");
        }

        String message = parts[1];
        int start = message.indexOf('[');
        int end = message.indexOf(']');
        if (start == -1 || end == -1 || end <= start) {
            throw new Exception("Date non trouvée dans log");
        }

        String dateString = message.substring(start + 1, end);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy à HH'h'mm'min'ss's'");
        Date date = format.parse(dateString);
        return date.getTime();
    }

    private static long parseDurationToMillis(String input) {
        long millis = 0L;
        String remaining = input.toLowerCase();

        while (!remaining.isEmpty()) {
            if (remaining.matches("^\\d+[dhms].*")) {
                int i = 0;
                while (i < remaining.length() && Character.isDigit(remaining.charAt(i))) i++;
                long value = Long.parseLong(remaining.substring(0, i));
                char unit = remaining.charAt(i);
                remaining = remaining.substring(i + 1);

                switch (unit) {
                    case 'd': millis += TimeUnit.DAYS.toMillis(value); break;
                    case 'h': millis += TimeUnit.HOURS.toMillis(value); break;
                    case 'm': millis += TimeUnit.MINUTES.toMillis(value); break;
                    case 's': millis += TimeUnit.SECONDS.toMillis(value); break;
                }
            } else {
                break;
            }
        }

        return millis;
    }
}
