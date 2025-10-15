package com.flux;

import java.awt.*;
import javax.swing.*;

import com.flux.constants.EntrySelect;
import com.flux.ui.CardManager;
import com.flux.ui.EventGlowManager;
import com.flux.ui.FooterButtonManager;
import com.flux.ui.HeaderDropdownManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;
import lombok.extern.slf4j.Slf4j;

/**
 * Main panel for the Flux plugin.
 * Delegates responsibilities to specialized manager classes.
 */
@Slf4j
public class FluxPanel extends PluginPanel {
    private static final int SCROLL_UNIT_INCREMENT = 16;

    // Configuration
    private FluxConfig config;
    private ConfigManager configManager;

    // UI Panels
    private final JPanel headerPanel = new JPanel();
    private final JPanel centerPanel = new JPanel();
    private final JPanel footerPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();

    // Managers
    private CardManager cardManager;
    private HeaderDropdownManager headerManager;
    private FooterButtonManager footerManager;
    private EventGlowManager glowManager;

    // State
    private boolean isAdmiralOrHigher = false;
    private boolean adminHubInitialized = false;

    public FluxPanel() {
        super(false);
        setupLayout();
    }

    public void init(FluxConfig config, ConfigManager configManager) {
        this.config = config;
        this.configManager = configManager;

        initializeManagers();
        initializeCards();
        setupHeader();
        setupFooter();

        glowManager.start();
        headerManager.selectFirst();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        centerPanel.setLayout(cardLayout);
        centerPanel.setOpaque(false);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void initializeManagers() {
        cardManager = new CardManager(config, configManager);
        headerManager = new HeaderDropdownManager(headerPanel);
        footerManager = new FooterButtonManager(footerPanel);
        glowManager = new EventGlowManager(footerManager.getButtons(), cardManager, footerPanel);

        // Set up coordination between header and footer
        headerManager.setOnSelectionChanged(cardId -> {
            String selectedLabel = getSelectedLabelFromCardId(cardId);
            footerManager.activateButtonByLabel(selectedLabel);
            cardLayout.show(centerPanel, cardId);
        });
    }

    private void initializeCards() {
        addEntry(config.entry_1());  // Home
        addEntry(config.entry_3());  // SOTW
        addEntry(config.entry_4());  // BOTM
        addEntry(config.entry_5());  // Hunt
        addEntry(config.entry_6());
        addEntry(config.entry_7());
        addEntry(config.entry_8());
    }

    private void setupHeader() {
        headerManager.buildHeader();
    }

    private void setupFooter() {
        footerManager.rebuildFooter();
    }

    private void addEntry(EntrySelect entry) {
        CardManager.CardEntry cardEntry = cardManager.createCard(entry);
        if (cardEntry == null) {
            return;
        }

        // Add to dropdown
        headerManager.addEntry(
                cardEntry.label,
                cardEntry.iconPath,
                entry.name().toLowerCase(),
                getClass()
        );

        // Add to footer
        footerManager.addButton(
                cardEntry.label,
                cardEntry.iconPath,
                entry,
                e -> cardLayout.show(centerPanel, entry.name().toLowerCase())
        );

        // Add to center panel
        centerPanel.add(makeScrollable(cardEntry.card), entry.name().toLowerCase());
    }

    /**
     * Updates admin hub visibility based on clan rank.
     */
    public void updateClanRankStatus(boolean isAdmiralOrHigher) {
        if (this.isAdmiralOrHigher == isAdmiralOrHigher && adminHubInitialized) {
            return;
        }

        this.isAdmiralOrHigher = isAdmiralOrHigher;

        if (isAdmiralOrHigher && !adminHubInitialized) {
            addEntry(config.entry_2());  // Admin Hub
            setupFooter();
            headerManager.repaint();
            centerPanel.revalidate();
            centerPanel.repaint();
            adminHubInitialized = true;
        }
    }

    /**
     * Refreshes all card data.
     */
    public void refreshAllCards() {
        if (cardManager.getHomeCard() != null) {
            cardManager.getHomeCard().refreshSotwStatus();
            cardManager.getHomeCard().refreshBotmStatus();
            cardManager.getHomeCard().refreshHuntStatus();
            cardManager.getHomeCard().refreshPluginAnnouncement();
            cardManager.getHomeCard().refreshButtonLinks();
        }

        if (cardManager.getSotwCard() != null) {
            cardManager.getSotwCard().refreshLeaderboard();
        }

        glowManager.updateAllEventGlows();
    }

    private JScrollPane makeScrollable(JPanel card) {
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private String getSelectedLabelFromCardId(String cardId) {
        // This is a bit hacky but works with the current structure
        // Could be improved by storing a mapping
        for (EntrySelect entry : EntrySelect.values()) {
            if (entry.name().equalsIgnoreCase(cardId)) {
                CardManager.CardEntry cardEntry = cardManager.createCard(entry);
                if (cardEntry != null) {
                    return cardEntry.label;
                }
            }
        }
        return "";
    }

    // Public API for card access
    public CardManager getCardManager() {
        return cardManager;
    }

    // Convenience methods for backward compatibility
    public com.flux.cards.HomeCard getHomeCard() {
        return cardManager != null ? cardManager.getHomeCard() : null;
    }

    public com.flux.cards.SotwCard getSotwCard() {
        return cardManager != null ? cardManager.getSotwCard() : null;
    }

    public com.flux.cards.BotmCard getBotmCard() {
        return cardManager != null ? cardManager.getBotmCard() : null;
    }

    public com.flux.cards.HuntCard getHuntCard() {
        return cardManager != null ? cardManager.getHuntCard() : null;
    }

    public com.flux.cards.AdminHubCard getAdminHubCard() {
        return cardManager != null ? cardManager.getAdminHubCard() : null;
    }
}