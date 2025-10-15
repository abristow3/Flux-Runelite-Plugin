package com.flux.ui;

import com.flux.components.combobox.ComboBoxIconEntry;
import com.flux.components.combobox.ComboBoxIconListRenderer;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
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
     */
    public void syncWithButtonLabel(String buttonLabel) {
        for (int i = 0; i < dropdown.getItemCount(); i++) {
            ComboBoxIconEntry item = dropdown.getItemAt(i);
            if (item.getText().trim().equals(buttonLabel) && dropdown.getSelectedIndex() != i) {
                dropdown.setSelectedIndex(i);
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
        dropdown.addActionListener(e -> handleSelection());
    }

    private void handleSelection() {
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