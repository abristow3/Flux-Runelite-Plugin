package com.flux;

import com.flux.services.EventPasswordManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class FluxOverlay extends OverlayPanel {
	private final ConfigManager configManager;
	private final FluxConfig config;
    private final EventPasswordManager passwordManager;

	@Inject
	private FluxOverlay(ConfigManager configManager, FluxConfig config) {
		this.configManager = configManager;
		this.config = config;
        this.passwordManager = new EventPasswordManager(configManager);

		setPosition(OverlayPosition.TOP_CENTER);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Clan Events overlay"));
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.overlay()) {
			return null;
		}

        String overlayString = passwordManager.buildOverlayString();
		Color passColor = config.passColor();
		Color timeColor = config.timeColor();

		if (passColor.toString().equals(timeColor.toString())) {
			passColor = Color.green;
			timeColor = Color.WHITE;
		}


		if (config.overlay()) {
			panelComponent.getChildren().add(LineComponent.builder().left(overlayString).leftColor(passColor).build());

			if (config.dtm()) {
				overlayString = overlayString + " " + localToGMT();
				List<LayoutableRenderableEntity> elem = panelComponent.getChildren();
				((LineComponent) elem.get(0)).setRight(localToGMT());
				((LineComponent) elem.get(0)).setRightColor(timeColor);
			}
			panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(overlayString) + 10, 0));
		}
		return super.render(graphics);
	}

	public static String localToGMT() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(date) + " UTC";
	}
}