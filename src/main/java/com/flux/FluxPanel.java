package com.flux;

import java.awt.*;
import java.util.*;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.Timer;

import com.flux.components.InverseCornerButton;
import com.flux.components.combobox.ComboBoxIconEntry;
import com.flux.components.combobox.ComboBoxIconListRenderer;
import com.flux.constants.EntrySelect;
import com.flux.services.CompetitionScheduler;
import net.runelite.client.config.ConfigManager;
import com.flux.cards.*;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import okhttp3.*;

public class FluxPanel extends PluginPanel {
    private static final int GLOW_CHECK_INTERVAL = 500;
    private static final int SCROLL_UNIT_INCREMENT = 16;

    private final FluxConfig config;
    private final ConfigManager configManager;
    private final CompetitionScheduler competitionScheduler;

    private final JPanel headerPanel = new JPanel();
    private final JPanel centerPanel = new JPanel();
    private final JPanel footerPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();

    private final JComboBox<ComboBoxIconEntry> dropdown = new JComboBox<>();
    private final ComboBoxIconListRenderer renderer = new ComboBoxIconListRenderer();
    private final java.util.List<InverseCornerButton> footerButtons = new ArrayList<>();

    private final Map<EntrySelect, JPanel> cards = new EnumMap<>(EntrySelect.class);
    private HomeCard homeCard;
    private SotwCard sotwCard;
    private BotmCard botmCard;
    private AdminHubCard adminHubCard;
    private HuntCard huntCard;

    private InverseCornerButton activeFooterButton;
    private boolean isAdminOrHigher = false;
    private boolean adminHubInitialized = false;

    @Inject
    private OkHttpClient okHttpClient;

    private final Timer glowTimer = new Timer(GLOW_CHECK_INTERVAL, e -> updateEventGlows());

    public FluxPanel(CompetitionScheduler competitionScheduler, FluxConfig config, ConfigManager configManager) {
        super(false);
        this.competitionScheduler = competitionScheduler;
        this.config = config;
        this.configManager = configManager;

        setupLayout();
        glowTimer.start();
        initializeCards();
        setupHeader();
        setupFooter();
        selectFirstEntry();
    }

    @Override
    public void onActivate()
    {
        competitionScheduler.start();
    }

