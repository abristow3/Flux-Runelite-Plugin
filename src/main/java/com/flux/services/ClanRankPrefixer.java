package com.flux.services;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.MessageNode;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClanRankPrefixer {

	private final Client client;
	private final ChatMessageManager chatMessageManager;
	private final ClientThread clientThread;
	private final ChatIconManager chatIconManager;

	public ClanRankPrefixer(Client client, ChatMessageManager chatMessageManager, ClientThread clientThread, ChatIconManager chatIconManager) {
		this.client = client;
		this.chatMessageManager = chatMessageManager;
		this.clientThread = clientThread;
		this.chatIconManager = chatIconManager;
	}

	public void onChatMessage(ChatMessage event) {
		if (event.getType() != ChatMessageType.CLAN_MESSAGE) {
			return;
		}

		ClanChannel clanChannel = client.getClanChannel();
		ClanSettings clanSettings = client.getClanSettings();
		if (clanChannel == null || clanSettings == null) {
			return;
		}

		// check the clan member name in the message
		String rawMessage = event.getMessage();
		String strippedMessage = Text.toJagexName(Text.removeTags(rawMessage)).trim();

		if (strippedMessage.startsWith("To talk in your clan's channel")) {
			return;
		}

		strippedMessage = strippedMessage.replaceFirst("^CA_ID:\\d+\\|", "");

		ClanChannelMember matched = null;
		String matchedName = null;

		for (ClanChannelMember member : clanChannel.getMembers()) {
			String name = Text.toJagexName(member.getName());
			if (strippedMessage.startsWith(name) && (matchedName == null || name.length() > matchedName.length())) {
				matched = member;
				matchedName = name;
			}
		}

		if (matched == null) {
			log.debug("No clan member match for broadcast: '{}'", strippedMessage);
			log.debug("Clan members: {}", clanChannel.getMembers().stream()
					.map(m -> "'" + Text.toJagexName(m.getName()) + "'")
					.collect(java.util.stream.Collectors.joining(", ")));
			return;
		}

		ClanTitle title = clanSettings.titleForRank(matched.getRank());
		if (title == null) {
			log.debug("No title found for rank {} (member {})", matched.getRank(), matchedName);
			return;
		}

		int iconIndex = chatIconManager.getIconNumber(title);
		String newMessage = iconIndex >= 0
				? "<img=" + iconIndex + "> " + rawMessage
				: "[" + title.getName() + "] " + rawMessage;

		log.debug("Matched '{}' to rank '{}', icon index {}, new message: '{}'",
				matchedName, title.getName(), iconIndex, newMessage);

		MessageNode messageNode = event.getMessageNode();
		clientThread.invokeLater(() -> {
			messageNode.setRuneLiteFormatMessage(newMessage);
			chatMessageManager.update(messageNode);
			client.refreshChat();
		});
	}
}