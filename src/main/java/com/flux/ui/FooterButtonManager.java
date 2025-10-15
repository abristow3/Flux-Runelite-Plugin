package com.flux.ui;

import com.flux.components.InverseCornerButton;
import com.flux.constants.EntrySelect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages footer navigation buttons and their layout.
 */
public class FooterButtonManager {
    private final JPanel footerPanel;
    private final List<InverseCornerButton> buttons = new ArrayList<>();
    private InverseCornerButton activeButton;

    public FooterButtonManager(JPanel footerPanel) {
        this.footerPanel = footerPanel;
    }

    /**
     * Creates and adds a footer button for the given card entry.
     */
    public InverseCornerButton addButton(String label, String iconPath,
                                         EntrySelect entry, ActionListener clickListener) {
        InverseCornerButton button = InverseCornerButton.withImage(label, iconPath);
        button.addActionListener(e -> {
            activateButton(button);
            clickListener.actionPerformed(e);
        });
        buttons.add(button);
        return button;
    }

    /**
     * Rebuilds the footer panel with all current buttons.
     */
    public void rebuildFooter() {
        footerPanel.removeAll();
        footerPanel.setLayout(new GridBagLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        GridBagConstraints gbc = createConstraints();

        for (int i = 0; i < buttons.size(); i++) {
            configureButtonPosition(gbc, i);
            footerPanel.add(buttons.get(i), gbc);
        }

        footerPanel.revalidate();
        footerPanel.repaint();

        activateFirstButton();
    }

    /**
     * Activates a specific button by its label text.
     */
    public void activateButtonByLabel(String label) {
        buttons.stream()
                .filter(btn -> btn.getText().trim().equals(label))
                .findFirst()
                .ifPresent(this::activateButton);
    }

    /**
     * Returns the label of the currently active button.
     */
    public String getActiveButtonLabel() {
        return activeButton != null ? activeButton.getText().trim() : "";
    }

    /**
     * Returns all footer buttons.
     */
    public List<InverseCornerButton> getButtons() {
        return buttons;
    }

    private void activateButton(InverseCornerButton button) {
        if (activeButton != null) {
            activeButton.setActive(false);
        }

        activeButton = button;
        activeButton.setActive(true);
    }

    private void activateFirstButton() {
        if (!buttons.isEmpty()) {
            buttons.get(0).doClick();
        }
    }

    private GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        return gbc;
    }

    private void configureButtonPosition(GridBagConstraints gbc, int index) {
        gbc.gridx = index % 2;
        gbc.gridy = index / 2;
        gbc.gridwidth = 1;

        // Last button in odd count spans both columns
        if (index == buttons.size() - 1 && buttons.size() % 2 != 0) {
            gbc.gridx = 0;
            gbc.gridwidth = 2;
        }
    }
}