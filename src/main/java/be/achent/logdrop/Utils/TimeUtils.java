package be.achent.logdrop.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    public static long parseTime(String input) {
        long total = 0L;
        Matcher matcher = Pattern.compile("(\\d+)([dhms])").matcher(input.toLowerCase());
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
                case "d": total += value * 86400000L; break;
                case "h": total += value * 3600000L; break;
                case "m": total += value * 60000L; break;
                case "s": total += value * 1000L; break;
            }
        }
        return total;
    }
}