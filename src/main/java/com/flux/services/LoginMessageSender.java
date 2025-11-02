package com.flux.services;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    public void sendLoginMessage() {
        if (hasSentMessage) {
            return;
        }

        String loginMessage = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (loginMessage == null || loginMessage.isEmpty()) {
            hasSentMessage = true;
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

    public void reset() {
        hasSentMessage = false;
    }

    public boolean hasSentMessage() {
        return hasSentMessage;
    }
}