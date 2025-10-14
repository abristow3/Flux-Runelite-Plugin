package com.flux.components;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class AnimatedShinyBorder extends AbstractBorder {
    private final Color baseColor;
    private final int thickness;
    private float shinePosition = 0f;

    public AnimatedShinyBorder(Color baseColor, int thickness) {
        this.baseColor = baseColor;
        this.thickness = thickness;
    }

    public void setShinePosition(float pos) {
        shinePosition = pos % 1f;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw base border
        g2.setColor(baseColor);
        for (int i = 0; i < thickness; i++) {
            g2.drawRect(x + i, y + i, width - 1 - i * 2, height - 1 - i * 2);
        }

        // Lighter color similar to base (slightly brighter, not full white)
        Color shineColor = lightenColor(baseColor, 0.5f); // 50% lighter

        g2.setColor(new Color(shineColor.getRed(), shineColor.getGreen(), shineColor.getBlue(), 180));

        int perimeter = 2 * (width + height - 4 * thickness);
        int shineLength = 8; // smaller shine length
        int shinePosPixel = (int) (shinePosition * perimeter);

        for (int i = 0; i < shineLength; i++) {
            int pos = (shinePosPixel + i) % perimeter;
            Point p = pointOnOuterPerimeter(x, y, width, height, pos);
            int size = 3; // smaller dot
            g2.fillOval(p.x - size / 2, p.y - size / 2, size, size);
        }

        g2.dispose();
    }

    private Point pointOnOuterPerimeter(int x, int y, int width, int height, int pos) {
        int w = width - 1;
        int h = height - 1;

        if (pos < w)
            return new Point(x + pos, y); // Top edge
        pos -= w;

        if (pos < h)
            return new Point(x + w, y + pos); // Right edge
        pos -= h;

        if (pos < w)
            return new Point(x + w - pos, y + h); // Bottom edge
        pos -= w;

        return new Point(x, y + h - pos); // Left edge
    }

    private Color lightenColor(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = thickness;
        return insets;
    }
}
