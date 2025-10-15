package com.flux.ui;

import com.flux.components.combobox.ComboBoxIconEntry;
import com.flux.components.combobox.ComboBoxIconListRenderer;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Manages the header dropdown navigation component.
 */
public class HeaderDropdownManager {
    private final JPanel headerPanel;
    private final JComboBox<ComboBoxIconEntry> dropdown;
    private final ComboBoxIconListRenderer renderer;
    private Consumer<String> onSelectionChanged;
    private ActionListener selectionListener;
    private boolean isUpdating = false; // Flag to prevent recursive updates

    public HeaderDropdownManager(JPanel headerPanel) {
        this.headerPanel = headerPanel;
        this.dropdown = new JComboBox<>();
        this.renderer = new ComboBoxIconListRenderer();
        configureDropdown();
    }

    /**
     * Builds the header panel with the dropdown.
     */
    public void buildHeader() {
        headerPanel.removeAll();
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        headerPanel.add(dropdown);
        headerPanel.revalidate();
        headerPanel.repaint();
    }

    /**
     * Adds an entry to the dropdown.
     */
    public void addEntry(String label, String iconPath, String cardId, Class<?> resourceClass) {
        dropdown.addItem(new ComboBoxIconEntry(
                new ImageIcon(ImageUtil.loadImageResource(resourceClass, iconPath)),
                label,
                Optional.of(cardId)
        ));
    }

    /**
     * Sets the selection changed callback.
     */
    public void setOnSelectionChanged(Consumer<String> callback) {
        this.onSelectionChanged = callback;
    }

    /**
     * Selects the first item in the dropdown.
     */
    public void selectFirst() {
        if (dropdown.getItemCount() > 0) {
            dropdown.setSelectedIndex(0);
        }
    }

    /**
     * Synchronizes dropdown selection with the given button label.
     * Does not trigger the selection callback to avoid circular updates.
     */
    public void syncWithButtonLabel(String buttonLabel) {
        if (isUpdating) {
            return; // Prevent recursive calls
        }

        for (int i = 0; i < dropdown.getItemCount(); i++) {
            ComboBoxIconEntry item = dropdown.getItemAt(i);
            if (item.getText().trim().equals(buttonLabel.trim()) && dropdown.getSelectedIndex() != i) {
                isUpdating = true;
                try {
                    dropdown.setSelectedIndex(i);
                } finally {
                    isUpdating = false;
                }
                break;
            }
        }
    }

    /**
     * Forces a repaint of the dropdown.
     */
    public void repaint() {
        dropdown.repaint();
    }

    private void configureDropdown() {
        dropdown.setFocusable(false);
        dropdown.setForeground(Color.WHITE);
        dropdown.setRenderer(renderer);

        selectionListener = e -> handleSelection();
        dropdown.addActionListener(selectionListener);
    }

    private void handleSelection() {
        if (isUpdating) {
            return; // Don't trigger callback during programmatic updates
        }

        ComboBoxIconEntry selectedItem = (ComboBoxIconEntry) dropdown.getSelectedItem();
        if (selectedItem == null || !selectedItem.getId().isPresent()) {
            return;
        }

        String cardId = selectedItem.getId().get();
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(cardId);
        }
    }
}