package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.GoogleSheetParser;
import com.flux.InverseCornerButton;
import com.flux.components.LeaderboardCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BotmCard extends FluxCard {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 25;
    private static final int SPACING_SMALL = 10;
    private static final int SPACING_MEDIUM = 20;
    private static final int SPACING_LARGE = 25;
    private static final int CONTENT_PADDING = 40;
    
    private static final int TIMER_INTERVAL = 1000; // 1 second
    private static final int TABLE_ROW_HEIGHT = 26;
    private static final int TABLE_ROW_SPACING = 3;
    
    private final ConfigManager configManager;
    private final Map<String, InverseCornerButton> buttons = new HashMap<>();
    private final AtomicReference<String> lastLeaderboardJson = new AtomicReference<>("");
    
    private DefaultTableModel tableModel;
    private JLabel eventTitle;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private GoogleSheetParser sheetParser;
    private boolean wasEventActive = false;

    public BotmCard(ConfigManager configManager) {
        super();
        this.configManager = configManager;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        startCountdownTimer();
    }

    private void buildUI() {
        add(createVerticalStrut(SPACING_MEDIUM));
        addTitle();
        addVerticalSpace(SPACING_MEDIUM);
        addEventTitle();
        addCountdownLabel();
        addVerticalSpace(SPACING_SMALL);
        addLeaderboardTable();
        addVerticalSpace(SPACING_LARGE);
        addButtons();
        
        checkEventStateChanged();
    }

    private void addTitle() {
        JLabel title = createCenteredLabel("Boss of the Month", FONT_TITLE, COLOR_YELLOW);
        add(title);
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
        
        // Set initial countdown text
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

        for (LinkButton linkButton : linkButtons) {
            InverseCornerButton button = createLinkButton(linkButton);
            buttons.put(linkButton.label, button);
            add(button);
            addVerticalSpace(SPACING_SMALL);
        }
    }

    private InverseCornerButton createLinkButton(LinkButton linkButton) {
        InverseCornerButton button = InverseCornerButton.withLabelImageAndUrl(
            linkButton.label,
            linkButton.iconPath,
            linkButton.url
        );
        
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension size = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);
        
        return button;
    }

    private JLabel createWrappedLabelWithUnderline(String text, Font font, Color color) {
        String htmlText = String.format(
            "<html><div style='text-align: center; word-wrap: break-word;'><u>%s</u></div></html>",
            text
        );
        
        JLabel label = new JLabel(htmlText);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(getWidth() - CONTENT_PADDING, Integer.MAX_VALUE));
        
        return label;
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
            JSONArray leaderboard = new JSONArray(leaderboardJson);
            tableModel.setRowCount(0);
            
            for (int i = 0; i < leaderboard.length(); i++) {
                JSONObject entry = leaderboard.getJSONObject(i);
                String username = entry.getString("username");
                int score = entry.getInt("score");
                tableModel.addRow(new Object[]{username, score});
            }
        } catch (Exception e) {
            handleAsyncError(e);
            System.err.println("Failed to parse BOTM leaderboard");
        }
    }

    public void updateEventTitle() {
        String titleText = getConfigValue("botmTitle", "No active competition");
        updateWrappedLabelText(eventTitle, titleText, true);
        eventTitle.revalidate();
        eventTitle.repaint();
    }

    public void refreshButtonLinks() {
        String botmWomLink = configManager.getConfiguration("flux", "botmWomUrl");
        InverseCornerButton womButton = buttons.get("BOTM Wise Old Man");
        
        if (womButton != null && botmWomLink != null && !botmWomLink.isEmpty()) {
            womButton.setUrl(botmWomLink);
        }
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
        return "true".equals(configManager.getConfiguration("flux", key));
    }

    @Override
    protected String getConfigValue(String key, String defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
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
        countdownTimer = new Timer(TIMER_INTERVAL, e -> handleTimerTick());
        countdownTimer.start();
    }

    private void handleTimerTick() {
        boolean isActiveNow = isEventActive();
        updateCountdownLabel();
        
        // Stop timer if event has ended to save resources
        if (isActiveNow && hasEventEnded()) {
            countdownTimer.stop();
        }

        // Detect event state change
        if (wasEventActive != isActiveNow) {
            wasEventActive = isActiveNow;
            checkEventStateChanged();
        }
    }

    private boolean hasEventEnded() {
        try {
            String endRaw = configManager.getConfiguration("flux", "botm_end_time");
            if (endRaw != null && !endRaw.isEmpty()) {
                Instant now = Instant.now();
                Instant end = Instant.parse(endRaw);
                return now.isAfter(end);
            }
        } catch (Exception ex) {
            // Ignore parsing errors
        }
        return false;
    }

    private void updateCountdownLabel() {
        if (!isEventActive()) {
            updateWrappedLabelText(countdownLabel, "No active BOTM event.", false);
            return;
        }

        try {
            String startRaw = configManager.getConfiguration("flux", "botm_start_time");
            String endRaw = configManager.getConfiguration("flux", "botm_end_time");

            if (startRaw == null || startRaw.isEmpty() || endRaw == null || endRaw.isEmpty()) {
                updateWrappedLabelText(countdownLabel, "BOTM timing unavailable.", false);
                return;
            }

            Instant now = Instant.now();
            Instant start = Instant.parse(startRaw);
            Instant end = Instant.parse(endRaw);

            if (now.isBefore(start)) {
                Duration untilStart = Duration.between(now, start);
                updateWrappedLabelText(countdownLabel, "Starts in:<br>" + formatDuration(untilStart), false);
            } else if (now.isBefore(end)) {
                Duration remaining = Duration.between(now, end);
                updateWrappedLabelText(countdownLabel, "Ends in:<br>" + formatDuration(remaining), false);
            } else {
                String winner = configManager.getConfiguration("flux", "botmWinner");
                String message = (winner != null && !winner.isEmpty())
                    ? "BOTM has ended.<br>Winner: " + winner
                    : "The event has ended.";
                updateWrappedLabelText(countdownLabel, message, false);
            }
        } catch (Exception ex) {
            updateWrappedLabelText(countdownLabel, "BOTM timing error.", false);
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
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