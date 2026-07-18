package com.flux.services;

import com.flux.FluxPanel;
import com.flux.FluxPlugin;
import com.flux.cards.BotmCard;
import com.flux.cards.SotwCard;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;

@Singleton
public class ChatCommandHandler {
	public static final String BOTM_COMMAND = "!BOTM";
	public static final String SOTW_COMMAND = "!SOTW";

	private final ChatCommandManager chatCommandManager;
	private final FluxPlugin plugin;
	private final Client client;

	@Inject
	public ChatCommandHandler(ChatCommandManager chatCommandManager, FluxPlugin plugin, Client client) {
		this.chatCommandManager = chatCommandManager;
		this.plugin = plugin;
		this.client = client;
	}

	public void registerChatCommands() {
		chatCommandManager.registerCommand(BOTM_COMMAND, this::botmLookup);
		chatCommandManager.registerCommand(SOTW_COMMAND, this::sotwLookup);
	}

	public void unregisterChatCommands() {
		chatCommandManager.unregisterCommand(BOTM_COMMAND);
		chatCommandManager.unregisterCommand(SOTW_COMMAND);
	}

	private void botmLookup(ChatMessage chatMessage, String message) {
		FluxPanel panel = plugin.getPanel();
		if (panel.getBotmCard() == null) return;

		BotmCard card = panel.getBotmCard();
		ChatMessageBuilder chatMessageBuilder = new ChatMessageBuilder()
			.append(ChatColorType.NORMAL)
			.append("Current Boss of the Month: ")
			.append(ChatColorType.HIGHLIGHT)
			.append(card.isEventActive() ? card.getBoss().getName() : "Inactive");

		replaceChatMessage(chatMessage, chatMessageBuilder.build());
	}

	private void sotwLookup(ChatMessage chatMessage, String message) {
		FluxPanel panel = plugin.getPanel();
		if (panel.getSotwCard() == null) return;

		SotwCard card = panel.getSotwCard();
		ChatMessageBuilder chatMessageBuilder = new ChatMessageBuilder()
			.append(ChatColorType.NORMAL)
			.append("Current Skill of the Week: ")
			.append(ChatColorType.HIGHLIGHT)
			.append(card.isEventActive() ? card.getSkill().getName() : "Inactive");

		replaceChatMessage(chatMessage, chatMessageBuilder.build());
	}

	private void replaceChatMessage(ChatMessage chatMessage, String message) {
		final MessageNode messageNode = chatMessage.getMessageNode();
		messageNode.setRuneLiteFormatMessage(message);
		client.refreshChat();
	}
}
