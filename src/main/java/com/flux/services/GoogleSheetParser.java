package com.flux.services;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.json.JSONArray;
import org.json.JSONObject;
import net.runelite.client.config.ConfigManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GoogleSheetParser {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final String API_KEY = "AIzaSyBu-qDCAFvD_z00uohkfD_ub0sZj-H8s1E";
    private static final String SPREADSHEET_ID = "1qqkjx4YjuQ9FIBDgAGzSpmoKcDow3yEa9lYFmc-JeDA";

    private final ConfigManager configManager;
    private Consumer<JSONArray> leaderboardCallback;
    private Consumer<Map<String, Integer>> huntScoreCallback;
    private Consumer<Map<String, String>> configCallback;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SheetType sheetType;

    public enum SheetType {
        BOTM,
        HUNT,
        CONFIG
    }

    // Constructor for BOTM leaderboard
    public GoogleSheetParser(ConfigManager configManager, Consumer<JSONArray> leaderboardCallback) {
        this.configManager = configManager;
        this.leaderboardCallback = leaderboardCallback;
        this.sheetType = SheetType.BOTM;
    }

    // Constructor for Hunt scores
    public GoogleSheetParser(ConfigManager configManager, SheetType type, Consumer<Map<String, Integer>> huntScoreCallback) {
        this.configManager = configManager;
        this.huntScoreCallback = huntScoreCallback;
        this.sheetType = type;
    }

    // Constructor for Config values
    public GoogleSheetParser(ConfigManager configManager, SheetType type, Consumer<Map<String, String>> configCallback, boolean isConfigSheet) {
        this.configManager = configManager;
        this.configCallback = configCallback;
        this.sheetType = type;
    }

    private static Sheets getSheets() {
        NetHttpTransport transport = new NetHttpTransport.Builder().build();
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        HttpRequestInitializer httpRequestInitializer = request -> request
                .setInterceptor(intercepted -> intercepted.getUrl().set("key", API_KEY));
        return new Sheets.Builder(transport, jsonFactory, httpRequestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static ValueRange getValues(String range) throws IOException {
        return getSheets()
                .spreadsheets()
                .values()
                .get(SPREADSHEET_ID, range)
                .execute();
    }

    public static JSONArray parseTop10Leaderboard() {
        JSONArray leaderboard = new JSONArray();

        try {
            ValueRange response = getValues("BOTM");
            List<List<Object>> values = response.getValues();

            if (values != null && !values.isEmpty()) {
                int startRow = findLeaderboardStartRow(values);
                if (startRow >= 0) {
                    List<Object> headers = values.get(startRow - 1);
                    Map<String, Integer> headerIndexMap = mapHeaders(headers);

                    for (int i = startRow; i < values.size(); i++) {
                        List<Object> row = values.get(i);
                        if (row.size() >= headerIndexMap.size()) {
                            JSONObject playerData = new JSONObject();
                            if (headerIndexMap.containsKey("Rank") && row.size() > headerIndexMap.get("Rank"))
                                playerData.put("rank", String.valueOf(row.get(headerIndexMap.get("Rank"))));
                            if (headerIndexMap.containsKey("Players") && row.size() > headerIndexMap.get("Players"))
                                playerData.put("username", String.valueOf(row.get(headerIndexMap.get("Players"))));
                            if (headerIndexMap.containsKey("Points") && row.size() > headerIndexMap.get("Points"))
                                playerData.put("score", Integer.parseInt(String.valueOf(row.get(headerIndexMap.get("Points")))));
                            if (headerIndexMap.containsKey("KC") && row.size() > headerIndexMap.get("KC"))
                                playerData.put("kc", Integer.parseInt(String.valueOf(row.get(headerIndexMap.get("KC")))));

                            leaderboard.put(playerData);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching Google Sheets data: " + e.getMessage());
        }

        if (leaderboard.length() > 10) {
            leaderboard = new JSONArray(leaderboard.toList().subList(0, 10));
        }

        return leaderboard;
    }

    public static Map<String, Integer> parseHuntScores() {
        Map<String, Integer> scores = new HashMap<>();

        try {
            ValueRange response = getValues("Hunt");
            List<List<Object>> values = response.getValues();

            if (values != null && !values.isEmpty()) {
                int scoreStartRow = findCurrentScoreSection(values);

                if (scoreStartRow >= 0) {
                    System.out.println("Found Current Score section at row: " + scoreStartRow);

                    int dataStartRow = scoreStartRow + 2;

                    for (int i = dataStartRow; i < values.size() && i < dataStartRow + 10; i++) {
                        List<Object> row = values.get(i);

                        if (row.size() >= 2) {
                            String teamName = String.valueOf(row.get(0)).trim();
                            String pointsStr = String.valueOf(row.get(1)).trim();

                            if (teamName.isEmpty() || pointsStr.isEmpty()) {
                                break;
                            }

                            try {
                                int points = Integer.parseInt(pointsStr);
                                scores.put(teamName, points);
                                System.out.println("Parsed Hunt Score: " + teamName + " = " + points);
                            } catch (NumberFormatException e) {
                                System.err.println("Failed to parse points for team: " + teamName + ", value: " + pointsStr);
                            }
                        }
                    }
                } else {
                    System.err.println("Could not find 'Current Score' section in Hunt sheet");
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching Hunt scores from Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }

        return scores;
    }

    public static Map<String, String> parseConfigValues() {
        Map<String, String> configValues = new HashMap<>();

        try {
            ValueRange response = getValues("Config");
            List<List<Object>> values = response.getValues();

            if (values != null && !values.isEmpty()) {
                System.out.println("Parsing Config sheet...");

                // Parse key-value pairs from Config sheet
                // Expected format: Key in column A, Value in column B
                for (int i = 0; i < values.size(); i++) {
                    List<Object> row = values.get(i);

                    if (row.size() >= 2) {
                        String key = String.valueOf(row.get(0)).trim();
                        String value = String.valueOf(row.get(1)).trim();

                        // Skip empty rows or header rows
                        if (key.isEmpty() || key.equalsIgnoreCase("key") || key.equalsIgnoreCase("config")) {
                            continue;
                        }

                        // Look for specific config keys
                        if (key.equalsIgnoreCase("LOGIN_MESSAGE") ||
                                key.equalsIgnoreCase("ANNOUNCEMENT_MESSAGE") ||
                                key.equalsIgnoreCase("ROLL_CALL_ACTIVE")) {

                            configValues.put(key.toUpperCase(), value);
                            System.out.println("Parsed Config: " + key + " = " + value);
                        }
                    }
                }

                System.out.println("Config parsing complete. Found " + configValues.size() + " values.");
            }
        } catch (IOException e) {
            System.err.println("Error fetching Config values from Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }

        return configValues;
    }

    private static int findCurrentScoreSection(List<List<Object>> values) {
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (!row.isEmpty()) {
                String cellValue = String.valueOf(row.get(0)).trim().toLowerCase();
                if (cellValue.contains("current score")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findLeaderboardStartRow(List<List<Object>> values) {
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 4) {
                if (row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("Rank")) &&
                        row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("Points")) &&
                        row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("Players")) &&
                        row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("KC"))) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private static Map<String, Integer> mapHeaders(List<Object> headers) {
        Map<String, Integer> headerIndexMap = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toString().trim();
            if (header.equalsIgnoreCase("Rank")) {
                headerIndexMap.put("Rank", i);
            } else if (header.equalsIgnoreCase("Players")) {
                headerIndexMap.put("Players", i);
            } else if (header.equalsIgnoreCase("Points")) {
                headerIndexMap.put("Points", i);
            } else if (header.equalsIgnoreCase("KC")) {
                headerIndexMap.put("KC", i);
            }
        }
        return headerIndexMap;
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            if (sheetType == SheetType.HUNT) {
                new Thread(this::pollHuntScores).start();
            } else if (sheetType == SheetType.CONFIG) {
                new Thread(this::pollConfigValues).start();
            } else {
                new Thread(this::pollLeaderboard).start();
            }
        }
    }

    public void setLeaderboardCallback(Consumer<JSONArray> leaderboardCallback) {
        this.leaderboardCallback = leaderboardCallback;
    }

    public void setHuntScoreCallback(Consumer<Map<String, Integer>> huntScoreCallback) {
        this.huntScoreCallback = huntScoreCallback;
    }

    public void setConfigCallback(Consumer<Map<String, String>> configCallback) {
        this.configCallback = configCallback;
    }

    private void pollLeaderboard() {
        while (isRunning.get()) {
            try {
                JSONArray leaderboard = parseTop10Leaderboard();
                if (leaderboardCallback != null) {
                    leaderboardCallback.accept(leaderboard);
                }

                Thread.sleep(5000); // 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void pollHuntScores() {
        while (isRunning.get()) {
            try {
                Map<String, Integer> scores = parseHuntScores();
                if (huntScoreCallback != null && !scores.isEmpty()) {
                    huntScoreCallback.accept(scores);
                }

                Thread.sleep(5000); // 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void pollConfigValues() {
        while (isRunning.get()) {
            try {
                Map<String, String> configValues = parseConfigValues();
                if (configCallback != null && !configValues.isEmpty()) {
                    configCallback.accept(configValues);
                }

                Thread.sleep(10000); // 10 seconds (config changes less frequently)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        isRunning.set(false);
    }
}