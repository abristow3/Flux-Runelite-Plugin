package com.flux.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.client.config.ConfigManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Inject;

@Slf4j
public class GoogleSheetParser {
    private static final String API_KEY = "AIzaSyBu-qDCAFvD_z00uohkfD_ub0sZj-H8s1E";
    private static final String SPREADSHEET_ID = "1qqkjx4YjuQ9FIBDgAGzSpmoKcDow3yEa9lYFmc-JeDA";

    private final ConfigManager configManager;
    private Consumer<JsonArray> leaderboardCallback;
    private Consumer<Map<String, Integer>> huntScoreCallback;
    private Consumer<Map<String, String>> configCallback;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SheetType sheetType;

    @Inject
    private OkHttpClient httpClient;

    public enum SheetType {
        BOTM,
        HUNT,
        CONFIG
    }

    public GoogleSheetParser(ConfigManager configManager, Consumer<JsonArray> leaderboardCallback) {
        this.configManager = configManager;
        this.leaderboardCallback = leaderboardCallback;
        this.sheetType = SheetType.BOTM;
    }

    public GoogleSheetParser(ConfigManager configManager, SheetType type, Consumer<Map<String, Integer>> huntScoreCallback) {
        this.configManager = configManager;
        this.huntScoreCallback = huntScoreCallback;
        this.sheetType = type;
    }

    public GoogleSheetParser(ConfigManager configManager, SheetType type, Consumer<Map<String, String>> configCallback, boolean isConfigSheet) {
        this.configManager = configManager;
        this.configCallback = configCallback;
        this.sheetType = type;
    }

    private String makeSheetsApiRequest(String range) throws IOException {
        String url = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/" + range + "?key=" + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Request failed with status code: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }

            return response.body().string();
        }
    }

    public static JsonArray getValues(String range) throws IOException {
        GoogleSheetParser googleSheetParser = new GoogleSheetParser(null, null);
        String jsonResponse = googleSheetParser.makeSheetsApiRequest(range);

        JsonObject jsonObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
        JsonArray values = jsonObject.getAsJsonArray("values");

        return values;
    }

    public static JsonArray parseTop10Leaderboard() {
        JsonArray leaderboard = new JsonArray();

        try {
            JsonArray jsonArray = getValues("BOTM");

            List<List<Object>> values = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray row = jsonArray.get(i).getAsJsonArray();
                List<Object> rowValues = new ArrayList<>();
                for (int j = 0; j < row.size(); j++) {
                    rowValues.add(row.get(j));
                }
                values.add(rowValues);
            }

            if (values != null && !values.isEmpty()) {
                int startRow = findLeaderboardStartRow(values);
                if (startRow >= 0) {
                    List<Object> headers = values.get(startRow - 1);
                    Map<String, Integer> headerIndexMap = mapHeaders(headers);

                    for (int i = startRow; i < values.size(); i++) {
                        List<Object> row = values.get(i);
                        if (row.size() >= headerIndexMap.size()) {
                            JsonObject playerData = new JsonObject();
                            if (headerIndexMap.containsKey("Rank") && row.size() > headerIndexMap.get("Rank"))
                                playerData.addProperty("rank", String.valueOf(row.get(headerIndexMap.get("Rank"))));
                            if (headerIndexMap.containsKey("Players") && row.size() > headerIndexMap.get("Players"))
                                playerData.addProperty("username", String.valueOf(row.get(headerIndexMap.get("Players"))));
                            if (headerIndexMap.containsKey("Points") && row.size() > headerIndexMap.get("Points"))
                                playerData.addProperty("score", Integer.parseInt(String.valueOf(row.get(headerIndexMap.get("Points")))));
                            if (headerIndexMap.containsKey("KC") && row.size() > headerIndexMap.get("KC"))
                                playerData.addProperty("kc", Integer.parseInt(String.valueOf(row.get(headerIndexMap.get("KC")))));

                            leaderboard.add(playerData);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error fetching Google Sheets data", e);
        }

        if (leaderboard.size() > 10) {
            JsonArray top10 = new JsonArray();
            for (int i = 0; i < 10; i++) {
                top10.add(leaderboard.get(i));
            }
            leaderboard = top10;
        }

        return leaderboard;
    }

    public static Map<String, Integer> parseHuntScores() {
        Map<String, Integer> scores = new HashMap<>();

        try {
            JsonArray jsonArray = getValues("Hunt");
            List<List<Object>> values = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray row = jsonArray.get(i).getAsJsonArray();
                List<Object> rowValues = new ArrayList<>();
                for (int j = 0; j < row.size(); j++) {
                    rowValues.add(row.get(j));
                }
                values.add(rowValues);
            }

            if (values != null && !values.isEmpty()) {
                int scoreStartRow = findCurrentScoreSection(values);

                if (scoreStartRow >= 0) {
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
                            } catch (NumberFormatException e) {
                                log.warn("Failed to parse points for team: {}, value: {}", teamName, pointsStr);
                            }
                        }
                    }
                } else {
                    log.warn("Could not find 'Current Score' section in Hunt sheet");
                }
            }
        } catch (IOException e) {
            log.error("Error fetching Hunt scores from Google Sheets", e);
        }

        return scores;
    }

    public static Map<String, String> parseConfigValues() {
        Map<String, String> configValues = new HashMap<>();

        try {
            JsonArray jsonArray = getValues("Config");
            List<List<Object>> values = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray row = jsonArray.get(i).getAsJsonArray();
                List<Object> rowValues = new ArrayList<>();
                for (int j = 0; j < row.size(); j++) {
                    rowValues.add(row.get(j));
                }
                values.add(rowValues);
            }

            if (values != null && !values.isEmpty()) {
                for (int i = 0; i < values.size(); i++) {
                    List<Object> row = values.get(i);

                    if (row.size() >= 2) {
                        String key = String.valueOf(row.get(0)).trim();
                        String value = String.valueOf(row.get(1)).trim();

                        if (key.isEmpty() || key.equalsIgnoreCase("key") || key.equalsIgnoreCase("config")) {
                            continue;
                        }

                        if (key.equalsIgnoreCase("LOGIN_MESSAGE") ||
                                key.equalsIgnoreCase("ANNOUNCEMENT_MESSAGE") ||
                                key.equalsIgnoreCase("ROLL_CALL_ACTIVE") ||
                                key.equalsIgnoreCase("TEAM_1_COLOR") ||
                                key.equalsIgnoreCase("TEAM_2_COLOR") ||
                                key.equalsIgnoreCase("BOTM_PASS")) {

                            configValues.put(key.toUpperCase(), value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error fetching Config values from Google Sheets", e);
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

    public void setLeaderboardCallback(Consumer<JsonArray> leaderboardCallback) {
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
                JsonArray leaderboard = parseTop10Leaderboard();
                if (leaderboardCallback != null) {
                    leaderboardCallback.accept(leaderboard);
                }
                Thread.sleep(420000);
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
                Thread.sleep(420000);
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
                Thread.sleep(420000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        isRunning.set(false);
    }
}
