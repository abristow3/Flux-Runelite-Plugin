package com.flux.cards;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all Flux plugin cards.
 * Provides scrollable panel functionality and async execution utilities.
 */
public abstract class FluxCard extends JPanel implements Scrollable {
    
    // Constants
    protected static final int SCROLL_UNIT_INCREMENT = 16;
    protected static final int SCROLL_BLOCK_INCREMENT = 64;
    protected static final int SIDE_PADDING = 10;
    
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

    protected FluxCard() {
        // Don't set layout here — let subclasses decide
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
    
    /**
     * Executes a task asynchronously on a background thread.
     * @param task the task to run
     */
    protected void runAsync(Runnable task) {
        executor.submit(task);
    }

    /**
     * Executes a task asynchronously and runs a callback on the EDT when complete.
     * @param backgroundTask the task to run on background thread
     * @param edtCallback the callback to run on EDT after completion
     */
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

    /**
     * Override this method to handle async errors in subclasses.
     * Default implementation prints to stderr.
     * @param e the exception that occurred
     */
    protected void handleAsyncError(Exception e) {
        System.err.println("Async error in " + getClass().getSimpleName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    // ========== UI Helper Methods ==========
    
    /**
     * Creates a centered label with the specified properties.
     * @param text the label text
     * @param font the font (null for default)
     * @param color the foreground color
     * @return the configured label
     */
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

    /**
     * Creates an HTML-wrapped label for text wrapping and centering.
     * @param text the text content (plain text, will be wrapped in HTML)
     * @param font the font (null for default)
     * @param color the foreground color
     * @return the configured label
     */
    protected JLabel createWrappedLabel(String text, Font font, Color color) {
        String html = String.format(
            "<html><div style='text-align: center; word-wrap: break-word;'>%s</div></html>",
            text
        );
        return createCenteredLabel(html, font, color);
    }

    /**
     * Creates a section title with underline.
     * @param text the section title text
     * @return the configured label
     */
    protected JLabel createSectionTitle(String text) {
        String html = String.format("<html><u>%s</u></html>", text);
        return createCenteredLabel(html, FONT_SECTION, COLOR_YELLOW);
    }

    /**
     * Updates an HTML label's text content while preserving formatting.
     * @param label the label to update
     * @param text the new text content
     * @param underline whether to underline the text
     */
    protected void updateWrappedLabelText(JLabel label, String text, boolean underline) {
        String format = underline
            ? "<html><div style='text-align: center; word-wrap: break-word;'><u>%s</u></div></html>"
            : "<html><div style='text-align: center; word-wrap: break-word;'>%s</div></html>";
        
        label.setText(String.format(format, text));
    }

    /**
     * Adds vertical spacing to the layout.
     * @param height the height in pixels
     */
    protected void addVerticalSpace(int height) {
        add(Box.createRigidArea(new Dimension(0, height)));
    }

    /**
     * Creates a vertical strut (fixed height spacer).
     * @param height the height in pixels
     * @return the strut component
     */
    protected Component createVerticalStrut(int height) {
        return Box.createVerticalStrut(height);
    }

    // ========== Configuration Helpers ==========
    
    /**
     * Helper to get a boolean config value.
     * Subclasses should override if they need config access.
     * @param key the config key
     * @return the boolean value
     */
    protected boolean getConfigBoolean(String key) {
        throw new UnsupportedOperationException(
            "getConfigBoolean not implemented. Override in subclass if needed."
        );
    }

    /**
     * Helper to get a string config value with default.
     * Subclasses should override if they need config access.
     * @param key the config key
     * @param defaultValue the default value if not found or empty
     * @return the config value or default
     */
    protected String getConfigValue(String key, String defaultValue) {
        throw new UnsupportedOperationException(
            "getConfigValue not implemented. Override in subclass if needed."
        );
    }

    // ========== Lifecycle ==========
    
    /**
     * Called when the card should refresh its data.
     * Subclasses should override to implement refresh logic.
     */
    public void refresh() {
        // Default: no-op, override in subclasses
    }

    /**
     * Cleanup executor and resources.
     * Should be called when the plugin is shut down.
     */
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
    
    /**
     * Configuration class for link buttons.
     * Encapsulates label, icon path, and URL for button creation.
     */
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