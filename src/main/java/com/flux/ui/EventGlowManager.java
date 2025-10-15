package com.flux.ui;

import com.flux.components.InverseCornerButton;
import com.flux.cards.BotmCard;
import com.flux.cards.HomeCard;
import com.flux.cards.HuntCard;
import com.flux.cards.SotwCard;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manages the glowing effect on footer buttons based on event activity.
 */
public class EventGlowManager {
    private static final int GLOW_CHECK_INTERVAL_MS = 500;

    private final List<InverseCornerButton> footerButtons;
    private final CardManager cardManager;
    private final JPanel footerPanel;
    private final Timer glowTimer;

    public EventGlowManager(List<InverseCornerButton> footerButtons,
                            CardManager cardManager,
                            JPanel footerPanel) {
        this.footerButtons = footerButtons;
        this.cardManager = cardManager;
        this.footerPanel = footerPanel;
        this.glowTimer = new Timer(GLOW_CHECK_INTERVAL_MS, e -> updateAllEventGlows());
    }

    /**
     * Starts the periodic glow update timer.
     */
    public void start() {
        glowTimer.start();
    }

    /**
     * Stops the glow update timer.
     */
    public void stop() {
        glowTimer.stop();
    }

    /**
     * Updates all event button glows based on current card states.
     */
    public void updateAllEventGlows() {
        updateEventGlow(" SOTW", () -> isCardActive(cardManager.getSotwCard()));
        updateEventGlow(" BOTM", () -> isCardActive(cardManager.getBotmCard()));
        updateEventGlow(" The Hunt", () -> isCardActive(cardManager.getHuntCard()));
    }

    private boolean isCardActive(SotwCard card) {
        return card != null && card.isEventActive();
    }

    private boolean isCardActive(BotmCard card) {
        return card != null && card.isEventActive();
    }

    private boolean isCardActive(HuntCard card) {
        return card != null && card.isEventActive();
    }

    private void updateEventGlow(String label, Supplier<Boolean> activeChecker) {
        boolean active = activeChecker.get();
        setButtonGlow(label, active);
        updateHomeCardEventStatus(label, active);
    }

    private void setButtonGlow(String label, boolean glow) {
        footerButtons.stream()
                .filter(btn -> btn.getText().trim().equalsIgnoreCase(label.trim()))
                .findFirst()
                .ifPresent(btn -> {
                    btn.setGlowing(glow);
                    if (!glow) {
                        btn.repaint();
                        footerPanel.repaint();
                    }
                });
    }

    private void updateHomeCardEventStatus(String label, boolean active) {
        HomeCard homeCard = cardManager.getHomeCard();
        if (homeCard == null) {
            return;
        }

        switch (label.trim()) {
            case "SOTW":
                homeCard.updateSotwStatus(active);
                break;
            case "BOTM":
                homeCard.updateBotmStatus(active);
                break;
            case "The Hunt":
                homeCard.updateHuntStatus(active);
                break;
        }
    }
}