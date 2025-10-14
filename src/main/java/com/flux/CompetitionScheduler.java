package com.flux;

import net.runelite.client.config.ConfigManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class CompetitionScheduler {
    private static final String BASE_API_URL = "https://api.wiseoldman.net/v2";
    private static final String GROUP_ID = "141";
    private static final int CHECK_INTERVAL_MINUTES = 7;
    private static final int TOP_PARTICIPANTS_COUNT = 10;
    
    private final ConfigManager configManager;
    private final ScheduledExecutorService schedulerService;

    public CompetitionScheduler(ConfigManager configManager) {
        this.configManager = configManager;
        this.schedulerService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startScheduler() {
        schedulerService.scheduleAtFixedRate(
            this::checkCompetitionsAndUpdateConfig,
            0,
            CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    public void stopScheduler() {
        schedulerService.shutdown();
        try {
            if (!schedulerService.awaitTermination(5, TimeUnit.SECONDS)) {
                schedulerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            schedulerService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void checkCompetitionsAndUpdateConfig() {
        System.out.println("\n[" + Instant.now() + "] Checking competitions...");

        try {
            Map<EventType, CompetitionData> activeCompetitions = fetchActiveCompetitions();
            
            for (Map.Entry<EventType, CompetitionData> entry : activeCompetitions.entrySet()) {
                EventType type = entry.getKey();
                CompetitionData data = entry.getValue();
                
                if (data != null) {
                    System.out.println("Found active " + type.name() + " competition: " + data.title);
                    updateConfigForEvent(type, data);
                } else {
                    System.out.println("No active " + type.name() + " competition found.");
                    setEventInactive(type);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during scheduled check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<EventType, CompetitionData> fetchActiveCompetitions() {
        Map<EventType, CompetitionData> results = new EnumMap<>(EventType.class);
        
        // Initialize all as inactive
        for (EventType type : EventType.values()) {
            results.put(type, null);
        }

        try {
            JSONArray competitions = fetchCompetitionsList();
            Instant now = Instant.now();

            for (int i = 0; i < competitions.length(); i++) {
                JSONObject comp = competitions.getJSONObject(i);
                Instant startsAt = Instant.parse(comp.getString("startsAt"));
                Instant endsAt = Instant.parse(comp.getString("endsAt"));

                // Check if competition is currently active
                if (now.isBefore(startsAt) || !now.isBefore(endsAt)) {
                    continue;
                }

                String title = comp.getString("title").toLowerCase();
                int competitionId = comp.getInt("id");

                // Check each event type
                for (EventType type : EventType.values()) {
                    if (title.contains(type.getKeyword()) && results.get(type) == null) {
                        JSONObject details = fetchCompetitionDetails(competitionId);
                        if (details != null) {
                            CompetitionData data = new CompetitionData(
                                competitionId,
                                comp.getString("title"),
                                startsAt,
                                endsAt,
                                type == EventType.SOTW ? getTop10Participants(details) : null
                            );
                            results.put(type, data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching competitions: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private JSONArray fetchCompetitionsList() throws Exception {
        String urlString = BASE_API_URL + "/groups/" + GROUP_ID + "/competitions";
        String response = makeHttpRequest(urlString);
        return new JSONArray(response);
    }

    private JSONObject fetchCompetitionDetails(int competitionId) {
        try {
            String urlString = BASE_API_URL + "/competitions/" + competitionId;
            String response = makeHttpRequest(urlString);
            return new JSONObject(response);
        } catch (Exception e) {
            System.err.println("Error fetching competition details for ID " + competitionId + ": " + e.getMessage());
            return null;
        }
    }

    private String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } finally {
            conn.disconnect();
        }
    }

    private LinkedHashMap<String, Integer> getTop10Participants(JSONObject competitionDetails) {
        LinkedHashMap<String, Integer> leaderboard = new LinkedHashMap<>();

        try {
            JSONArray participants = competitionDetails.getJSONArray("participations");
            List<ParticipantEntry> entries = new ArrayList<>();

            for (int i = 0; i < participants.length(); i++) {
                JSONObject participant = participants.getJSONObject(i);
                String username = participant.getJSONObject("player").getString("username");
                
                JSONObject progress = participant.optJSONObject("progress");
                int xpGained = progress != null ? progress.optInt("gained", 0) : 0;

                entries.add(new ParticipantEntry(username, xpGained));
            }

            // Sort by XP descending and take top 10
            entries.stream()
                .sorted((a, b) -> Integer.compare(b.xp, a.xp))
                .limit(TOP_PARTICIPANTS_COUNT)
                .forEach(entry -> leaderboard.put(entry.username, entry.xp));

        } catch (Exception e) {
            System.err.println("Error parsing leaderboard: " + e.getMessage());
        }

        return leaderboard;
    }

    private void updateConfigForEvent(EventType type, CompetitionData data) {
        String prefix = type.getConfigPrefix();
        
        // Update title
        setConfigIfChanged(prefix + "Title", data.title);
        
        // Set active status
        setConfigIfChanged(prefix + "Active", "true");
        
        // Update times
        if (data.startTime != null) {
            setConfigIfChanged(prefix + "_start_time", data.startTime.toString());
        }
        if (data.endTime != null) {
            setConfigIfChanged(prefix + "_end_time", data.endTime.toString());
        }
        
        // Update WOM link
        String womLink = "https://wiseoldman.net/competitions/" + data.competitionId;
        setConfigIfChanged(prefix + "_wom_link", womLink);
        
        // For BOTM, also update the botmWomUrl
        if (type == EventType.BOTM) {
            setConfigIfChanged("botmWomUrl", womLink);
        }
        
        // Update leaderboard (SOTW only)
        if (type == EventType.SOTW && data.leaderboard != null) {
            saveLeaderboardToConfig(data.leaderboard);
        }
    }

    private void setEventInactive(EventType type) {
        String prefix = type.getConfigPrefix();
        setConfigIfChanged(prefix + "Active", "false");
    }

    private void setConfigIfChanged(String key, String value) {
        String currentValue = configManager.getConfiguration("flux", key);
        if (!value.equals(currentValue)) {
            configManager.setConfiguration("flux", key, value);
        }
    }

    private void saveLeaderboardToConfig(LinkedHashMap<String, Integer> leaderboard) {
        JSONArray json = new JSONArray();
        leaderboard.forEach((username, xp) -> {
            JSONObject obj = new JSONObject();
            obj.put("username", username);
            obj.put("xp", xp);
            json.put(obj);
        });

        String jsonString = json.toString();
        setConfigIfChanged("sotwLeaderboard", jsonString);
    }

    // ========== Helper Classes ==========

    private enum EventType {
        SOTW("sotw", "sotw"),
        BOTM("botm", "botm"),
        HUNT("hunt", "hunt");

        private final String keyword;
        private final String configPrefix;

        EventType(String keyword, String configPrefix) {
            this.keyword = keyword;
            this.configPrefix = configPrefix;
        }

        public String getKeyword() {
            return keyword;
        }

        public String getConfigPrefix() {
            return configPrefix;
        }
    }

    private static class CompetitionData {
        final int competitionId;
        final String title;
        final Instant startTime;
        final Instant endTime;
        final LinkedHashMap<String, Integer> leaderboard;

        CompetitionData(int competitionId, String title, Instant startTime, Instant endTime,
                       LinkedHashMap<String, Integer> leaderboard) {
            this.competitionId = competitionId;
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
            this.leaderboard = leaderboard;
        }
    }

    private static class ParticipantEntry {
        final String username;
        final int xp;

        ParticipantEntry(String username, int xp) {
            this.username = username;
            this.xp = xp;
        }
    }
}