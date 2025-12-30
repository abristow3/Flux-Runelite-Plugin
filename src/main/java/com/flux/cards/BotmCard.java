package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.services.GoogleSheetParser;
import com.flux.components.LeaderboardCellRenderer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class BotmCard extends FluxCard {
    private final ConfigManager configManager;
    private final AtomicReference<String> lastLeaderboardJson = new AtomicReference<>("");

    private DefaultTableModel tableModel;
    private JLabel eventTitle;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private GoogleSheetParser sheetParser;
    private boolean wasEventActive = false;
    private static final Logger logger = LoggerFactory.getLogger(BotmCard.class);

    public BotmCard(ConfigManager configManager) {
        super();
        this.configManager = configManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        startCountdownTimer();
    }

    private void buildUI() {
        add(createVerticalStrut(SPACING_MEDIUM));
        add(createCenteredLabel("Boss of the Month", FONT_TITLE, COLOR_YELLOW));
        addVerticalSpace(SPACING_MEDIUM);
        addEventTitle();
        addCountdownLabel();
        addVerticalSpace(SPACING_SMALL);
        addLeaderboardTable();
        addVerticalSpace(SPACING_LARGE);
        addButtons();

        checkEventStateChanged();
    }

    private void addEventTitle() {
        String titleText = getConfigValue("botmTitle", "No active competition");
        eventTitle = createWrappedLabelWithUnderline(titleText, FONT_SECTION, COLOR_YELLOW);

        // Center using FlowLayout panel
        JPanel eventTitlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        eventTitlePanel.setOpaque(false);
        eventTitlePanel.add(eventTitle);
        add(eventTitlePanel);
    }

    private void addCountdownLabel() {
        countdownLabel = createWrappedLabel("Loading...", FONT_NORMAL, COLOR_LIGHT_GRAY);

        // Center using FlowLayout panel
        JPanel countdownPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        countdownPanel.setOpaque(false);
        countdownPanel.add(countdownLabel);
        add(countdownPanel);

        updateCountdownLabel();
    }

    private void addLeaderboardTable() {
        tableModel = new DefaultTableModel(new Object[]{"Username", "Score"}, 0) {
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
                new LinkButton("BOTM", "/discord.png", "https://discord.com/channels/414435426007384075/1014523362711715860"),
                new LinkButton("BOTM Drops", "/discord.png", "https://discord.com/channels/414435426007384075/1047792122914406420"),
                new LinkButton("BOTM Wise Old Man", "/wom.png", getConfigValue("botmWomUrl", "https://wiseoldman.net/groups/141/competitions"))
        };
        addLinkButtons(linkButtons);
    }

    // Public API
    public boolean isEventActive() {
        return getConfigBoolean("botmActive");
    }

    public void refreshLeaderboard() {
        String leaderboardJson = configManager.getConfiguration("flux", "botmLeaderboard");
        if (leaderboardJson == null || leaderboardJson.isEmpty()) {
            return;
        }

        try {
            JsonArray leaderboard = JsonParser.parseString(leaderboardJson).getAsJsonArray();
            tableModel.setRowCount(0);

            for (int i = 0; i < leaderboard.size(); i++) {
                JsonObject entry = leaderboard.get(i).getAsJsonObject();
                String username = entry.get("username").getAsString();
                int score = entry.get("score").getAsInt();
                tableModel.addRow(new Object[]{username, score});
            }
        } catch (Exception e) {
            handleAsyncError(e);
            logger.error("Failed to parse BOTM leaderboard", e);
        }
    }

    public void updateEventTitle() {
        String titleText = getConfigValue("botmTitle", "No active competition");
        updateWrappedLabelText(eventTitle, titleText, true);
        eventTitle.revalidate();
        eventTitle.repaint();
    }

    public void refreshButtonLinks() {
        updateButtonUrl("BOTM Wise Old Man", "botmWomUrl", configManager);
    }

    public void checkEventStateChanged() {
        updateCountdownLabel();
        updateEventTitle();
        refreshButtonLinks();

        if (isEventActive()) {
            startSheetPolling();
        } else {
            stopSheetPolling();
        }
    }

    @Override
    public void refresh() {
        checkEventStateChanged();
        refreshLeaderboard();
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
    private void startSheetPolling() {
        if (sheetParser == null) {
            sheetParser = new GoogleSheetParser(configManager, leaderboardJsonArray -> {
                String newJson = leaderboardJsonArray.toString();
                if (!newJson.equals(lastLeaderboardJson.get())) {
                    lastLeaderboardJson.set(newJson);
                    configManager.setConfiguration("flux", "botmLeaderboard", newJson);
                    SwingUtilities.invokeLater(this::refreshLeaderboard);
                }
            });
        }
        sheetParser.start();
    }

    private void stopSheetPolling() {
        if (sheetParser != null) {
            sheetParser.stop();
        }
    }

    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        wasEventActive = isEventActive();
        countdownTimer = createCountdownTimer(this::handleTimerTick);
        countdownTimer.start();
    }

    private void handleTimerTick() {
        boolean isActiveNow = isEventActive();
        updateCountdownLabel();

        if (isActiveNow && hasEventEnded("botm_end_time", configManager)) {
            countdownTimer.stop();
        }

        if (wasEventActive != isActiveNow) {
            wasEventActive = isActiveNow;
            checkEventStateChanged();
        }
    }

    private void updateCountdownLabel() {
        if (!isEventActive()) {
            updateWrappedLabelText(countdownLabel, "No active BOTM event.", false);
            return;
        }

        String message = formatCountdownMessage("botm_start_time", "botm_end_time", configManager);

        // Override end message with winner info if available
        if (message.equals("Event has ended.")) {
            String winner = configManager.getConfiguration("flux", "botmWinner");
            message = (winner != null && !winner.isEmpty())
                    ? "BOTM has ended.<br>Winner: " + winner
                    : "The event has ended.";
        } else if (message.equals("Timing unavailable.")) {
            message = "BOTM timing unavailable.";
        } else if (message.equals("Timing error.")) {
            message = "BOTM timing error.";
        }

        updateWrappedLabelText(countdownLabel, message, false);
    }

    @Override
    public void shutdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        stopSheetPolling();
        super.shutdown();
    }
}