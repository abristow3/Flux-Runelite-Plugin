package com.flux.services;

import com.flux.services.wom.*;
import com.flux.services.wom.CompetitionModels.CompetitionData;
import com.flux.services.wom.CompetitionModels.EventType;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import com.google.inject.Inject;
import static com.flux.services.wom.CompetitionModels.*;


//Scheduler made for checking and updating comp data from wom API.
@Slf4j
public class CompetitionScheduler {
    private static final int CHECK_INTERVAL_MINUTES = 7;
    // TODO in future release make configurable, next hunt is 8 months away
    private static final String DEFAULT_HUNT_COMPETITION_ID = "100262";

    private final ConfigManager configManager;
    private final ScheduledExecutorService schedulerService;

    private final WiseOldManApiClient apiClient;
    private final CompetitionDataParser dataParser;
    private final CompetitionFinder finder;
    private final CompetitionConfigUpdater configUpdater;

    @Inject
    public CompetitionScheduler(ConfigManager configManager,
                                WiseOldManApiClient apiClient,
                                CompetitionDataParser dataParser,
                                CompetitionFinder finder,
                                CompetitionConfigUpdater configUpdater) {
        this.configManager = configManager;
        this.schedulerService = Executors.newSingleThreadScheduledExecutor();
        
        this.apiClient = apiClient;
        this.dataParser = dataParser;
        this.finder = finder;
        this.configUpdater = configUpdater;
    }

    // Starts the periodic wom comp check scheduler.
    public void startScheduler() {
        schedulerService.scheduleAtFixedRate(
                this::checkAndUpdateCompetitions,
                0,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public void shutdown() {
        schedulerService.shutdownNow();
    }

    private void checkAndUpdateCompetitions() {
        log.debug("Checking competitions...");

        try {
            checkSotwAndBotm();
            checkHunt();
        } catch (Exception e) {
            log.error("Error during scheduled check", e);
        }
    }

    private void checkSotwAndBotm() {
        Map<EventType, CompetitionData> activeCompetitions = finder.findActiveCompetitions();

        for (Map.Entry<EventType, CompetitionData> entry : activeCompetitions.entrySet()) {
            EventType type = entry.getKey();
            CompetitionData data = entry.getValue();

            if (data != null) {
                updateEvent(type, data, true);
            } else {
                CompetitionData lastCompleted = finder.findLastCompletedCompetition(type);
                if (lastCompleted != null) {
                    updateEvent(type, lastCompleted, false);
                } else {
                    configUpdater.setEventInactive(type);
                }
            }
        }
    }

    private void checkHunt() {
        try {
            int huntCompId = getHuntCompetitionId();
            CompetitionData huntData = finder.findHuntCompetition(huntCompId);
            if (huntData == null) {
                log.error("Failed to fetch Hunt competition details");
                return;
            }

            Instant now = Instant.now();
            boolean isActive = huntData.isActive(now);

            // Always update config with Hunt data, even if not active
            updateEvent(EventType.HUNT, huntData, isActive);
        } catch (Exception e) {
            log.error("Error checking Hunt competition", e);
            e.printStackTrace();
        }
    }

    private void updateEvent(EventType type, CompetitionData data, boolean isActive) {
        String womUrl = apiClient.getCompetitionUrl(data.competitionId);
        configUpdater.updateEventConfig(type, data, isActive, womUrl);
    }

    private int getHuntCompetitionId() {
        String huntCompIdStr = configUpdater.getConfig("hunt_competition_id", DEFAULT_HUNT_COMPETITION_ID);

        // Save default if not set
        if (huntCompIdStr.equals(DEFAULT_HUNT_COMPETITION_ID)) {
            configManager.setConfiguration("flux", "hunt_competition_id", DEFAULT_HUNT_COMPETITION_ID);
        }

        return Integer.parseInt(huntCompIdStr);
    }
}