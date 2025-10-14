package com.flux;

import com.flux.constants.EntrySelect;
import net.runelite.client.config.*;
import java.awt.*;
import java.awt.event.KeyEvent;

@ConfigGroup(FluxPlugin.CONFIG_GROUP)
public interface FluxConfig extends Config {
	@ConfigSection(name = "Overlay", description = "Overlay configuration.", position = 0)
	String overlaySection = "Overlay section";

	@ConfigSection(name = "Plugin Panel", description = "Plugin panel configuration", position = 1)
	String panelSection = "Plugin Panel section";

	@ConfigSection(name = "Button Config", description = "Plugin panel button configuraton", position = 2)
	String buttonSection = "Button section";

	@ConfigItem(position = 1, keyName = "overlay", name = "Display Overlay", description = "Displays the overlay on your game screen.", section = overlaySection)
	default boolean overlay() {
		return false;
	}

	@ConfigItem(position = 2, keyName = "dtm", name = "Date & Time", description = "Adds the date and time to the overlay.", section = overlaySection, hidden = true)
	default boolean dtm() {
		return true;
	}

	@ConfigItem(position = 3, keyName = "eventPass", name = "Event Password:", description = "Adds the event password to the overlay.", section = overlaySection)
	default String eventPass() {
		return "";
	}

	@ConfigItem(position = 4, keyName = "challengePass", name = "Challenge Password:", description = "Adds the challenge password to the overlay.", section = overlaySection)
	default String challengePass() {
		return "";
	}

	@ConfigItem(position = 5, keyName = "disclaimer", name = "Colors below must be different", description = "The Password Color and the Date & Time Color must be different.", section = overlaySection)
	default void disclaimer() {
	}

	@ConfigItem(position = 6, keyName = "passColor", name = "Password Color", description = "The color of the Event Password and Challenge Password.", section = overlaySection)
	default Color passColor() {
		return Color.GREEN;
	}

	@ConfigItem(position = 7, keyName = "timeColor", name = "Date & Time Color", description = "The color of the Date & Time.", section = overlaySection)
	default Color timeColor() {
		return Color.WHITE;
	}

	@ConfigItem(position = 6, keyName = "entryKeybind", name = "Entry Select Keybind", description = "Sets the keybind used for opening the entry menu in the Clan Events panel.", section = panelSection, hidden = true)
	default Keybind entryKeybind() {
		return new Keybind(KeyEvent.VK_SPACE, 0);
	}

	@ConfigItem(position = 7, keyName = "entry_1", name = "Entry 1", description = "Selects what to show for entry 1 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_1() {
		return EntrySelect.HOME;
	}

	@ConfigItem(position = 8, keyName = "entry_2", name = "Entry 2", description = "Selects what to show for entry 2 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_2() {
		return EntrySelect.HUB;
	}

	@ConfigItem(position = 9, keyName = "entry_3", name = "Entry 3", description = "Selects what to show for entry 3 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_3() {
		return EntrySelect.SOTW;
	}

	@ConfigItem(position = 10, keyName = "entry_4", name = "Entry 4", description = "Selects what to show for entry 4 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_4() {
		return EntrySelect.BOTM;
	}

	@ConfigItem(position = 11, keyName = "entry_5", name = "Entry 5", description = "Selects what to show for entry 5 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_5() {
		return EntrySelect.HOF_OVERALL;
	}

	@ConfigItem(position = 12, keyName = "entry_6", name = "Entry 6", description = "Selects what to show for entry 6 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_6() {
		return EntrySelect.HOF_KC;
	}

	@ConfigItem(position = 13, keyName = "entry_7", name = "Entry 7", description = "Selects what to show for entry 7 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_7() {
		return EntrySelect.HOF_PB;
	}

	@ConfigItem(position = 14, keyName = "entry_8", name = "Entry 8", description = "Selects what to show for entry 8 of the Clan Events panel.", section = panelSection, hidden = true)
	default EntrySelect entry_8() {
		return EntrySelect.HUNT;
	}

