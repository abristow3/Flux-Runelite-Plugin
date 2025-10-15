package com.flux.cards;

import com.flux.components.InverseCornerButton;
import net.runelite.client.config.ConfigManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all Flux plugin cards.
 * Provides scrollable panel functionality, async execution utilities, and common UI helpers.
 */
public abstract class FluxCard extends JPanel implements Scrollable {

    // Constants
    protected static final int SCROLL_UNIT_INCREMENT = 16;
    protected static final int SCROLL_BLOCK_INCREMENT = 64;
    protected static final int SIDE_PADDING = 10;
    protected static final int CONTENT_PADDING = 40;
    protected static final int TIMER_INTERVAL = 1000;

    // Common spacing
    protected static final int SPACING_SMALL = 10;
    protected static final int SPACING_MEDIUM = 20;
    protected static final int SPACING_LARGE = 25;

    // Common button dimensions
    protected static final int BUTTON_WIDTH = 200;
    protected static final int BUTTON_HEIGHT = 25;

    // Common table dimensions
    protected static final int TABLE_ROW_HEIGHT = 26;
    protected static final int TABLE_ROW_SPACING = 3;

    // Common colors used across cards
    protected static final Color COLOR_YELLOW = Color.YELLOW;
    protected static final Color COLOR_LIGHT_GRAY = Color.LIGHT_GRAY;
    protected static final Color COLOR_WHITE = Color.WHITE;
    protected static final Color COLOR_GREEN = Color.GREEN;
    protected static final Color COLOR_RED = Color.RED;

