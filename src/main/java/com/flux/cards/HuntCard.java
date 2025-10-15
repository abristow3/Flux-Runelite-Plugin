package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.services.GoogleSheetParser;
import com.flux.components.LeaderboardCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class HuntCard extends FluxCard {
    private final ConfigManager configManager;

    private JLabel eventTitle;
    private JLabel countdownLabel;
    private JLabel scoreTitleLabel;
    private JLabel scoreLabel;
    private JLabel team1Label;
    private JLabel team2Label;
    private DefaultTableModel team1TableModel;
    private DefaultTableModel team2TableModel;
    private Timer countdownTimer;
    private GoogleSheetParser sheetParser;
    private boolean wasEventActive = false;

    public HuntCard(ConfigManager configManager) {
        super();
        this.configManager = configManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        startCountdownTimer();

        SwingUtilities.invokeLater(() -> {
            refreshLeaderboards();
            updateTeamScores();
        });
    }

    private void buildUI() {
        add(createVerticalStrut(SPACING_MEDIUM));
        add(createCenteredLabel("The Hunt", FONT_TITLE, COLOR_YELLOW));
        addVerticalSpace(SPACING_MEDIUM);

        eventTitle = createWrappedLabelWithUnderline(
                getConfigValue("huntTitle", "No active hunt event", configManager),
                FONT_SECTION,
                COLOR_YELLOW
        );
        setupDynamicResize(eventTitle);
        add(eventTitle);
        addVerticalSpace(SPACING_SMALL);

        countdownLabel = createWrappedLabel("Loading...", FONT_NORMAL, COLOR_LIGHT_GRAY);
        setupDynamicResize(countdownLabel);
        add(countdownLabel);
        updateCountdownLabel();
        addVerticalSpace(SPACING_SMALL);

        addScoreSection();
        addVerticalSpace(SPACING_MEDIUM);
        addTeamLeaderboards();
        addVerticalSpace(SPACING_MEDIUM);
        addButtons();
    }

    private void addScoreSection() {
        scoreTitleLabel = createWrappedLabelWithUnderline("Current Score", FONT_SECTION, COLOR_YELLOW);
        setupDynamicResize(scoreTitleLabel);
        add(scoreTitleLabel);
        addVerticalSpace(SPACING_SMALL);

        String scoreText = buildScoreText();
        scoreLabel = createWrappedLabel(scoreText, FONT_NORMAL, COLOR_LIGHT_GRAY);
        setupDynamicResize(scoreLabel);
        add(scoreLabel);
    }

    private String buildScoreText() {
        String team1Name = getConfigValue("hunt_team_1_name", "Team 1", configManager);
        String team2Name = getConfigValue("hunt_team_2_name", "Team 2", configManager);
        int team1Score = getConfigInt("hunt_team_1_score", 0, configManager);
        int team2Score = getConfigInt("hunt_team_2_score", 0, configManager);
        return String.format("%s: %,d<br>%s: %,d", team1Name, team1Score, team2Name, team2Score);
    }

    private void addTeamLeaderboards() {
        addTeamSection(true);
        addVerticalSpace(SPACING_MEDIUM);
        addTeamSection(false);
        refreshLeaderboards();
    }

    private void addTeamSection(boolean isTeam1) {
        String nameKey = isTeam1 ? "hunt_team_1_name" : "hunt_team_2_name";
        String colorKey = isTeam1 ? "hunt_team_1_color" : "hunt_team_2_color";

        String teamName = getConfigValue(nameKey, isTeam1 ? "Team 1" : "Team 2", configManager);
        Color teamColor = parseColor(getConfigValue(colorKey, "#FF0000", configManager));

        JLabel label = createCenteredLabel(teamName, FONT_NORMAL, teamColor);
        if (isTeam1) {
            team1Label = label;
        } else {
            team2Label = label;
        }
        add(label);
        addVerticalSpace(SPACING_SMALL);
        add(createTeamTable(isTeam1));
    }

    private JScrollPane createTeamTable(boolean isTeam1) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Username", "EHB"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        if (isTeam1) {
            team1TableModel = model;
        } else {
            team2TableModel = model;
        }

        JTable table = new JTable(model);
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

        int scrollHeight = (TABLE_ROW_HEIGHT + TABLE_ROW_SPACING) * 4 +
                table.getTableHeader().getPreferredSize().height + 4;

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(300, scrollHeight));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, scrollHeight));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        return scrollPane;
    }

    private void addButtons() {
        String gdocUrl = getConfigValue("hunt_gdoc_url", "https://discord.com", configManager);
        String womUrl = getConfigValue("hunt_wom_url", "https://wiseoldman.net/competitions", configManager);

        addLinkButtons(new LinkButton[] {
                new LinkButton("Hunt Signup", "/discord.png", "https://discord.com/channels/414435426007384075/414458243499425792"),
                new LinkButton("The Hunt GDoc", "/hunt.png", gdocUrl),
                new LinkButton("Hunt WOM", "/wom.png", womUrl)
        });
    }

    // Public API
    public boolean isEventActive() {
        return getConfigBoolean("huntActive", configManager);
    }

    public void refreshLeaderboards() {
        refreshTeamTable(team1TableModel, "hunt_team_1_leaderboard");
        refreshTeamTable(team2TableModel, "hunt_team_2_leaderboard");
    }

    private void refreshTeamTable(DefaultTableModel model, String configKey) {
        LinkedHashMap<String, Double> leaderboard = loadLeaderboardFromConfig(configKey);
        model.setRowCount(0);

        for (Map.Entry<String, Double> entry : leaderboard.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), String.format("%.2f", entry.getValue())});
        }

        model.fireTableDataChanged();
    }

    private LinkedHashMap<String, Double> loadLeaderboardFromConfig(String configKey) {
        LinkedHashMap<String, Double> leaderboard = new LinkedHashMap<>();
        String raw = configManager.getConfiguration("flux", configKey);

        if (raw == null || raw.isEmpty()) return leaderboard;

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                leaderboard.put(obj.getString("username"), obj.getDouble("ehb"));
            }
        } catch (Exception e) {
            handleAsyncError(e);
        }

        return leaderboard;
    }

    public void updateEventTitle() {
        String titleText = getConfigValue("huntTitle", "No active hunt event", configManager);
        updateWrappedLabelText(eventTitle, titleText, true);
        eventTitle.setMaximumSize(new Dimension(getWidth() - CONTENT_PADDING, Integer.MAX_VALUE));
    }

    public void updateTeamLabels() {
        updateTeamLabel(team1Label, "hunt_team_1_name", "hunt_team_1_color", "Team 1");
        updateTeamLabel(team2Label, "hunt_team_2_name", "hunt_team_2_color", "Team 2");
        updateTeamScores();
    }

    private void updateTeamLabel(JLabel label, String nameKey, String colorKey, String defaultName) {
        label.setText(getConfigValue(nameKey, defaultName, configManager));
        label.setForeground(parseColor(getConfigValue(colorKey, "#FF0000", configManager)));
    }

    public void updateTeamScores() {
        updateWrappedLabelText(scoreLabel, buildScoreText(), false);
    }

    public void refreshButtonLinks() {
        updateButtonUrl("The Hunt GDoc", "hunt_gdoc_url");
        updateButtonUrl("Hunt WOM", "hunt_wom_url");
    }

    private void updateButtonUrl(String buttonLabel, String configKey) {
        String url = getConfigValue(configKey, "", configManager);
        if (buttons.containsKey(buttonLabel) && !url.isEmpty()) {
            buttons.get(buttonLabel).setUrl(url);
        }
    }

    public void checkEventStateChanged() {
        updateCountdownLabel();
        updateEventTitle();
        updateTeamLabels();
        refreshButtonLinks();
        refreshLeaderboards();

        if (isEventActive()) {
            startSheetPolling();
        } else {
            stopSheetPolling();
        }
    }

    private void startSheetPolling() {
        if (sheetParser == null) {
            sheetParser = new GoogleSheetParser(configManager, GoogleSheetParser.SheetType.HUNT, this::handleSheetScoreUpdate);
        }
        sheetParser.start();
    }

    private void stopSheetPolling() {
        if (sheetParser != null) {
            sheetParser.stop();
        }
    }

    private void handleSheetScoreUpdate(Map<String, Integer> scores) {
        String team1Name = getConfigValue("hunt_team_1_name", "Team 1", configManager);
        String team2Name = getConfigValue("hunt_team_2_name", "Team 2", configManager);

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String teamName = entry.getKey();
            int score = entry.getValue();

            if (teamName.equalsIgnoreCase(team1Name) || teamName.contains(team1Name)) {
                configManager.setConfiguration("flux", "hunt_team_1_score", String.valueOf(score));
            } else if (teamName.equalsIgnoreCase(team2Name) || teamName.contains(team2Name)) {
                configManager.setConfiguration("flux", "hunt_team_2_score", String.valueOf(score));
            }
        }

        SwingUtilities.invokeLater(this::updateTeamScores);
    }

    @Override
    public void refresh() {
        checkEventStateChanged();
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

        if (isActiveNow && hasEventEnded("hunt_end_time", configManager)) {
            countdownTimer.stop();
        }

        if (wasEventActive != isActiveNow) {
            wasEventActive = isActiveNow;
            checkEventStateChanged();
        }
    }

    private void updateCountdownLabel() {
        String message = isEventActive()
                ? formatCountdownMessage("hunt_start_time", "hunt_end_time", configManager)
                : "No active Hunt event.";
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