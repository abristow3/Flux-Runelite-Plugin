package com.flux.services.wom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

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

        JsonArray competitions = apiClient.fetchGroupCompetitions();
        Instant now = Instant.now();

        for (JsonElement element : competitions) {
            JsonObject comp = element.getAsJsonObject();

            Instant startsAt = Instant.parse(comp.get("startsAt").getAsString());
            Instant endsAt = Instant.parse(comp.get("endsAt").getAsString());

            // Skip if not currently active
            if (now.isBefore(startsAt) || !now.isBefore(endsAt)) {
                continue;
            }

            String title = comp.get("title").getAsString().toLowerCase();
            int competitionId = comp.get("id").getAsInt();

            // Check for SOTW and BOTM
            for (EventType type : new EventType[]{EventType.SOTW, EventType.BOTM}) {
                if (type.matchesTitle(title) && results.get(type) == null) {
                    Optional<CompetitionData> data = fetchCompetitionData(competitionId, type, startsAt, endsAt);
                    if (data.isPresent()) {
                        results.put(type, data.get());
                    }
                }
            }
        }

        return results;
    }


    public Optional<CompetitionData> findLastCompletedCompetition(EventType type) {
        // Hunt is handled differently
        if (type == EventType.HUNT) {
            return Optional.empty();
        }

        JsonArray competitions = apiClient.fetchGroupCompetitions();
        Instant now = Instant.now();

        JsonObject mostRecentCompleted = null;
        Instant mostRecentEndTime = null;

        for (JsonElement element : competitions) {
            JsonObject comp = element.getAsJsonObject();
            String title = comp.get("title").getAsString().toLowerCase();

            if (!type.matchesTitle(title)) {
                continue;
            }

            Instant endsAt = Instant.parse(comp.get("endsAt").getAsString());

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
            int competitionId = mostRecentCompleted.get("id").getAsInt();
            Instant startsAt = Instant.parse(mostRecentCompleted.get("startsAt").getAsString());
            Instant endsAt = Instant.parse(mostRecentCompleted.get("endsAt").getAsString());

            return fetchCompetitionData(competitionId, type, startsAt, endsAt);
        }

        return Optional.empty();
    }


    // Get Hunt competition data by ID
    public Optional<CompetitionData> findHuntCompetition(int competitionId) {
        JsonObject details = apiClient.fetchCompetitionDetails(competitionId);
        if (details == null || details.getAsString().isEmpty()) {
            return Optional.empty();
        }

        Instant startsAt = Instant.parse(details.get("startsAt").getAsString());
        Instant endsAt = Instant.parse(details.get("endsAt").getAsString());

        HuntTeamData huntData = dataParser.parseHuntTeamData(details);

        return Optional.of(new CompetitionData(
                competitionId,
                details.get("title").getAsString(),
                startsAt,
                endsAt,
                null,
                huntData
        ));
    }


    private Optional<CompetitionData> fetchCompetitionData(int competitionId, EventType type,
                                                 Instant startsAt, Instant endsAt) {
        JsonObject details = apiClient.fetchCompetitionDetails(competitionId);
        if(details.isJsonNull()) {
            return Optional.empty();
        }

        return Optional.of(new CompetitionData(
                competitionId,
                details.get("title").getAsString(),
                startsAt,
                endsAt,
                type == EventType.SOTW ? dataParser.parseSotwLeaderboard(details) : null,
                null
        ));
    }
}