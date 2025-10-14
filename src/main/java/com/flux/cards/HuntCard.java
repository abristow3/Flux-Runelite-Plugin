package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.InverseCornerButton;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class HuntCard extends FluxCard {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 25;
    private static final int SPACING_SMALL = 10;
    private static final int SPACING_MEDIUM = 20;
    private static final int CONTENT_PADDING = 40;
    
    private static final int TIMER_INTERVAL = 1000; // 1 second
    
    private final ConfigManager configManager;
    private final Map<String, InverseCornerButton> buttons = new HashMap<>();
    
    private JLabel eventTitle;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private boolean wasEventActive = false;

    public HuntCard(ConfigManager configManager) {
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
        addButtons();
    }

    private void addTitle() {
        JLabel title = createCenteredLabel("The Hunt", FONT_TITLE, COLOR_YELLOW);
        add(title);
    }

    private void addEventTitle() {
        String titleText = getConfigValue("huntTitle", "No active hunt event");
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

    private void addButtons() {
        LinkButton[] linkButtons = {
            new LinkButton("Hunt Signup", "/discord.png", "https://discord.com"),
            new LinkButton("The Hunt GDoc", "/hunt.png", "https://discord.com")
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
        label.setHorizontalAlignment(SwingConstants.CENTER);
        
        return label;
    }

    private void setupDynamicResize(JLabel label) {
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

    // Public API
    public boolean isEventActive() {
        return getConfigBoolean("huntActive");
    }

    public void updateEventTitle() {
        String titleText = getConfigValue("huntTitle", "No active hunt event");
        updateWrappedLabelText(eventTitle, titleText, true);
        
        int maxWidth = getWidth() > CONTENT_PADDING ? getWidth() - CONTENT_PADDING : 180;
        eventTitle.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
    }

    public void refreshButtonLinks() {
        // Placeholder for future enhancement to pull URLs from config
        // Currently buttons are hardcoded
    }

    public void checkEventStateChanged() {
        updateCountdownLabel();
        updateEventTitle();
    }

    @Override
    public void refresh() {
        checkEventStateChanged();
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
    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        wasEventActive = isEventActive();
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
            String endRaw = configManager.getConfiguration("flux", "hunt_end_time");
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
            updateWrappedLabelText(countdownLabel, "No active Hunt event.", false);
            return;
        }

        try {
            String startRaw = configManager.getConfiguration("flux", "hunt_start_time");
            String endRaw = configManager.getConfiguration("flux", "hunt_end_time");

            if (startRaw == null || startRaw.isEmpty() || endRaw == null || endRaw.isEmpty()) {
                updateWrappedLabelText(countdownLabel, "Hunt timing unavailable.", false);
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
                updateWrappedLabelText(countdownLabel, "Hunt has ended.", false);
            }
        } catch (Exception ex) {
            updateWrappedLabelText(countdownLabel, "Hunt timing error.", false);
        }
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