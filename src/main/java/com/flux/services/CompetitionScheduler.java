package com.flux.services;

import com.flux.services.wom.*;
import com.flux.services.wom.CompetitionModels.CompetitionData;
import com.flux.services.wom.CompetitionModels.EventType;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;


//Scheduler made for checking and updating comp data from wom API.
@Slf4j
public class CompetitionScheduler {
    // TODO in future release make configurable, next hunt is 8 months away
    private static final String DEFAULT_HUNT_COMPETITION_ID = "100262";
    private volatile boolean active = false;
    long initialDelaySeconds = 3;
    long periodMinutes = 3;

    private final ConfigManager configManager;
    private volatile ScheduledExecutorService schedulerService;
    private final WiseOldManApiClient apiClient;
    private final CompetitionFinder finder;
    private final CompetitionConfigUpdater configUpdater;

    public CompetitionScheduler(ConfigManager configManager,
                                WiseOldManApiClient apiClient,
                                CompetitionFinder finder,
                                CompetitionConfigUpdater configUpdater) {
        this.configManager = configManager;
        this.apiClient = apiClient;
        this.finder = finder;
        this.configUpdater = configUpdater;
    }

    // Starts the periodic wom comp check scheduler.
    public synchronized void start() {
        if (schedulerService != null && !schedulerService.isShutdown()) return;

        active = true;
        schedulerService = Executors.newSingleThreadScheduledExecutor();
        schedulerService.scheduleAtFixedRate(() -> {
            if (!active) return;
            checkAndUpdateCompetitions();
        }, initialDelaySeconds, periodMinutes * 60, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        active = false;
        if (schedulerService != null) {
            schedulerService.shutdown();
            schedulerService = null;
        }
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
                Optional<CompetitionData> lastCompleted = finder.findLastCompletedCompetition(type);
                if (lastCompleted.isPresent()) {
                    updateEvent(type, lastCompleted.get(), false);
                } else {
                    configUpdater.setEventInactive(type);
                }
            }
        }
    }

    private void checkHunt() {
        int huntCompId = getHuntCompetitionId();
        Optional<CompetitionData> huntData = finder.findHuntCompetition(huntCompId);
        if (huntData.isEmpty()) {
            log.error("Failed to fetch Hunt competition details");
            return;
        }

        Instant now = Instant.now();
        boolean isActive = huntData.get().isActive(now);

        // Always update config with Hunt data, even if not active
        updateEvent(EventType.HUNT, huntData.get(), isActive);
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