    // Common fonts used across cards
    protected static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 20);
    protected static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 16);
    protected static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 14);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    protected final Map<String, InverseCornerButton> buttons = new HashMap<>();

    protected FluxCard() {
        setOpaque(false);
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(0, SIDE_PADDING, 0, SIDE_PADDING));
    }

    // ========== Scrollable Interface ==========

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return SCROLL_UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return SCROLL_BLOCK_INCREMENT;
    }

    // ========== Async Utilities ==========

    protected void runAsync(Runnable task) {
        executor.submit(task);
    }

    protected void runAsyncWithCallback(Runnable backgroundTask, Runnable edtCallback) {
        executor.submit(() -> {
            try {
                backgroundTask.run();
                SwingUtilities.invokeLater(edtCallback);
            } catch (Exception e) {
                handleAsyncError(e);
            }
        });
    }

    protected void handleAsyncError(Exception e) {
        System.err.println("Async error in " + getClass().getSimpleName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    // ========== UI Helper Methods ==========

    protected JLabel createCenteredLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        if (font != null) {
            label.setFont(font);
        }
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    protected JLabel createWrappedLabel(String text, Font font, Color color) {
        String html = String.format(
                "<html><div style='text-align: center; word-wrap: break-word;'>%s</div></html>",
                text
        );
        return createCenteredLabel(html, font, color);
    }

    protected JLabel createWrappedLabelWithUnderline(String text, Font font, Color color) {
        String htmlText = String.format(
                "<html><div style='text-align: center; word-wrap: break-word;'><u>%s</u></div></html>",
                text
        );

        JLabel label = new JLabel(htmlText);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        return label;
    }

    protected JLabel createSectionTitle(String text) {
        String html = String.format("<html><u>%s</u></html>", text);
        return createCenteredLabel(html, FONT_SECTION, COLOR_YELLOW);
    }

    protected void updateWrappedLabelText(JLabel label, String text, boolean underline) {
        String format = underline
                ? "<html><div style='text-align: center; word-wrap: break-word;'><u>%s</u></div></html>"
                : "<html><div style='text-align: center; word-wrap: break-word;'>%s</div></html>";

        label.setText(String.format(format, text));
    }

    protected void addVerticalSpace(int height) {
        add(Box.createRigidArea(new Dimension(0, height)));
    }

    protected Component createVerticalStrut(int height) {
        return Box.createVerticalStrut(height);
    }

    protected void setupDynamicResize(JLabel label) {
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                int maxWidth = getWidth() > CONTENT_PADDING ? getWidth() - CONTENT_PADDING : 180;
                label.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
                label.revalidate();
                label.repaint();
            }
        });
    }

    protected Color parseColor(String hexColor) {
        try {
            if (hexColor != null && hexColor.startsWith("#") && hexColor.length() == 7) {
                return Color.decode(hexColor);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse color: " + hexColor);
        }
        return COLOR_YELLOW;
    }

    // ========== Button Creation ==========

    protected InverseCornerButton createLinkButton(LinkButton linkButton) {
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

    protected void addLinkButtons(LinkButton[] linkButtons) {
        for (LinkButton linkButton : linkButtons) {
            InverseCornerButton button = createLinkButton(linkButton);
            buttons.put(linkButton.label, button);
            add(button);
            addVerticalSpace(SPACING_SMALL);
        }
    }

    protected void updateButtonUrl(String buttonLabel, String configKey, ConfigManager configManager) {
        String url = getConfigValue(configKey, "", configManager);
        if (buttons.containsKey(buttonLabel) && !url.isEmpty()) {
            buttons.get(buttonLabel).setUrl(url);
        }
    }

    // ========== Timer Utilities ==========

    protected Timer createCountdownTimer(Runnable tickHandler) {
        return new Timer(TIMER_INTERVAL, e -> tickHandler.run());
    }

    protected String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }

    protected String formatCountdownMessage(String startTimeKey, String endTimeKey, ConfigManager configManager) {
        try {
            String startRaw = configManager.getConfiguration("flux", startTimeKey);
            String endRaw = configManager.getConfiguration("flux", endTimeKey);

            if (startRaw == null || startRaw.isEmpty() || endRaw == null || endRaw.isEmpty()) {
                return "Timing unavailable.";
            }

            Instant now = Instant.now();
            Instant start = Instant.parse(startRaw);
            Instant end = Instant.parse(endRaw);

            if (now.isBefore(start)) {
                Duration untilStart = Duration.between(now, start);
                return "Starts in:<br>" + formatDuration(untilStart);
            } else if (now.isBefore(end)) {
                Duration remaining = Duration.between(now, end);
                return "Ends in:<br>" + formatDuration(remaining);
            } else {
                return "Event has ended.";
            }
        } catch (Exception ex) {
            return "Timing error.";
        }
    }

    protected boolean hasEventEnded(String endTimeKey, ConfigManager configManager) {
        try {
            String endRaw = configManager.getConfiguration("flux", endTimeKey);
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

    // ========== Configuration Helpers ==========

    protected boolean getConfigBoolean(String key, ConfigManager configManager) {
        return "true".equals(configManager.getConfiguration("flux", key));
    }

    protected String getConfigValue(String key, String defaultValue, ConfigManager configManager) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    protected int getConfigInt(String key, int defaultValue, ConfigManager configManager) {
        String value = configManager.getConfiguration("flux", key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Simplified config methods for subclasses with direct ConfigManager access.
     * Subclasses can override these to avoid passing configManager repeatedly.
     */
    protected boolean getConfigBoolean(String key) {
        throw new UnsupportedOperationException("Subclass must override getConfigBoolean(String) or use getConfigBoolean(String, ConfigManager)");
    }

    protected String getConfigValue(String key, String defaultValue) {
        throw new UnsupportedOperationException("Subclass must override getConfigValue(String, String) or use getConfigValue(String, String, ConfigManager)");
    }

    protected int getConfigInt(String key, int defaultValue) {
        throw new UnsupportedOperationException("Subclass must override getConfigInt(String, int) or use getConfigInt(String, int, ConfigManager)");
    }

    // ========== JSON Leaderboard Parsing ==========

    /**
     * Parse a JSON leaderboard with username and integer score field.
     * @param configKey The config key containing the JSON array
     * @param scoreField The name of the score field (e.g., "score", "xp")
     * @param configManager The ConfigManager instance
     * @return LinkedHashMap preserving insertion order
     */
    protected LinkedHashMap<String, Integer> parseLeaderboardJson(String configKey, String scoreField, ConfigManager configManager) {
        LinkedHashMap<String, Integer> leaderboard = new LinkedHashMap<>();
        String raw = configManager.getConfiguration("flux", configKey);

        if (raw == null || raw.isEmpty()) {
            return leaderboard;
        }

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String username = obj.getString("username");
                int score = obj.getInt(scoreField);
                leaderboard.put(username, score);
            }
        } catch (Exception e) {
            handleAsyncError(e);
            System.err.println("Failed to parse leaderboard from config key: " + configKey);
        }

        return leaderboard;
    }

    /**
     * Parse a JSON leaderboard with username and double score field.
     * @param configKey The config key containing the JSON array
     * @param scoreField The name of the score field (e.g., "ehb")
     * @param configManager The ConfigManager instance
     * @return LinkedHashMap preserving insertion order
     */
    protected LinkedHashMap<String, Double> parseLeaderboardJsonDouble(String configKey, String scoreField, ConfigManager configManager) {
        LinkedHashMap<String, Double> leaderboard = new LinkedHashMap<>();
        String raw = configManager.getConfiguration("flux", configKey);

        if (raw == null || raw.isEmpty()) {
            return leaderboard;
        }

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String username = obj.getString("username");
                double score = obj.getDouble(scoreField);
                leaderboard.put(username, score);
            }
        } catch (Exception e) {
            handleAsyncError(e);
            System.err.println("Failed to parse leaderboard from config key: " + configKey);
        }

        return leaderboard;
    }

    // ========== Lifecycle ==========

    public void refresh() {
        // Default: no-op, override in subclasses
    }

    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ========== Helper Classes ==========

    protected static class LinkButton {
        public final String label;
        public final String iconPath;
        public final String url;

        public LinkButton(String label, String iconPath, String url) {
            this.label = label;
            this.iconPath = iconPath;
            this.url = url;
        }
    }
}