package com.flux.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Singleton
public class HuntListSyncService {
    private static final String API_KEY = "AIzaSyBu-qDCAFvD_z00uohkfD_ub0sZj-H8s1E";
    private static final String SPREADSHEET_ID = "1qqkjx4YjuQ9FIBDgAGzSpmoKcDow3yEa9lYFmc-JeDA";
    private static final String SHEET_NAME = "Hunt";
    private static final long SYNC_COOLDOWN_MS = 60000; // 60 seconds
    private static final long MANUAL_SYNC_COOLDOWN_MS = 5000; // 5 seconds for manual sync button
    
    private final OkHttpClient httpClient;
    
    // In-memory storage
    private Set<String> monsterList = new HashSet<>();
    private Set<String> itemList = new HashSet<>();
    private Set<String> whitelist = new HashSet<>();
    private Set<String> blacklist = new HashSet<>();
    private String discordWebhook = "";
    
    private long lastSyncTime = 0;
    private long lastManualSyncTime = 0;
    private boolean hasInitialSync = false;
    private volatile boolean syncInProgress = false;
    
    @Inject
    public HuntListSyncService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    /**
     * Sync all lists from Google Sheets
     * @return true if sync was successful, false if on cooldown or failed
     */
    public boolean syncFromGoogleSheets() {
        return syncFromGoogleSheets(false);
    }
    
    /**
     * Force sync all lists from Google Sheets, bypassing cooldown
     * Sets manual sync timestamp for button cooldown
     * @return true if sync was successful, false if failed
     */
    public boolean forceSyncFromGoogleSheets() {
        // Check if sync already in progress
        if (syncInProgress) {
            log.warn("Sync already in progress, please wait...");
            return false;
        }
        
        // Update manual sync time
        lastManualSyncTime = System.currentTimeMillis();
        
        return syncFromGoogleSheets(true);
    }
    
    /**
     * Sync all lists from Google Sheets
     * @param bypassCooldown if true, ignores the cooldown timer
     * @return true if sync was successful, false if on cooldown or failed
     */
    private boolean syncFromGoogleSheets(boolean bypassCooldown) {
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown (except for first sync or if bypassing)
        if (!bypassCooldown && hasInitialSync && (currentTime - lastSyncTime) < SYNC_COOLDOWN_MS) {
            long remainingSeconds = (SYNC_COOLDOWN_MS - (currentTime - lastSyncTime)) / 1000;
            log.warn("Sync on cooldown. Try again in {} seconds.", remainingSeconds);
            return false;
        }
        
        // Set sync in progress flag
        syncInProgress = true;
        
        try {
            log.info("Syncing Hunt lists from Google Sheets...");
            
            // Fetch the sheet data
            JsonArray rows = fetchSheetData();
            
            if (rows == null || rows.size() == 0) {
                log.error("No data returned from Google Sheets");
                return false;
            }
            
            // Parse the data
            parseSheetData(rows);
            
            lastSyncTime = currentTime;
            hasInitialSync = true;
            
            log.info("Successfully synced Hunt lists:");
            log.info("  Monsters: {}", monsterList.size());
            log.info("  Items: {}", itemList.size());
            log.info("  Whitelist: {}", whitelist.size());
            log.info("  Blacklist: {}", blacklist.size());
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to sync from Google Sheets", e);
            return false;
        } finally {
            // Always clear sync in progress flag
            syncInProgress = false;
        }
    }
    
    private JsonArray fetchSheetData() throws IOException {
        // Build the API URL
        HttpUrl url = new HttpUrl.Builder()
            .scheme("https")
            .host("sheets.googleapis.com")
            .addPathSegment("v4")
            .addPathSegment("spreadsheets")
            .addPathSegment(SPREADSHEET_ID)
            .addPathSegment("values")
            .addPathSegment(SHEET_NAME)
            .addQueryParameter("key", API_KEY)
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Google Sheets API returned status {}", response.code());
                return null;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                log.error("Empty response from Google Sheets API");
                return null;
            }
            
            String jsonString = body.string();
            JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
            
