package com.flux.cards;

import javax.swing.*;
import net.runelite.client.config.ConfigManager;
import com.flux.FluxConfig;

import java.awt.*;
import java.util.*;

public class AdminHubCard extends FluxCard {
    private static final int TEXT_FIELD_COLUMNS = 30;
    private static final int SPACING_VERTICAL = 5;
    private static final int SPACING_HORIZONTAL = 10;

    private final FluxConfig config;
    private final ConfigManager configManager;
    private final Map<String, JTextField> textFields = new LinkedHashMap<>();
    private JCheckBox rollCallActiveCheckbox;

    public AdminHubCard(FluxConfig config, ConfigManager configManager) {
        super();
        this.config = config;
        this.configManager = configManager;

        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        GridBagConstraints gbc = createBaseConstraints();

        ConfigField[] configFields = {
                new ConfigField("Roll Call Active", "rollCallActive", true),
                new ConfigField("BOTM Password", "botm_password", false),
                new ConfigField("BOTM GDoc URL", "botmGdocUrl", false),
                new ConfigField("Clan Login Message", "clan_login_message", false),
                new ConfigField("Plugin Announcement", "plugin_announcement_message", false),
                new ConfigField("Hunt GDoc URL", "hunt_gdoc_url", false),
                new ConfigField("Hunt Passwords", "hunt_passwords", false)
        };

        int row = 0;
        for (ConfigField field : configFields) {
            gbc.gridy = row++;
            addFieldLabel(field.label, gbc);

            gbc.gridy = row++;
            if (field.isCheckbox) {
                addCheckboxField(field.configKey, gbc);
            } else {
                addTextField(field.label, field.configKey, gbc);
            }
        }

        // Add vertical glue at the end to push everything to the top
        gbc.gridy = row;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    private GridBagConstraints createBaseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SPACING_VERTICAL, SPACING_HORIZONTAL, 0, SPACING_HORIZONTAL);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        return gbc;
    }

    private void addFieldLabel(String labelText, GridBagConstraints gbc) {
        JLabel label = new JLabel(labelText + ":");
        label.setForeground(COLOR_YELLOW);
        label.setFont(FONT_NORMAL);
        add(label, gbc);
    }

    private void addCheckboxField(String configKey, GridBagConstraints gbc) {
        rollCallActiveCheckbox = new JCheckBox("Active");

        // Load initial state from config
        boolean isActive = getConfigBoolean(configKey);
        rollCallActiveCheckbox.setSelected(isActive);

        // Update config when checkbox changes
        rollCallActiveCheckbox.addActionListener(e -> {
            boolean selected = rollCallActiveCheckbox.isSelected();
            configManager.setConfiguration("flux", configKey, String.valueOf(selected));
        });

        add(rollCallActiveCheckbox, gbc);
    }

    private void addTextField(String fieldKey, String configKey, GridBagConstraints gbc) {
        JTextField textField = new JTextField(TEXT_FIELD_COLUMNS);

        // Load initial value from config
        String value = configManager.getConfiguration("flux", configKey);
        textField.setText(value != null ? value : "");

        // Store original value in component name for change detection
        textField.putClientProperty("originalValue", textField.getText());

        // Update config on Enter key
        textField.addActionListener(e -> saveTextField(textField, configKey));

        // Update config on focus loss (if changed)
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                saveTextField(textField, configKey);
            }
        });

        textFields.put(fieldKey, textField);
        add(textField, gbc);
    }

    private void saveTextField(JTextField textField, String configKey) {
        String oldValue = (String) textField.getClientProperty("originalValue");
        String newValue = textField.getText();

        if (!Objects.equals(oldValue, newValue)) {
            configManager.setConfiguration("flux", configKey, newValue);
            textField.putClientProperty("originalValue", newValue);
        }
    }

    // Public API
    public void setValue(String fieldLabel, String value) {
        JTextField textField = textFields.get(fieldLabel);
        if (textField != null) {
            textField.setText(value);
            textField.putClientProperty("originalValue", value);
        }
    }

    public String getFieldValue(String fieldLabel) {
        JTextField textField = textFields.get(fieldLabel);
        return textField != null ? textField.getText() : null;
    }

    @Override
    public void refresh() {
        // Reload all values from config
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            String fieldLabel = entry.getKey();
            JTextField textField = entry.getValue();

            // Convert field label back to config key
            String configKey = convertLabelToConfigKey(fieldLabel);
            String value = configManager.getConfiguration("flux", configKey);

            textField.setText(value != null ? value : "");
            textField.putClientProperty("originalValue", textField.getText());
        }

        // Refresh checkbox
        if (rollCallActiveCheckbox != null) {
            boolean isActive = getConfigBoolean("rollCallActive");
            rollCallActiveCheckbox.setSelected(isActive);
        }
    }

    @Override
    protected boolean getConfigBoolean(String key) {
        return getConfigBoolean(key, configManager);
    }

    @Override
    protected String getConfigValue(String key, String defaultValue) {
        return getConfigValue(key, defaultValue, configManager);
    }

    // Helper methods
    private String convertLabelToConfigKey(String label) {
        // Convert display label back to config key format
        Map<String, String> labelToKey = new LinkedHashMap<>();
        labelToKey.put("BOTM Password", "botm_password");
        labelToKey.put("BOTM GDoc URL", "botmGdocUrl");
        labelToKey.put("Clan Login Message", "clan_login_message");
        labelToKey.put("Plugin Announcement", "plugin_announcement_message");
        labelToKey.put("Hunt GDoc URL", "hunt_gdoc_url");
        labelToKey.put("Hunt Team 1 Color", "hunt_team_1_color");
        labelToKey.put("Hunt Team 2 Color", "hunt_team_2_color");
        labelToKey.put("Hunt Passwords", "hunt_passwords");

        return labelToKey.getOrDefault(label, label.toLowerCase().replace(" ", "_"));
    }

    // Helper class
    private static class ConfigField {
        final String label;
        final String configKey;
        final boolean isCheckbox;

        ConfigField(String label, String configKey, boolean isCheckbox) {
            this.label = label;
            this.configKey = configKey;
            this.isCheckbox = isCheckbox;
        }
    }
}