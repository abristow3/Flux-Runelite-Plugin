package com.flux.services;

import com.flux.services.wom.*;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

import static com.flux.services.wom.CompetitionModels.*;

/**
 * Scheduler for checking and updating competition data from Wise Old Man API.
 * Delegates responsibilities to specialized classes for API calls, parsing, and config updates.
 */
@Slf4j
public class CompetitionScheduler {
    private static final int CHECK_INTERVAL_MINUTES = 7;
    private static final String DEFAULT_HUNT_COMPETITION_ID = "100262";

    private final ConfigManager configManager;
    private final ScheduledExecutorService schedulerService;

    private final WiseOldManApiClient apiClient;
    private final CompetitionDataParser dataParser;
    private final CompetitionFinder finder;
    private final CompetitionConfigUpdater configUpdater;

    public CompetitionScheduler(ConfigManager configManager) {
        this.configManager = configManager;
        this.schedulerService = Executors.newSingleThreadScheduledExecutor();

        // Initialize service components
        this.apiClient = new WiseOldManApiClient();
        this.dataParser = new CompetitionDataParser();
        this.finder = new CompetitionFinder(apiClient, dataParser);
        this.configUpdater = new CompetitionConfigUpdater(configManager);
    }

    /**
     * Starts the periodic competition check scheduler.
     */
    public void startScheduler() {
        schedulerService.scheduleAtFixedRate(
                this::checkAndUpdateCompetitions,
                0,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    /**
     * Stops the scheduler gracefully.
     */
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

    /**
     * Main method that checks all competitions and updates config.
     */
    private void checkAndUpdateCompetitions() {
        log.debug("Checking competitions...");

        try {
            checkSotwAndBotm();
            checkHunt();
        } catch (Exception e) {
            log.error("Error during scheduled check", e);
        }
    }

    /**
     * Checks SOTW and BOTM competitions from the group.
     */
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

    /**
     * Checks Hunt competition (fetched by specific competition ID).
     */
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

    /**
     * Updates config for a specific event.
     */
    private void updateEvent(EventType type, CompetitionData data, boolean isActive) {
        String womUrl = apiClient.getCompetitionUrl(data.competitionId);
        configUpdater.updateEventConfig(type, data, isActive, womUrl);
    }

    /**
     * Gets the Hunt competition ID from config, or uses default.
     */
    private int getHuntCompetitionId() {
        String huntCompIdStr = configUpdater.getConfig("hunt_competition_id", DEFAULT_HUNT_COMPETITION_ID);

        // Save default if not set
        if (huntCompIdStr.equals(DEFAULT_HUNT_COMPETITION_ID)) {
            configManager.setConfiguration("flux", "hunt_competition_id", DEFAULT_HUNT_COMPETITION_ID);
        }

        return Integer.parseInt(huntCompIdStr);
    }
}