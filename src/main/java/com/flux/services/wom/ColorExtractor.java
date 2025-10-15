package com.flux.services.wom;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for extracting color codes from team names.
 */
public class ColorExtractor {
    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    private static final String DEFAULT_COLOR = "#FFFF00";

    static {
        COLOR_MAP.put("red", "#FF0000");
        COLOR_MAP.put("blue", "#0000FF");
        COLOR_MAP.put("green", "#00FF00");
        COLOR_MAP.put("yellow", "#FFFF00");
        COLOR_MAP.put("orange", "#FF8800");
        COLOR_MAP.put("purple", "#9900FF");
        COLOR_MAP.put("pink", "#FF00FF");
        COLOR_MAP.put("cyan", "#00FFFF");
        COLOR_MAP.put("gold", "#FFD700");
        COLOR_MAP.put("silver", "#C0C0C0");
        COLOR_MAP.put("bronze", "#CD7F32");
        COLOR_MAP.put("white", "#FFFFFF");
        COLOR_MAP.put("black", "#000000");
        COLOR_MAP.put("brown", "#8B4513");
        COLOR_MAP.put("lime", "#00FF00");
        COLOR_MAP.put("navy", "#000080");
        COLOR_MAP.put("teal", "#008080");
        COLOR_MAP.put("maroon", "#800000");
        COLOR_MAP.put("olive", "#808000");
        COLOR_MAP.put("aqua", "#00FFFF");
        COLOR_MAP.put("fuchsia", "#FF00FF");
    }

    /**
     * Extracts a hex color code from a team name.
     * Supports color names like "Team Red", "Gold", "Blue", etc.
     */
    public static String extractColorFromTeamName(String teamName) {
        if (teamName == null || teamName.isEmpty()) {
            return DEFAULT_COLOR;
        }

        String lowerName = teamName.toLowerCase();

        for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return DEFAULT_COLOR;
    }
}