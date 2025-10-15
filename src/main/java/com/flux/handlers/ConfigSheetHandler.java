package com.flux.handlers;

import com.flux.FluxPanel;
import net.runelite.client.config.ConfigManager;

import javax.swing.*;
import java.util.Map;

/**
 * Handles updates from the Google Sheets config parser.
 */
public class ConfigSheetHandler {
    private static final String CONFIG_GROUP = "flux";

    private final ConfigManager configManager;
    private FluxPanel panel;  // Add this field
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

            // Manually trigger the UI update since setConfiguration doesn't fire events
            if (panel != null && panel.getHomeCard() != null) {
                SwingUtilities.invokeLater(() -> panel.getHomeCard().refreshPluginAnnouncement());
            }
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