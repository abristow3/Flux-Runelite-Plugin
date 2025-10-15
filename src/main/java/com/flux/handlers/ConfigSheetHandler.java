package com.flux.handlers;

import net.runelite.client.config.ConfigManager;

import java.util.Map;

/**
 * Handles updates from the Google Sheets config parser.
 */
public class ConfigSheetHandler {
    private static final String CONFIG_GROUP = "flux";

    private final ConfigManager configManager;

    public ConfigSheetHandler(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Processes config updates from Google Sheets.
     */
    public void handleConfigUpdate(Map<String, String> configValues) {
        System.out.println("Received config updates from Google Sheets:");

        updateLoginMessage(configValues);
        updateAnnouncementMessage(configValues);
        updateRollCallStatus(configValues);
    }

    private void updateLoginMessage(Map<String, String> configValues) {
        String loginMsg = configValues.get("LOGIN_MESSAGE");
        if (loginMsg != null && !loginMsg.isEmpty()) {
            configManager.setConfiguration(CONFIG_GROUP, "clan_login_message", loginMsg);
            System.out.println("  Updated LOGIN_MESSAGE: " + loginMsg);
        }
    }

    private void updateAnnouncementMessage(Map<String, String> configValues) {
        String announcement = configValues.get("ANNOUNCEMENT_MESSAGE");
        if (announcement != null && !announcement.isEmpty()) {
            configManager.setConfiguration(CONFIG_GROUP, "plugin_announcement_message", announcement);
            System.out.println("  Updated ANNOUNCEMENT_MESSAGE: " + announcement);
        }
    }

    private void updateRollCallStatus(Map<String, String> configValues) {
        String rollCallActive = configValues.get("ROLL_CALL_ACTIVE");
        if (rollCallActive != null && !rollCallActive.isEmpty()) {
            boolean isActive = rollCallActive.equalsIgnoreCase("TRUE");
            configManager.setConfiguration(CONFIG_GROUP, "rollCallActive", String.valueOf(isActive));
            System.out.println("  Updated ROLL_CALL_ACTIVE: " + isActive);
        }
    }
}