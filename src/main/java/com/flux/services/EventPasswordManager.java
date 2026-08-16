package com.flux.services;

import net.runelite.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventPasswordManager {
    private static final String CONFIG_GROUP = "flux";
    private static final String DELIMITER = " | ";

    private static final String EVENT_PASS_KEY = "eventPass";
    private static final String AUTO_REENABLE_KEY = "auto_reenable_hunt_passwords";

    private static final String BOTM_ACTIVE_KEY = "botmActive";
    private static final String HUNT_ACTIVE_KEY = "huntActive";

    private static final String DISPLAY_HUNT_MASTER_KEY = "display_hunt_master_password";
    private static final String DISPLAY_HUNT_BOUNTY_KEY = "display_hunt_bounty_password";
    private static final String DISPLAY_HUNT_DAILY_KEY = "display_hunt_daily_password";
    private static final String DISPLAY_BOTM_KEY = "display_botm_password";
    private static final String DISPLAY_MISC_EVENT_KEY = "display_misc_event_password";

    private static final String HUNT_MASTER_PASS_KEY = "hunt_master_password";
    private static final String HUNT_BOUNTY_PASS_KEY = "hunt_bounty_password";
    private static final String HUNT_DAILY_PASS_KEY = "hunt_daily_password";
    private static final String BOTM_PASS_KEY = "botm_password";
    private static final String MISC_EVENT_PASS_KEY = "misc_event_password";

    // hunt toggle config key -> hunt password config key
    private static final Map<String, String> HUNT_PASSWORD_TOGGLE_MAP = new LinkedHashMap<>();

    static {
        HUNT_PASSWORD_TOGGLE_MAP.put(DISPLAY_HUNT_MASTER_KEY, HUNT_MASTER_PASS_KEY);
        HUNT_PASSWORD_TOGGLE_MAP.put(DISPLAY_HUNT_BOUNTY_KEY, HUNT_BOUNTY_PASS_KEY);
        HUNT_PASSWORD_TOGGLE_MAP.put(DISPLAY_HUNT_DAILY_KEY, HUNT_DAILY_PASS_KEY);
    }

    private final ConfigManager configManager;

    public EventPasswordManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public String buildOverlayString() {
        List<String> activePasswords = new ArrayList<>();

        // custom user pass, show anytime it's set
        String eventPass = getString(EVENT_PASS_KEY);
        if (!isNullOrEmpty(eventPass)) {
            activePasswords.add(eventPass);
        }

        // misc. event passwords, show anytime set if toggled
        if (getBoolean(DISPLAY_MISC_EVENT_KEY)) {
            String miscPass = getString(MISC_EVENT_PASS_KEY);
            if (!isNullOrEmpty(miscPass)) {
                activePasswords.add(miscPass);
            }
        }

        // BOTM pass only show when BOTM event is active
        if (getBoolean(BOTM_ACTIVE_KEY) && getBoolean(DISPLAY_BOTM_KEY)) {
            String botmPass = getString(BOTM_PASS_KEY);
            if (!isNullOrEmpty(botmPass)) {
                activePasswords.add(botmPass);
            }
        }

        // Hunt passwords only when Hunt event is active
        if (getBoolean(HUNT_ACTIVE_KEY)) {
            for (Map.Entry<String, String> entry : HUNT_PASSWORD_TOGGLE_MAP.entrySet()) {
                String toggleKey = entry.getKey();
                String passwordKey = entry.getValue();

                if (getBoolean(toggleKey)) {
                    String password = getString(passwordKey);
                    if (!isNullOrEmpty(password)) {
                        activePasswords.add(password);
                    }
                }
            }
        }

        return String.join(DELIMITER, activePasswords);
    }

    public void reenableBountyPasswordIfNeeded() {
        if (getBoolean(AUTO_REENABLE_KEY)) {
            configManager.setConfiguration(CONFIG_GROUP, DISPLAY_HUNT_BOUNTY_KEY, true);
        }
    }

    public void reenableDailyPasswordIfNeeded() {
        if (getBoolean(AUTO_REENABLE_KEY)) {
            configManager.setConfiguration(CONFIG_GROUP, DISPLAY_HUNT_DAILY_KEY, true);
        }
    }

    private String getString(String key) {
        String value = configManager.getConfiguration(CONFIG_GROUP, key);
        return value == null ? "" : value;
    }

    private boolean getBoolean(String key) {
        String value = configManager.getConfiguration(CONFIG_GROUP, key);
        return !isNullOrEmpty(value) && Boolean.parseBoolean(value);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}