package com.flux.services.wom;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import static com.flux.services.wom.CompetitionModels.*;

/**
 * Parses WOM API responses into structured competition data.
 */
@Slf4j
public class CompetitionDataParser {
    private static final int TOP_PARTICIPANTS_COUNT = 10;

    /**
     * Parses SOTW leaderboard from competition details.
     */
    public LinkedHashMap<String, Integer> parseSotwLeaderboard(JSONObject competitionDetails) {
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
            log.error("Error parsing SOTW leaderboard: " + e);
        }

        return leaderboard;
    }

    /**
     * Parses Hunt team data from competition details.
     */
    public HuntTeamData parseHuntTeamData(JSONObject competitionDetails) {
        try {
            JSONArray participants = competitionDetails.getJSONArray("participations");

            // Extract team names
            TeamNames teamNames = extractTeamNames(participants);

            // Collect participants by team
            List<HuntParticipant> team1Participants = new ArrayList<>();
            List<HuntParticipant> team2Participants = new ArrayList<>();

            for (int i = 0; i < participants.length(); i++) {
                JSONObject participant = participants.getJSONObject(i);
                String username = participant.getJSONObject("player").getString("displayName");
                String teamName = participant.optString("teamName", "");

                JSONObject progress = participant.optJSONObject("progress");
                double ehb = progress != null ? progress.optDouble("gained", 0.0) : 0.0;

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
                    teamNames.team1Color,
                    teamNames.team2Color,
                    team1Top10,
                    team2Top10,
                    (int) Math.round(team1Total),
                    (int) Math.round(team2Total)
            );

        } catch (Exception e) {
            log.error("Error parsing Hunt team data: " + e);
        }

        return null;
    }

    private TeamNames extractTeamNames(JSONArray participants) {
        Set<String> teamNames = new HashSet<>();

        for (int i = 0; i < participants.length(); i++) {
            JSONObject participant = participants.getJSONObject(i);
            String teamName = participant.optString("teamName", "");
            if (!teamName.isEmpty()) {
                teamNames.add(teamName);
            }
        }

        if (teamNames.size() >= 2) {
            Iterator<String> iter = teamNames.iterator();
            String team1 = iter.next();
            String team2 = iter.next();

            return new TeamNames(
                    team1,
                    team2,
                    ColorExtractor.extractColorFromTeamName(team1),
                    ColorExtractor.extractColorFromTeamName(team2)
            );
        }

        return new TeamNames("Team 1", "Team 2", "#FFFF00", "#FF0000");
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
        final String team1Color;
        final String team2Color;

        TeamNames(String team1, String team2, String team1Color, String team2Color) {
            this.team1 = team1;
            this.team2 = team2;
            this.team1Color = team1Color;
            this.team2Color = team2Color;
        }
    }
}