package net.runelite.client.plugins.flux.cards;

import net.runelite.client.plugins.flux.InverseCornerButton;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;

public class HomeCard extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private final int sotwRowIndex = 3; // SOTW row index
    private final int botmRowIndex = 2; // BOTM row index
    
    private JLabel announcementsLabel; // Add this field
    private final Font customFont;

    public HomeCard(Font customFont) {
        this.customFont = customFont != null ? customFont : new Font("SansSerif", Font.PLAIN, 12);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        // Title
        JLabel title = new JLabel("Welcome to Flux!");
        title.setFont(this.customFont.deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.YELLOW);
        add(Box.createVerticalStrut(20));
        add(title);

        // Announcements Section
        add(Box.createRigidArea(new Dimension(0, 10)));

        // announcements Title
        JLabel announcementsTitle = new JLabel("Announcements");
        announcementsTitle.setFont(this.customFont.deriveFont(Font.BOLD, 16f));
        announcementsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        announcementsTitle.setForeground(Color.YELLOW);
        add(announcementsTitle);
        add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel announcements = new JLabel("<html><div style='text-align: center;'>"
            + "🎉 <b>Don't forget to participate in this week's SOTW!</b><br>"
            + "</div></html>");
        announcements.setFont(this.customFont.deriveFont(Font.PLAIN, 12f));
        announcements.setAlignmentX(Component.CENTER_ALIGNMENT);
        announcements.setForeground(Color.LIGHT_GRAY);
        add(announcements);

        add(Box.createRigidArea(new Dimension(0, 20)));

        // Events Title
        JLabel eventsTitle = new JLabel("Events");
        eventsTitle.setFont(this.customFont.deriveFont(Font.BOLD, 16f));
        eventsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        eventsTitle.setForeground(Color.YELLOW);
        add(eventsTitle);

        add(Box.createRigidArea(new Dimension(0, 10)));

        // --- Table Setup ---
        String[] columnNames = {"Event", "Status"};
        Object[][] rowData = {
            {"Roll Call", "Ongoing"},
            {"The Hunt", "Idle"},
            {"BOTM", "Idle"},
            {"SOTW", "Idle"}
        };

        tableModel = new DefaultTableModel(rowData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setFont(this.customFont.deriveFont(Font.PLAIN, 12f));
        table.setForeground(Color.WHITE);
        table.setBackground(new Color(30, 30, 30));
        table.setGridColor(new Color(70, 70, 70));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(10, 8));

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setFont(this.customFont.deriveFont(Font.BOLD, 12f));
        header.setForeground(Color.YELLOW);
        header.setBackground(new Color(50, 50, 50));
        header.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Center-align table data content
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusColorRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        int rowHeight = table.getRowHeight();
        int headerHeight = table.getTableHeader().getPreferredSize().height;
        int rowCount = table.getRowCount();
        int totalHeight = headerHeight + (rowHeight * rowCount) + 1;

        scrollPane.setPreferredSize(new Dimension(400, totalHeight));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        add(scrollPane);

        add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Links Title
        JLabel linksTitle = new JLabel("Links");
        linksTitle.setFont(this.customFont.deriveFont(Font.BOLD, 16f));
        linksTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        linksTitle.setForeground(Color.YELLOW);
        add(linksTitle);

        // spacing
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Buttons
        String[][] buttonData = {
            {"Flux Clan Server", "https://discord.gg/fluxosrs", "discord.png"},
            {"Flux Hunt Server", "https://discord.gg/thehunt", "discord.png"},
            {"Roll Call", "https://discord.gg/fluxosrs/roll-call", "discord.png"},
            {"Announcements", "https://discord.gg/fluxosrs/announcements", "discord.png"},
            {"Events", "https://discord.gg/fluxosrs/events", "discord.png"},
            {"Wise Old Man", "https://wiseoldman.net/groups/141", "wom.png"}
        };

        for (String[] data : buttonData) {
            String label = data[0];
            String url = data[1];
            String iconPath = data[2];
            InverseCornerButton button = InverseCornerButton.withLabelImageAndUrl(label, iconPath, url);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            // 🔧 Force consistent sizing
            Dimension size = new Dimension(200, 25);
            button.setPreferredSize(size);
            button.setMaximumSize(size);
            button.setMinimumSize(size);
            button.setFont(this.customFont.deriveFont(Font.PLAIN, 14f));
            add(button);
            add(Box.createRigidArea(new Dimension(0, 10)));
        }

        add(Box.createVerticalGlue());
    }

    /**
     * Update the SOTW status dynamically.
     */
    public void updateSotwStatus(boolean isActive) {
        if (tableModel != null && sotwRowIndex >= 0) {
            String newStatus = isActive ? "Ongoing" : "Idle";
            tableModel.setValueAt(newStatus, sotwRowIndex, 1);
            table.repaint();
        }
    }

    /**
     * Update the BOTM status dynamically.
     */
    public void updateBotmStatus(boolean isActive) {
        if (tableModel != null && botmRowIndex >= 0) {
            String newStatus = isActive ? "Ongoing" : "Idle";
            tableModel.setValueAt(newStatus, botmRowIndex, 1);
            table.repaint();
        }
    }

    /**
     * Custom renderer to color the status cell.
     */
    private static class StatusColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            String status = value.toString();
            if (column == 1) {
                if ("Ongoing".equalsIgnoreCase(status)) {
                    c.setForeground(Color.GREEN);
                } else {
                    c.setForeground(Color.RED);
                }
            } else {
                c.setForeground(Color.WHITE);
            }

            setBackground(new Color(30, 30, 30)); // Ensure dark background
            return c;
        }
    }

    // Optional: Default fallback message
    private String getDefaultAnnouncementHtml() {
        return "<html><div style='text-align: center;'>🎉 <b>Announcement:</b> Stay tuned for updates!</div></html>";
    }

    // NEW: Allow updating the announcement text dynamically
    public void setAnnouncementText(String rawText) {
        if (announcementsLabel != null) {
            String html = "<html><div style='text-align: center;'>" + rawText.replaceAll("\n", "<br>") + "</div></html>";
            announcementsLabel.setText(html);
            revalidate();
            repaint();
        }
    }
}
