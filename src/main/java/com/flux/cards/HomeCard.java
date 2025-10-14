package com.flux.cards;

import net.runelite.client.config.ConfigManager;
import com.flux.FluxConfig;
import com.flux.InverseCornerButton;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class HomeCard extends FluxCard {
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 14);
    
    private static final Color TABLE_BG = new Color(30, 30, 30);
    private static final Color HEADER_BG = new Color(50, 50, 50);
    private static final Color GRID_COLOR = new Color(70, 70, 70);
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 25;
    private static final int SPACING_SMALL = 10;
    private static final int SPACING_MEDIUM = 20;
    
    // Event row indices
    private static final int BOTM_ROW = 0;
    private static final int SOTW_ROW = 1;
    private static final int HUNT_ROW = 2;
    
    private final FluxConfig config;
    private final ConfigManager configManager;
    private final Map<String, InverseCornerButton> buttons = new HashMap<>();
    
    private DefaultTableModel tableModel;
    private JLabel announcementsLabel;

    public HomeCard(FluxConfig config, ConfigManager configManager) {
        super();
        this.config = config;
        this.configManager = configManager;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        refreshAllStatuses();
    }

    private void buildUI() {
        add(createVerticalStrut(SPACING_MEDIUM));
        addTitle();
        addVerticalSpace(SPACING_MEDIUM);
        addAnnouncementsSection();
        addVerticalSpace(SPACING_MEDIUM);
        addEventsSection();
        addVerticalSpace(SPACING_MEDIUM);
        addLinksSection();
        add(Box.createVerticalGlue());
    }

    private void addTitle() {
        JLabel title = createCenteredLabel("Welcome to Flux!", FONT_TITLE, COLOR_YELLOW);
        add(title);
    }

    private void addAnnouncementsSection() {
        add(createSectionTitle("Announcements"));
        addVerticalSpace(SPACING_SMALL);
        
        String announcement = configManager.getConfiguration("flux", "plugin_announcement_message");
        announcementsLabel = createWrappedLabel(
            announcement != null ? announcement : "No Announcements.",
            null,
            COLOR_LIGHT_GRAY
        );
        add(announcementsLabel);
    }

    private void addEventsSection() {
        add(createSectionTitle("Events"));
        addVerticalSpace(SPACING_SMALL);
        
        JScrollPane eventsTable = createEventsTable();
        add(eventsTable);
    }

    private void addLinksSection() {
        add(createSectionTitle("Links"));
        addVerticalSpace(SPACING_SMALL);
        
        addLinkButtons();
    }

    private JScrollPane createEventsTable() {
        String[] columnNames = {"Event", "Status"};
        Object[][] rowData = {
            {"BOTM", "Idle"},
            {"SOTW", "Idle"},
            {"The Hunt", "Idle"}
        };

        tableModel = new DefaultTableModel(rowData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        configureTable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        configureScrollPane(scrollPane, table);
        
        return scrollPane;
    }

    private void configureTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setForeground(COLOR_WHITE);
        table.setBackground(TABLE_BG);
        table.setGridColor(GRID_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(10, 8));

        configureTableHeader(table.getTableHeader());
        configureTableRenderers(table);
    }

    private void configureTableHeader(JTableHeader header) {
        header.setFont(HEADER_FONT);
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setForeground(COLOR_YELLOW);
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    }

    private void configureTableRenderers(JTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusColorRenderer());
    }

    private void configureScrollPane(JScrollPane scrollPane, JTable table) {
        int totalHeight = calculateTableHeight(table, scrollPane);
        
        scrollPane.setPreferredSize(new Dimension(400, totalHeight));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRID_COLOR, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
    }

    private int calculateTableHeight(JTable table, JScrollPane scrollPane) {
        int rowHeight = table.getRowHeight();
        int intercellSpacing = table.getIntercellSpacing().height;
        int headerHeight = table.getTableHeader().getPreferredSize().height;
        int rowCount = table.getRowCount();
        
        int totalHeight = headerHeight + (rowHeight + intercellSpacing) * rowCount;
        totalHeight += scrollPane.getInsets().top + scrollPane.getInsets().bottom + 2;
        
        return totalHeight;
    }

    private void addLinkButtons() {
        LinkButton[] linkButtons = {
            new LinkButton("Flux Clan Server", "/discord.png", "https://discord.gg/Sr4r6wXy"),
            new LinkButton("Roll Call", "/discord.png", "https://discord.com/channels/414435426007384075/636902420403847168"),
            new LinkButton("Name Changes", "/discord.png", "https://discord.com/channels/414435426007384075/415499145017557032"),
            new LinkButton("Announcements", "/discord.png", "https://discord.com/channels/414435426007384075/1349697176183246868"),
            new LinkButton("Events", "/discord.png", "https://discord.com/channels/414435426007384075/414458243499425792"),
            new LinkButton("Wise Old Man", "/wom.png", "https://wiseoldman.net/groups/141")
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

    // Public API for status updates
    public void updateSotwStatus(boolean isActive) {
        updateEventStatus(SOTW_ROW, isActive);
    }

    public void updateBotmStatus(boolean isActive) {
        updateEventStatus(BOTM_ROW, isActive);
    }

    public void updateHuntStatus(boolean isActive) {
        updateEventStatus(HUNT_ROW, isActive);
    }

    private void updateEventStatus(int rowIndex, boolean isActive) {
        if (tableModel != null && rowIndex >= 0 && rowIndex < tableModel.getRowCount()) {
            String status = isActive ? "Ongoing" : "Idle";
            tableModel.setValueAt(status, rowIndex, 1);
        }
    }

    // Refresh methods
    public void refreshPluginAnnouncement() {
        String announcement = configManager.getConfiguration("flux", "plugin_announcement_message");
        announcementsLabel.setText(announcement);
    }

    public void refreshButtonLinks() {
        String womLink = configManager.getConfiguration("flux", "wom_url");
        InverseCornerButton womButton = buttons.get("Wise Old Man");
        
        if (womButton != null && womLink != null && !womLink.isEmpty()) {
            womButton.setUrl(womLink);
        }
    }

    public void refreshHuntStatus() {
        boolean isActive = getConfigBoolean("HuntActive");
        updateHuntStatus(isActive);
    }

    public void refreshSotwStatus() {
        boolean isActive = getConfigBoolean("sotwActive");
        updateSotwStatus(isActive);
    }

    public void refreshBotmStatus() {
        boolean isActive = getConfigBoolean("botmActive");
        updateBotmStatus(isActive);
    }

    public void isRollCallActive() {
        boolean active = getConfigBoolean("rollCallActive");
        InverseCornerButton rollCallButton = buttons.get("Roll Call");
        
        if (rollCallButton != null) {
            rollCallButton.setGlowing(active);
            if (!active) {
                rollCallButton.repaint();
            }
        }
    }

    @Override
    public void refresh() {
        refreshAllStatuses();
    }

    private void refreshAllStatuses() {
        refreshSotwStatus();
        refreshBotmStatus();
        refreshHuntStatus();
        isRollCallActive();
    }

    @Override
    protected boolean getConfigBoolean(String key) {
        String value = configManager.getConfiguration("flux", key);
        return Boolean.parseBoolean(value);
    }

    @Override
    protected String getConfigValue(String key, String defaultValue) {
        String value = configManager.getConfiguration("flux", key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    // Helper classes
    private static class StatusColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            );

            if (column == 1) {
                String status = value.toString();
                c.setForeground("Ongoing".equalsIgnoreCase(status) ? COLOR_GREEN : COLOR_RED);
            } else {
                c.setForeground(COLOR_WHITE);
            }

            setBackground(TABLE_BG);
            return c;
        }
    }
}