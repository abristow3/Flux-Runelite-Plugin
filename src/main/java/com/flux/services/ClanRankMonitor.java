package com.flux.services;

import net.runelite.api.Client;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// periodically check users clan rank for Flux to authorize the admin card rednering
@Slf4j
public class ClanRankMonitor {
    private static final String TARGET_CLAN_NAME = "Flux";
    private static final int CHECK_DELAY_SECONDS = 7;
    private static final int CHECK_INTERVAL_SECONDS = 5;

    private final Client client;
    private final ScheduledExecutorService scheduler;
    private final Consumer<Boolean> rankChangeCallback;

    private ScheduledFuture<?> updateTask;
    private boolean isAdminOrHigher = false;

    public ClanRankMonitor(Client client, Consumer<Boolean> rankChangeCallback) {
        this.client = client;
        this.rankChangeCallback = rankChangeCallback;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

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

    public void stopMonitoring() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
    }

    public void shutdown() {
        stopMonitoring();
        scheduler.shutdownNow();
    }

    private void checkAndUpdateRank() {
        boolean wasAdminOrHigher = isAdminOrHigher;
        isAdminOrHigher = false;

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
        isAdminOrHigher = myRankValue >= adminThreshold;

        // Notify if rank changed
        if (wasAdminOrHigher != isAdminOrHigher) {
            rankChangeCallback.accept(isAdminOrHigher);
        }

        // Stop monitoring if admin or higher (no need to keep checking)
        if (isAdminOrHigher) {
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

    public boolean isAdminOrHigher() {
        return isAdminOrHigher;
    }
}