package com.flux.services;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

@Slf4j
@Singleton
public class HuntAutoScreenshot {
    private static final String CONFIG_GROUP = "flux";
    private static final String SCREENSHOT_BASE_DIR = "Hunt_Screenshots";
    private static final String PENDING_DIR = "Pending Upload";
    private static final String UPLOADED_DIR = "Uploaded";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 10000; // 10 seconds between retries
    
    private final Client client;
    private final ConfigManager configManager;
    private final ImageCapture imageCapture;
    private final DrawManager drawManager;
    private final EventBus eventBus;
    private final ItemManager itemManager;
    private final OkHttpClient okHttpClient;
    private final HuntListSyncService huntListSyncService;
    private final net.runelite.client.callback.ClientThread clientThread;
    
    private boolean isRegistered = false;
    private final ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // Cached lists to avoid recreating HashSets on every loot event
    private Set<String> cachedMonsterList = new HashSet<>();
    private Set<String> cachedItemList = new HashSet<>();
    private Set<String> cachedWhitelist = new HashSet<>();
    private Set<String> cachedBlacklist = new HashSet<>();
    private String cachedDiscordWebhook = ""; // For logging only - always fetch fresh from sync service for uploads
    private long lastCacheUpdate = 0;
    private static final long CACHE_REFRESH_INTERVAL_MS = 60000; // Refresh cache every 60 seconds
    
    @Inject
    public HuntAutoScreenshot(
            Client client,
            ConfigManager configManager,
            ImageCapture imageCapture,
            DrawManager drawManager,
            ItemManager itemManager,
            OkHttpClient okHttpClient,
            HuntListSyncService huntListSyncService,
            net.runelite.client.callback.ClientThread clientThread,
            EventBus eventBus) {
        this.client = client;
        this.configManager = configManager;
        this.imageCapture = imageCapture;
        this.drawManager = drawManager;
        this.itemManager = itemManager;
        this.okHttpClient = okHttpClient;
        this.huntListSyncService = huntListSyncService;
        this.clientThread = clientThread;
        this.eventBus = eventBus;
    }
    
    public void startMonitoring() {
        if (!isRegistered) {
            eventBus.register(this);
            isRegistered = true;
            log.info("Hunt auto-screenshot monitoring started");
            
            // Force refresh cache from Google Sheets when starting
            forceRefreshCache();
            
            // Check for pending uploads and retry them
            retryPendingUploads();
            
            // Schedule periodic retry check (every 30 seconds)
            retryExecutor.scheduleAtFixedRate(this::retryPendingUploads, 30, 30, TimeUnit.SECONDS);
        }
    }
    
    public void stopMonitoring() {
        if (isRegistered) {
            eventBus.unregister(this);
            isRegistered = false;
            log.info("Hunt auto-screenshot monitoring stopped");
        }
    }
    
    /**
     * Force refresh cache immediately (used when plugin is toggled)
     */
    public void forceRefreshCache() {
        log.info("Force refreshing Hunt lists from Google Sheets (bypassing cooldown)...");
        
        // Force sync from Google Sheets (bypasses cooldown)
        boolean syncSuccess = huntListSyncService.forceSyncFromGoogleSheets();
        
        if (syncSuccess) {
            // Then update our cache with fresh data
            refreshCache();
            log.info("Hunt lists force refreshed successfully");
        } else {
            log.error("Failed to force refresh Hunt lists from Google Sheets");
        }
    }
    
    /**
     * Refresh cached lists from sync service
     */
    private void refreshCache() {
        cachedMonsterList = huntListSyncService.getMonsterList();
        cachedItemList = huntListSyncService.getItemList();
        cachedWhitelist = huntListSyncService.getWhitelist();
        cachedBlacklist = huntListSyncService.getBlacklist();
        cachedDiscordWebhook = huntListSyncService.getDiscordWebhook();
        lastCacheUpdate = System.currentTimeMillis();
        
        log.debug("Cached lists refreshed - Monsters: {}, Items: {}, Whitelist: {}, Blacklist: {}, Webhook: {}", 
            cachedMonsterList.size(), cachedItemList.size(), cachedWhitelist.size(), cachedBlacklist.size(),
            cachedDiscordWebhook.isEmpty() ? "NOT SET" : "SET");
    }
    
