package com.flux;

import com.flux.handlers.ConfigChangeHandler;
import com.flux.handlers.ConfigSheetHandler;
import com.flux.services.ClanRankMonitor;
import com.flux.services.CompetitionScheduler;
import com.flux.services.GoogleSheetParser;
import com.flux.services.LoginMessageSender;
import com.flux.FluxPanel;
import com.google.inject.Provides;

import javax.inject.Inject;

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

    // Service handlers
    private CompetitionScheduler competitionScheduler;
    private GoogleSheetParser configParser;
    private ClanRankMonitor clanRankMonitor;
    private LoginMessageSender loginMessageSender;
    private ConfigChangeHandler configChangeHandler;
    private ConfigSheetHandler configSheetHandler;

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
        // Competition scheduler
        competitionScheduler = new CompetitionScheduler(configManager);

        // Config sheet parser
        configSheetHandler = new ConfigSheetHandler(configManager);
        configParser = new GoogleSheetParser(
                configManager,
                GoogleSheetParser.SheetType.CONFIG,
                configSheetHandler::handleConfigUpdate,
                true
        );

        // Clan rank monitor
        clanRankMonitor = new ClanRankMonitor(client, this::handleRankChange);

        // Login message sender
        loginMessageSender = new LoginMessageSender(chatMessageManager, configManager);

        // Config change handler
        configChangeHandler = new ConfigChangeHandler(panel, configManager);
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
        if (panel == null) return;

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

    private void handleRankChange(boolean isAdmiralOrHigher) {
        if (panel != null) {
            panel.updateClanRankStatus(isAdmiralOrHigher);
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
        configChangeHandler.handleConfigChange(event);
    }

    @Provides
    FluxConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FluxConfig.class);
    }
}