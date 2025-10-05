package net.runelite.client.plugins.flux.cards;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminHubCard extends JPanel {

    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public AdminHubCard() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        String[] configKeys = {
            "BOTM_WOM_URL",
            "BOTM_POINTS_SHEET_URL",
            "BOTM_DROPS_SHEET_URL",
            "BOTM_PASSWORD",
            "BOTM_DISCORD_CHANNEL_LINK",
            "BOTM_DROPS_DISCORD_CHANNEL_LINK",
            "SOTW_WOM_URL",
            "DISCORD_ANNOUNCEMENTS_CHANNEL_LINK",
            "DISCORD_EVENTS_CHANNEL_LINK",
            "DISCORD_ROLLCALL_CHANNEL_LINK",
            "DISCORD_CLAN_SERVER_INVITE_LINK",
            "DISCORD_HUNT_SERVER_INVITE_LINK",
            "ANNOUNCEMENT_MESSAGE",
            "ROLLCALL_ACTIVE",
            "CLAN_WOM_URL",
            "LOGIN_MESSAGE"
        };

        gbc.insets = new Insets(5, 10, 0, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;
        for (String key : configKeys) {
            JLabel label = new JLabel(key + ":");
            JTextField textField = new JTextField(30);

            textField.setName(textField.getText()); // Initial value tracking

            // Fire property change when Enter is pressed
            textField.addActionListener(e -> {
                String oldVal = textField.getName();
                String newVal = textField.getText();
                pcs.firePropertyChange(key, oldVal, newVal);
                textField.setName(newVal);
            });

            // Fire property change on focus lost
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusLost(java.awt.event.FocusEvent e) {
                    String oldVal = textField.getName();
                    String newVal = textField.getText();
                    if (!newVal.equals(oldVal)) {
                        pcs.firePropertyChange(key, oldVal, newVal);
                        textField.setName(newVal);
                    }
                }
            });

            fields.put(key, textField);

            // Add label
            gbc.gridy = row++;
            add(label, gbc);

            // Add text field
            gbc.gridy = row++;
            add(textField, gbc);
        }

        // Add spacing glue at bottom to prevent stretching
        gbc.gridy = row;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    /** Set value for a given config key */
    public void setValue(String key, String value) {
        JTextField field = fields.get(key);
        if (field != null) {
            String oldVal = field.getText();
            field.setText(value);
            field.setName(value);
            pcs.firePropertyChange(key, oldVal, value);
        }
    }

    // /** Get value for a given config key */
    // public String getValue(String key) {
    //     JTextField field = fields.get(key);
    //     return field != null ? field.getText() : null;
    // }

    // // Optional: PropertyChangeListener hooks
    // public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
    //     pcs.addPropertyChangeListener(listener);
    // }

    // public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
    //     pcs.removePropertyChangeListener(listener);
    // }
}