    /**
     * Check if cache needs refresh and update if needed (non-blocking check)
     */
    private void checkCacheRefresh() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_REFRESH_INTERVAL_MS) {
            refreshCache();
        }
    }
    
    @Subscribe
    public void onLootReceived(LootReceived lootReceived) {
        log.debug("LootReceived event fired - Source: {}", lootReceived.getName());
        
        // Only proceed if auto-screenshot is enabled
        if (!isAutoScreenshotEnabled()) {
            log.debug("Auto-screenshot is disabled");
            return;
        }
        
        // Refresh cache if needed (quick check, no blocking)
        checkCacheRefresh();
        
        // Check if player is allowed (whitelist/blacklist check) - use cached lists
        String playerName = client.getLocalPlayer() != null ? 
            client.getLocalPlayer().getName() : null;
        
        log.debug("Player name: {}", playerName);
        
        if (!isPlayerAllowed(playerName)) {
            log.debug("Player {} not allowed to post screenshots", playerName);
            return;
        }
        
        log.debug("Player {} is allowed", playerName);
        
        // Step 1: Check if the source (monster/activity) is in the cached Monster List
        String source = lootReceived.getName();
        if (!isMonitoredMonster(source)) {
            log.debug("Loot from non-monitored source: {}", source);
            log.debug("Current monster list: {}", cachedMonsterList);
            return;
        }
        
        log.info("Loot received from monitored source: {}", source);
        
        // Step 2: Check ALL items in the loot and collect matching ones
        List<String> matchedItems = new ArrayList<>();
        
        log.debug("Checking {} items against item list", lootReceived.getItems().size());
        
        for (ItemStack item : lootReceived.getItems()) {
            String itemName = itemManager.getItemComposition(item.getId()).getName();
            log.debug("Checking item: {}", itemName);
            
            if (isMonitoredItem(itemName)) {
                matchedItems.add(itemName);
                log.info("Monitored item detected: {}", itemName);
            }
        }
        
        if (matchedItems.isEmpty()) {
            log.debug("No monitored items found. Current item list: {}", cachedItemList);
            return;
        }
        
        // Create combined item name for filename and message
        String itemsForFilename = String.join("_and_", matchedItems);
        String itemsForMessage = formatItemList(matchedItems);
        
        log.info("Taking screenshot for {} from {} (items: {})", itemsForMessage, source, matchedItems.size());
        
        // Take screenshot with all matched items
        takeScreenshot("drop_" + sanitizeFileName(itemsForFilename) + "_from_" + sanitizeFileName(source), 
                      itemsForMessage, source);
    }
    
    /**
     * Format list of items for Discord message
     * Examples: 
     *   ["Bones"] -> "Bones"
     *   ["Bones", "Cowhide"] -> "Bones and Cowhide"
     *   ["Bones", "Cowhide", "Raw beef"] -> "Bones, Cowhide and Raw beef"
     */
    private String formatItemList(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        } else if (items.size() == 2) {
            return items.get(0) + " and " + items.get(1);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                if (i > 0 && i < items.size() - 1) {
                    sb.append(", ");
                } else if (i == items.size() - 1) {
                    sb.append(" and ");
                }
                sb.append(items.get(i));
            }
            return sb.toString();
        }
    }
    
    /**
     * Check if player is allowed using cached lists (fast, in-memory check)
     */
    private boolean isPlayerAllowed(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        String lowerUsername = username.toLowerCase();
        
        // If on blacklist, always deny
        if (cachedBlacklist.contains(lowerUsername)) {
            return false;
        }
        
        // If whitelist is empty, allow everyone (except blacklisted)
        if (cachedWhitelist.isEmpty()) {
            return true;
        }
        
        // Check if on whitelist
        return cachedWhitelist.contains(lowerUsername);
    }
    
    /**
     * Check if monster is monitored using cached lists (fast, in-memory check)
     */
    private boolean isMonitoredMonster(String monsterName) {
        if (monsterName == null || monsterName.isEmpty()) {
            return false;
        }
        
        for (String monster : cachedMonsterList) {
            if (monsterName.toLowerCase().contains(monster.toLowerCase()) ||
                monster.toLowerCase().contains(monsterName.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if item is monitored using cached lists (fast, in-memory check)
     */
    private boolean isMonitoredItem(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }
        
        for (String item : cachedItemList) {
            if (itemName.toLowerCase().contains(item.toLowerCase()) ||
                item.toLowerCase().contains(itemName.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean shouldScreenshotPets() {
        String petsSetting = configManager.getConfiguration(CONFIG_GROUP, "hunt_screenshot_pets");
        return petsSetting != null && Boolean.parseBoolean(petsSetting);
    }
    
    private void takeScreenshot(String prefix, String itemName, String monsterName) {
        // Request screenshot from draw manager (must be on client thread)
        drawManager.requestNextFrameListener(image -> {
            // Convert to BufferedImage (quick operation)
            BufferedImage screenshot;
            if (image instanceof BufferedImage) {
                screenshot = (BufferedImage) image;
            } else {
                screenshot = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB
                );
                java.awt.Graphics2D g = screenshot.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            
            // Get metadata
            String playerName = client.getLocalPlayer() != null ? 
                client.getLocalPlayer().getName() : "Unknown";
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            String filename = String.format("%s_%s_%s.png", 
                prefix, sanitizeFileName(playerName), timestamp);
            
            // Do ALL file I/O and Discord upload on background thread (non-blocking)
            new Thread(() -> {
                try {
                    // Save to Pending Upload directory
                    saveToPendingUpload(screenshot, filename, playerName);
                    
                    // Attempt Discord upload if enabled
                    if (shouldAutoPostDiscord()) {
                        uploadToDiscordWithRetry(filename, playerName, itemName, monsterName, 0);
                    } else {
                        log.info("Auto-post to Discord is disabled. Screenshot saved to Pending Upload.");
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing screenshot", e);
                    sendChatMessage("Failed to capture screenshot!");
                }
            }, "HuntScreenshot-" + System.currentTimeMillis()).start();
        });
    }
    
    private Path getPendingUploadDir(String playerName) {
        Path runeliteDir = Paths.get(System.getProperty("user.home"), ".runelite");
        return runeliteDir.resolve("screenshots").resolve(playerName).resolve(SCREENSHOT_BASE_DIR).resolve(PENDING_DIR);
    }
    
    private Path getUploadedDir(String playerName) {
        Path runeliteDir = Paths.get(System.getProperty("user.home"), ".runelite");
        return runeliteDir.resolve("screenshots").resolve(playerName).resolve(SCREENSHOT_BASE_DIR).resolve(UPLOADED_DIR);
    }
    
    private void saveToPendingUpload(BufferedImage screenshot, String filename, String playerName) throws IOException {
        Path pendingDir = getPendingUploadDir(playerName);
        Files.createDirectories(pendingDir);
        
        Path screenshotPath = pendingDir.resolve(filename);
        ImageIO.write(screenshot, "png", screenshotPath.toFile());
        
        log.info("Screenshot saved to Pending Upload: {}", filename);
    }
    
    private void uploadToDiscordWithRetry(String filename, String playerName, String itemName, String monsterName, int attemptNumber) {
        // Always get fresh webhook URL from sync service (no caching for webhook)
        String webhookUrl = huntListSyncService.getDiscordWebhook();
        
        // Check if we have a webhook URL
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.error("No Discord webhook URL found in Google Sheets! Cannot upload screenshot.");
            sendChatMessage("Discord webhook not configured!");
            return;
        }
        
        log.debug("Using webhook URL from Google Sheets for upload attempt {}", attemptNumber + 1);
        
        Path pendingDir = getPendingUploadDir(playerName);
        Path uploadedDir = getUploadedDir(playerName);
        Path pendingFile = pendingDir.resolve(filename);
        
        try {
            // Check if file still exists in pending
            if (!Files.exists(pendingFile)) {
                log.debug("File no longer in pending directory: {}", filename);
                return;
            }
            
            // Read file bytes
            byte[] imageBytes = Files.readAllBytes(pendingFile);
            
            // Create Discord message content
            String discordMessage = String.format("**%s** has received **%s** from **%s**!", 
                playerName, itemName, monsterName);
            
            // Create multipart request body
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename,
                    RequestBody.create(MediaType.parse("image/png"), imageBytes))
                .addFormDataPart("content", discordMessage)
                .build();
            
            // Create request with fresh webhook from Google Sheets
            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build();
            
            // Send synchronously to check response
            Response response = null;
            boolean uploadSuccess = false;
            
            try {
                response = okHttpClient.newCall(request).execute();
                uploadSuccess = response.isSuccessful();
                
                if (uploadSuccess) {
                    log.info("Successfully uploaded screenshot to Discord: {}", filename);
                    sendChatMessage("Screenshot uploaded to Discord!");
                } else {
                    log.warn("Discord webhook returned status {}: {}", response.code(), response.message());
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            
            // Wait a moment for any file locks to release
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted during sleep", e);
            }
            
            // Force garbage collection to release any file handles
            System.gc();
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Now handle file operations after response is fully closed
            if (uploadSuccess) {
                log.info("Upload successful, proceeding with file operations. Save local: {}", shouldSaveLocal());
                log.info("Pending file path: {}", pendingFile);
                log.info("Pending file exists: {}", Files.exists(pendingFile));
                
                try {
                    // Verify file exists
                    if (!Files.exists(pendingFile)) {
                        log.warn("Pending file no longer exists, skipping file operations");
                        return;
                    }
                    
                    if (shouldSaveLocal()) {
                        // Move to Uploaded directory
                        Files.createDirectories(uploadedDir);
                        Path uploadedFile = uploadedDir.resolve(filename);
                        
                        log.info("Copying to: {}", uploadedFile);
                        
                        // Copy to uploaded directory
                        Files.copy(pendingFile, uploadedFile, StandardCopyOption.REPLACE_EXISTING);
                        log.info("File copied successfully");
                        
                        // Try multiple times to delete
                        boolean deleted = false;
                        for (int i = 0; i < 5; i++) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            deleted = pendingFile.toFile().delete();
                            if (deleted) {
                                log.info("Screenshot moved to Uploaded directory: {}", uploadedFile);
                                break;
                            } else {
                                log.warn("Delete attempt {} failed, retrying...", i + 1);
                                System.gc(); // Force GC between attempts
                            }
                        }
                        
                        if (!deleted) {
                            log.error("FAILED to delete from Pending after 5 attempts: {} - File still exists: {}", 
                                filename, Files.exists(pendingFile));
                        }
                    } else {
                        // Delete from Pending Upload
                        log.info("Attempting to delete from Pending Upload: {}", pendingFile);
                        
                        // Try multiple times to delete
                        boolean deleted = false;
                        for (int i = 0; i < 5; i++) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            deleted = pendingFile.toFile().delete();
                            if (deleted) {
                                log.info("Screenshot deleted from Pending Upload (save local disabled): {}", filename);
                                break;
                            } else {
                                log.warn("Delete attempt {} failed, retrying...", i + 1);
                                System.gc(); // Force GC between attempts
                            }
                        }
                        
                        if (!deleted) {
                            log.error("FAILED to delete from Pending after 5 attempts: {} - File still exists: {}", 
                                filename, Files.exists(pendingFile));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error moving/deleting screenshot after successful upload: {}", filename, e);
                }
            } else {
                // Upload failed, retry
                handleUploadFailure(filename, playerName, itemName, monsterName, attemptNumber);
            }
            
        } catch (IOException e) {
            log.error("Error uploading screenshot to Discord (attempt {})", attemptNumber + 1, e);
            handleUploadFailure(filename, playerName, itemName, monsterName, attemptNumber);
        }
    }
    
    private void handleUploadFailure(String filename, String playerName, String itemName, String monsterName, int attemptNumber) {
        if (attemptNumber < MAX_RETRY_ATTEMPTS - 1) {
            int nextAttempt = attemptNumber + 1;
            log.info("Retrying upload in {} seconds (attempt {}/{})", 
                RETRY_DELAY_MS / 1000, nextAttempt + 1, MAX_RETRY_ATTEMPTS);
            
            // Schedule retry
            retryExecutor.schedule(() -> {
                uploadToDiscordWithRetry(filename, playerName, itemName, monsterName, nextAttempt);
            }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
            
        } else {
            log.error("Failed to upload screenshot after {} attempts: {}", MAX_RETRY_ATTEMPTS, filename);
            sendChatMessage("Screenshot upload failed! Check Pending Upload folder.");
        }
    }
    
    private void retryPendingUploads() {
        try {
            String playerName = client.getLocalPlayer() != null ? 
                client.getLocalPlayer().getName() : null;
                
            if (playerName == null) {
                return; // Not logged in
            }
            
            Path pendingDir = getPendingUploadDir(playerName);
            
            // Check if pending directory exists
            if (!Files.exists(pendingDir)) {
                return;
            }
            
            // Find all PNG files in pending directory
            File[] pendingFiles = pendingDir.toFile().listFiles((dir, name) -> name.endsWith(".png"));
            
            if (pendingFiles != null && pendingFiles.length > 0) {
                log.info("Found {} pending screenshot(s) to retry for player {}", pendingFiles.length, playerName);
                
                // Force refresh from Google Sheets to get latest webhook and lists
                log.info("Refreshing from Google Sheets before retry...");
                huntListSyncService.forceSyncFromGoogleSheets();
                refreshCache();
                
                for (File file : pendingFiles) {
                    String filename = file.getName();
                    
                    // Parse filename to extract info
                    // Format: drop_ItemName_from_MonsterName_PlayerName_timestamp.png
                    String itemName = "Item";
                    String monsterName = "Monster";
                    
                    try {
                        // Remove .png extension and split by underscore
                        String nameWithoutExt = filename.replace(".png", "");
                        String[] parts = nameWithoutExt.split("_");
                        
                        // Find "from" separator
                        int fromIndex = -1;
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("from")) {
                                fromIndex = i;
                                break;
                            }
                        }
                        
                        if (fromIndex > 1 && parts.length > fromIndex + 1) {
                            // Item name is between "drop" and "from"
                            // drop_Dragon_warhammer_from_Vorkath_Player_timestamp
                            // parts[1] to parts[fromIndex-1] is the item name
                            StringBuilder itemBuilder = new StringBuilder();
                            for (int i = 1; i < fromIndex; i++) {
                                if (itemBuilder.length() > 0) {
                                    itemBuilder.append(" ");
                                }
                                itemBuilder.append(parts[i]);
                            }
                            itemName = itemBuilder.toString();
                            
                            // Monster name is between "from" and player name
                            // We need to find where the player name starts
                            // Player name appears twice: once after monster, once in timestamp
                            // Look for the player name by checking if a part matches the known playerName
                            int playerNameIndex = -1;
                            for (int i = fromIndex + 1; i < parts.length; i++) {
                                if (parts[i].equals(playerName)) {
                                    playerNameIndex = i;
                                    break;
                                }
                            }
                            
                            // If we found the player name, monster is from "from" to player name
                            // Otherwise, monster is from "from" to 3rd from last (before date)
                            int monsterEndIndex = playerNameIndex > 0 ? playerNameIndex : parts.length - 3;
                            
                            StringBuilder monsterBuilder = new StringBuilder();
                            for (int i = fromIndex + 1; i < monsterEndIndex; i++) {
                                if (monsterBuilder.length() > 0) {
                                    monsterBuilder.append(" ");
                                }
                                monsterBuilder.append(parts[i]);
                            }
                            
                            if (monsterBuilder.length() > 0) {
                                monsterName = monsterBuilder.toString();
                            }
                        }
                        
                        log.debug("Parsed filename '{}': item='{}', monster='{}'", filename, itemName, monsterName);
                        
                    } catch (Exception e) {
                        log.warn("Failed to parse filename '{}', using defaults", filename, e);
                    }
                    
                    // Retry upload with parsed names
                    uploadToDiscordWithRetry(filename, playerName, itemName, monsterName, 0);
                }
            }
            
        } catch (Exception e) {
            log.error("Error retrying pending uploads", e);
        }
    }
    
    private void sendChatMessage(String message) {
        if (!shouldShowNotifications()) {
            return;
        }
        
        // Must be called on client thread
        if (client.getGameState() == GameState.LOGGED_IN) {
            // Schedule on client thread
            clientThread.invokeLater(() -> {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
            });
        }
    }
    
    private String sanitizeFileName(String name) {
        // Remove invalid filename characters
        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
    
    private boolean isAutoScreenshotEnabled() {
        String enabled = configManager.getConfiguration(CONFIG_GROUP, "hunt_auto_screenshot");
        return enabled != null && Boolean.parseBoolean(enabled);
    }
    
    private boolean shouldAutoPostDiscord() {
        String autoPost = configManager.getConfiguration(CONFIG_GROUP, "hunt_auto_post_discord");
        return autoPost == null || Boolean.parseBoolean(autoPost); // Default to true
    }
    
    private boolean shouldSaveLocal() {
        String saveLocal = configManager.getConfiguration(CONFIG_GROUP, "hunt_screenshot_save_local");
        // Default to true if not set
        return saveLocal == null || saveLocal.isEmpty() || Boolean.parseBoolean(saveLocal);
    }
    
    private boolean shouldShowNotifications() {
        String showNotifs = configManager.getConfiguration(CONFIG_GROUP, "hunt_screenshot_notifications");
        return showNotifs == null || Boolean.parseBoolean(showNotifs);
    }
}
