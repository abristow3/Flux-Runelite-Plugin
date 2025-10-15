package com.flux.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.net.URI;

/**
 * A custom button with concave corners and optional glow effect.
 * Supports optional icon, label, and URL clicking.
 */
public class InverseCornerButton extends JButton {
    // Style constants
    private static final int CONCAVE_DEPTH = 10;
    private static final Color ACTIVE_COLOR = Color.decode("#811F1D");
    private static final Color DEFAULT_COLOR = Color.BLACK;
    private static final Color GLOW_COLOR = Color.YELLOW;
    private static final Color BORDER_COLOR = Color.WHITE;
    
    // Glow animation constants
    private static final int GLOW_UPDATE_DELAY_MS = 50;
    private static final float GLOW_STEP = 0.05f;
    private static final int GLOW_SIZE = 5;
    
    // Border constants
    private static final float BORDER_STROKE_WIDTH = 1.5f;
    
    // Default size
    private static final int DEFAULT_HEIGHT = 40;
    
    // State
    private String url;
    private boolean isActive = false;
    private boolean glowing = false;
    private float glowAlpha = 0f;
    private boolean glowIncreasing = true;
    private Timer glowTimer;

    // Private constructor - use factory methods
    private InverseCornerButton(String label, String url, ImageIcon icon) {
        super(label);
        this.url = url;

        if (icon != null) {
            setIcon(icon);
            setHorizontalTextPosition(label != null && !label.isEmpty() 
                ? SwingConstants.RIGHT 
                : SwingConstants.CENTER);
        }

        setupButtonStyle();
        setupListeners();
    }

    // ========== Factory Methods ==========

    /**
     * Creates a button with only a label and URL.
     */
    public static InverseCornerButton withUrl(String label, String url) {
        return new InverseCornerButton(label, url, null);
    }

    /**
     * Creates a button with label and icon (no URL).
     */
    public static InverseCornerButton withImage(String label, String imagePath) {
        ImageIcon icon = loadIcon(imagePath);
        return new InverseCornerButton(label, null, icon);
    }

    /**
     * Creates a button with icon only (no label or URL).
     */
    public static InverseCornerButton iconOnly(String imagePath) {
        ImageIcon icon = loadIcon(imagePath);
        return new InverseCornerButton(null, null, icon);
    }

    /**
     * Creates a button with label, icon, and URL.
     */
    public static InverseCornerButton withLabelImageAndUrl(String label, String imagePath, String url) {
        ImageIcon icon = loadIcon(imagePath);
        return new InverseCornerButton(label, url, icon);
    }

    // ========== Setup Methods ==========

    private void setupButtonStyle() {
        setFocusPainted(false);
        setBackground(DEFAULT_COLOR);
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        setHorizontalAlignment(SwingConstants.CENTER);
        setPreferredSize(new Dimension(0, DEFAULT_HEIGHT));
        setContentAreaFilled(false);
    }

    private void setupListeners() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!isActive) {
                    setBackground(ACTIVE_COLOR);
                    repaint();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!isActive) {
                    setBackground(DEFAULT_COLOR);
                    repaint();
                }
            }
        });

        addActionListener(e -> openLink());
    }

    // ========== Public API ==========

    /**
     * Sets the active state of the button.
     * Active buttons remain highlighted.
     */
    public void setActive(boolean active) {
        this.isActive = active;
        setBackground(active ? ACTIVE_COLOR : DEFAULT_COLOR);
        repaint();
    }

    /**
     * Returns whether the button is currently active.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets the URL to open when clicked.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the current URL.
     */
    public String getUrl() {
        return url;
    }

     /**
     * Enables or disables the glow effect animation.
     */
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;

        if (glowing) {
            startGlowAnimation();
        } else {
            stopGlowAnimation();
        }

        // Always repaint to ensure visual updates
        repaint();
    }

    /**
     * Returns whether the button is currently glowing.
     */
    public boolean isGlowing() {
        return glowing;
    }

    // ========== Glow Animation ==========

    private void startGlowAnimation() {
        if (glowTimer == null) {
            glowTimer = new Timer(GLOW_UPDATE_DELAY_MS, e -> updateGlow());
        }
        
        if (!glowTimer.isRunning()) {
            glowTimer.start();
        }
    }

    private void stopGlowAnimation() {
        if (glowTimer != null && glowTimer.isRunning()) {
            glowTimer.stop();
        }
        glowAlpha = 0f;
        glowIncreasing = true;
        repaint();
    }

    private void updateGlow() {
        if (glowIncreasing) {
            glowAlpha += GLOW_STEP;
            if (glowAlpha >= 1f) {
                glowAlpha = 1f;
                glowIncreasing = false;
            }
        } else {
            glowAlpha -= GLOW_STEP;
            if (glowAlpha <= 0f) {
                glowAlpha = 0f;
                glowIncreasing = true;
            }
        }
        repaint();
    }

    // ========== URL Handling ==========

    private void openLink() {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            } else {
                System.err.println("Desktop browsing not supported");
            }
        } catch (Exception e) {
            System.err.println("Error opening URL: " + url);
            e.printStackTrace();
        }
    }

    // ========== Painting ==========

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Paint background
            paintBackground(g2);

            // Paint glow effect if active
            if (glowing && glowAlpha > 0) {
                paintGlow(g2);
            }
        } finally {
            g2.dispose();
        }

        // Paint text/icon on top
        super.paintComponent(g);
    }

    private void paintBackground(Graphics2D g2) {
        g2.setColor(getBackground());
        Shape shape = createConcaveShape(getWidth(), getHeight());
        g2.fill(shape);
    }

    private void paintGlow(Graphics2D g2) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha));
        g2.setColor(GLOW_COLOR);
        
        Shape glowShape = createConcaveShape(getWidth(), getHeight());
        
        for (int i = 1; i <= GLOW_SIZE; i++) {
            g2.setStroke(new BasicStroke(i));
            g2.draw(glowShape);
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Shape borderShape = createConcaveShape(getWidth() - 1, getHeight() - 1);
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(BORDER_STROKE_WIDTH));
            g2.draw(borderShape);
        } finally {
            g2.dispose();
        }
    }

    private Shape createConcaveShape(int width, int height) {
        Path2D.Float path = new Path2D.Float();
        int d = CONCAVE_DEPTH;

        path.moveTo(d, 0);
        path.lineTo(width - d, 0);
        path.lineTo(width, d);
        path.lineTo(width, height - d);
        path.lineTo(width - d, height);
        path.lineTo(d, height);
        path.lineTo(0, height - d);
        path.lineTo(0, d);
        path.closePath();

        return path;
    }

    // ========== Icon Loading ==========

    private static ImageIcon loadIcon(String path) {
        if (path == null || path.isEmpty()) {
            System.err.println("Image path is null or empty");
            return null;
        }

        java.net.URL imgURL = InverseCornerButton.class.getResource(path);
        if (imgURL == null) {
            System.err.println("Could not find image resource: " + path);
            return null;
        }

        return new ImageIcon(imgURL);
    }

    // ========== Cleanup ==========

    /**
     * Cleanup method to stop animations when button is removed.
     * Call this when removing the button from the UI.
     */
    public void cleanup() {
        stopGlowAnimation();
        if (glowTimer != null) {
            glowTimer = null;
        }
    }
}