package com.flux.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;

public class LeaderboardCellRenderer extends DefaultTableCellRenderer {
    private final AnimatedShinyBorder goldBorder = new AnimatedShinyBorder(new Color(212, 175, 55), 3);
    private final AnimatedShinyBorder silverBorder = new AnimatedShinyBorder(new Color(192, 192, 192), 3);
    private final AnimatedShinyBorder bronzeBorder = new AnimatedShinyBorder(new Color(205, 127, 50), 3);
    private final Border emptyBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);

    private final Color darkGray = new Color(30, 30, 30);
    private final Color darkerGray = new Color(45, 45, 45);

    private float shinePos = 0f;

    public LeaderboardCellRenderer() {
        // Timer to update shine animation every 50ms (~20 FPS)
        Timer timer = new Timer(50, e -> {
            shinePos += 0.01f; // Move shine forward
            if (shinePos > 1f)
                shinePos = 0f;

            goldBorder.setShinePosition(shinePos);
            silverBorder.setShinePosition(shinePos);
            bronzeBorder.setShinePosition(shinePos);

            // Repaint table to show shine movement
            // Ideally repaint only visible cells with border, but here we repaint all
            SwingUtilities.invokeLater(() -> {
                if (leaderboardTable != null)
                    leaderboardTable.repaint();
            });
        });
        timer.start();
    }

    private JTable leaderboardTable;

    // You need to pass the table instance to renderer, so we can repaint on timer
    public void setTable(JTable table) {
        this.leaderboardTable = table;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        leaderboardTable = table; // just in case not set yet

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        JComponent comp = (JComponent) c;

        if (!isSelected) {
            comp.setBackground((row % 2 == 0) ? darkGray : darkerGray);
        }

        // Set animated shiny border for top 3 rows
        if (row == 0)
            comp.setBorder(goldBorder);
        else if (row == 1)
            comp.setBorder(silverBorder);
        else if (row == 2)
            comp.setBorder(bronzeBorder);
        else
            comp.setBorder(emptyBorder);

        setHorizontalAlignment(column == 1 ? SwingConstants.CENTER : SwingConstants.LEFT);

        return comp;
    }
}
