package com.flux;

import com.flux.services.ClanRankMonitor;
import com.flux.services.CompetitionScheduler;
import com.flux.services.GoogleSheetParser;
import com.flux.services.LoginMessageSender;
import com.flux.services.HuntAutoScreenshot;
import com.flux.services.HuntListSyncService;
import com.flux.services.wom.CompetitionConfigUpdater;
import com.flux.services.wom.CompetitionDataParser;
import com.flux.services.wom.CompetitionFinder;
import com.flux.services.wom.WiseOldManApiClient;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.ImageCapture;
import okhttp3.OkHttpClient;
import java.awt.image.BufferedImage;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
    @Inject private OkHttpClient okHttpClient;
    @Inject private net.runelite.client.eventbus.EventBus eventBus;
    @Inject private net.runelite.client.util.ImageCapture imageCapture;
    @Inject private net.runelite.client.ui.DrawManager drawManager;
    @Inject private net.runelite.client.game.ItemManager itemManager;
    @Inject private net.runelite.client.callback.ClientThread clientThread;

    private FluxPanel panel;
    private NavigationButton uiNavigationButton;
    private CompetitionScheduler competitionScheduler;
    private GoogleSheetParser configParser;
    private ClanRankMonitor clanRankMonitor;
    private LoginMessageSender loginMessageSender;
    private HuntListSyncService huntListSyncService;
    private HuntAutoScreenshot huntAutoScreenshot;
    private boolean huntScreenshotWarningShown = false;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        initializeServices();
        initializePanel();
        configParser.start();
        refreshAllCards();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(uiNavigationButton);
        panel.shutdown();
        configParser.shutdown();
        clanRankMonitor.shutdown();
        huntAutoScreenshot.stopMonitoring();
    }

    private void initializePanel() {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/flux-icon-tiny.png");

        panel = new FluxPanel(competitionScheduler, config, configManager);

        uiNavigationButton = NavigationButton.builder()
                .tooltip("Flux")
                .icon(icon)
                .priority(config.menuPriority())
                .panel(panel)
                .build();

        clientToolbar.addNavigation(uiNavigationButton);
    }

    private void initializeServices() {
        WiseOldManApiClient apiClient = new WiseOldManApiClient(okHttpClient);
        CompetitionDataParser dataParser = new CompetitionDataParser();
        CompetitionFinder finder = new CompetitionFinder(apiClient, dataParser);
        CompetitionConfigUpdater configUpdater = new CompetitionConfigUpdater(configManager);

        competitionScheduler = new CompetitionScheduler(
                configManager,
                apiClient,
                finder,
                configUpdater
        );

        configParser = new GoogleSheetParser(
                configManager,
                GoogleSheetParser.SheetType.CONFIG,
                this::handleConfigUpdate,
                true,
                okHttpClient
        );

        clanRankMonitor = new ClanRankMonitor(client, this::handleRankChange);
        loginMessageSender = new LoginMessageSender(chatMessageManager, configManager, config.loginColor());
        
        // Initialize Hunt list sync service
        huntListSyncService = new HuntListSyncService(okHttpClient);
        
        // Perform initial sync from Google Sheets in background
        new Thread(() -> {
            try {
                log.info("Performing initial Hunt lists sync from Google Sheets...");
                boolean success = huntListSyncService.syncFromGoogleSheets();
                if (success) {
                    log.info("Initial sync completed successfully!");
                } else {
                    log.error("Initial sync failed!");
                }
            } catch (Exception e) {
                log.error("Error during initial Hunt lists sync", e);
            }
        }).start();
        
        huntAutoScreenshot = new HuntAutoScreenshot(client, configManager, imageCapture, drawManager, itemManager, okHttpClient, huntListSyncService, clientThread, eventBus);
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
        log.debug("Received config updates from Google Sheets");

        updateLoginMessage(configValues);
        updateAnnouncementMessage(configValues);
        updateRollCallStatus(configValues);
        updateHuntTeamColors(configValues);
        updateBotmPass(configValues);
    }

    private void updateLoginMessage(java.util.Map<String, String> configValues) {
        String loginMsg = configValues.get("LOGIN_MESSAGE");
        if (loginMsg != null && !loginMsg.isEmpty()) {
            String currentValue = configManager.getConfiguration(CONFIG_GROUP, "clan_login_message");
            if (!loginMsg.equals(currentValue)) {
                configManager.setConfiguration(CONFIG_GROUP, "clan_login_message", loginMsg);
                log.debug("Updated LOGIN_MESSAGE: {}", loginMsg);
            }
        }
    }

    private void updateAnnouncementMessage(java.util.Map<String, String> configValues) {
        String announcement = configValues.get("ANNOUNCEMENT_MESSAGE");
        if (announcement != null && !announcement.isEmpty()) {
            String currentAnnouncement = configManager.getConfiguration(CONFIG_GROUP, "plugin_announcement_message");

            if (!announcement.equals(currentAnnouncement)) {
                log.debug("Updating ANNOUNCEMENT_MESSAGE: {}", announcement);
                configManager.setConfiguration(CONFIG_GROUP, "plugin_announcement_message", announcement);

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
            boolean currentActive = Boolean.parseBoolean(currentStatus);

            if (isActive != currentActive) {
                log.debug("Updating ROLL_CALL_ACTIVE: {}", isActive);
                configManager.setConfiguration(CONFIG_GROUP, "rollCallActive", String.valueOf(isActive));

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

    private void updateHuntTeamColors(java.util.Map<String, String> configValues) {
        // TEAM 1 COLOR
        String team1Color = configValues.get("TEAM_1_COLOR");
        if (team1Color != null && !team1Color.isEmpty()) {
            String currentTeam1Color = configManager.getConfiguration(CONFIG_GROUP, "hunt_team_1_color");
            if (!team1Color.equals(currentTeam1Color)) {
                configManager.setConfiguration(CONFIG_GROUP, "hunt_team_1_color", team1Color);
                log.debug("Updated TEAM_1_COLOR: {}", team1Color);
            }
        }

        // TEAM 2 COLOR
        String team2Color = configValues.get("TEAM_2_COLOR");
        if (team2Color != null && !team2Color.isEmpty()) {
            String currentTeam2Color = configManager.getConfiguration(CONFIG_GROUP, "hunt_team_2_color");
            if (!team2Color.equals(currentTeam2Color)) {
                configManager.setConfiguration(CONFIG_GROUP, "hunt_team_2_color", team2Color);
                log.debug("Updated TEAM_2_COLOR: {}", team2Color);
            }
        }

        // refresh the Hunt card UI if it existsto apply color change
        if (panel != null && panel.getHuntCard() != null) {
            javax.swing.SwingUtilities.invokeLater(() -> panel.getHuntCard().updateTeamLabels());
        }
    }

    private void updateBotmPass(java.util.Map<String, String> configValues) {
        String botmPass = configValues.get("BOTM_PASS");
        if (botmPass != null && !botmPass.isEmpty()) {
            String currentValue = configManager.getConfiguration(CONFIG_GROUP, "botm_password");
            if (!botmPass.equals(currentValue)) {
                configManager.setConfiguration(CONFIG_GROUP, "botm_password", botmPass);
                log.debug("Updated BOTM_PASS: {}", botmPass);
            }
        } else {
            log.warn("BOTM_PASS is missing or empty in the Google Sheet values.");
        }
    }


    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();

        if (state == GameState.LOGGED_IN) {
            loginMessageSender.sendLoginMessage();
            clanRankMonitor.startMonitoring();
            
            // Start hunt auto-screenshot if enabled
            if (config.huntAutoScreenshot()) {
                huntAutoScreenshot.startMonitoring();
            }
        } else if (state == GameState.LOGIN_SCREEN) {
            clanRankMonitor.stopMonitoring();
            huntAutoScreenshot.stopMonitoring();
        }
    }

    @Subscribe
    //TODO: please for the love of god fix this giant list of if statements. Case statement can be used. @alex
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(CONFIG_GROUP)) {
            return;
        }

        String key = event.getKey();
        log.debug("CONFIG CHANGE EVENT KEY: {}", key);

        // Handle Hunt Sync Checkbox
        if (key.equals("hunt_sync_trigger")) {
            boolean checked = Boolean.parseBoolean(event.getNewValue());
            
            if (checked) {
                // Check if manual sync is allowed (dual cooldown check)
                if (huntListSyncService != null && huntAutoScreenshot != null) {
                    if (!huntListSyncService.canManualSync()) {
                        long secondsRemaining = huntListSyncService.getSecondsUntilManualSync();
                        
                        if (secondsRemaining == -1) {
                            sendChatMessage("Sync already in progress. Please wait...", ChatMessageType.GAMEMESSAGE);
                        } else {
                            sendChatMessage("Sync on cooldown. Wait " + secondsRemaining + " seconds.", ChatMessageType.GAMEMESSAGE);
                        }
                        return;
                    }
                    
                    // Sync is allowed, proceed
                    new Thread(() -> {
                        try {
                            log.info("Manual Hunt sync triggered from checkbox...");
                            boolean syncSuccess = huntAutoScreenshot.forceRefreshCache();
                            
                            if (syncSuccess) {
                                log.info("Sync completed successfully. 5-second cooldown active.");
                                sendChatMessage("Hunt configuration synced successfully!", ChatMessageType.CONSOLE); // Green text
                            } else {
                                log.warn("Sync failed or was blocked.");
                                sendChatMessage("Hunt sync failed. Check logs for details.", ChatMessageType.GAMEMESSAGE);
                            }
                            
                        } catch (Exception e) {
                            log.error("Error during sync", e);
                            sendChatMessage("Hunt sync error occurred.", ChatMessageType.GAMEMESSAGE);
                        }
                    }, "HuntSyncCheckbox").start();
                }
            }
            return;
        }

        if (key.equals("plugin_announcement_message")) {
            if (panel != null && panel.getHomeCard() != null) {
                panel.getHomeCard().refreshPluginAnnouncement();
                panel.getHomeCard().refreshButtonLinks();
            }
        }

        if (key.equals("rollCallActive")) {
            log.debug("Roll Call Status Changed");
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

        if (key.equals("huntTitle")) {
            if (panel != null && panel.getHuntCard() != null) {
                panel.getHuntCard().updateEventTitle();
            }
        }

        if (key.equals("hunt_team_1_name") || key.equals("hunt_team_2_name") ||
                key.equals("hunt_team_1_color") || key.equals("hunt_team_2_color")) {
            if (panel != null && panel.getHuntCard() != null) {
                panel.getHuntCard().updateTeamLabels();
            }
        }

        if (key.equals("hunt_team_1_leaderboard") || key.equals("hunt_team_2_leaderboard")) {
            if (panel != null && panel.getHuntCard() != null) {
                panel.getHuntCard().refreshLeaderboard();
            }
        }

        if (key.equals("hunt_team_1_score") || key.equals("hunt_team_2_score")) {
            if (panel != null && panel.getHuntCard() != null) {
                panel.getHuntCard().updateTeamScores();
            }
        }

        if (key.equals("hunt_wom_url") || key.equals("hunt_gdoc_url")) {
            if (panel != null && panel.getHuntCard() != null) {
                panel.getHuntCard().refreshButtonLinks();
            }
        }

        if (key.equals("hunt_auto_screenshot")) {
            boolean enabled = Boolean.parseBoolean(event.getNewValue());
            if (enabled) {
                // Show warning dialog if not shown before
                if (!huntScreenshotWarningShown) {
                    SwingUtilities.invokeLater(() -> {
                        int result = JOptionPane.showConfirmDialog(
                            null,
                            "Caution! This will automatically screenshot and upload to the Hunt discord server.\n" +
                            "Any private messages on screen will NOT be hidden.\n\n" +
                            "Do you want to continue?",
                            "Hunt Auto-Screenshot Warning",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        
                        if (result == JOptionPane.YES_OPTION) {
                            huntScreenshotWarningShown = true;
                            huntAutoScreenshot.startMonitoring();
                            log.info("Hunt auto-screenshot enabled");
                        } else {
                            // User declined, disable the setting
                            configManager.setConfiguration(CONFIG_GROUP, "hunt_auto_screenshot", false);
                            log.debug("Hunt auto-screenshot declined by user");
                        }
                    });
                } else {
                    // Warning already shown, just enable
                    huntAutoScreenshot.startMonitoring();
                    log.info("Hunt auto-screenshot enabled");
                }
            } else {
                huntAutoScreenshot.stopMonitoring();
                log.debug("Hunt auto-screenshot disabled");
            }
        }
    }

    @Provides
    FluxConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FluxConfig.class);
    }
    
    /**
     * Get the Hunt List Sync Service for syncing monster/item/whitelist/blacklist
     */
    public HuntListSyncService getHuntListSyncService() {
        return huntListSyncService;
    }
    
    /**
     * Send a message to in-game chat with optional color
     */
    private void sendChatMessage(String message, ChatMessageType type) {
        if (type == ChatMessageType.CONSOLE) {
            // Green text for success messages
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.CONSOLE)
                    .runeLiteFormattedMessage(message)
                    .build()
            );
        } else {
            // Regular game message
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage(message)
                    .build()
            );
        }
    }
}
