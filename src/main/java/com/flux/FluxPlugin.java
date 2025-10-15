package com.flux;

import com.flux.cards.HomeCard;
import com.flux.cards.BotmCard;
import com.flux.cards.SotwCard;
import com.flux.cards.HuntCard;
import com.flux.cards.AdminHubCard;
import com.flux.services.ClanRankMonitor;
import com.flux.services.CompetitionScheduler;
import com.flux.services.GoogleSheetParser;
import com.flux.services.LoginMessageSender;
import com.google.inject.Provides;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Flux",
        description = "A plugin used to keep track of clan events.",
        tags = {"flux", "cc", "hunt", "pass", "event", "clan"}
)
public class FluxPlugin extends Plugin {
    public static final String CONFIG_GROUP = "flux";

    @Inject private Client client;
    @Inject private ChatMessageManager chatMessageManager;
    @Inject private FluxConfig config;
    @Inject private ConfigManager configManager;
    @Inject private OverlayManager overlayManager;
    @Inject private FluxOverlay overlay;
    @Inject private ClientToolbar clientToolbar;

    private FluxPanel panel;
    private NavigationButton uiNavigationButton;

    private CompetitionScheduler competitionScheduler;
    private GoogleSheetParser configParser;
    private ClanRankMonitor clanRankMonitor;
    private LoginMessageSender loginMessageSender;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);

        initializePanel();
        initializeServices();
        startServices();
        refreshAllCards();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(uiNavigationButton);
        stopServices();
    }

    private void initializePanel() {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/flux-icon-tiny.png");

        panel = new FluxPanel();
        panel.init(config, configManager);

        uiNavigationButton = NavigationButton.builder()
                .tooltip("Flux")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(uiNavigationButton);
    }

    private void initializeServices() {
        competitionScheduler = new CompetitionScheduler(configManager);

        configParser = new GoogleSheetParser(
                configManager,
                GoogleSheetParser.SheetType.CONFIG,
                this::handleConfigUpdate,
                true
        );

        clanRankMonitor = new ClanRankMonitor(client, this::handleRankChange);
        loginMessageSender = new LoginMessageSender(chatMessageManager, configManager);
    }

    private void startServices() {
        competitionScheduler.startScheduler();
        configParser.start();
    }

    private void stopServices() {
        if (competitionScheduler != null) {
            competitionScheduler.stopScheduler();
        }
        if (configParser != null) {
            configParser.stop();
        }
        if (clanRankMonitor != null) {
            clanRankMonitor.shutdown();
        }
    }

    private void refreshAllCards() {
        if (panel != null) {
            if (panel.getSotwCard() != null) {
                panel.getSotwCard().checkEventStateChanged();
            }
            if (panel.getBotmCard() != null) {
                panel.getBotmCard().checkEventStateChanged();
            }
            if (panel.getHuntCard() != null) {
                panel.getHuntCard().checkEventStateChanged();
            }
        }
    }

    private void handleRankChange(boolean isAdmiralOrHigher) {
        if (panel != null) {
            panel.updateClanRankStatus(isAdmiralOrHigher);
        }
    }

    private void handleConfigUpdate(java.util.Map<String, String> configValues) {
        System.out.println("Received config updates from Google Sheets:");

        updateLoginMessage(configValues);
        updateAnnouncementMessage(configValues);
        updateRollCallStatus(configValues);
    }

    private void updateLoginMessage(java.util.Map<String, String> configValues) {
        String loginMsg = configValues.get("LOGIN_MESSAGE");
        if (loginMsg != null && !loginMsg.isEmpty()) {
            String currentValue = configManager.getConfiguration(CONFIG_GROUP, "clan_login_message");
            if (!loginMsg.equals(currentValue)) {
                configManager.setConfiguration(CONFIG_GROUP, "clan_login_message", loginMsg);
                System.out.println("  Updated LOGIN_MESSAGE: " + loginMsg);
            }
        }
    }

    private void updateAnnouncementMessage(java.util.Map<String, String> configValues) {
        String announcement = configValues.get("ANNOUNCEMENT_MESSAGE");
        if (announcement != null && !announcement.isEmpty()) {
            String currentAnnouncement = configManager.getConfiguration(CONFIG_GROUP, "plugin_announcement_message");

            if (!announcement.equals(currentAnnouncement)) {
                System.out.println("  Updating ANNOUNCEMENT_MESSAGE: " + announcement);
                configManager.setConfiguration(CONFIG_GROUP, "plugin_announcement_message", announcement);

                // Trigger UI update directly
                if (panel != null && panel.getHomeCard() != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        panel.getHomeCard().refreshPluginAnnouncement();
                        panel.getHomeCard().refreshButtonLinks();
                    });
                }
            }
        }
    }

    private void updateRollCallStatus(java.util.Map<String, String> configValues) {
        String rollCallActive = configValues.get("ROLL_CALL_ACTIVE");
        if (rollCallActive != null && !rollCallActive.isEmpty()) {
            boolean isActive = rollCallActive.equalsIgnoreCase("TRUE");
            String currentStatus = configManager.getConfiguration(CONFIG_GROUP, "rollCallActive");
            boolean currentActive = currentStatus != null && Boolean.parseBoolean(currentStatus);

            if (isActive != currentActive) {
                System.out.println("  Updating ROLL_CALL_ACTIVE: " + isActive);
                configManager.setConfiguration(CONFIG_GROUP, "rollCallActive", String.valueOf(isActive));

                // Trigger UI update directly
                if (panel != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        if (panel.getHomeCard() != null) {
                            panel.getHomeCard().isRollCallActive();
                        }
                        if (panel.getAdminHubCard() != null) {
                            panel.getAdminHubCard().refresh();
                        }
                    });
                }
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();

        if (state == GameState.LOGGED_IN) {
            loginMessageSender.sendLoginMessage();
            clanRankMonitor.startMonitoring();
        } else if (state == GameState.LOGIN_SCREEN) {
            loginMessageSender.reset();
            clanRankMonitor.stopMonitoring();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(CONFIG_GROUP)) {
            return;
        }

        String key = event.getKey();
        System.out.println("CONFIG CHANGE EVENT KEY: " + key);

        if (key.equals("plugin_announcement_message")) {
            if (panel != null && panel.getHomeCard() != null) {
                panel.getHomeCard().refreshPluginAnnouncement();
                panel.getHomeCard().refreshButtonLinks();
            }
        }

        if (key.equals("rollCallActive")) {
            System.out.println("Roll Call Status Changed");
            if (panel != null && panel.getHomeCard() != null) {
                panel.getHomeCard().isRollCallActive();
            }
            if (panel != null && panel.getAdminHubCard() != null) {
                panel.getAdminHubCard().refresh();
            }
        }

        if (key.equals("botmActive") || key.equals("botm_active")) {
            if (panel != null && panel.getBotmCard() != null) {
                panel.getBotmCard().checkEventStateChanged();
            }
        }

        if (key.equals("sotw_active") || key.equals("sotwActive")) {
            if (panel != null) {
                panel.refreshAllCards();
                if (panel.getSotwCard() != null) {
                    panel.getSotwCard().checkEventStateChanged();
                }
            }
        }

        if (key.equals("sotw_wom_link") || key.equals("sotwWomLink")) {
            if (panel != null && panel.getSotwCard() != null) {
                panel.getSotwCard().refreshButtonLinks();
            }
        }

        if (key.equals("sotw_leaderboard") || key.equals("sotwLeaderboard")) {
            logLeaderboard();
            if (panel != null) {
                panel.refreshAllCards();
            }
        }

        if (key.equals("huntActive")) {
            if (panel != null) {
                if (panel.getHuntCard() != null) {
                    panel.getHuntCard().checkEventStateChanged();
                }
                if (panel.getHomeCard() != null) {
                    panel.getHomeCard().refreshHuntStatus();
                }
            }
        }
    }

    private void logLeaderboard() {
        String leaderboardJson = configManager.getConfiguration(CONFIG_GROUP, "sotwLeaderboard", String.class);

        if (leaderboardJson != null && !leaderboardJson.isEmpty()) {
            try {
                JSONArray leaderboardArray = new JSONArray(leaderboardJson);
                System.out.println("Current SOTW Leaderboard:");
                for (int i = 0; i < leaderboardArray.length(); i++) {
                    JSONObject entry = leaderboardArray.getJSONObject(i);
                    String username = entry.getString("username");
                    int xp = entry.getInt("xp");
                    System.out.printf(" - %-20s %,d XP%n", username, xp);
                }
            } catch (Exception e) {
                System.out.println("Error parsing leaderboard JSON: " + e.getMessage());
            }
        }
    }

    @Provides
    FluxConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FluxConfig.class);
    }
}