            if (jsonObject.has("values")) {
                return jsonObject.getAsJsonArray("values");
            } else {
                log.error("No 'values' field in Google Sheets response");
                return null;
            }
        }
    }
    
    private void parseSheetData(JsonArray rows) {
        // Clear existing lists
        Set<String> newMonsterList = new HashSet<>();
        Set<String> newItemList = new HashSet<>();
        Set<String> newWhitelist = new HashSet<>();
        Set<String> newBlacklist = new HashSet<>();
        String newWebhook = "";
        
        // Find column headers (first row)
        if (rows.size() < 1) {
            log.error("Sheet has no rows");
            return;
        }
        
        JsonArray headerRow = rows.get(0).getAsJsonArray();
        int monsterCol = -1;
        int itemCol = -1;
        int whitelistCol = -1;
        int blacklistCol = -1;
        
        // Find column indices
        for (int i = 0; i < headerRow.size(); i++) {
            String header = headerRow.get(i).getAsString().toLowerCase().trim();
            
            if (header.contains("monster")) {
                monsterCol = i;
            } else if (header.contains("item")) {
                itemCol = i;
            } else if (header.contains("whitelist") || header.contains("white list")) {
                whitelistCol = i;
            } else if (header.contains("blacklist") || header.contains("black list")) {
                blacklistCol = i;
            }
        }
        
        log.debug("Column indices - Monster: {}, Item: {}, Whitelist: {}, Blacklist: {}", 
            monsterCol, itemCol, whitelistCol, blacklistCol);
        
        // Check for webhook in cell B4 (row index 3, column index 1)
        // A4 should contain key "DISCORD_WEBHOOK_URL"
        if (rows.size() >= 4) {
            JsonArray row4 = rows.get(3).getAsJsonArray(); // Row 4 (0-indexed as 3)
            
            // Check if A4 has the key
            if (row4.size() >= 1) {
                String keyCell = row4.get(0).getAsString().trim();
                if (keyCell.equals("DISCORD_WEBHOOK_URL")) {
                    // Get value from B4
                    if (row4.size() >= 2) {
                        String webhookUrl = row4.get(1).getAsString().trim();
                        if (!webhookUrl.isEmpty() && webhookUrl.startsWith("https://")) {
                            newWebhook = webhookUrl;
                            log.info("Found Discord webhook in B4: {}", webhookUrl.substring(0, Math.min(50, webhookUrl.length())) + "...");
                        }
                    }
                }
            }
        }
        
        // Parse data rows (skip header row)
        for (int i = 1; i < rows.size(); i++) {
            JsonArray row = rows.get(i).getAsJsonArray();
            
            // Parse monster list
            if (monsterCol >= 0 && monsterCol < row.size()) {
                String monster = row.get(monsterCol).getAsString().trim();
                if (!monster.isEmpty()) {
                    newMonsterList.add(monster);
                }
            }
            
            // Parse item list
            if (itemCol >= 0 && itemCol < row.size()) {
                String item = row.get(itemCol).getAsString().trim();
                if (!item.isEmpty()) {
                    newItemList.add(item);
                }
            }
            
            // Parse whitelist
            if (whitelistCol >= 0 && whitelistCol < row.size()) {
                String username = row.get(whitelistCol).getAsString().trim();
                if (!username.isEmpty()) {
                    newWhitelist.add(username.toLowerCase()); // Store lowercase for case-insensitive comparison
                }
            }
            
            // Parse blacklist
            if (blacklistCol >= 0 && blacklistCol < row.size()) {
                String username = row.get(blacklistCol).getAsString().trim();
                if (!username.isEmpty()) {
                    newBlacklist.add(username.toLowerCase()); // Store lowercase for case-insensitive comparison
                }
            }
        }
        
        // Update in-memory lists
        monsterList = newMonsterList;
        itemList = newItemList;
        whitelist = newWhitelist;
        blacklist = newBlacklist;
        discordWebhook = newWebhook;
        
        if (!discordWebhook.isEmpty()) {
            log.info("Discord webhook loaded from Google Sheets (B4)");
        } else {
            log.warn("No Discord webhook found in B4! Expected key 'DISCORD_WEBHOOK_URL' in A4");
        }
    }
    
    /**
     * Check if a player is allowed to post screenshots
     * @param username Player username
     * @return true if allowed (on whitelist and not on blacklist)
     */
    public boolean isPlayerAllowed(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        String lowerUsername = username.toLowerCase();
        
        // If on blacklist, always deny
        if (blacklist.contains(lowerUsername)) {
            log.debug("Player {} is on blacklist - screenshot blocked", username);
            return false;
        }
        
        // If whitelist is empty, allow everyone (except blacklisted)
        if (whitelist.isEmpty()) {
            return true;
        }
        
        // Check if on whitelist
        boolean allowed = whitelist.contains(lowerUsername);
        if (!allowed) {
            log.debug("Player {} is not on whitelist - screenshot blocked", username);
        }
        
        return allowed;
    }
    
    /**
     * Check if a monster is in the monitored list
     */
    public boolean isMonitoredMonster(String monsterName) {
        if (monsterName == null || monsterName.isEmpty()) {
            return false;
        }
        
        for (String monster : monsterList) {
            if (monsterName.toLowerCase().contains(monster.toLowerCase()) ||
                monster.toLowerCase().contains(monsterName.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if an item is in the monitored list
     */
    public boolean isMonitoredItem(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }
        
        for (String item : itemList) {
            if (itemName.toLowerCase().contains(item.toLowerCase()) ||
                item.toLowerCase().contains(itemName.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get time remaining until next sync is allowed
     * @return seconds remaining, or 0 if sync is available
     */
    public long getSecondsUntilNextSync() {
        if (!hasInitialSync) {
            return 0; // Allow immediate first sync
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSync = currentTime - lastSyncTime;
        
        if (timeSinceLastSync >= SYNC_COOLDOWN_MS) {
            return 0;
        }
        
        return (SYNC_COOLDOWN_MS - timeSinceLastSync) / 1000;
    }
    
    /**
     * Check if manual sync from button is allowed
     * Requires both time-based cooldown AND previous sync to be complete
     * @return true if manual sync is allowed
     */
    public boolean canManualSync() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastManualSync = currentTime - lastManualSyncTime;
        
        // Check 1: Time-based cooldown (5 seconds)
        if (timeSinceLastManualSync < MANUAL_SYNC_COOLDOWN_MS) {
            return false;
        }
        
        // Check 2: No sync currently in progress
        if (syncInProgress) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get seconds remaining until manual sync is available
     * Returns 0 if sync is available
     */
    public long getSecondsUntilManualSync() {
        if (syncInProgress) {
            return -1; // Special value indicating sync in progress
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastManualSync = currentTime - lastManualSyncTime;
        
        if (timeSinceLastManualSync >= MANUAL_SYNC_COOLDOWN_MS) {
            return 0;
        }
        
        return (MANUAL_SYNC_COOLDOWN_MS - timeSinceLastManualSync) / 1000;
    }
    
    // Getters for debugging/display
    public Set<String> getMonsterList() {
        return new HashSet<>(monsterList);
    }
    
    public Set<String> getItemList() {
        return new HashSet<>(itemList);
    }
    
    public Set<String> getWhitelist() {
        return new HashSet<>(whitelist);
    }
    
    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }
    
    public String getDiscordWebhook() {
        return discordWebhook;
    }
    
    public boolean hasInitialSync() {
        return hasInitialSync;
    }
}
