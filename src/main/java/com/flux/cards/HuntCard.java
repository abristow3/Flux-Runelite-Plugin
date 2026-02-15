package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.services.GoogleSheetParser;
import com.flux.components.LeaderboardCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.OkHttpClient;

public class HuntCard extends FluxCard {
    private final ConfigManager configManager;
    private LeaderboardCellRenderer leaderboardCellRenderer;
    private JLabel eventTitle;
    private JLabel countdownLabel;
    private JLabel scoreTitleLabel;
    private JLabel scoreLabel;
    private DefaultTableModel combinedTableModel;
    private Timer countdownTimer;
    private GoogleSheetParser sheetParser;
    private boolean wasEventActive = false;
    private static final Logger logger = LoggerFactory.getLogger(HuntCard.class);
    private final OkHttpClient httpClient;

    public HuntCard(ConfigManager configManager, OkHttpClient httpClient) {
        super();
        this.configManager = configManager;
        this.httpClient = httpClient;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        startCountdownTimer();

        SwingUtilities.invokeLater(() -> {
            refreshLeaderboard();
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
        addCombinedLeaderboard();
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

        Color team1Color = parseColor(getConfigValue("hunt_team_1_color", "#FF0000", configManager));
        Color team2Color = parseColor(getConfigValue("hunt_team_2_color", "#0000FF", configManager));

        String team1ColorHex = String.format("#%02x%02x%02x", team1Color.getRed(), team1Color.getGreen(), team1Color.getBlue());
        String team2ColorHex = String.format("#%02x%02x%02x", team2Color.getRed(), team2Color.getGreen(), team2Color.getBlue());

        return String.format("<span style='color: %s;'>%s: %,d</span><br><span style='color: %s;'>%s: %,d</span>",
                team1ColorHex, team1Name, team1Score,
                team2ColorHex, team2Name, team2Score);
    }

    private void addCombinedLeaderboard() {
        add(createSectionTitle("Top 10 EHB"));
        addVerticalSpace(SPACING_SMALL);

        combinedTableModel = new DefaultTableModel(new Object[]{"Player", "EHB"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(combinedTableModel);
        configureTable(table);

        int scrollHeight = (TABLE_ROW_HEIGHT + TABLE_ROW_SPACING) * 5 +
                table.getTableHeader().getPreferredSize().height + 4;

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(300, scrollHeight));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, scrollHeight));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(scrollPane);
    }

    private void configureTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        table.setShowGrid(false);
        table.setFont(FONT_NORMAL);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setIntercellSpacing(new Dimension(0, TABLE_ROW_SPACING));

        TeamColoredLeaderboardRenderer renderer = new TeamColoredLeaderboardRenderer();
        renderer.setTable(table);
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);
    }

    private void addButtons() {
        String gdocUrl = getConfigValue("hunt_gdoc_url", "https://docs.google.com/spreadsheets/d/e/2PACX-1vSLCxscAVFZY9wuDqmeBPu4UZio2I39DHDGy_8DXrvHqYKmZc8NgsC4DWv_olXOTjGQktcBnU88Fmf4/pubhtml?gid=0&single=true", configManager);
        String womUrl = getConfigValue("hunt_wom_url", "https://wiseoldman.net/competitions", configManager);
        String huntSignupChannelUrl = getConfigValue("hunt_signup_discord_channel_url", "discord://discord.com/channels/414435426007384075/414458243499425792", configManager);

        addLinkButtons(new LinkButton[]{
                new LinkButton("Hunt Signup", "/discord.png", huntSignupChannelUrl),
                new LinkButton("The Hunt GDoc", "/hunt.png", gdocUrl),
                new LinkButton("Hunt WOM", "/wom.png", womUrl)
        });
    }

    public boolean isEventActive() {
        return getConfigBoolean("huntActive", configManager);
    }

    public void refreshLeaderboard() {
        String team1Name = getConfigValue("hunt_team_1_name", "Team 1", configManager);
        String team2Name = getConfigValue("hunt_team_2_name", "Team 2", configManager);

        Map<String, Double> team1Data = loadLeaderboardFromConfig("hunt_team_1_leaderboard");
        Map<String, Double> team2Data = loadLeaderboardFromConfig("hunt_team_2_leaderboard");

        List<PlayerEntry> allPlayers = new ArrayList<>();

        for (Map.Entry<String, Double> entry : team1Data.entrySet()) {
            allPlayers.add(new PlayerEntry(entry.getKey(), team1Name, entry.getValue()));
        }

        for (Map.Entry<String, Double> entry : team2Data.entrySet()) {
            allPlayers.add(new PlayerEntry(entry.getKey(), team2Name, entry.getValue()));
        }

        allPlayers.sort((a, b) -> Double.compare(b.ehb, a.ehb));

        List<PlayerEntry> top10 = allPlayers.size() > 10 ? allPlayers.subList(0, 10) : allPlayers;

        combinedTableModel.setRowCount(0);
        for (PlayerEntry player : top10) {
            combinedTableModel.addRow(new Object[]{
                    player.username,
                    String.format("%.2f", player.ehb)
            });
        }

        combinedTableModel.fireTableDataChanged();
    }

    private Map<String, Double> loadLeaderboardFromConfig(String configKey) {
        Map<String, Double> leaderboard = new LinkedHashMap<>();
        String raw = configManager.getConfiguration("flux", configKey);

        if (raw == null || raw.isEmpty()) return leaderboard;

        try {
            JsonParser jsonParser = new JsonParser();
            JsonArray array = jsonParser.parse(raw).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                leaderboard.put(obj.get("username").getAsString(), obj.get("ehb").getAsDouble());
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
        eventTitle.revalidate();
        eventTitle.repaint();
    }

    public void updateTeamLabels() {
        updateTeamScores();
        refreshLeaderboard();
    }

    public void updateTeamScores() {
        updateWrappedLabelText(scoreLabel, buildScoreText(), false);
        scoreLabel.revalidate();
        scoreLabel.repaint();
    }

    public void refreshButtonLinks() {
        updateButtonUrl("The Hunt GDoc", "hunt_gdoc_url", configManager);
        updateButtonUrl("Hunt WOM", "hunt_wom_url", configManager);
        updateButtonUrl("Hunt Signup", "hunt_signup_discord_channel_url", configManager);
    }

    public void checkEventStateChanged() {
        updateCountdownLabel();
        updateEventTitle();
        updateTeamLabels();
        refreshButtonLinks();
        refreshLeaderboard();

        if (isEventActive()) {
            startSheetPolling();
        } else {
            stopSheetPolling();
        }
    }

    private void startSheetPolling() {
        if (sheetParser == null) {
            sheetParser = new GoogleSheetParser(configManager, GoogleSheetParser.SheetType.HUNT, this::handleSheetScoreUpdate, httpClient);
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
        if (!isEventActive()) {
            updateWrappedLabelText(countdownLabel, getEventEndedMessage(), false);
            return;
        }

        String message = formatCountdownMessage("hunt_start_time", "hunt_end_time", configManager);
        updateWrappedLabelText(countdownLabel, message, false);
    }

    private String getEventEndedMessage() {
        String winner = getWinnerFromGoogleSheetScores();

        if (winner != null && !winner.isEmpty()) {
            return "The Hunt has ended!<br>Winning team: " + winner;
        }

        return "No active Hunt event.";
    }

    private String getWinnerFromGoogleSheetScores() {
        int team1Score = getConfigInt("hunt_team_1_score", 0, configManager);
        int team2Score = getConfigInt("hunt_team_2_score", 0, configManager);

        if (team1Score == 0 && team2Score == 0) {
            return null;
        }

        String team1Name = getConfigValue("hunt_team_1_name", "Team 1", configManager);
        String team2Name = getConfigValue("hunt_team_2_name", "Team 2", configManager);

        if (team1Score > team2Score) {
            return team1Name;
        } else if (team2Score > team1Score) {
            return team2Name;
        } else {
            return team1Name + " and " + team2Name + " (Tie)";
        }
    }

    @Override
    public void shutdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        if (leaderboardCellRenderer != null) {
            leaderboardCellRenderer.shutdown(); // stop table timers
        }

        stopSheetPolling();
        super.shutdown();
    }

    private static class PlayerEntry {
        final String username;
        final String teamName;
        final double ehb;

        PlayerEntry(String username, String teamName, double ehb) {
            this.username = username;
            this.teamName = teamName;
            this.ehb = ehb;
        }
    }

    private class TeamColoredLeaderboardRenderer extends LeaderboardCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String playerName = "";
            if (row < table.getRowCount() && table.getColumnCount() > 0) {
                Object nameValue = table.getValueAt(row, 0);
                if (nameValue != null) {
                    playerName = nameValue.toString();
                }
            }

            Map<String, Double> team1Data = loadLeaderboardFromConfig("hunt_team_1_leaderboard");
            Map<String, Double> team2Data = loadLeaderboardFromConfig("hunt_team_2_leaderboard");

            Color team1Color = parseColor(getConfigValue("hunt_team_1_color", "#FF0000", configManager));
            Color team2Color = parseColor(getConfigValue("hunt_team_2_color", "#0000FF", configManager));

            if (team1Data.containsKey(playerName)) {
                c.setForeground(team1Color);
            } else if (team2Data.containsKey(playerName)) {
                c.setForeground(team2Color);
            } else {
                c.setForeground(COLOR_WHITE);
            }

            if (column == 1) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            return c;
        }
    }
}
