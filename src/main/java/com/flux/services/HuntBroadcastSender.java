package com.flux.services;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import java.awt.Color;

@Slf4j
public class HuntBroadcastSender {
    private static final String CONFIG_GROUP = "flux";
    private static final String BROADCASTS_ENABLED_KEY = "hunt_broadcasts";
    private static final String HUNT_ACTIVE_KEY = "huntActive";

    private final ChatMessageManager chatMessageManager;
    private final ConfigManager configManager;
    private final Color color;

    public HuntBroadcastSender(ChatMessageManager chatMessageManager, ConfigManager configManager, Color color) {
        this.chatMessageManager = chatMessageManager;
        this.configManager = configManager;
        this.color = color;
    }

    public void sendBountyBroadcast() {
        send("A new Hunt Bounty challenge has dropped!");
    }

    public void sendDailyBroadcast() {
        send("A new Hunt Daily challenge has dropped!");
    }

    private void send(String message) {
        if (!isEnabled()) {
            return;
        }

        String hex = String.format("%06x", color.getRGB() & 0xFFFFFF);

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.BROADCAST)
                        .runeLiteFormattedMessage("<col=" + hex + ">[Flux] " + message + "</col>")
                        .build()
        );
    }

    private boolean isEnabled() {
        String broadcastsEnabled = configManager.getConfiguration(CONFIG_GROUP, BROADCASTS_ENABLED_KEY);
        String huntActive = configManager.getConfiguration(CONFIG_GROUP, HUNT_ACTIVE_KEY);

        return Boolean.parseBoolean(broadcastsEnabled) && Boolean.parseBoolean(huntActive);
    }
}