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

        // Check if overlay is enabled
        boolean overlayEnabled = config.overlay();
        if (!overlayEnabled) {
            return null; // Overlay is disabled
        }

        // Fetch active event statuses
        boolean botmActive = getBooleanConfig("botmActive", false);
        boolean huntActive = getBooleanConfig("huntActive", false);

        // Fetch passwords
        String eventPass = config.eventPass();
        String botmPass = config.botmPass();
        String huntPasswords = getStringConfig("hunt_passwords", "");

        // Build the display text
        StringBuilder textBuilder = new StringBuilder();

        // Always show event password if configured (no prefix)
        if (eventPass != null && !eventPass.isEmpty()) {
            textBuilder.append(eventPass);
        }

        // Show BOTM password if BOTM is active (with prefix)
        if (botmActive && botmPass != null && !botmPass.isEmpty()) {
            if (textBuilder.length() > 0) {
                textBuilder.append(" | ");
            }
            textBuilder.append("BOTM: ").append(botmPass);
        }

        // Show Hunt passwords if Hunt is active (with prefix)
        if (huntActive && huntPasswords != null && !huntPasswords.isEmpty()) {
            if (textBuilder.length() > 0) {
                textBuilder.append(" | ");
            }
            textBuilder.append("Hunt: ").append(huntPasswords);
        }

        String text = textBuilder.toString().trim();

        // If no text to display, don't render
        if (text.isEmpty()) {
            return null;
        }

        // Get colors
        Color passColor = config.passColor();
        Color timeColor = config.timeColor();

        // Ensure pass color isn't the same as time color
        if (passColor.equals(timeColor)) {
            passColor = Color.GREEN;
            timeColor = Color.WHITE;
        }

        // Add password text
        panelComponent.getChildren().add(LineComponent.builder()
                .left(text)
                .leftColor(passColor)
                .build());

        // Add date/time if enabled
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

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private String getStringConfig(String key, String defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static String localToGMT() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date) + " UTC";
    }
}