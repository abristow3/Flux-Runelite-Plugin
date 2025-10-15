package com.flux.services.wom;

import net.runelite.client.config.ConfigManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;

import static com.flux.services.wom.CompetitionModels.*;

/**
 * Updates RuneLite config with competition data.
 */
public class CompetitionConfigUpdater {
    private final ConfigManager configManager;

    public CompetitionConfigUpdater(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Updates config for a specific event type.
     */
    public void updateEventConfig(EventType type, CompetitionData data, boolean isActive, String womUrl) {
        String prefix = type.getConfigPrefix();

        setConfigIfChanged(prefix + "Title", data.title);
        setConfigIfChanged(prefix + "Active", String.valueOf(isActive));

        if (data.startTime != null) {
            setConfigIfChanged(prefix + "_start_time", data.startTime.toString());
        }
        if (data.endTime != null) {
            setConfigIfChanged(prefix + "_end_time", data.endTime.toString());
        }

        setConfigIfChanged(prefix + "_wom_link", womUrl);

        // Type-specific updates
        switch (type) {
            case BOTM:
                setConfigIfChanged("botmWomUrl", womUrl);
                break;

            case SOTW:
                if (data.sotwLeaderboard != null) {
                    saveSotwLeaderboard(data.sotwLeaderboard);
                    if (!isActive && !data.sotwLeaderboard.isEmpty()) {
                        String winner = data.sotwLeaderboard.keySet().iterator().next();
                        setConfigIfChanged(prefix + "_winner", winner);
                    }
                }
                break;

            case HUNT:
                setConfigIfChanged("hunt_wom_url", womUrl);
                if (data.huntTeamData != null) {
                    saveHuntTeamData(data.huntTeamData);
                }
                break;
        }
    }

    /**
     * Sets an event as inactive.
     */
    public void setEventInactive(EventType type) {
        String prefix = type.getConfigPrefix();
        setConfigIfChanged(prefix + "Active", "false");
    }

    /**
     * Gets a config value with a default fallback.
     */
    public String getConfig(String key, String defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private void saveSotwLeaderboard(LinkedHashMap<String, Integer> leaderboard) {
        JSONArray json = new JSONArray();
        leaderboard.forEach((username, xp) -> {
            JSONObject obj = new JSONObject();
            obj.put("username", username);
            obj.put("xp", xp);
            json.put(obj);
        });
        setConfigIfChanged("sotwLeaderboard", json.toString());
    }

    private void saveHuntTeamData(HuntTeamData huntData) {
        // Save team names and colors
        setConfigIfChanged("hunt_team_1_name", huntData.team1Name);
        setConfigIfChanged("hunt_team_2_name", huntData.team2Name);
        setConfigIfChanged("hunt_team_1_color", huntData.team1Color);
        setConfigIfChanged("hunt_team_2_color", huntData.team2Color);

        // Save Team 1 leaderboard
        JSONArray team1Json = new JSONArray();
        huntData.team1Leaderboard.forEach((username, ehb) -> {
            JSONObject obj = new JSONObject();
            obj.put("username", username);
            obj.put("ehb", ehb);
            team1Json.put(obj);
        });
        setConfigIfChanged("hunt_team_1_leaderboard", team1Json.toString());

        // Save Team 2 leaderboard
        JSONArray team2Json = new JSONArray();
        huntData.team2Leaderboard.forEach((username, ehb) -> {
            JSONObject obj = new JSONObject();
            obj.put("username", username);
            obj.put("ehb", ehb);
            team2Json.put(obj);
        });
        setConfigIfChanged("hunt_team_2_leaderboard", team2Json.toString());

        // Save team scores
        setConfigIfChanged("hunt_team_1_score", String.valueOf(huntData.team1TotalScore));
        setConfigIfChanged("hunt_team_2_score", String.valueOf(huntData.team2TotalScore));
    }

    private void setConfigIfChanged(String key, String value) {
        String currentValue = configManager.getConfiguration("flux", key);
        if (!value.equals(currentValue)) {
            configManager.setConfiguration("flux", key, value);
        }
    }
}