package com.flux.services;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import java.awt.*;

@Slf4j
public class LoginMessageSender {
    private static final String CONFIG_GROUP = "flux";
    private static final String CONFIG_KEY = "clan_login_message";

    private final ChatMessageManager chatMessageManager;
    private final ConfigManager configManager;
    private final Color color;
    private boolean hasSentMessage = false;

    public LoginMessageSender(ChatMessageManager chatMessageManager, ConfigManager configManager, Color color) {
        this.chatMessageManager = chatMessageManager;
        this.configManager = configManager;
        this.color = color;
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

        String hex = String.format("%06x", color.getRGB() & 0xFFFFFF);

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.GAMEMESSAGE)
                        .runeLiteFormattedMessage("<col=" + hex + ">" + loginMessage + "</col>")
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