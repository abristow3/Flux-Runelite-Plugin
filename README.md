# Flux Plugin for RuneLite

A comprehensive clan management plugin designed for the Flux OSRS clan community. This plugin provides real-time event tracking, competition leaderboards, and clan information directly in your RuneLite client.

## Features

### 🏠 Home Dashboard
- **Event Status Overview**: See which clan events are currently active (SOTW, BOTM, The Hunt)
- **Announcements**: Stay updated with the latest clan news and announcements
- **Quick Links**: Direct access to Discord channels, Wise Old Man, and clan resources
- **Roll Call Notifications**: Get notified when roll call is active

### 🏆 Competition Tracking

#### Skill of the Week (SOTW)
- **Live Leaderboards**: Top 10 players ranked by XP gained
- **Real-time Updates**: Automatically syncs with Wise Old Man API every 7 minutes
- **Event Countdown**: See time remaining or when the event starts
- **Winner Display**: Shows the winner after the event ends
- **Animated Rankings**: Top 3 players get special gold, silver, and bronze highlights

#### Boss of the Month (BOTM)
- **Live Leaderboards**: Top 10 players ranked by boss points
- **Google Sheets Integration**: Pulls real-time scores from the official BOTM spreadsheet
- **Event Countdown**: Track time remaining until the event ends
- **Winner Display**: Automatically shows the winner when the event concludes
- **Animated Rankings**: Top 3 players feature animated shiny borders

#### The Hunt
- **Team Competition**: Track two competing teams in real-time
- **Combined Leaderboard**: View top 10 players across both teams
- **Team Scores**: Live team totals updated from Google Sheets
- **Color-Coded Players**: Each player displays in their team's color
- **Individual Rankings**: Shows Efficient Hours Bossed (EHB) for each player
- **Winner Announcement**: Displays winning team when the event ends
- **Animated Top 3**: First three places get special animated borders

### 🎮 In-Game Overlay
- **Event Passwords**: Display current event passwords on-screen
- **Date & Time**: Shows current UTC time for coordination
- **Customizable Colors**: Choose your own colors for passwords and time display
- **Toggle On/Off**: Enable or disable the overlay as needed

### 👑 Admin Hub
*(Only visible to Admiral rank and higher)*
- **Roll Call Management**: Enable/disable roll call notifications
- **Event Configuration**: Update event passwords and settings
- **Message Management**: Edit clan login messages and announcements
- **GDoc Links**: Update Google Sheets URLs for BOTM and Hunt
- **Team Customization**: Modify Hunt team colors

## How It Works

### Automatic Updates
The plugin automatically checks for new competition data every 7 minutes by:
1. Fetching active competitions from Wise Old Man API
2. Pulling scores from Google Sheets for BOTM and Hunt
3. Updating leaderboards and event status
4. Notifying you of changes

### Data Sources
- **Wise Old Man API**: Competition details, player rankings, and EHB data
- **Google Sheets**: Real-time BOTM scores and Hunt team scores
- **Config Sheet**: Clan announcements, login messages, and roll call status

### Smart Event Detection
The plugin intelligently determines event status:
- **Active Events**: Shows live countdown and updates leaderboards
- **Completed Events**: Displays winner information and final standings
- **Upcoming Events**: Shows when the event will start
- **No Event**: Indicates when no event is currently scheduled

## Installation

1. Open RuneLite
2. Click the Plugin Hub button (puzzle piece icon)
3. Search for "Flux"
4. Click "Install"
5. The plugin panel will appear in your sidebar

## Usage

### Viewing Competitions
1. Click the Flux icon in your RuneLite sidebar
2. Navigate between events using the footer buttons or dropdown menu
3. Leaderboards update automatically - no manual refresh needed

### Enabling the Overlay
1. Open the Flux plugin settings (gear icon)
2. Toggle "Display Overlay" to ON
3. Configure event passwords in the settings
4. The overlay will appear at the top-center of your game screen

### Admin Features
- Admin Hub automatically appears for Admiral+ ranks when you log in
- Make changes directly in the Admin Hub
- Updates sync across all plugin users within 10 seconds

## Event Password Display

The overlay intelligently shows passwords based on active events:
- **Event Password**: Always visible when configured
- **BOTM Password**: Only shown when BOTM is active
- **Hunt Passwords**: Only shown when The Hunt is active

## Competition Details

### What is SOTW?
Skill of the Week challenges clan members to gain the most experience in a specific skill over one week. The plugin tracks your progress and ranks you against other participants.

### What is BOTM?
Boss of the Month awards points for defeating specific bosses. Different bosses are worth different point values, and the player with the most points wins.

### What is The Hunt?
The Hunt is a team-based bossing competition where two teams compete for the highest total Efficient Hours Bossed (EHB). The plugin shows individual contributions and team totals.

## Privacy & Permissions

This plugin:
- ✅ Only accesses public Wise Old Man competition data
- ✅ Only reads from public Google Sheets
- ✅ Does NOT access your RuneScape account data
- ✅ Does NOT send any personal information anywhere
- ✅ Only checks your in-game clan rank to show/hide Admin Hub

## Support & Feedback

Having issues or suggestions?
- Join the Flux Discord server
- Report bugs through the RuneLite Discord
- Contact clan leadership

## Credits

Developed for the Flux OSRS Clan Community

**Special Thanks:**
- Wise Old Man for providing the competition API
- RuneLite team for the excellent plugin framework
- Flux clan leadership and members for feedback and testing

---

*This plugin is designed exclusively for Flux clan members. Some features require clan membership and specific ranks.*