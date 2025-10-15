package com.flux.services;

import net.runelite.api.Client;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Monitors the player's clan rank and notifies when it changes.
 */
public class ClanRankMonitor {
    private static final String TARGET_CLAN_NAME = "Flux";
    private static final int CHECK_DELAY_SECONDS = 7;
    private static final int CHECK_INTERVAL_SECONDS = 5;

    private final Client client;
    private final ScheduledExecutorService scheduler;
    private final Consumer<Boolean> rankChangeCallback;

    private ScheduledFuture<?> updateTask;
    private boolean isAdmiralOrHigher = false;

    public ClanRankMonitor(Client client, Consumer<Boolean> rankChangeCallback) {
        this.client = client;
        this.rankChangeCallback = rankChangeCallback;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts monitoring clan rank.
     */
    public void startMonitoring() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel(false);
        }

        updateTask = scheduler.scheduleAtFixedRate(
                this::checkAndUpdateRank,
                CHECK_DELAY_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Stops monitoring clan rank.
     */
    public void stopMonitoring() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
    }

    /**
     * Shuts down the monitor completely.
     */
    public void shutdown() {
        stopMonitoring();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void checkAndUpdateRank() {
        boolean wasAdmiralOrHigher = isAdmiralOrHigher;
        isAdmiralOrHigher = false;

        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null || clanChannel.getName() == null) {
            return;
        }

        String clanName = clanChannel.getName().trim();
        if (!clanName.equalsIgnoreCase(TARGET_CLAN_NAME)) {
            return;
        }

        ClanRank rank = getLocalPlayerClanRank();
        if (rank == null) {
            return;
        }

        int myRankValue = rank.getRank();
        int adminThreshold = ClanRank.ADMINISTRATOR.getRank();
        isAdmiralOrHigher = myRankValue >= adminThreshold;

        // Notify if rank changed
        if (wasAdmiralOrHigher != isAdmiralOrHigher) {
            rankChangeCallback.accept(isAdmiralOrHigher);
        }

        // Stop monitoring if admin or higher (no need to keep checking)
        if (isAdmiralOrHigher) {
            stopMonitoring();
        }
    }

    private ClanRank getLocalPlayerClanRank() {
        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null) {
            return null;
        }

        String localPlayerName = client.getLocalPlayer() != null
                ? client.getLocalPlayer().getName()
                : null;

        if (localPlayerName == null) {
            return null;
        }

        for (ClanChannelMember member : clanChannel.getMembers()) {
            if (localPlayerName.equalsIgnoreCase(member.getName())) {
                return member.getRank();
            }
        }

        return null;
    }

    public boolean isAdmiralOrHigher() {
        return isAdmiralOrHigher;
    }
}