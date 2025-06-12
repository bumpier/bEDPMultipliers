// File: src/main/java/net/bumpier/bedpmultipliers/utils/TimeUtil.java
package net.bumpier.bedpmultipliers.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public static long parseTime(String timeString) {
        long totalMillis = 0;
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "s":
                    totalMillis += TimeUnit.SECONDS.toMillis(value);
                    break;
                case "m":
                    totalMillis += TimeUnit.MINUTES.toMillis(value);
                    break;
                case "h":
                    totalMillis += TimeUnit.HOURS.toMillis(value);
                    break;
                case "d":
                    totalMillis += TimeUnit.DAYS.toMillis(value);
                    break;
            }
        }
        return totalMillis;
    }

    public static String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
    // Note: A utility for parsing and formatting time strings like "1d2h3m".
}