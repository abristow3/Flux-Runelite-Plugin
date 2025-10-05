/*
 * Copyright (c) 2022, cmsu224 <https://github.com/cmsu224>
 * Copyright (c) 2022, Brianmm94 <https://github.com/Brianmm94>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.flux;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
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
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.UIManager;
import java.util.Enumeration;

@Slf4j
@PluginDescriptor(name = "Flux", description = "A plugin used to keep track of clan events.", tags = { "flux", "cc",
		"hunt", "pass", "event", "clan" })
public class FluxPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private FluxConfig config;

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

	public static final String CONFIG_GROUP = "flux";

	private boolean hasSentMessage = false;

	@Override
	protected void startUp() {
		Font runescapeFont = loadCustomFont();
		setGlobalFont(runescapeFont);

		overlayManager.add(overlay);
		startClanPanel(runescapeFont);  // Pass the font here
	}



	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(uiNavigationButton);
	}

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> clanRankUpdateTask;

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
			// Cancel existing task if running
			if (clanRankUpdateTask != null && !clanRankUpdateTask.isCancelled()) {
				clanRankUpdateTask.cancel(false);
			}

			clanRankUpdateTask = scheduler.scheduleAtFixedRate(() -> {
				updateClanRank();
			}, 7, 5, TimeUnit.SECONDS);
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
		if (event.getGroup().equals(CONFIG_GROUP)) {
			if (panel != null) {
				panel.removeAll();
				panel.init(config);
			}
			// No need to add navigation again if already added
		}
	}


	private void startClanPanel(Font runescapeFont) {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		panel = new FluxPanel(runescapeFont);
		panel.init(config);

		uiNavigationButton = NavigationButton.builder()
				.tooltip("Clan Events")
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

	private boolean isAdmiralOrHigher = false;

	private ClanRank getLocalPlayerClanRank() {
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null) {
			return null; // Not in a clan channel
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

		return null; // Player not found in clan channel
	}

	private void updateClanRank() {
		isAdmiralOrHigher = false;

		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null || clanChannel.getName() == null) {
			return;
		}

		// Ensure the user is in the Flux clan
		String clanName = clanChannel.getName().trim();
		if (!clanName.equalsIgnoreCase("Flux")) {
			return; // Not in the Flux clan
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
	}

	private Font loadCustomFont() {
		try {
			InputStream fontStream = getClass().getResourceAsStream("/net/runelite/client/plugins/flux/fonts/RunescapeUF.ttf");
			if (fontStream == null) {
				log.error("Font resource not found!");
				return new Font("SansSerif", Font.PLAIN, 12);
			}
			Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(font);
			return font;
		} catch (Exception e) {
			log.warn("Failed to load custom font, using default.", e);
			return new Font("SansSerif", Font.PLAIN, 12);
		}
	}


	private void setGlobalFont(Font font) {
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof Font) {
				UIManager.put(key, font);
			}
		}
	}

}
