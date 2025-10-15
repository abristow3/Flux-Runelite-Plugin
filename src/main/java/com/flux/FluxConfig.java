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

    @ConfigSection(name = "Button Config", description = "Plugin panel button configuration", position = 2)
    String buttonSection = "Button section";

    // ========== OVERLAY SECTION ==========

    @ConfigItem(position = 1, keyName = "overlay", name = "Display Overlay", description = "Displays the overlay on your game screen.", section = overlaySection)
    default boolean overlay() {
        return false;
    }

    @ConfigItem(position = 2, keyName = "dtm", name = "Date & Time", description = "Adds the date and time to the overlay.", section = overlaySection)
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

    @ConfigItem(position = 8, keyName = "botm_password", name = "BOTM Password", description = "Adds the BOTM event password to the overlay.", section = overlaySection, hidden = true)
    default String botmPass() {
        return "";
    }

    // ========== PLUGIN PANEL SECTION ==========

    @ConfigItem(position = 1, keyName = "entryKeybind", name = "Entry Select Keybind", description = "Sets the keybind used for opening the entry menu in the Clan Events panel.", section = panelSection, hidden = true)
    default Keybind entryKeybind() {
        return new Keybind(KeyEvent.VK_SPACE, 0);
    }

    @ConfigItem(position = 2, keyName = "entry_1", name = "Entry 1", description = "Selects what to show for entry 1 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_1() {
        return EntrySelect.HOME;
    }

    @ConfigItem(position = 3, keyName = "entry_2", name = "Entry 2", description = "Selects what to show for entry 2 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_2() {
        return EntrySelect.HUB;
    }

    @ConfigItem(position = 4, keyName = "entry_3", name = "Entry 3", description = "Selects what to show for entry 3 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_3() {
        return EntrySelect.SOTW;
    }

    @ConfigItem(position = 5, keyName = "entry_4", name = "Entry 4", description = "Selects what to show for entry 4 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_4() {
        return EntrySelect.BOTM;
    }

    @ConfigItem(position = 6, keyName = "entry_5", name = "Entry 5", description = "Selects what to show for entry 5 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_5() {
        return EntrySelect.HOF_OVERALL;
    }

    @ConfigItem(position = 7, keyName = "entry_6", name = "Entry 6", description = "Selects what to show for entry 6 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_6() {
        return EntrySelect.HOF_KC;
    }

    @ConfigItem(position = 8, keyName = "entry_7", name = "Entry 7", description = "Selects what to show for entry 7 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_7() {
        return EntrySelect.HOF_PB;
    }

    @ConfigItem(position = 9, keyName = "entry_8", name = "Entry 8", description = "Selects what to show for entry 8 of the Clan Events panel.", section = panelSection, hidden = true)
    default EntrySelect entry_8() {
        return EntrySelect.HUNT;
    }

    @ConfigItem(position = 10, keyName = "clan_login_message", name = "Login Message", description = "Message to broadcast to clan members upon login.", section = panelSection, hidden = true)
    default String clanLoginMessage() {
        return "Check out the Flux Plugin Panel!";
    }

    @ConfigItem(position = 11, keyName = "plugin_announcement_message", name = "Announcement Message", description = "Announcement message in the home card of the plugin panel.", section = panelSection, hidden = true)
    default String pluginAnnouncementMessage() {
        return "The Flux plugin is available for download from the plugin hub!";
    }

    @ConfigItem(position = 12, keyName = "rollCallActive", name = "Roll Call Active", description = "Is roll call active?", section = panelSection, hidden = true)
    default Boolean rollCallActive() {
        return false;
    }

    // ========== BUTTON CONFIG SECTION ==========

    @ConfigItem(position = 1, keyName = "wom_url", name = "WOM URL", description = "Configures the WOM button URL", section = buttonSection, hidden = true)
    default String womurl() {
        return "https://wiseoldman.net/groups/141";
    }

    @ConfigItem(position = 2, keyName = "wom_comps_url", name = "WOM Competitions URL", description = "URL to the Flux WOM Competitions page.", section = buttonSection, hidden = true)
    default String womCompsUrl() {
        return "https://wiseoldman.net/groups/141/competitions";
    }

    // ========== SOTW EVENT CONFIGURATION ==========

    @ConfigItem(position = 20, keyName = "sotwActive", name = "SOTW Active", description = "Is SOTW Active?", section = panelSection, hidden = true)
    default Boolean sotwActive() {
        return false;
    }

    @ConfigItem(position = 21, keyName = "sotwTitle", name = "SOTW Title", description = "Title for the SOTW Event in WOM.", section = panelSection, hidden = true)
    default String sotwTitle() {
        return "No Active SOTW Event.";
    }

    @ConfigItem(position = 22, keyName = "sotwLeaderboard", name = "SOTW Leaderboard", description = "Current SOTW Leaderboard", section = panelSection, hidden = true)
    default String sotwLeaderboard() {
        return "[]";
    }

    @ConfigItem(position = 23, keyName = "sotw_wom_link", name = "SOTW WOM Link", description = "SOTW wise old man link.", section = panelSection, hidden = true)
    default String sotwWomLink() {
        return "https://wiseoldman.net/groups/141/competitions";
    }

    @ConfigItem(position = 24, keyName = "sotw_start_time", name = "SOTW Start Time", description = "Start time for the SOTW Event.", section = panelSection, hidden = true)
    default String sotwStartTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(position = 25, keyName = "sotw_end_time", name = "SOTW End Time", description = "End time for the SOTW Event.", section = panelSection, hidden = true)
    default String sotwEndTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(position = 26, keyName = "sotw_winner", name = "SOTW Winner", description = "Winner of the last active SOTW Event.", section = panelSection, hidden = true)
    default String sotwWinner() {
        return "";
    }

    // ========== BOTM EVENT CONFIGURATION ==========

    @ConfigItem(position = 30, keyName = "botmActive", name = "BOTM Active", description = "Is BOTM Active?", section = panelSection, hidden = true)
    default Boolean botmActive() {
        return false;
    }

    @ConfigItem(position = 31, keyName = "botmTitle", name = "BOTM Title", description = "Title for the BOTM Event in WOM.", section = panelSection, hidden = true)
    default String botmTitle() {
        return "No Active BOTM Event.";
    }

    @ConfigItem(position = 32, keyName = "botmLeaderboard", name = "BOTM Leaderboard", description = "Current BOTM Leaderboard", section = panelSection, hidden = true)
    default String botmLeaderboard() {
        return "[]";
    }

    @ConfigItem(position = 33, keyName = "botmWomUrl", name = "BOTM WOM URL", description = "BOTM wise old man URL.", section = panelSection, hidden = true)
    default String botmWomUrl() {
        return "https://wiseoldman.net/groups/141/competitions";
    }

    @ConfigItem(position = 34, keyName = "botm_start_time", name = "BOTM Start Time", description = "Start time for the BOTM Event.", section = panelSection, hidden = true)
    default String botmStartTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(position = 35, keyName = "botm_end_time", name = "BOTM End Time", description = "End time for the BOTM Event.", section = panelSection, hidden = true)
    default String botmEndTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(position = 36, keyName = "botm_winner", name = "BOTM Winner", description = "Winner of the last active BOTM Event.", section = panelSection, hidden = true)
    default String botmWinner() {
        return "";
    }

    @ConfigItem(position = 37, keyName = "botmGdocUrl", name = "BOTM GDoc URL", description = "URL to the GDoc for BOTM Score", section = panelSection, hidden = true)
    default String botmGdocUrl() {
        return "https://docs.google.com/spreadsheets/d/e/2PACX-1vQXOtUM0Y3OMvnZKVw7PRwM9HmkGrbXha2K75Ev2bf_9Ev_EliUHT18BpJ5Djyp1ebdeWSrEoPnF064/pubhtml?gid=0&single=true";
    }

    // ========== HUNT EVENT CONFIGURATION ==========

    @ConfigItem(position = 40, keyName = "huntActive", name = "Hunt Active", description = "Is The Hunt Active?", section = panelSection, hidden = true)
    default Boolean huntActive() {
        return false;
    }

    @ConfigItem(position = 41, keyName = "huntTitle", name = "Hunt Title", description = "Title for the Hunt Event.", section = panelSection, hidden = true)
    default String huntTitle() {
        return "No Active Hunt Event.";
    }

    @ConfigItem(position = 42, keyName = "hunt_competition_id", name = "Hunt Competition ID", description = "WiseOldMan competition ID for The Hunt event.", section = panelSection, hidden = true)
    default String huntCompetitionId() {
        return "100262";
    }

    @ConfigItem(position = 43, keyName = "hunt_start_time", name = "Hunt Start Time", description = "Start time for the Hunt Event.", section = panelSection, hidden = true)
    default String huntStartTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(position = 44, keyName = "hunt_end_time", name = "Hunt End Time", description = "End time for the Hunt Event.", section = panelSection, hidden = true)
    default String huntEndTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(position = 45, keyName = "hunt_wom_url", name = "Hunt WOM URL", description = "Hunt Wise Old Man competition URL.", section = panelSection, hidden = true)
    default String huntWomUrl() {
        return "https://wiseoldman.net/competitions/100262";
    }

    @ConfigItem(position = 45, keyName = "hunt_gdoc_url", name = "The Hunt GDoc", description = "The Hunt GDoc URL.", section = panelSection, hidden = true)
    default String huntGdocUrl() {
        return "";
    }

    @ConfigItem(position = 46, keyName = "hunt_team_1_name", name = "Hunt Team 1 Name", description = "Name of Hunt Team 1.", section = panelSection, hidden = true)
    default String huntTeam1Name() {
        return "Team 1";
    }

    @ConfigItem(position = 47, keyName = "hunt_team_1_color", name = "Hunt Team 1 Color", description = "Hex color for Hunt Team 1 (e.g., #FF0000).", section = panelSection, hidden = true)
    default String huntTeamOneColor() {
        return "#FF0000";
    }

    @ConfigItem(position = 48, keyName = "hunt_team_1_leaderboard", name = "Hunt Team 1 Leaderboard", description = "Top 10 EHB leaderboard for Team 1.", section = panelSection, hidden = true)
    default String huntTeam1Leaderboard() {
        return "[]";
    }

    @ConfigItem(position = 49, keyName = "hunt_team_2_name", name = "Hunt Team 2 Name", description = "Name of Hunt Team 2.", section = panelSection, hidden = true)
    default String huntTeam2Name() {
        return "Team 2";
    }

    @ConfigItem(position = 50, keyName = "hunt_team_2_color", name = "Hunt Team 2 Color", description = "Hex color for Hunt Team 2 (e.g., #0000FF).", section = panelSection, hidden = true)
    default String huntTeamTwoColor() {
        return "#0000FF";
    }

    @ConfigItem(position = 51, keyName = "hunt_team_2_leaderboard", name = "Hunt Team 2 Leaderboard", description = "Top 10 EHB leaderboard for Team 2.", section = panelSection, hidden = true)
    default String huntTeam2Leaderboard() {
        return "[]";
    }

    @ConfigItem(position = 52, keyName = "hunt_team_1_score", name = "Hunt Team 1 Score", description = "Current total score of Hunt Team 1.", section = panelSection, hidden = true)
    default int huntTeamOneScore() {
        return 0;
    }

    @ConfigItem(position = 53, keyName = "hunt_team_2_score", name = "Hunt Team 2 Score", description = "Current total score of Hunt Team 2.", section = panelSection, hidden = true)
    default int huntTeamTwoScore() {
        return 0;
    }

    @ConfigItem(position = 54, keyName = "hunt_passwords", name = "Hunt Passwords", description = "Hunt Passwords", section = panelSection, hidden = true)
    default String huntPasswords() {
        return "";
    }
}