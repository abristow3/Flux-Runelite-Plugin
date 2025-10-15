package com.flux.services;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;

/**
 * Handles sending the clan login message once per session.
 */
public class LoginMessageSender {
    private static final String CONFIG_GROUP = "flux";
    private static final String CONFIG_KEY = "clan_login_message";
    private static final String MESSAGE_COLOR = "ff9600";

    private final ChatMessageManager chatMessageManager;
    private final ConfigManager configManager;
    private boolean hasSentMessage = false;

    public LoginMessageSender(ChatMessageManager chatMessageManager, ConfigManager configManager) {
        this.chatMessageManager = chatMessageManager;
        this.configManager = configManager;
    }

    /**
     * Sends the login message if it hasn't been sent yet this session.
     */
    public void sendLoginMessage() {
        if (hasSentMessage) {
            return;
        }

        String loginMessage = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (loginMessage == null || loginMessage.isEmpty()) {
            hasSentMessage = true; // Mark as sent even if empty to avoid checking again
            return;
        }

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.GAMEMESSAGE)
                        .runeLiteFormattedMessage("<col=" + MESSAGE_COLOR + ">" + loginMessage + "</col>")
                        .build()
        );

        hasSentMessage = true;
    }

    /**
     * Resets the message sent flag (call on logout).
     */
    public void reset() {
        hasSentMessage = false;
    }

    public boolean hasSentMessage() {
        return hasSentMessage;
    }
}