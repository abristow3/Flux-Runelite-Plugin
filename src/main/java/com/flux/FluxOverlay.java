package com.flux;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import javax.inject.Inject;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class FluxOverlay extends OverlayPanel {
    private final ConfigManager configManager;
    private final FluxConfig config;

    @Inject
    private FluxOverlay(ConfigManager configManager, FluxConfig config) {
        this.configManager = configManager;
        this.config = config;

        setPosition(OverlayPosition.TOP_CENTER);
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Clan Events overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        // Fetch real-time config values
        boolean overlayEnabled = config.overlay(); // displayOverlay
        boolean botmActive = Boolean.parseBoolean(configManager.getConfiguration("flux", "botmActive"));

        String eventPass = configManager.getConfiguration("flux", "eventPass");
        String botmPass = configManager.getConfiguration("flux", "botmPassword");

        // Final string to display
        StringBuilder textBuilder = new StringBuilder();

        if (eventPass != null && !eventPass.isEmpty()) {
            textBuilder.append(eventPass);
        }

        if (botmActive && overlayEnabled && botmPass != null && !botmPass.isEmpty()) {
            if (textBuilder.length() > 0) {
                textBuilder.append(" ");
            }
            textBuilder.append(botmPass);
        }

        String text = textBuilder.toString().trim();

        if (text.isEmpty() || !overlayEnabled) {
            return null; // Nothing to render
        }

        Color passColor = config.passColor();
        Color timeColor = config.timeColor();

        // Ensure pass color isn't the same as time color
        if (passColor.equals(timeColor)) {
            passColor = Color.GREEN;
            timeColor = Color.WHITE;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left(text)
                .leftColor(passColor)
                .build());

        if (config.dtm()) {
            String time = localToGMT();
            LineComponent line = (LineComponent) panelComponent.getChildren().get(0);
            line.setRight(time);
            line.setRightColor(timeColor);

            int widthLeft = graphics.getFontMetrics().stringWidth(text);
            int widthRight = graphics.getFontMetrics().stringWidth(time);
            panelComponent.setPreferredSize(new Dimension(widthLeft + widthRight + 20, 0));
        } else {
            int widthLeft = graphics.getFontMetrics().stringWidth(text);
            panelComponent.setPreferredSize(new Dimension(widthLeft + 10, 0));
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
