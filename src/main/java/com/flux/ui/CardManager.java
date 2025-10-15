package com.flux.ui;

import com.flux.FluxConfig;
import com.flux.cards.*;
import com.flux.constants.EntrySelect;
import net.runelite.client.config.ConfigManager;

import javax.swing.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages the creation and access of all card instances.
 */
public class CardManager {
    private final FluxConfig config;
    private final ConfigManager configManager;
    private final Map<EntrySelect, JPanel> cards = new EnumMap<>(EntrySelect.class);

    private HomeCard homeCard;
    private SotwCard sotwCard;
    private BotmCard botmCard;
    private AdminHubCard adminHubCard;
    private HuntCard huntCard;

    public CardManager(FluxConfig config, ConfigManager configManager) {
        this.config = config;
        this.configManager = configManager;
    }

    /**
     * Creates a card for the given entry type.
     */
    public CardEntry createCard(EntrySelect entry) {
        JPanel card = instantiateCard(entry);
        if (card == null) {
            return null;
        }

        cards.put(entry, card);
        return new CardEntry(card, getIconPath(entry), getLabel(entry));
    }

    private JPanel instantiateCard(EntrySelect entry) {
        switch (entry) {
            case HOME:
                homeCard = new HomeCard(config, configManager);
                return homeCard;

            case SOTW:
                sotwCard = new SotwCard(configManager);
                return sotwCard;

            case BOTM:
                botmCard = new BotmCard(configManager);
                return botmCard;

            case HUB:
                adminHubCard = new AdminHubCard(config, configManager);
                return adminHubCard;

            case HUNT:
                huntCard = new HuntCard(configManager);
                return huntCard;

            default:
                return null;
        }
    }

    private String getLabel(EntrySelect entry) {
        switch (entry) {
            case HOME: return " Home";
            case SOTW: return " SOTW";
            case BOTM: return " BOTM";
            case HUB: return " Admin Hub";
            case HUNT: return " The Hunt";
            default: return "";
        }
    }

    private String getIconPath(EntrySelect entry) {
        switch (entry) {
            case HOME: return "/home.png";
            case SOTW: return "/sotw.png";
            case BOTM: return "/botm.png";
            case HUB: return "/hub.png";
            case HUNT: return "/hunt.png";
            default: return "";
        }
    }

    // Getters
    public HomeCard getHomeCard() { return homeCard; }
    public SotwCard getSotwCard() { return sotwCard; }
    public BotmCard getBotmCard() { return botmCard; }
    public AdminHubCard getAdminHubCard() { return adminHubCard; }
    public HuntCard getHuntCard() { return huntCard; }
    public Map<EntrySelect, JPanel> getCards() { return cards; }

    /**
     * Container for card creation results.
     */
    public static class CardEntry {
        public final JPanel card;
        public final String iconPath;
        public final String label;

        public CardEntry(JPanel card, String iconPath, String label) {
            this.card = card;
            this.iconPath = iconPath;
            this.label = label;
        }
    }
}