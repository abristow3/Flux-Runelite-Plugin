package com.flux.services;

import net.runelite.client.config.ConfigManager;


public class EventPasswordManager {
	private static final String CONFIG_GROUP = "flux";
	private final ConfigManager configManager;

	public EventPasswordManager(ConfigManager configManager) {
		this.configManager = configManager;
	}
}
