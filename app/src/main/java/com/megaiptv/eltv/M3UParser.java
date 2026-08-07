package com.megaiptv.eltv;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3UParser {
    private static final String TAG = "M3UParser";

    private static final Map<String, String[]> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("Sports", new String[]{"sport", "football", "soccer", "basketball", "tennis", "golf", "racing", "espn", "bein", "sky sport", "eurosport", "ufc", "wwe", "fight", "nba", "nfl", "mlb", "cricket"});
        CATEGORY_MAP.put("News", new String[]{"news", "cnn", "bbc", "al jazeera", "sky news", "france 24", "reuters", "info", "weather", "meteo", "journal", "actualité", "presse"});
        CATEGORY_MAP.put("Movies & Series", new String[]{"movie", "cinema", "film", "blockbuster", "hbo", "showtime", "starz", "cinemax", "cine", "action", "thriller", "horror", "comedy", "series", "tv show", "netflix", "prime", "drama", "soap", "sitcom"});
        CATEGORY_MAP.put("Kids", new String[]{"kids", "cartoon", "animation", "disney", "nickelodeon", "nick", "boomer", "baby", "junior", "teen", "anime", "manga", "pixar", "gulli", "canal j"});
        CATEGORY_MAP.put("Knowledge", new String[]{"culture", "edu", "art", "history", "museum", "learn", "school", "university", "savoir", "connaissance", "public", "doc", "discovery", "nat geo", "science", "animal", "planet", "wild", "geo", "voyage", "nature", "espace"});
        CATEGORY_MAP.put("Music", new String[]{"music", "mtv", "vh1", "radio", "rock", "pop", "jazz", "classical", "dance", "dj", "clip"});
        CATEGORY_MAP.put("Lifestyle", new String[]{"entertainment", "variety", "show", "reality", "lifestyle", "food", "cooking", "travel", "fashion", "divertissement", "loisir", "cuisine", "mode"});
    }

    public static Map<String, String> parseAttributes(String line) {
        Map<String, String> attrs = new HashMap<>();
        // Regex to match key="value", key='value', or key=value
        Pattern pattern = Pattern.compile("([a-z0-9-]+)=(\"[^\"]*\"|'[^']*'|[^,\\s]+)", Pattern.CASE_INSENSITIVE);
        
        int lastComma = line.lastIndexOf(',');
        String attrPart = lastComma != -1 ? line.substring(0, lastComma) : line;
        
        Matcher matcher = pattern.matcher(attrPart);
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = matcher.group(2);
            if (value != null && value.length() >= 2) {
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
            }
            attrs.put(key, value != null ? value.trim() : "");
        }
        return attrs;
    }

    public static String categorizeChannel(String name, String originalGroup) {
        if (originalGroup != null && !originalGroup.trim().isEmpty()) {
            String sanitizedGroup = originalGroup.replace(";", "/").trim();
            String searchStr = (name + " " + sanitizedGroup).toLowerCase();

            for (Map.Entry<String, String[]> entry : CATEGORY_MAP.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (searchStr.contains(keyword.toLowerCase())) {
                        return entry.getKey();
                    }
                }
            }
            return sanitizedGroup; // Use original if no mapping found
        }
        
        // No original group, try mapping by name only
        String searchName = name.toLowerCase();
        for (Map.Entry<String, String[]> entry : CATEGORY_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (searchName.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        return "General";
    }

    public static List<Channel> parseM3U(String content, String sourceId) {
        String[] lines = content.split("\\r?\\n");
        List<Channel> channels = new ArrayList<>();
        Channel currentChannel = null;

        Log.d(TAG, "Parsing M3U content, lines: " + lines.length);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                Map<String, String> attrs = parseAttributes(line);
                int lastComma = line.lastIndexOf(',');
                String name = lastComma != -1 ? line.substring(lastComma + 1).trim() : "Unknown Channel";
                
                String logo = attrs.get("tvg-logo");
                if (logo == null) logo = attrs.get("logo");
                if (logo == null) logo = "";
                
                String rawGroup = attrs.get("group-title");
                if (rawGroup == null) rawGroup = "";

                currentChannel = new Channel();
                currentChannel.setName(name);
                currentChannel.setLogo(logo);
                currentChannel.setCategory(categorizeChannel(name, rawGroup));
                currentChannel.setFavorite(false);
                currentChannel.setSourceId(sourceId);
            } else {
                String lowerLine = line.toLowerCase();
                if (lowerLine.startsWith("http") || lowerLine.startsWith("rtmp") || lowerLine.startsWith("rtsp")) {
                    if (currentChannel != null) {
                        currentChannel.setUrl(line);
                        channels.add(currentChannel);
                        currentChannel = null;
                    }
                }
            }
        }
        Log.d(TAG, "Parsing finished. Found " + channels.size() + " channels");
        return channels;
    }
}
