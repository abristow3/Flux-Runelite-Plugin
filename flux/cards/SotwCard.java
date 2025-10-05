package net.runelite.client.plugins.flux.cards;

import javax.swing.*;
import java.awt.*;

public class SotwCard extends JPanel
{
    private boolean eventActive = false;
    private final JCheckBox activeCheckBox;

    public SotwCard()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JLabel label = new JLabel("Welcome to the SOTW Card!");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(label);

        add(Box.createRigidArea(new Dimension(0, 10)));

        activeCheckBox = new JCheckBox("Event Active");
        activeCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        activeCheckBox.setOpaque(false);
        activeCheckBox.setForeground(Color.WHITE);

        activeCheckBox.addActionListener(e -> {
            boolean oldVal = eventActive;
            eventActive = activeCheckBox.isSelected();
            firePropertyChange("eventActive", oldVal, eventActive);
        });

        add(activeCheckBox);

        add(Box.createRigidArea(new Dimension(0, 10)));

        JButton testButton = new JButton("Click me");
        testButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(testButton);

        add(Box.createVerticalGlue());
    }

    public boolean isEventActive()
    {
        return eventActive;
    }
}