	@ConfigItem(position = 15, keyName = "botm_password", name = "BOTM Password", description = "Adds the BOTM event password to the overlay.", section = overlaySection, hidden = true)
	default String botmPass() {
		return "";
	}

	@ConfigItem(position = 16, keyName = "wom_url", name = "WOM url", description = "Configures the wom button url", section = buttonSection, hidden = true)
	default String womurl() {
		return "https://wiseoldman.net/groups/141";
	}

	@ConfigItem(position = 17, keyName = "wom_comps_url", name = "WOM Comps url", description = "url to the Flux WoM Competitons page.", section = buttonSection, hidden = true)
	default String womCompsUrl() {
		return "https://wiseoldman.net/groups/141/competitions";
	}

	@ConfigItem(position = 18, keyName = "wom_sotw_url", name = "WOM SOTW url", description = "url to the Flux WoM SOTW page.", section = buttonSection, hidden = true)
	default String womSotwUrl() {
		return "https://wiseoldman.net/groups/141/competitions";
	}

	@ConfigItem(position = 19, keyName = "clan_login_message", name = "Login Message", description = "Message to broadcast to clan members upon login.", section = panelSection, hidden = true)
	default String clanLoginMessage() {
		return "Check out the Flux Plugin Panel!";
	}

	@ConfigItem(position = 20, keyName = "plugin_announcement_message", name = "Login Message", description = "Announcement message in the home card of the plugin panel.", section = panelSection, hidden = true)
	default String pluginAnnouncementMessage() {
		return "The Flux plugin is available for download from the plugin hub!";
	}

	@ConfigItem(position = 21, keyName = "botm_active", name = "Botm Active", description = "Is BOTM Active?", section = panelSection, hidden = true)
	default Boolean botmActive() {
		return false;
	}

	@ConfigItem(position = 22, keyName = "sotw_active", name = "Sotw Active", description = "Is SOTW Active?", section = panelSection, hidden = true)
	default Boolean sotwActive() {
		return false;
	}

	@ConfigItem(position = 23, keyName = "huntActive", name = "Hunt Active", description = "Is The Hunt Active?", section = panelSection, hidden = true)
	default Boolean huntActive() {
		return false;
	}

	@ConfigItem(position = 24, keyName = "hunt_gdoc_url", name = "The Hunt GDoc", description = "The Hunt GDoc", section = panelSection, hidden = true)
	default String huntGdocUrl() {
		return "";
	}

	@ConfigItem(position = 25, keyName = "hunt_team_one_color", name = "Hunt Team One Color", description = "Hunt Team One Color", section = panelSection, hidden = true)
	default String huntTeamOneColor() {
		return "";
	}

	@ConfigItem(position = 26, keyName = "hunt_team_two_color", name = "Hunt Team Two Color", description = "Hunt Team Two Color", section = panelSection, hidden = true)
	default String huntTeamTwoColor() {
		return "";
	}

	@ConfigItem(position = 27, keyName = "sotw_title", name = "SOTW Title", description = "Title for the SOTW Event in WOM.", section = panelSection, hidden = false)
	default String sotwTitle() {
		return "No Active SOTW Event.";
	}

	@ConfigItem(position = 28, keyName = "sotw_leaderboard", name = "SOTW Leaderboard", description = "Current SOTW Leaderboard", section = panelSection, hidden = false)
	default String sotwLeaderboard() {
		return "[]";
	}

	@ConfigItem(position = 29, keyName = "sotw_wom_link", name = "SOTW WOM Link", description = "SOTW wise old man link.", section = panelSection, hidden = false)
	default String sotwWomLink() {
		return "https://wiseoldman.net/groups/141/competitions";
	}

	@ConfigItem(position = 30, keyName = "sotw_start_time", name = "SOTW Start Time", description = "Start time for the SOTW Event.", section = panelSection, hidden = false)
	default String sotwStartTime() {
		return "1970-01-01T00:00:00Z";
	}

