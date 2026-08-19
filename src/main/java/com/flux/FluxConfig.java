package com.flux;
import net.runelite.client.config.*;
import java.awt.*;

@ConfigGroup(FluxPlugin.CONFIG_GROUP)
public interface FluxConfig extends Config {
    @ConfigSection(name = "Password Overlay Settings", description = "Overlay configuration.", position = 0)
    String overlaySection = "Overlay section";

    @ConfigSection(name = "Event Password Toggles", description = "Event password display configuration.", position = 100)
    String eventPasswordsSection = "Event Password Toggles Section";

    @ConfigSection(name = "Misc. Settings", description = "Miscellaneous plugin configuration options.", position = 200)
    String miscSettingsSection = "Miscellaneous Settings";

    @ConfigSection(name = "", description = "Hidden configuration settings", position = 1000)
    String hiddenConfigsSection = "Hidden Configs";

//##############################################################################
//################### VISIBLE SECTION - POSITION 0 - 999 #######################
//##############################################################################

    // ========== PASSWORD OVERLAY SETTINGS SECTION - VISIBLE - POSITION 1-99 ==========

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

    @ConfigItem(position = 4, keyName = "disclaimer", name = "Colors below must be different", description = "The Password Color and the Date & Time Color must be different.", section = overlaySection)
    default void disclaimer() {}

    @ConfigItem(position = 5, keyName = "passColor", name = "Password Color", description = "The color of the Event Password.", section = overlaySection)
    default Color passColor() {
        return Color.GREEN;
    }

    @ConfigItem(position = 6, keyName = "timeColor", name = "Date & Time Color", description = "The color of the Date & Time.", section = overlaySection)
    default Color timeColor() {
        return Color.WHITE;
    }

    // ========== EVENT PASSWORD TOGGLES - VISIBLE - POSITION 101-199 ==========

    @ConfigItem(position = 101, keyName = "display_hunt_master_password", name = "Show Hunt Master Password", description = "Displays the hunt master password on your screen", section = eventPasswordsSection)
    default boolean displayHuntMasterPassword() {
        return true;
    }

    @ConfigItem(position = 102, keyName = "display_hunt_bounty_password", name = "Show Hunt Bounty Password", description = "Displays the hunt bounty password on your screen", section = eventPasswordsSection)
    default boolean displayHuntBountyPassword() {
        return true;
    }

    @ConfigItem(position = 103, keyName = "display_hunt_daily_password", name = "Show Hunt Daily Password", description = "Displays the hunt daily password on your screen", section = eventPasswordsSection)
    default boolean displayHuntDailyPassword() {
        return true;
    }

    @ConfigItem(position = 104, keyName = "display_botm_password", name = "Show BOTM Password", description = "Displays the BOTM password on your screen", section = eventPasswordsSection)
    default boolean displayBotmPassword() {
        return true;
    }

    @ConfigItem(position = 105, keyName = "display_misc_event_password", name = "Show Misc. Event Password", description = "Displays miscellaneous event passwords on your screen", section = eventPasswordsSection)
    default boolean displayMiscEventPassword() {
        return true;
    }

	@ConfigItem(position = 106, keyName = "auto_reenable_hunt_passwords", name = "Auto Re-enable Hunt Passwords", description = "Auto re-enables the daily and bounty challenge password display when a new bounty or daily occurs.", section = eventPasswordsSection)
	default boolean autoReenableHuntPasswords() {
		return true;
	}

    // ========== MISC. SETTINGS - VISIBLE - POSITION 201 - 299 ==========

    @ConfigItem(position = 201, keyName = "menuPriority", name = "Sidebar Priority", description = "Adjust the runelite sidebar priority. Lower priority => higher on sidebar. Restart the client to take effect", section = miscSettingsSection)
    default int menuPriority() {
        return 5;
    }

    @ConfigItem(position = 202, keyName = "loginColor", name = "Login Message Color", description = "The color of the Login Message.", section = miscSettingsSection)
    default Color loginColor() {return new Color(255, 255, 0); } //Custom dark red.