    @Override
    public void onDeactivate()
    {
        competitionScheduler.stop();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        centerPanel.setLayout(cardLayout);
        centerPanel.setOpaque(false);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void initializeCards() {
        addEntry(config.entry_1());
        addEntry(config.entry_3());
        addEntry(config.entry_4());
        addEntry(config.entry_5());
        addEntry(config.entry_6());
        addEntry(config.entry_7());
        addEntry(config.entry_8());
    }

    private void setupHeader() {
        headerPanel.removeAll();
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        configureDropdown();
        headerPanel.add(dropdown);

        headerPanel.revalidate();
        headerPanel.repaint();
    }

    private void configureDropdown() {
        dropdown.setFocusable(false);
        dropdown.setForeground(Color.WHITE);
        dropdown.setRenderer(renderer);
        dropdown.addActionListener(e -> handleDropdownSelection());
    }

    private void handleDropdownSelection() {
        ComboBoxIconEntry selectedItem = (ComboBoxIconEntry) dropdown.getSelectedItem();
        if (selectedItem == null || !selectedItem.getId().isPresent()) {
            return;
        }

        String cardId = selectedItem.getId().get();
        String selectedText = selectedItem.getText().trim();

        activateButtonByLabel(selectedText);
        cardLayout.show(centerPanel, cardId);
    }

    private void setupFooter() {
        footerPanel.removeAll();
        footerPanel.setLayout(new GridBagLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        GridBagConstraints gbc = createFooterConstraints();

        for (int i = 0; i < footerButtons.size(); i++) {
            configureButtonPosition(gbc, i);
            footerPanel.add(footerButtons.get(i), gbc);
        }

        footerPanel.revalidate();
        footerPanel.repaint();

        activateFirstButton();
    }

    private GridBagConstraints createFooterConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        return gbc;
    }

    private void configureButtonPosition(GridBagConstraints gbc, int index) {
        gbc.gridx = index % 2;
        gbc.gridy = index / 2;
        gbc.gridwidth = 1;

        if (index == footerButtons.size() - 1 && footerButtons.size() % 2 != 0) {
            gbc.gridx = 0;
            gbc.gridwidth = 2;
        }
    }

    private void addEntry(EntrySelect entry) {
        EntryConfig entryConfig = createEntryConfig(entry);
        if (entryConfig == null) {
            return;
        }

        addToDropdown(entryConfig);
        InverseCornerButton button = createFooterButton(entryConfig, entry);
        footerButtons.add(button);

        centerPanel.add(makeScrollable(entryConfig.card), entry.name().toLowerCase());
        cards.put(entry, entryConfig.card);
    }

    private EntryConfig createEntryConfig(EntrySelect entry) {
        switch (entry) {
            case HOME:
                homeCard = new HomeCard(config, configManager);
                return new EntryConfig(" Home", "/home.png", homeCard, entry);

            case SOTW:
                sotwCard = new SotwCard(configManager);
                return new EntryConfig(" SOTW", "/sotw.png", sotwCard, entry);

            case BOTM:
                botmCard = new BotmCard(configManager, okHttpClient);
                return new EntryConfig(" BOTM", "/botm.png", botmCard, entry);

            case HUB:
                adminHubCard = new AdminHubCard(config, configManager);
                return new EntryConfig(" Admin Hub", "/hub.png", adminHubCard, entry);

            case HUNT:
                huntCard = new HuntCard(configManager, okHttpClient);
                return new EntryConfig(" The Hunt", "/hunt.png", huntCard, entry);

            default:
                return null;
        }
    }

    private void addToDropdown(EntryConfig config) {
        dropdown.addItem(new ComboBoxIconEntry(
                new ImageIcon(ImageUtil.loadImageResource(getClass(), config.iconPath)),
                config.label,
                Optional.of(config.entry.name().toLowerCase())
        ));
    }

    private InverseCornerButton createFooterButton(EntryConfig config, EntrySelect entry) {
        InverseCornerButton button = InverseCornerButton.withImage(config.label, config.iconPath);
        button.addActionListener(e -> {
            activateFooterButton(button);
            cardLayout.show(centerPanel, entry.name().toLowerCase());
        });
        return button;
    }

    public void updateClanRankStatus(boolean isAdminOrHigher) {
        if (this.isAdminOrHigher == isAdminOrHigher && adminHubInitialized) {
            return;
        }

        this.isAdminOrHigher = isAdminOrHigher;

        if (isAdminOrHigher && !adminHubInitialized) {
            addEntry(config.entry_2());
            setupFooter();
            dropdown.repaint();
            centerPanel.revalidate();
            centerPanel.repaint();
            adminHubInitialized = true;
        }
    }

    private void activateFooterButton(InverseCornerButton button) {
        if (activeFooterButton != null) {
            activeFooterButton.setActive(false);
        }

        activeFooterButton = button;
        activeFooterButton.setActive(true);

        syncDropdownWithButton(button.getText().trim());
    }

    private void activateButtonByLabel(String label) {
        footerButtons.stream()
                .filter(btn -> btn.getText().trim().equals(label))
                .findFirst()
                .ifPresent(this::activateFooterButton);
    }

    private void syncDropdownWithButton(String buttonLabel) {
        for (int i = 0; i < dropdown.getItemCount(); i++) {
            ComboBoxIconEntry item = dropdown.getItemAt(i);
            if (item.getText().trim().equals(buttonLabel) && dropdown.getSelectedIndex() != i) {
                dropdown.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setButtonGlow(String label, boolean glow) {
        footerButtons.stream()
                .filter(btn -> btn.getText().trim().equalsIgnoreCase(label.trim()))
                .findFirst()
                .ifPresent(btn -> {
                    btn.setGlowing(glow);
                    if (!glow) {
                        btn.repaint();
                        footerPanel.repaint();
                    }
                });
    }

    private void updateEventGlows() {
        updateCardGlow(sotwCard, " SOTW", card -> card.isEventActive());
        updateCardGlow(botmCard, " BOTM", card -> card.isEventActive());
        updateCardGlow(huntCard, " The Hunt", card -> card.isEventActive());

        if (homeCard != null) {
            homeCard.isRollCallActive();
        }
    }

    private <T extends JPanel> void updateCardGlow(T card, String label, EventActiveChecker<T> checker) {
        if (card != null) {
            boolean active = checker.isActive(card);
            setButtonGlow(label, active);

            if (homeCard != null) {
                updateHomeCardEventStatus(label, active);
            }
        }
    }

    private void updateHomeCardEventStatus(String label, boolean active) {
        switch (label.trim()) {
            case "SOTW":
                homeCard.updateSotwStatus(active);
                break;
            case "BOTM":
                homeCard.updateBotmStatus(active);
                break;
            case "The Hunt":
                homeCard.updateHuntStatus(active);
                break;
        }
    }

    private void selectFirstEntry() {
        if (dropdown.getItemCount() > 0) {
            dropdown.setSelectedIndex(0);
        }
    }

    private void activateFirstButton() {
        if (!footerButtons.isEmpty()) {
            footerButtons.get(0).doClick();
        }
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

    public HomeCard getHomeCard() { return homeCard; }
    public BotmCard getBotmCard() { return botmCard; }
    public SotwCard getSotwCard() { return sotwCard; }
    public HuntCard getHuntCard() { return huntCard; }
    public AdminHubCard getAdminHubCard() { return adminHubCard; }

    public void refreshAllCards() {
        refreshHomeCard();
        refreshSotwCard();
        updateEventGlows();
    }

    private void refreshHomeCard() {
        if (homeCard != null) {
            homeCard.refreshSotwStatus();
            homeCard.refreshBotmStatus();
            homeCard.refreshHuntStatus();
            homeCard.refreshPluginAnnouncement();
            homeCard.refreshButtonLinks();
        }
    }

    private void refreshSotwCard() {
        if (sotwCard != null) {
            sotwCard.refreshLeaderboard();
        }
    }

    public void shutdown() {
        glowTimer.stop();
        botmCard.shutdown();
        sotwCard.shutdown();
        huntCard.shutdown();
    }

    private static class EntryConfig {
        final String label;
        final String iconPath;
        final JPanel card;
        final EntrySelect entry;

        EntryConfig(String label, String iconPath, JPanel card, EntrySelect entry) {
            this.label = label;
            this.iconPath = iconPath;
            this.card = card;
            this.entry = entry;
        }
    }

    @FunctionalInterface
    private interface EventActiveChecker<T> {
        boolean isActive(T card);
    }
}