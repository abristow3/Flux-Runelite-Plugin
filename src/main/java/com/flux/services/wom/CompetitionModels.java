package com.flux.services.wom;

import java.time.Instant;
import java.util.LinkedHashMap;

/**
 * Data models for competition-related objects.
 */
public class CompetitionModels {

    /**
     * Complete competition data.
     */
    public static class CompetitionData {
        public final int competitionId;
        public final String title;
        public final Instant startTime;
        public final Instant endTime;
        public final LinkedHashMap<String, Integer> sotwLeaderboard;
        public final HuntTeamData huntTeamData;

        public CompetitionData(int competitionId, String title, Instant startTime, Instant endTime,
                               LinkedHashMap<String, Integer> sotwLeaderboard, HuntTeamData huntTeamData) {
            this.competitionId = competitionId;
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
            this.sotwLeaderboard = sotwLeaderboard;
            this.huntTeamData = huntTeamData;
        }

        public boolean isActive(Instant now) {
            return !now.isBefore(startTime) && now.isBefore(endTime);
        }

        public boolean hasEnded(Instant now) {
            return !now.isBefore(endTime);
        }
    }

    /**
     * Hunt team data including leaderboards and scores.
     */
    public static class HuntTeamData {
        public final String team1Name;
        public final String team2Name;
        public final String team1Color;
        public final String team2Color;
        public final LinkedHashMap<String, Double> team1Leaderboard;
        public final LinkedHashMap<String, Double> team2Leaderboard;
        public final int team1TotalScore;
        public final int team2TotalScore;

        public HuntTeamData(String team1Name, String team2Name,
                            String team1Color, String team2Color,
                            LinkedHashMap<String, Double> team1Leaderboard,
                            LinkedHashMap<String, Double> team2Leaderboard,
                            int team1TotalScore, int team2TotalScore) {
            this.team1Name = team1Name;
            this.team2Name = team2Name;
            this.team1Color = team1Color;
            this.team2Color = team2Color;
            this.team1Leaderboard = team1Leaderboard;
            this.team2Leaderboard = team2Leaderboard;
            this.team1TotalScore = team1TotalScore;
            this.team2TotalScore = team2TotalScore;
        }
    }

    /**
     * Hunt participant entry.
     */
    public static class HuntParticipant {
        public final String username;
        public final double ehb;

        public HuntParticipant(String username, double ehb) {
            this.username = username;
            this.ehb = ehb;
        }
    }

    /**
     * Event type enumeration.
     */
    public enum EventType {
        SOTW("sotw", "sotw"),
        BOTM("botm", "botm"),
        HUNT("the hunt", "hunt");

        private final String keyword;
        private final String configPrefix;

        EventType(String keyword, String configPrefix) {
            this.keyword = keyword;
            this.configPrefix = configPrefix;
        }

        public String getConfigPrefix() {
            return configPrefix;
        }

        /**
         * Checks if a competition title matches this event type.
         */
        public boolean matchesTitle(String title) {
            String lowerTitle = title.toLowerCase();

            switch (this) {
                case SOTW:
                    return lowerTitle.matches(".*\\bsotw\\b.*");
                case BOTM:
                    return lowerTitle.matches(".*\\bbotm\\b.*");
                case HUNT:
                    return false; // Hunt is fetched by ID, not title
                default:
                    return lowerTitle.contains(keyword);
            }
        }
    }
}