    @ConfigItem(position = 203, keyName = "hunt_broadcasts", name = "Hunt Broadcasts", description = "Sends a local chat broadcast when the Hunt daily or bounty password rotates.", section = eventPasswordsSection)
    default boolean huntBroadcasts() {
        return true;
    }

//############################################################################
//################### HIDDEN SECTION - POSITION 1000+ ########################
//############################################################################

    // ========== BOTM SETTINGS SECTION - HIDDEN ==========

    @ConfigItem(keyName = "botm_password", name = "BOTM Password", description = "Adds the BOTM event password to the overlay.", section = hiddenConfigsSection, hidden = true)
    default String botmPass() {
        return "";
    }

    @ConfigItem(keyName = "botmActive", name = "BOTM Active", description = "Is BOTM Active?", section = hiddenConfigsSection, hidden = true)
    default Boolean botmActive() {
        return false;
    }

    @ConfigItem(keyName = "botmTitle", name = "BOTM Title", description = "Title for the BOTM Event in WOM.", section = hiddenConfigsSection, hidden = true)
    default String botmTitle() {
        return "No Active BOTM Event.";
    }

    @ConfigItem(keyName = "botmLeaderboard", name = "BOTM Leaderboard", description = "Current BOTM Leaderboard", section = hiddenConfigsSection, hidden = true)
    default String botmLeaderboard() {
        return "[]";
    }

    @ConfigItem(keyName = "botmBoss", name = "BOTM Boss", description = "Current BOTM Boss", section = hiddenConfigsSection, hidden = true)
    default String botmBoss() {
        return "VORKATH";
    }

    @ConfigItem(keyName = "botmWomUrl", name = "BOTM WOM URL", description = "BOTM wise old man URL.", section = hiddenConfigsSection, hidden = true)
    default String botmWomUrl() {
        return "https://wiseoldman.net/groups/141/competitions";
    }

    @ConfigItem(keyName = "botm_start_time", name = "BOTM Start Time", description = "Start time for the BOTM Event.", section = hiddenConfigsSection, hidden = true)
    default String botmStartTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(keyName = "botm_end_time", name = "BOTM End Time", description = "End time for the BOTM Event.", section = hiddenConfigsSection, hidden = true)
    default String botmEndTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(keyName = "botm_winner", name = "BOTM Winner", description = "Winner of the last active BOTM Event.", section = hiddenConfigsSection, hidden = true)
    default String botmWinner() {
        return "";
    }

    @ConfigItem(keyName = "botmGdocUrl", name = "BOTM GDoc URL", description = "URL to the GDoc for BOTM Score", section = hiddenConfigsSection, hidden = true)
    default String botmGdocUrl() {
        return "https://docs.google.com/spreadsheets/d/e/2PACX-1vQXOtUM0Y3OMvnZKVw7PRwM9HmkGrbXha2K75Ev2bf_9Ev_EliUHT18BpJ5Djyp1ebdeWSrEoPnF064/pubhtml?gid=0&single=true";
    }

    // ========== SOTW SETTINGS SECTION - HIDDEN ==========

    @ConfigItem(keyName = "sotwActive", name = "SOTW Active", description = "Is SOTW Active?", section = hiddenConfigsSection, hidden = true)
    default Boolean sotwActive() {
        return false;
    }

    @ConfigItem(keyName = "sotwTitle", name = "SOTW Title", description = "Title for the SOTW Event in WOM.", section = hiddenConfigsSection, hidden = true)
    default String sotwTitle() {
        return "No Active SOTW Event.";
    }

    @ConfigItem(keyName = "sotwLeaderboard", name = "SOTW Leaderboard", description = "Current SOTW Leaderboard", section = hiddenConfigsSection, hidden = true)
    default String sotwLeaderboard() {
        return "[]";
    }

    @ConfigItem(keyName = "sotwSkill", name = "SOTW Skill", description = "Current SOTW Skill", section = hiddenConfigsSection, hidden = true)
    default String sotwSkill() {
        return "OVERALL";
    }

    @ConfigItem(keyName = "sotw_wom_link", name = "SOTW WOM Link", description = "SOTW wise old man link.", section = hiddenConfigsSection, hidden = true)
    default String sotwWomLink() {
        return "https://wiseoldman.net/groups/141/competitions";
    }

