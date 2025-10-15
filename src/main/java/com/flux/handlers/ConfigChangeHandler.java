package com.flux.handlers;

import com.flux.FluxPanel;
import com.flux.cards.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles configuration change events and updates the appropriate cards.
 */
public class ConfigChangeHandler {
    private static final String CONFIG_GROUP = "flux";

    private final FluxPanel panel;
    private final ConfigManager configManager;

    public ConfigChangeHandler(FluxPanel panel, ConfigManager configManager) {
        this.panel = panel;
        this.configManager = configManager;
    }

    /**
     * Processes a config change event.
     */
    public void handleConfigChange(ConfigChanged event) {
        if (!event.getGroup().equals(CONFIG_GROUP)) {
            return;
        }

        String key = event.getKey();
        System.out.println("CONFIG CHANGE EVENT KEY: " + key);

        // Route to appropriate handler
        if (isHomeCardEvent(key)) {
            handleHomeCardChange(key);
        } else if (isSotwEvent(key)) {
            handleSotwChange(key);
        } else if (isBotmEvent(key)) {
            handleBotmChange(key);
        } else if (isHuntEvent(key)) {
            handleHuntChange(key);
        }
    }

    private boolean isHomeCardEvent(String key) {
        return key.equals("plugin_announcement_message") || key.equals("rollCallActive");
    }

    private boolean isSotwEvent(String key) {
        return key.equals("sotw_active") || key.equals("sotwActive") ||
                key.equals("sotw_wom_link") || key.equals("sotwWomLink") ||
                key.equals("sotw_leaderboard") || key.equals("sotwLeaderboard");
    }

    private boolean isBotmEvent(String key) {
        return key.equals("botmActive") || key.equals("botm_active");
    }

    private boolean isHuntEvent(String key) {
        return key.startsWith("hunt") || key.equals("huntActive") || key.equals("huntTitle");
    }

    private void handleHomeCardChange(String key) {
        HomeCard homeCard = panel.getHomeCard();
        if (homeCard == null) return;

        if (key.equals("plugin_announcement_message")) {
            homeCard.refreshPluginAnnouncement();
            homeCard.refreshButtonLinks();
        } else if (key.equals("rollCallActive")) {
            System.out.println("Roll Call Status Changed");
            homeCard.isRollCallActive();
        }
    }

    private void handleSotwChange(String key) {
        SotwCard sotwCard = panel.getSotwCard();
        if (sotwCard == null) return;

        if (key.equals("sotw_active") || key.equals("sotwActive")) {
            panel.refreshAllCards();
            sotwCard.checkEventStateChanged();
        } else if (key.equals("sotw_wom_link") || key.equals("sotwWomLink")) {
            sotwCard.refreshButtonLinks();
        } else if (key.equals("sotw_leaderboard") || key.equals("sotwLeaderboard")) {
            logLeaderboard();
            panel.refreshAllCards();
        }
    }

    private void handleBotmChange(String key) {
        BotmCard botmCard = panel.getBotmCard();
        if (botmCard == null) return;

        if (key.equals("botmActive") || key.equals("botm_active")) {
            botmCard.checkEventStateChanged();
        }
    }

    private void handleHuntChange(String key) {
        HuntCard huntCard = panel.getHuntCard();
        HomeCard homeCard = panel.getHomeCard();

        if (key.equals("huntActive")) {
            System.out.println("Hunt Active Status Changed");
            if (huntCard != null) {
                huntCard.checkEventStateChanged();
            }
            if (homeCard != null) {
                homeCard.refreshHuntStatus();
            }
        } else if (key.equals("huntTitle")) {
            System.out.println("Hunt Title Changed");
            if (huntCard != null) {
                huntCard.updateEventTitle();
            }
        } else if (key.equals("hunt_team_1_name") || key.equals("hunt_team_2_name") ||
                key.equals("hunt_team_1_color") || key.equals("hunt_team_2_color")) {
            System.out.println("Hunt Team Names/Colors Changed");
            if (huntCard != null) {
                huntCard.updateTeamLabels();
            }
        } else if (key.equals("hunt_team_1_leaderboard") || key.equals("hunt_team_2_leaderboard")) {
            System.out.println("Hunt Leaderboard Changed - Refreshing tables");
            if (huntCard != null) {
                huntCard.refreshLeaderboards();
            }
        } else if (key.equals("hunt_wom_url") || key.equals("hunt_gdoc_url")) {
            System.out.println("Hunt Button URLs Changed");
            if (huntCard != null) {
                huntCard.refreshButtonLinks();
            }
        } else if (key.equals("hunt_team_1_score") || key.equals("hunt_team_2_score")) {
            System.out.println("Hunt Team Scores Changed");
            if (huntCard != null) {
                huntCard.updateTeamScores();
            }
        }
    }

    private void logLeaderboard() {
        String leaderboardJson = configManager.getConfiguration(CONFIG_GROUP, "sotwLeaderboard", String.class);

        if (leaderboardJson != null && !leaderboardJson.isEmpty()) {
            try {
                JSONArray leaderboardArray = new JSONArray(leaderboardJson);
                System.out.println("Current SOTW Leaderboard:");
                for (int i = 0; i < leaderboardArray.length(); i++) {
                    JSONObject entry = leaderboardArray.getJSONObject(i);
                    String username = entry.getString("username");
                    int xp = entry.getInt("xp");
                    System.out.printf(" - %-20s %,d XP%n", username, xp);
                }
            } catch (Exception e) {
                System.out.println("Error parsing leaderboard JSON: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Leaderboard data is empty or null.");
        }
    }
}