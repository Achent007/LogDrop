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

public class DropLogger {

    private static final File file = new File(LogDrop.getInstance().getDataFolder(), "droplogs.yml");
    private static final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd-MM-yyyy à HH'h'mm'min'ss's'");

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

        String entry = line + ";" + hover;
        List<String> logs = config.getStringList("logs");
        logs.add(entry);
        config.set("logs", logs);
        save();
    }

    public static List<String> getAllLogs() {
        return config.getStringList("logs");
    }

    public static List<String> getLogs(String playerName) {
        List<String> allLogs = getAllLogs();
        List<String> filtered = new ArrayList<>();

        String lowerName = playerName.toLowerCase();

        for (String log : allLogs) {
            String messagePart = log.split(";", 2)[0].toLowerCase();

            if (messagePart.contains(lowerName)) {
                filtered.add(log);
            }
        }
        return filtered;
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

    public static List<String> getLogsPaged(int page, int logsPerPage) {
        List<String> allLogs = new ArrayList<>(getAllLogs());
        Collections.reverse(allLogs);
        int start = Math.max(0, (page - 1) * logsPerPage);
        int end = Math.min(start + logsPerPage, allLogs.size());
        if (start >= end) return Collections.emptyList();
        return allLogs.subList(start, end);
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

    private static long getTimestamp(String log, long now) {
        String[] parts = log.split(";", 2);
        String timePart = parts[0];

        String hourStr = timePart.substring(1, 3);
        String minuteStr = timePart.substring(4, 6);
        String secondStr = timePart.substring(10, 12);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourStr));
        cal.set(Calendar.MINUTE, Integer.parseInt(minuteStr));
        cal.set(Calendar.SECOND, Integer.parseInt(secondStr));
        cal.set(Calendar.MILLISECOND, 0);

        long timestamp = cal.getTimeInMillis();

        if (timestamp > now) {
            timestamp -= TimeUnit.DAYS.toMillis(1);
        }
        return timestamp;
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
                    case 'd':
                        millis += TimeUnit.DAYS.toMillis(value);
                        break;
                    case 'h':
                        millis += TimeUnit.HOURS.toMillis(value);
                        break;
                    case 'm':
                        millis += TimeUnit.MINUTES.toMillis(value);
                        break;
                    case 's':
                        millis += TimeUnit.SECONDS.toMillis(value);
                        break;
                }
            } else {
                break;
            }
        }

        return millis;
    }
}