    @ConfigItem(keyName = "sotw_start_time", name = "SOTW Start Time", description = "Start time for the SOTW Event.", section = hiddenConfigsSection, hidden = true)
    default String sotwStartTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(keyName = "sotw_end_time", name = "SOTW End Time", description = "End time for the SOTW Event.", section = hiddenConfigsSection, hidden = true)
    default String sotwEndTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(keyName = "sotw_winner", name = "SOTW Winner", description = "Winner of the last active SOTW Event.", section = hiddenConfigsSection, hidden = true)
    default String sotwWinner() {
        return "";
    }

    // ========== HUNT SETTINGS SECTION - HIDDEN ==========

    @ConfigItem(keyName = "huntActive", name = "Hunt Active", description = "Is The Hunt Active?", section = hiddenConfigsSection, hidden = true)
    default Boolean huntActive() {
        return false;
    }

    @ConfigItem(keyName = "huntTitle", name = "Hunt Title", description = "Title for the Hunt Event.", section = hiddenConfigsSection, hidden = true)
    default String huntTitle() {
        return "No Active Hunt Event.";
    }

    @ConfigItem(keyName = "hunt_competition_id", name = "Hunt Competition ID", description = "WiseOldMan competition ID for The Hunt event.", section = hiddenConfigsSection, hidden = true)
    default String huntCompetitionId() {
        return "100262";
    }

    @ConfigItem(keyName = "hunt_start_time", name = "Hunt Start Time", description = "Start time for the Hunt Event.", section = hiddenConfigsSection, hidden = true)
    default String huntStartTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(keyName = "hunt_end_time", name = "Hunt End Time", description = "End time for the Hunt Event.", section = hiddenConfigsSection, hidden = true)
    default String huntEndTime() {
        return "1970-01-01T00:00:00Z";
    }

    @ConfigItem(keyName = "hunt_wom_url", name = "Hunt WOM URL", description = "Hunt Wise Old Man competition URL.", section = hiddenConfigsSection, hidden = true)
    default String huntWomUrl() {
        return "https://wiseoldman.net/competitions/100262";
    }

    @ConfigItem(keyName = "hunt_gdoc_url", name = "The Hunt GDoc", description = "The Hunt GDoc URL.", section = hiddenConfigsSection, hidden = true)
    default String huntGdocUrl() {
        return "https://docs.google.com/spreadsheets/d/e/2PACX-1vSLCxscAVFZY9wuDqmeBPu4UZio2I39DHDGy_8DXrvHqYKmZc8NgsC4DWv_olXOTjGQktcBnU88Fmf4/pubhtml?gid=0&single=true";
    }

    @ConfigItem(keyName = "hunt_team_1_name", name = "Hunt Team 1 Name", description = "Name of Hunt Team 1.", section = hiddenConfigsSection, hidden = true)
    default String huntTeam1Name() {
        return "Team 1";
    }

    @ConfigItem(keyName = "hunt_team_1_color", name = "Hunt Team 1 Color", description = "Hex color for Hunt Team 1 (e.g., #FF0000).", section = hiddenConfigsSection, hidden = true)
    default String huntTeamOneColor() {
        return "#FF0000";
    }

    @ConfigItem(keyName = "hunt_team_1_leaderboard", name = "Hunt Team 1 Leaderboard", description = "Top 10 EHB leaderboard for Team 1.", section = hiddenConfigsSection, hidden = true)
    default String huntTeam1Leaderboard() {
        return "[]";
    }

    @ConfigItem(keyName = "hunt_team_2_name", name = "Hunt Team 2 Name", description = "Name of Hunt Team 2.", section = hiddenConfigsSection, hidden = true)
    default String huntTeam2Name() {
        return "Team 2";
    }

    @ConfigItem(keyName = "hunt_team_2_color", name = "Hunt Team 2 Color", description = "Hex color for Hunt Team 2 (e.g., #0000FF).", section = hiddenConfigsSection, hidden = true)
    default String huntTeamTwoColor() {
        return "#0000FF";
    }

