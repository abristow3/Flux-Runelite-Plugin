package com.flux.services.wom;

import net.runelite.client.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.flux.services.wom.CompetitionModels.CompetitionData;
import com.flux.services.wom.CompetitionModels.EventType;
import com.flux.services.wom.CompetitionModels.HuntTeamData;
import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;

@Slf4j
public class CompetitionConfigUpdater {
    private final ConfigManager configManager;

    public CompetitionConfigUpdater(ConfigManager configManager) {
        this.configManager = configManager;
    }

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

    public void setEventInactive(EventType type) {
        String prefix = type.getConfigPrefix();
        setConfigIfChanged(prefix + "Active", "false");
    }

    public String getConfig(String key, String defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private void saveSotwLeaderboard(LinkedHashMap<String, Integer> leaderboard) {
        JsonArray json = new JsonArray();
        leaderboard.forEach((username, xp) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("username", username);
            obj.addProperty("xp", xp);
            json.add(obj);
        });
        setConfigIfChanged("sotwLeaderboard", json.toString());
    }

    private void saveHuntTeamData(HuntTeamData huntData) {
        setConfigIfChanged("hunt_team_1_name", huntData.team1Name);
        setConfigIfChanged("hunt_team_2_name", huntData.team2Name);

        JsonArray team1Json = new JsonArray();
        huntData.team1Leaderboard.forEach((username, ehb) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("username", username);
            obj.addProperty("ehb", ehb);
            team1Json.add(obj);
        });
        setConfigIfChanged("hunt_team_1_leaderboard", team1Json.toString());

        JsonArray team2Json = new JsonArray();
        huntData.team2Leaderboard.forEach((username, ehb) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("username", username);
            obj.addProperty("ehb", ehb);
            team2Json.add(obj);
        });
        setConfigIfChanged("hunt_team_2_leaderboard", team2Json.toString());
    }

    private void setConfigIfChanged(String key, String value) {
        String currentValue = configManager.getConfiguration("flux", key);
        if (!value.equals(currentValue)) {
            configManager.setConfiguration("flux", key, value);
        }
    }
}
