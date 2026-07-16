package com.flux.components;

import com.flux.components.combobox.ComboBoxIconEntry;
import com.flux.components.combobox.EntrySelect;
import java.awt.Image;
import java.util.Optional;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;

@Getter
public class EntryConfig {

	private static final int ICON_SIZE = 18;

	private final EntrySelect entry;
	private final JPanel card;
	private final ComboBoxIconEntry comboEntry;
	private final InverseCornerButton button;

	public EntryConfig(EntrySelect entry, String label, String iconPath, JPanel card,
		Runnable onSelected) {
		this.entry = entry;
		this.card = card;
		ImageIcon icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), iconPath));
		comboEntry = new ComboBoxIconEntry(icon, label, Optional.of(entry.name().toLowerCase()));
		button = InverseCornerButton.withImage(label, iconPath);
		button.addActionListener(e -> onSelected.run());
	}

	public void setIcon(Icon icon) {
		Icon scaledIcon = scaleIcon(icon);
		comboEntry.setIcon(scaledIcon);
		button.setIcon(scaledIcon);
	}

	private Icon scaleIcon(Icon icon) {
		if (!(icon instanceof ImageIcon)) {
			return icon;
		}

		Image image = ((ImageIcon) icon).getImage();
		Image scaled = image.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
		return new ImageIcon(scaled);
	}

	public void setGlowing(boolean glow) {
		button.setGlowing(glow);
	}

	public void setActive(boolean active) {
		button.setActive(active);
	}

	public String getName() {
		return entry.name().toLowerCase();
	}
}
