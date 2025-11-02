package com.flux.services.wom;

import org.json.JSONArray;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static com.flux.services.wom.CompetitionModels.*;

@Slf4j
public class CompetitionFinder {
    private final WiseOldManApiClient apiClient;
    private final CompetitionDataParser dataParser;

    public CompetitionFinder(WiseOldManApiClient apiClient, CompetitionDataParser dataParser) {
        this.apiClient = apiClient;
        this.dataParser = dataParser;
    }

    public Map<EventType, CompetitionData> findActiveCompetitions() {
        Map<EventType, CompetitionData> results = new EnumMap<>(EventType.class);
        results.put(EventType.SOTW, null);
        results.put(EventType.BOTM, null);

        try {
            JSONArray competitions = apiClient.fetchGroupCompetitions();
            Instant now = Instant.now();

            for (int i = 0; i < competitions.length(); i++) {
                JSONObject comp = competitions.getJSONObject(i);

                Instant startsAt = Instant.parse(comp.getString("startsAt"));
                Instant endsAt = Instant.parse(comp.getString("endsAt"));

                // Skip if not currently active
                if (now.isBefore(startsAt) || !now.isBefore(endsAt)) {
                    continue;
                }

                String title = comp.getString("title").toLowerCase();
                int competitionId = comp.getInt("id");

                // Check for SOTW and BOTM
                for (EventType type : new EventType[]{EventType.SOTW, EventType.BOTM}) {
                    if (type.matchesTitle(title) && results.get(type) == null) {
                        CompetitionData data = fetchCompetitionData(competitionId, type, startsAt, endsAt);
                        if (data != null) {
                            results.put(type, data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error finding active competitions: " + e);
        }

        return results;
    }

    public CompetitionData findLastCompletedCompetition(EventType type) {
        // Hunt is handled differently
        if (type == EventType.HUNT) {
            return null; 
        }

        try {
            JSONArray competitions = apiClient.fetchGroupCompetitions();
            Instant now = Instant.now();

            JSONObject mostRecentCompleted = null;
            Instant mostRecentEndTime = null;

            for (int i = 0; i < competitions.length(); i++) {
                JSONObject comp = competitions.getJSONObject(i);
                String title = comp.getString("title").toLowerCase();

                if (!type.matchesTitle(title)) {
                    continue;
                }

                Instant endsAt = Instant.parse(comp.getString("endsAt"));

                // Skip if not yet ended
                if (now.isBefore(endsAt)) {
                    continue;
                }

                // Check if more recent
                if (mostRecentEndTime == null || endsAt.isAfter(mostRecentEndTime)) {
                    mostRecentCompleted = comp;
                    mostRecentEndTime = endsAt;
                }
            }

            if (mostRecentCompleted != null) {
                int competitionId = mostRecentCompleted.getInt("id");
                Instant startsAt = Instant.parse(mostRecentCompleted.getString("startsAt"));
                Instant endsAt = Instant.parse(mostRecentCompleted.getString("endsAt"));

                return fetchCompetitionData(competitionId, type, startsAt, endsAt);
            }
        } catch (Exception e) {
            log.error("Error finding last completed " + type.name() + ": " + e);
        }

        return null;
    }

    // Get Hunt competition data by ID
    public CompetitionData findHuntCompetition(int competitionId) {
        try {
            JSONObject details = apiClient.fetchCompetitionDetails(competitionId);
            if (details == null) {
                return null;
            }

            Instant startsAt = Instant.parse(details.getString("startsAt"));
            Instant endsAt = Instant.parse(details.getString("endsAt"));

            HuntTeamData huntData = dataParser.parseHuntTeamData(details);

            return new CompetitionData(
                    competitionId,
                    details.getString("title"),
                    startsAt,
                    endsAt,
                    null,
                    huntData
            );
        } catch (Exception e) {
            log.error("Error finding Hunt competition: " + e);
        }

        return null;
    }

    private CompetitionData fetchCompetitionData(int competitionId, EventType type,
                                                 Instant startsAt, Instant endsAt) {
        try {
            JSONObject details = apiClient.fetchCompetitionDetails(competitionId);
            if (details == null) {
                return null;
            }

            return new CompetitionData(
                    competitionId,
                    details.getString("title"),
                    startsAt,
                    endsAt,
                    type == EventType.SOTW ? dataParser.parseSotwLeaderboard(details) : null,
                    null
            );
        } catch (Exception e) {
            log.error("Error fetching competition " + competitionId + ": " + e);
        }

        return null;
    }
}