	@ConfigItem(position = 31, keyName = "sotw_end_time", name = "SOTW End Time", description = "End time for the SOTW Event.", section = panelSection, hidden = false)
	default String sotwEndTime() {
		return "1970-01-01T00:00:00Z";
	}

	@ConfigItem(position = 32, keyName = "sotw_winner", name = "SOTW Winner", description = "Winner of the last active SOTW Event.", section = panelSection, hidden = false)
	default String sotwWinner() {
		return "Belinda";
	}

	// BOTM Event Configs

	@ConfigItem(position = 33, keyName = "botmTitle", name = "BOTM Title", description = "Title for the BOTM Event in WOM.", section = panelSection, hidden = false)
	default String botmTitle() {
		return "No Active BOTM Event.";
	}

	@ConfigItem(position = 34, keyName = "botmLeaderboard", name = "BOTM Leaderboard", description = "Current BOTM Leaderboard", section = panelSection, hidden = false)
	default String botmLeaderboard() {
		return "[]";
	}

	@ConfigItem(position = 35, keyName = "botmWomUrl", name = "BOTM WOM URL", description = "BOTM wise old man URL.", section = panelSection, hidden = false)
	default String botmWomUrl() {
		return "https://wiseoldman.net/groups/141/competitions";
	}

	@ConfigItem(position = 36, keyName = "botm_start_time", name = "BOTM Start Time", description = "Start time for the BOTM Event.", section = panelSection, hidden = false)
	default String botmStartTime() {
		return "1970-01-01T00:00:00Z";
	}

	@ConfigItem(position = 37, keyName = "botm_end_time", name = "BOTM End Time", description = "End time for the BOTM Event.", section = panelSection, hidden = false)
	default String botmEndTime() {
		return "1970-01-01T00:00:00Z";
	}

	@ConfigItem(position = 38, keyName = "botm_winner", name = "BOTM Winner", description = "Winner of the last active BOTM Event.", section = panelSection, hidden = false)
	default String botmWinner() {
		return "";
	}

	@ConfigItem(position = 39, keyName = "botmGdocUrl", name = "BOTM Gdoc Url", description = "URL to the GDoc for BOTM Score", section = panelSection, hidden = false)
	default String botmGdocUrl() {
		return "https://docs.google.com/spreadsheets/d/e/2PACX-1vQXOtUM0Y3OMvnZKVw7PRwM9HmkGrbXha2K75Ev2bf_9Ev_EliUHT18BpJ5Djyp1ebdeWSrEoPnF064/pubhtml?gid=0&single=true";
	}

	// Hunt Event Configurations
	@ConfigItem(position = 40, keyName = "hunt_start_time", name = "Hunt Start Time", description = "Start time for the Hunt Event.", section = panelSection)
	default String huntStartTime() {
		return "1970-01-01T00:00:00Z";
	}

	@ConfigItem(position = 41, keyName = "hunt_end_time", name = "Hunt End Time", description = "End time for the Hunt Event.", section = panelSection)
	default String huntEndTime() {
		return "1970-01-01T00:00:00Z";
	}

	@ConfigItem(position = 42, keyName = "hunt_team_one_score", name = "Hunt Team One Score", description = "Current score of Hunt Team One.", section = panelSection)
	default int huntTeamOneScore() {
		return 0;
	}

	@ConfigItem(position = 43, keyName = "hunt_team_two_score", name = "Hunt Team Two Score", description = "Current score of Hunt Team Two.", section = panelSection)
	default int huntTeamTwoScore() {
		return 0;
	}

	@ConfigItem(position = 44, keyName = "hunt_title", name = "Hunt Title", description = "Title for the Hunt Event.", section = panelSection)
	default String huntTitle() {
		return "No Active Hunt Event.";
	}

	@ConfigItem(position = 44, keyName = "rollcallActive", name = "Roll Call Active", description = "Is roll call active?", section = panelSection)
	default Boolean rollCallActive() {
		return false;
	}
}
