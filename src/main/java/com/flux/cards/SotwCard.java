package com.flux.cards;

import com.flux.components.LeaderboardCellRenderer;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.hiscore.HiscoreSkill;

@Slf4j
public class SotwCard extends FluxCard {

	private final ConfigManager configManager;
	private LeaderboardCellRenderer leaderboardCellRenderer;
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
			new LinkButton("SOTW", "/discord.png",
				"discord://discord.com/channels/414435426007384075/416998364601909288"),
			new LinkButton("SOTW Wise Old Man", "/wom.png",
				getConfigValue("sotw_wom_link", "https://wiseoldman.net/groups/141/competitions"))
		};
		addLinkButtons(linkButtons);
	}

	public void refreshLeaderboard() {
		if (tableModel == null) {
			return;
		}

		tableModel.setRowCount(0);
		LinkedHashMap<String, Integer> leaderboard = new LinkedHashMap<>();
		try {
			leaderboard = parseLeaderboardJson("sotwLeaderboard", "xp", configManager);
		} catch (Exception e) {
			log.warn("Failed to parse leaderboard JSON", e);
		}

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
		updateEventTitle();
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

	public void startCountdownTimer() {
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
		if (countdownLabel == null) {
			return;
		}

		if (!isEventActive()) {
			updateWrappedLabelText(countdownLabel, getEventEndedMessage(), false);
			return;
		}

		String message = "Event status unavailable.";
		try {
			if (configManager != null) {
				message = formatCountdownMessage("sotw_start_time", "sotw_end_time", configManager);
			}
		} catch (Exception e) {
			log.warn("Failed to format countdown message", e);
		}

		if ("Event has ended.".equals(message)) {
			message = getEventEndedMessage();
		}

		updateWrappedLabelText(countdownLabel, message, false);
	}

	private String getEventEndedMessage() {
		return "The SOTW Event has ended!";
	}

	public HiscoreSkill getSkill() {
		String skillName = getConfigValue("sotwSkill", HiscoreSkill.OVERALL.name());

		try {
			return HiscoreSkill.valueOf(skillName);
		} catch (IllegalArgumentException e) {
			log.debug("Could not find SOTW skill name {}", skillName);
			return HiscoreSkill.OVERALL;
		}
	}

	@Override
	public void shutdown() {
		if (countdownTimer != null) {
			countdownTimer.stop();
		}

		if (leaderboardCellRenderer != null) {
			leaderboardCellRenderer.shutdown();
		}

		super.shutdown();
	}
}