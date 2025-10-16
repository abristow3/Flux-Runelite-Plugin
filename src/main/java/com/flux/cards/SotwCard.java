package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.components.LeaderboardCellRenderer;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SotwCard extends FluxCard {
    private final ConfigManager configManager;

    private DefaultTableModel tableModel;
    private JLabel eventTitle;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private boolean wasEventActive = false;
    private static final Logger logger = LoggerFactory.getLogger(SotwCard.class);

    public SotwCard(ConfigManager configManager) {
        super();
        this.configManager = configManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        startCountdownTimer();
    }

    private void buildUI() {
        add(createVerticalStrut(SPACING_MEDIUM));
        add(createCenteredLabel("Skill of the Week", FONT_TITLE, COLOR_YELLOW));
        addVerticalSpace(SPACING_MEDIUM);
        addEventTitle();
        addVerticalSpace(SPACING_SMALL);
        addCountdownLabel();
        addVerticalSpace(SPACING_SMALL);
        addLeaderboardTable();
        addVerticalSpace(SPACING_LARGE);
        addButtons();
    }

    private void addEventTitle() {
        String titleText = getConfigValue("sotwTitle", "No active competition");
        eventTitle = createWrappedLabelWithUnderline(titleText, FONT_SECTION, COLOR_YELLOW);
        setupDynamicResize(eventTitle);
        add(eventTitle);
    }

    private void addCountdownLabel() {
        countdownLabel = createWrappedLabel("Loading...", FONT_NORMAL, COLOR_LIGHT_GRAY);
        setupDynamicResize(countdownLabel);
        add(countdownLabel);
        updateCountdownLabel();
    }

    private void addLeaderboardTable() {
        tableModel = new DefaultTableModel(new Object[]{"Username", "XP Gained"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        configureTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane);

        refreshLeaderboard();
    }

    private void configureTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        table.setShowGrid(false);
        table.setFont(FONT_NORMAL);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setIntercellSpacing(new Dimension(0, TABLE_ROW_SPACING));

        LeaderboardCellRenderer renderer = new LeaderboardCellRenderer();
        renderer.setTable(table);
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);
    }

    private void addButtons() {
        LinkButton[] linkButtons = {
                new LinkButton("SOTW", "/discord.png", "https://discord.com/channels/414435426007384075/416998364601909288"),
                new LinkButton("SOTW Wise Old Man", "/wom.png", getConfigValue("sotw_wom_link", "https://wiseoldman.net/groups/141/competitions"))
        };
        addLinkButtons(linkButtons);
    }

    // Public API
    public void refreshLeaderboard() {
        tableModel.setRowCount(0);
        LinkedHashMap<String, Integer> leaderboard = parseLeaderboardJson("sotwLeaderboard", "xp", configManager);

        for (Map.Entry<String, Integer> entry : leaderboard.entrySet()) {
            String username = entry.getKey();
            String xpString = String.format("%,d XP", entry.getValue());
            tableModel.addRow(new Object[]{username, xpString});
        }
    }

    public boolean isEventActive() {
        return getConfigBoolean("sotwActive");
    }

    public void refreshButtonLinks() {
        updateButtonUrl("SOTW Wise Old Man", "sotw_wom_link", configManager);
    }

    public void updateEventTitle() {
        String titleText = getConfigValue("sotwTitle", "No active competition");
        updateWrappedLabelText(eventTitle, titleText, true);
        eventTitle.revalidate();
        eventTitle.repaint();
    }

    public void checkEventStateChanged() {
        updateEventTitle();
        refreshLeaderboard();
        refreshButtonLinks();
    }

    @Override
    public void refresh() {
        checkEventStateChanged();
        updateCountdownLabel();
    }

    @Override
    protected boolean getConfigBoolean(String key) {
        return getConfigBoolean(key, configManager);
    }

    @Override
    protected String getConfigValue(String key, String defaultValue) {
        return getConfigValue(key, defaultValue, configManager);
    }

    // Private helpers
    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = createCountdownTimer(this::handleTimerTick);
        countdownTimer.start();
    }

    private void handleTimerTick() {
        boolean isActiveNow = isEventActive();
        updateCountdownLabel();

        if (isActiveNow && hasEventEnded("sotw_end_time", configManager)) {
            countdownTimer.stop();
        }

        if (wasEventActive != isActiveNow) {
            wasEventActive = isActiveNow;
            checkEventStateChanged();
        }
    }

    private void updateCountdownLabel() {
        if (!isEventActive()) {
            updateWrappedLabelText(countdownLabel, getEventEndedMessage(), false);
            return;
        }

        String message = formatCountdownMessage("sotw_start_time", "sotw_end_time", configManager);

        // Override end message with winner info if available
        if (message.equals("Event has ended.")) {
            message = getEventEndedMessageWithWinner();
        }

        updateWrappedLabelText(countdownLabel, message, false);
    }

    private String getEventEndedMessage() {
        LinkedHashMap<String, Integer> leaderboard = parseLeaderboardJson("sotwLeaderboard", "xp", configManager);

        if (leaderboard.isEmpty()) {
            return "The event has ended.";
        }

        String winner = leaderboard.keySet().iterator().next();
        return "The SOTW Event has ended! Congratulations to the winner: " + winner;
    }

    private String getEventEndedMessageWithWinner() {
        String winner = configManager.getConfiguration("flux", "sotw_winner");

        // Try to get winner from leaderboard if not in config
        if ((winner == null || winner.isEmpty()) && tableModel.getRowCount() > 0) {
            Object winnerObj = tableModel.getValueAt(0, 0);
            if (winnerObj != null) {
                winner = winnerObj.toString();
                configManager.setConfiguration("flux", "sotw_winner", winner);
            }
        }

        if (winner == null || winner.isEmpty()) {
            return "The event has ended.";
        }

        return "Event has ended.<br>Congratulations to the winner: " + winner;
    }

    @Override
    public void shutdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        super.shutdown();
    }
}