    @ConfigItem(keyName = "hunt_team_2_leaderboard", name = "Hunt Team 2 Leaderboard", description = "Top 10 EHB leaderboard for Team 2.", section = hiddenConfigsSection, hidden = true)
    default String huntTeam2Leaderboard() {
        return "[]";
    }

    @ConfigItem(keyName = "hunt_team_1_score", name = "Hunt Team 1 Score", description = "Current total score of Hunt Team 1.", section = hiddenConfigsSection, hidden = true)
    default int huntTeamOneScore() {
        return 0;
    }

    @ConfigItem(keyName = "hunt_team_2_score", name = "Hunt Team 2 Score", description = "Current total score of Hunt Team 2.", section = hiddenConfigsSection, hidden = true)
    default int huntTeamTwoScore() {
        return 0;
    }

    @ConfigItem(keyName = "hunt_passwords", name = "Hunt Passwords", description = "Hunt Passwords", section = hiddenConfigsSection, hidden = true)
    default String huntPasswords() {
        return "";
    }

    @ConfigItem(keyName = "discord_invite_url", name = "Discord Invite URL", description = "Discord server invite perma-link", section = hiddenConfigsSection, hidden = true)
    default String discordInviteUrl() {
        return "https://discord.gg/pTxsfJMNRJ";
    }

    @ConfigItem(keyName = "hunt_signup_discord_channel_url", name = "Hunt Signup Channel URL", description = "Discord channel to signup for the Hunt event.", section = hiddenConfigsSection, hidden = true)
    default String huntSignupDiscordChannelUrl() {
        return "discord://discord.com/channels/414435426007384075/414458243499425792";
    }

    @ConfigItem(keyName = "hunt_master_password", name = "Hunt Master Password", description = "The main event password for the Hunt", section = hiddenConfigsSection, hidden = true)
    default String huntMasterPassword() {
        return "";
    }

    @ConfigItem(keyName = "hunt_bounty_password", name = "Hunt Bounty Password", description = "The current bounty password for the Hunt", section = hiddenConfigsSection, hidden = true)
    default String huntBountyPassword() {
        return "";
    }

    @ConfigItem(keyName = "hunt_daily_password", name = "Hunt Daily Password", description = "The current daily password for the Hunt", section = hiddenConfigsSection, hidden = true)
    default String huntDailyPassword() {
        return "";
    }

    // ========== WOM SETTINGS SECTION - HIDDEN ==========

    @ConfigItem(keyName = "wom_url", name = "WOM URL", description = "Configures the WOM button URL", section = hiddenConfigsSection, hidden = true)
    default String womurl() {
        return "https://wiseoldman.net/groups/141";
    }

    @ConfigItem(keyName = "wom_comps_url", name = "WOM Competitions URL", description = "URL to the Flux WOM Competitions page.", section = hiddenConfigsSection, hidden = true)
    default String womCompsUrl() {
        return "https://wiseoldman.net/groups/141/competitions";
    }

    // ========== MISC. SETTINGS SECTION - HIDDEN ==========

    @ConfigItem(keyName = "clan_login_message", name = "Login Message", description = "Message to broadcast to clan members upon login.", section = hiddenConfigsSection, hidden = true)
    default String clanLoginMessage() {
        return "Check out the Flux Plugin Panel!";
    }

    @ConfigItem(keyName = "plugin_announcement_message", name = "Announcement Message", description = "Announcement message in the home card of the plugin panel.", section = hiddenConfigsSection, hidden = true)
    default String pluginAnnouncementMessage() {
        return "The Flux plugin is available for download from the plugin hub!";
    }

    @ConfigItem(keyName = "rollCallActive", name = "Roll Call Active", description = "Is roll call active?", section = hiddenConfigsSection, hidden = true)
    default Boolean rollCallActive() {
        return false;
    }

    @ConfigItem(keyName = "misc_event_password", name = "Misc. Event Password", description = "A password used for other miscellaneous events", section = hiddenConfigsSection, hidden = true)
    default String miscEventPassword() {
        return "";
    }
}