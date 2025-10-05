package net.runelite.client.plugins.flux;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.URI;

public class InverseCornerButton extends JButton {

    private String url;
    private boolean isActive = false;

    private static final int CONCAVE_DEPTH = 10;
    private static final Color ACTIVE_COLOR = Color.decode("#811F1D");
    private static final Color DEFAULT_COLOR = Color.BLACK;

    // --- Glow effect fields ---
    private boolean glowing = false;
    private float glowAlpha = 0f;
    private boolean glowIncreasing = true;
    private Timer glowTimer;

    private static final Color GLOW_COLOR = Color.YELLOW;
    private static final int GLOW_UPDATE_DELAY = 50; // ms
    private static final float GLOW_STEP = 0.05f;

    // --- Private base constructor ---
    private InverseCornerButton(String label, String url, ImageIcon icon) {
        super(label);
        this.url = url;

        if (icon != null) {
            setIcon(icon);
            setHorizontalTextPosition(SwingConstants.RIGHT);
        }

        setupButtonStyle();
        setupListeners();
    }

    // --- Private constructor for image-only buttons (no label) ---
    private InverseCornerButton(ImageIcon icon) {
        super();
        this.url = null;

        if (icon != null) {
            setIcon(icon);
            setHorizontalTextPosition(SwingConstants.CENTER);
        }

        setupButtonStyle();
        setupListeners();
    }

    // --- Factory method: URL button with optional icon ---
    public static InverseCornerButton withUrl(String label, String url) {
        return new InverseCornerButton(label, url, null);
    }

    // --- Factory method: Label + icon (no URL) ---
    public static InverseCornerButton withImage(String label, String imagePath) {
        ImageIcon icon = loadIcon(imagePath);
        return new InverseCornerButton(label, null, icon);
    }

    // --- Factory method: Image-only button (no label or URL) ---
    public static InverseCornerButton iconOnly(String imagePath) {
        ImageIcon icon = loadIcon(imagePath);
        return new InverseCornerButton(icon);
    }

    public static InverseCornerButton withLabelImageAndUrl(String label, String imagePath, String url) {
        ImageIcon icon = loadIcon(imagePath);
        return new InverseCornerButton(label, url, icon);
    }

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

    private void setupButtonStyle() {
        setFocusPainted(false);
        setBackground(DEFAULT_COLOR);
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        setHorizontalAlignment(SwingConstants.CENTER);
        setPreferredSize(new Dimension(0, 40));
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

        addActionListener(e -> openLink(url));
    }

    public void setActive(boolean active) {
        this.isActive = active;
        setBackground(active ? ACTIVE_COLOR : DEFAULT_COLOR);
        repaint();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;

        if (glowing) {
            if (glowTimer == null) {
                glowTimer = new Timer(GLOW_UPDATE_DELAY, e -> updateGlow());
                glowTimer.start();
            } else {
                glowTimer.restart();
            }
        } else {
            if (glowTimer != null) {
                glowTimer.stop();
            }
            glowAlpha = 0f;
            repaint();
        }
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

    private void openLink(String url) {
        if (url == null || url.isEmpty()) return;

        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape clip = createConcaveShape(getWidth(), getHeight());
        g2.setClip(clip);
        g2.setColor(getBackground());
        g2.fill(clip);

        // --- Glowing Effect ---
        if (glowing) {
            g2.setClip(null); // Disable clipping for outer glow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha));
            g2.setColor(GLOW_COLOR);

            int glowSize = 5;
            Shape glowShape = createConcaveShape(getWidth(), getHeight());
            for (int i = 1; i <= glowSize; i++) {
                g2.setStroke(new BasicStroke(i));
                g2.draw(glowShape);
            }

            g2.setComposite(AlphaComposite.SrcOver); // Reset composite
        }

        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape borderShape = createConcaveShape(getWidth() - 1, getHeight() - 1);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(borderShape);
        g2.dispose();
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
}
