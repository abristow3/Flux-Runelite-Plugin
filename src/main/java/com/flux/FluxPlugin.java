package com.flux;

import com.flux.cards.HomeCard;
import com.flux.cards.BotmCard;
import com.flux.cards.SotwCard;
import com.google.inject.Provides;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;

import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(name = "Flux", description = "A plugin used to keep track of clan events.", tags = { "flux", "cc",
		"hunt", "pass", "event", "clan" })
public class FluxPlugin extends Plugin {
	public static final String CONFIG_GROUP = "flux";

	@Inject
	private Client client;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private FluxConfig config;
	@Inject
	private ConfigManager configManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private FluxOverlay overlay;
	@Inject
	private SkillIconManager skillIconManager;
	@Inject
	private ClientToolbar clientToolbar;

	private FluxPanel panel;
	private NavigationButton uiNavigationButton;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> clanRankUpdateTask;

	private boolean hasSentMessage = false;
	private boolean isAdmiralOrHigher = false;
	private CompetitionScheduler competitionScheduler;

	@Override
	protected void startUp() {
		overlayManager.add(overlay);
		startClanPanel();

		competitionScheduler = new CompetitionScheduler(configManager);
		competitionScheduler.startScheduler();

		if (panel != null) {
			SotwCard sotwCard = panel.getSotwCard();
			if (sotwCard != null) {
				sotwCard.checkEventStateChanged();
			}

			BotmCard botmCard = panel.getBotmCard();
			if (botmCard != null) {
				botmCard.checkEventStateChanged();
			}
		}
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(uiNavigationButton);
		if (competitionScheduler != null) {
			competitionScheduler.stopScheduler();
		}
	}

	private void startClanPanel() {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/flux-icon-tiny.png");

		panel = new FluxPanel();
		panel.init(config, configManager);

		uiNavigationButton = NavigationButton.builder()
				.tooltip("Flux")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(uiNavigationButton);
	}

	@Provides
	FluxConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FluxConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN && !hasSentMessage) {
			chatMessageManager.queue(
					QueuedMessage.builder()
							.type(ChatMessageType.GAMEMESSAGE)
							.runeLiteFormattedMessage(
									"<col=ff9600>Welcome back! Flux plugin loaded successfully.</col>")
							.build());
			hasSentMessage = true;
		}

		if (event.getGameState() == GameState.LOGGED_IN) {
			if (clanRankUpdateTask != null && !clanRankUpdateTask.isCancelled()) {
				clanRankUpdateTask.cancel(false);
			}

			clanRankUpdateTask = scheduler.scheduleAtFixedRate(this::updateClanRank, 7, 5, TimeUnit.SECONDS);
		}

		if (event.getGameState() == GameState.LOGIN_SCREEN) {
			hasSentMessage = false;
			if (clanRankUpdateTask != null) {
				clanRankUpdateTask.cancel(false);
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event) {
		System.out.println("CONFIG CHANGE EVENT KEY: " + event.getKey());

		if (!event.getGroup().equals(CONFIG_GROUP)) {
			return;
		}

		if (event.getKey().equals("plugin_announcement_message")) {
			if (panel != null) {
				HomeCard homeCard = panel.getHomeCard();
				if (homeCard != null) {
					homeCard.refreshPluginAnnouncement();
					homeCard.refreshButtonLinks();
				}
			}
		}

		if (event.getKey().equals("botmActive") || event.getKey().equals("botm_active")) {
			if (panel != null) {
				BotmCard botmCard = panel.getBotmCard();
				if (botmCard != null) {
					botmCard.checkEventStateChanged(); // Refresh countdown + UI
				}
			}
		}

		if (event.getKey().equals("sotw_active") || event.getKey().equals("sotwActive")) {
			if (panel != null) {
				panel.refreshAllCards();

				SotwCard sotwCard = panel.getSotwCard();
				if (sotwCard != null) {
					sotwCard.checkEventStateChanged();
				}
			}
		}

		if (event.getKey().equals("rollCallActive")) {
			System.out.println("Roll Call Status Changed");
			if (panel != null) {
				HomeCard homeCard = panel.getHomeCard();
				homeCard.isRollCallActive();
			}
		}

		if (event.getKey().equals("sotw_wom_link") || event.getKey().equals("sotwWomLink")) {
			if (panel != null) {
				SotwCard sotwCard = panel.getSotwCard();
				if (sotwCard != null) {
					sotwCard.refreshButtonLinks();
				}
			}
		}

		if (event.getKey().equals("sotw_leaderboard") || event.getKey().equals("sotwLeaderboard")) {

			// Retrieve the JSON string from config
			String leaderboardJson = configManager.getConfiguration("flux", "sotwLeaderboard", String.class);

			if (leaderboardJson != null && !leaderboardJson.isEmpty()) {
				try {
					JSONArray leaderboardArray = new JSONArray(leaderboardJson);
					System.out.println("Current SOTW Leaderboard:");
					for (int i = 0; i < leaderboardArray.length(); i++) {
						JSONObject entry = leaderboardArray.getJSONObject(i);
						String username = entry.getString("username");
						int xp = entry.getInt("xp");
						System.out.printf(" - %-20s %,d XP%n", username, xp);
					}
				} catch (Exception e) {
					System.out.println("Error parsing leaderboard JSON: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				System.out.println("Leaderboard data is empty or null.");
			}

			if (panel != null) {
				panel.refreshAllCards(); // Or call a method just to update the table
			}
		}

	}

	private void updateClanRank() {
		isAdmiralOrHigher = false;

		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null || clanChannel.getName() == null) {
			return;
		}

		String clanName = clanChannel.getName().trim();
		if (!clanName.equalsIgnoreCase("Flux")) {
			return;
		}

		ClanRank rank = getLocalPlayerClanRank();
		if (rank == null) {
			return;
		}

		int myRankValue = rank.getRank();
		int adminThreshold = ClanRank.ADMINISTRATOR.getRank();
		isAdmiralOrHigher = myRankValue >= adminThreshold;

		if (panel != null) {
			panel.updateClanRankStatus(isAdmiralOrHigher);
		}

		// Stop the task if user is admin or higher
		if (isAdmiralOrHigher && clanRankUpdateTask != null) {
			clanRankUpdateTask.cancel(false);
			clanRankUpdateTask = null;
		}
	}

	private ClanRank getLocalPlayerClanRank() {
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null) {
			return null;
		}

		String localPlayerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (localPlayerName == null) {
			return null;
		}

		for (ClanChannelMember member : clanChannel.getMembers()) {
			if (localPlayerName.equalsIgnoreCase(member.getName())) {
				return member.getRank();
			}
		}

		return null;
	}
}
