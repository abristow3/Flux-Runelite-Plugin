package com.flux.services.wom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

import static com.flux.services.wom.CompetitionModels.*;

@Slf4j
public class CompetitionDataParser {
    private static final int TOP_PARTICIPANTS_COUNT = 10;

    // Parse SOTW leaderboard from WOM competition details.
    public LinkedHashMap<String, Integer> parseSotwLeaderboard(JsonObject competitionDetails) {
        LinkedHashMap<String, Integer> leaderboard = new LinkedHashMap<>();

        try {
            JsonArray participants = competitionDetails.has("participations")
                    ? competitionDetails.getAsJsonArray("participations")
                    : new JsonArray();
            List<ParticipantEntry> entries = new ArrayList<>();

            for (JsonElement element : participants) {
                JsonObject participant = element.getAsJsonObject();
                JsonObject player = participant.getAsJsonObject("player");

                String username = player != null && player.has("username")
                        ? player.get("username").getAsString()
                        : "";

                JsonObject progress = participant.has("progress")
                        ? participant.getAsJsonObject("progress")
                        : null;

                int xpGained = (progress != null && progress.has("gained"))
                        ? progress.get("gained").getAsInt()
                        : 0;


                entries.add(new ParticipantEntry(username, xpGained));
            }

            // Sort by XP descending and take top 10
            entries.stream()
                    .sorted((a, b) -> Integer.compare(b.xp, a.xp))
                    .limit(TOP_PARTICIPANTS_COUNT)
                    .forEach(entry -> leaderboard.put(entry.username, entry.xp));

        } catch (Exception e) {
            log.error("Error parsing SOTW leaderboard: " + e);
        }

        return leaderboard;
    }

    // Parses Hunt team data from competition JSON payload.
    public HuntTeamData parseHuntTeamData(JsonObject competitionDetails) {
        try {
            JsonArray participants = competitionDetails.has("participations")
                    ? competitionDetails.getAsJsonArray("participations")
                    : new JsonArray();

            // Extract team names
            TeamNames teamNames = extractTeamNames(participants);

            // Collect participants by team
            List<HuntParticipant> team1Participants = new ArrayList<>();
            List<HuntParticipant> team2Participants = new ArrayList<>();

            for (JsonElement element : participants) {
                JsonObject participant = element.getAsJsonObject();

                JsonObject player = participant.getAsJsonObject("player");
                String username = (player != null && player.has("displayName"))
                        ? player.get("displayName").getAsString()
                        : "";

                String teamName = participant.has("teamName")
                        ? participant.get("teamName").getAsString()
                        : "";

                JsonObject progress = participant.has("progress")
                        ? participant.getAsJsonObject("progress")
                        : null;

                double ehb = (progress != null && progress.has("gained"))
                        ? progress.get("gained").getAsDouble()
                        : 0.0;

                if (teamName.equals(teamNames.team1)) {
                    team1Participants.add(new HuntParticipant(username, ehb));
                } else if (teamName.equals(teamNames.team2)) {
                    team2Participants.add(new HuntParticipant(username, ehb));
                }
            }

            // Sort and get top 10 for each team
            LinkedHashMap<String, Double> team1Top10 = getTopParticipants(team1Participants);
            LinkedHashMap<String, Double> team2Top10 = getTopParticipants(team2Participants);

            // Calculate total scores
            double team1Total = team1Participants.stream().mapToDouble(p -> p.ehb).sum();
            double team2Total = team2Participants.stream().mapToDouble(p -> p.ehb).sum();

            return new HuntTeamData(
                    teamNames.team1,
                    teamNames.team2,
                    team1Top10,
                    team2Top10,
                    (int) Math.round(team1Total),
                    (int) Math.round(team2Total)
            );

        } catch (Exception e) {
            log.error("Error parsing Hunt team data: ", e);
        }

        return null;
    }


    private TeamNames extractTeamNames(JsonArray participants) {
        Set<String> teamNamesSet = new HashSet<>();

        for (JsonElement element : participants) {
            JsonObject participant = element.getAsJsonObject();
            String teamName = participant.has("teamName")
                    ? participant.get("teamName").getAsString()
                    : "";
            if (!teamName.isEmpty()) {
                teamNamesSet.add(teamName);
            }
        }

        if (teamNamesSet.size() >= 2) {
            Iterator<String> iter = teamNamesSet.iterator();
            String team1 = iter.next();
            String team2 = iter.next();
            return new TeamNames(team1, team2);
        }

        return new TeamNames("Team 1", "Team 2");
    }


    private LinkedHashMap<String, Double> getTopParticipants(List<HuntParticipant> participants) {
        LinkedHashMap<String, Double> top10 = new LinkedHashMap<>();

        participants.stream()
                .sorted((a, b) -> Double.compare(b.ehb, a.ehb))
                .limit(TOP_PARTICIPANTS_COUNT)
                .forEach(p -> top10.put(p.username, p.ehb));

        return top10;
    }

    // Helper classes
    private static class ParticipantEntry {
        final String username;
        final int xp;

        ParticipantEntry(String username, int xp) {
            this.username = username;
            this.xp = xp;
        }
    }

    private static class TeamNames {
        final String team1;
        final String team2;

        TeamNames(String team1, String team2) {
            this.team1 = team1;
            this.team2 = team2;
        }
    }
}
