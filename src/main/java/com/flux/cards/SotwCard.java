package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.InverseCornerButton;
import com.flux.components.LeaderboardCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class SotwCard extends FluxCard {
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
    private final Map<String, InverseCornerButton> buttons = new LinkedHashMap<>();
    
    private DefaultTableModel tableModel;
    private JLabel eventTitle;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private boolean wasEventActive = false;

    public SotwCard(ConfigManager configManager) {
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
        addVerticalSpace(SPACING_SMALL);
        addCountdownLabel();
        addVerticalSpace(SPACING_SMALL);
        addLeaderboardTable();
        addVerticalSpace(SPACING_LARGE);
        addButtons();
    }

    private void addTitle() {
        JLabel title = createCenteredLabel("Skill of the Week", FONT_TITLE, COLOR_YELLOW);
        add(title);
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
        
        // Set initial countdown text
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
        
        return label;
    }

    private void setupDynamicResize(JLabel label) {
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                int maxWidth = getWidth() - CONTENT_PADDING;
                label.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
                label.revalidate();
                label.repaint();
            }
        });
    }

    // Public API
    public void refreshLeaderboard() {
        tableModel.setRowCount(0);
        LinkedHashMap<String, Integer> leaderboard = loadLeaderboardFromConfig();

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
        String sotwWomLink = configManager.getConfiguration("flux", "sotw_wom_link");
        InverseCornerButton womButton = buttons.get("SOTW Wise Old Man");
        
        if (womButton != null && sotwWomLink != null && !sotwWomLink.isEmpty()) {
            womButton.setUrl(sotwWomLink);
        }
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
        return "true".equals(configManager.getConfiguration("flux", key));
    }

    @Override
    protected String getConfigValue(String key, String defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    // Private helpers
    private LinkedHashMap<String, Integer> loadLeaderboardFromConfig() {
        LinkedHashMap<String, Integer> leaderboard = new LinkedHashMap<>();
        String raw = configManager.getConfiguration("flux", "sotwLeaderboard");

        if (raw == null || raw.isEmpty()) {
            return leaderboard;
        }

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String username = obj.getString("username");
                int xp = obj.getInt("xp");
                leaderboard.put(username, xp);
            }
        } catch (Exception e) {
            handleAsyncError(e);
            System.err.println("Failed to parse SOTW leaderboard from config");
        }

        return leaderboard;
    }

    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

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
            String endRaw = configManager.getConfiguration("flux", "sotw_end_time");
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
            updateWrappedLabelText(countdownLabel, getEventEndedMessage(), false);
            return;
        }

        try {
            String startRaw = configManager.getConfiguration("flux", "sotw_start_time");
            String endRaw = configManager.getConfiguration("flux", "sotw_end_time");

            if (startRaw == null || startRaw.isEmpty() || endRaw == null || endRaw.isEmpty()) {
                updateWrappedLabelText(countdownLabel, "Timing data unavailable.", false);
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
                updateWrappedLabelText(countdownLabel, getEventEndedMessageWithWinner(), false);
            }
        } catch (Exception ex) {
            updateWrappedLabelText(countdownLabel, "Error loading timer.", false);
        }
    }

    private String getEventEndedMessage() {
        LinkedHashMap<String, Integer> leaderboard = loadLeaderboardFromConfig();
        
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
        super.shutdown();
    }
}