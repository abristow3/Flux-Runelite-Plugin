/*
 * Copyright (c) 2022, cmsu224 <https://github.com/cmsu224>
 * Copyright (c) 2022, Brianmm94 <https://github.com/Brianmm94>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.flux;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import net.runelite.client.plugins.flux.components.combobox.ComboBoxIconEntry;
import net.runelite.client.plugins.flux.components.combobox.ComboBoxIconListRenderer;
import net.runelite.client.plugins.flux.constants.EntrySelect;
import net.runelite.client.plugins.flux.InverseCornerButton;
import net.runelite.client.plugins.flux.cards.HomeCard;
import net.runelite.client.plugins.flux.cards.SotwCard;
import net.runelite.client.plugins.flux.cards.AdminHubCard;
import net.runelite.client.plugins.flux.cards.BotmCard;
import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Timer;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
class FluxPanel extends PluginPanel {
    private final JPanel headerPanel = new JPanel();
    private final Service service = new Service();
    private final GoogleSheet sheet = new GoogleSheet();
    private final JPanel footerPanel = new JPanel();

    private Boolean openedTab = false;
    final JComboBox<ComboBoxIconEntry> dropdown = new JComboBox<>();
    final ComboBoxIconListRenderer renderer = new ComboBoxIconListRenderer();
    private final Timer timer;
    private final ActionListener timertask = event -> {
        if (!openedTab) {
            service.refreshData();
        } else {
            openedTab = false;
        }
    };

    private final Color activeColor = Color.decode("#811F1D");
    private final Color defaultFooterButtonColor = UIManager.getColor("Button.background");
    private final List<InverseCornerButton> footerButtons = new ArrayList<>();
    private InverseCornerButton activeFooterButton = null;

    private JPanel centerPanel;
    private CardLayout cardLayout;
    private boolean isAdmiralOrHigher = false;
    private FluxConfig config;
    private SotwCard sotwCard; // Reference to track checkbox state
    private BotmCard botmCard;
    private HomeCard homeCard; // Reference to track checkbox state

    private final Timer glowTimer = new Timer(500, e -> checkEventGlows());
    private boolean adminHubInitialized = false;

    private final Font runescapeFont;

    public FluxPanel(Font runescapeFont) {
        super(false);
        this.runescapeFont = runescapeFont;
        timer = new Timer(0, timertask);
        glowTimer.start();
    }

    public void init(FluxConfig config) {
        this.config = config;
        // Timer setup
        if (config.autoRefresh()) {
            timer.setRepeats(true);
            timer.setDelay(config.refreshPeriod() * 1000 * 60);
        } else {
            timer.setRepeats(false);
        }

        setLayout(new BorderLayout());

        // ----- CENTER PANEL WITH CARDLAYOUT -----
        setupCenter();
        add(centerPanel, BorderLayout.CENTER);

        // // ----- HEADER PANEL -----
        setupHeader(config);
        add(headerPanel, BorderLayout.NORTH);

        // ----- FOOTER PANEL -----
        setupFooter();
        add(footerPanel, BorderLayout.SOUTH);

        // Now safely select first item
        if (dropdown.getItemCount() > 0) {
            dropdown.setSelectedIndex(0);
        }
    }

    public void setupHeader(FluxConfig config) {
        footerButtons.clear();
        dropdown.removeAllItems();

        headerPanel.removeAll();
        headerPanel.setPreferredSize(new Dimension(0, 50)); // Fixed height
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // Simple horizontal layout

        // Setup dropdown
        dropdown.setFocusable(false);
        dropdown.setForeground(Color.WHITE);
        dropdown.setRenderer(renderer);

        // Populate dropdown and footer buttons
        setEntry(config.entry_1());
        setEntry(config.entry_3());
        setEntry(config.entry_4());
        setEntry(config.entry_5());
        setEntry(config.entry_6());
        setEntry(config.entry_7());
        setEntry(config.entry_8());

        dropdown.addActionListener(e -> {
            ComboBoxIconEntry selectedItem = (ComboBoxIconEntry) dropdown.getSelectedItem();
            if (selectedItem == null) {
                return;
            }

            String cardId = selectedItem.getId().orElse(null);
            if (cardId == null) {
                return;
            }

            // Activate footer button by label matching
            String selectedText = selectedItem.getText().trim();
            for (InverseCornerButton button : footerButtons) {
                if (button.getText().trim().equals(selectedText)) {
                    activateFooterButton(button);
                    break;
                }
            }

            // Show card using enum name key
            cardLayout.show(centerPanel, cardId);
        });

        // Add components directly to headerPanel
        headerPanel.add(dropdown);

        // Force update (optional)
        headerPanel.revalidate();
        headerPanel.repaint();
    }

    public void setupCenter() {
        if (centerPanel == null) {
            centerPanel = new JPanel(new CardLayout());
            cardLayout = (CardLayout) centerPanel.getLayout();
            centerPanel.setOpaque(false);
        }
    }

    public void setupFooter() {
        footerPanel.removeAll(); // Clear previous content
        footerPanel.setPreferredSize(new Dimension(0, 200));
        footerPanel.setLayout(new GridBagLayout()); // <-- Use GridBagLayout
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // padding
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        for (int i = 0; i < footerButtons.size(); i++) {
            InverseCornerButton button = footerButtons.get(i);

            gbc.gridx = i % 2; // Column index: 0 or 1
            gbc.gridy = i / 2; // Row index
            gbc.gridwidth = 1; // Default span is 1

            // If it's the last button and total count is odd, make it span both columns
            if (i == footerButtons.size() - 1 && footerButtons.size() % 2 != 0) {
                gbc.gridx = 0;
                gbc.gridwidth = 2; // Span both columns
            }

            footerPanel.add(button, gbc);
        }

        footerPanel.revalidate();
        footerPanel.repaint();

        if (!footerButtons.isEmpty()) {
            footerButtons.get(0).doClick(); // Trigger first button
        }
    }

    public void setEntry(EntrySelect entry) {
        String label;
        String iconPath;
        JPanel card;
        InverseCornerButton button;

        switch (entry) {
            case HOME:
                label = " Home";
                iconPath = "home.png";
                homeCard = new HomeCard(runescapeFont.deriveFont(Font.PLAIN, 14f));
                card = homeCard;
                button = InverseCornerButton.withImage(label, iconPath);
                break;

            case SOTW:
                label = " SOTW";
                iconPath = "sotw.png";
                sotwCard = new SotwCard(); // Save reference to SotwCard
                card = sotwCard;
                button = InverseCornerButton.withImage(label, iconPath);

                sotwCard.addPropertyChangeListener(evt -> {
                    if ("eventActive".equals(evt.getPropertyName())) {
                        boolean active = (boolean) evt.getNewValue();
                        setButtonGlowByLabel(label, active);
                        if (homeCard != null) {
                            homeCard.updateSotwStatus(active); // Update SOTW status in HomeCard table
                        }
                    }
                });
                break;

            case BOTM:
                label = " BOTM";
                iconPath = "botm.png";
                botmCard = new BotmCard(); // Save reference to BotmCard
                card = botmCard;
                button = InverseCornerButton.withImage(label, iconPath);

                botmCard.addPropertyChangeListener(evt -> {
                    if ("eventActive".equals(evt.getPropertyName())) {
                        boolean active = (boolean) evt.getNewValue();
                        setButtonGlowByLabel(label, active);
                        if (homeCard != null) {
                            homeCard.updateBotmStatus(active); // Update BOTM status in HomeCard table
                        }
                    }
                });
                break;

            case HUB:
                label = " Admin Hub";
                iconPath = "hub.png";
                AdminHubCard adminHubCard = new AdminHubCard();
                card = adminHubCard;
                button = InverseCornerButton.withImage(label, iconPath);
                break;

            default:
                return; // Skip unknown entries
        }

        // Dropdown item
        dropdown.addItem(new ComboBoxIconEntry(
                new ImageIcon(ImageUtil.loadImageResource(getClass(), iconPath)),
                label,
                Optional.of(entry.name().toLowerCase())));

        // Footer button logic
        final InverseCornerButton currentButton = button;
        currentButton.addActionListener(e -> {
            activateFooterButton(currentButton);
            cardLayout.show(centerPanel, entry.name().toLowerCase());
        });

        footerButtons.add(currentButton);

        // Add card to center panel
        if (cardLayout != null && centerPanel != null) {
            centerPanel.add(card, entry.name().toLowerCase());
        }
    }

    public void updateClanRankStatus(boolean isAdmiralOrHigher) {
        if (this.isAdmiralOrHigher == isAdmiralOrHigher && adminHubInitialized) {
            return; // No change, and hub already added — skip
        }

        this.isAdmiralOrHigher = isAdmiralOrHigher;
        // adminHubInitialized = true;
        if (isAdmiralOrHigher && !adminHubInitialized) {
            addAdminHubEntry();
            adminHubInitialized = true;
        }
    }

    private void addAdminHubEntry() {
        setEntry(config.entry_2());
        setupFooter();
        footerPanel.repaint();
        dropdown.revalidate();
        dropdown.repaint();
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void activateFooterButton(InverseCornerButton button) {
        if (activeFooterButton != null) {
            activeFooterButton.setActive(false);
        }
        activeFooterButton = button;
        activeFooterButton.setActive(true);

        String buttonLabel = button.getText().trim();

        for (int i = 0; i < dropdown.getItemCount(); i++) {
            ComboBoxIconEntry item = dropdown.getItemAt(i);
            if (item.getText().trim().equals(buttonLabel)) {
                // Update dropdown selection only if different
                if (dropdown.getSelectedIndex() != i) {
                    dropdown.setSelectedIndex(i);
                }
                break;
            }
        }
    }

    private void setButtonGlowByLabel(String label, boolean glow) {
        for (InverseCornerButton button : footerButtons) {
            if (button.getText().trim().equalsIgnoreCase(label.trim())) {
                button.setGlowing(glow);
                break;
            }
        }
    }

    private void checkEventGlows() {
        if (sotwCard != null) {
            boolean active = sotwCard.isEventActive();
            setButtonGlowByLabel(" SOTW", active);
        }

        if (botmCard != null) {
            boolean active = botmCard.isEventActive();
            setButtonGlowByLabel(" BOTM", active);
        }
    }

}