package com.flux.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
public class GoogleSheetParser {
    private static final String API_KEY = "AIzaSyBu-qDCAFvD_z00uohkfD_ub0sZj-H8s1E";
    private static final String SPREADSHEET_ID = "1qqkjx4YjuQ9FIBDgAGzSpmoKcDow3yEa9lYFmc-JeDA";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConfigManager configManager;
    private Consumer<JsonArray> leaderboardCallback;
    private Consumer<Map<String, Integer>> huntScoreCallback;
    private Consumer<Map<String, String>> configCallback;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final SheetType sheetType;
    private Thread leaderboardThread;
    private Thread huntScoreThread;
    private Thread configThread;
    private final OkHttpClient httpClient;

    public enum SheetType {
        BOTM,
        HUNT,
        CONFIG
    }

    public GoogleSheetParser(ConfigManager configManager, Consumer<JsonArray> leaderboardCallback, OkHttpClient httpClient) {
        this.configManager = configManager;
        this.leaderboardCallback = leaderboardCallback;
        this.sheetType = SheetType.BOTM;
        this.httpClient = httpClient;
    }

    public GoogleSheetParser(ConfigManager configManager, SheetType type, Consumer<Map<String, Integer>> huntScoreCallback, OkHttpClient httpClient) {
        this.configManager = configManager;
        this.huntScoreCallback = huntScoreCallback;
        this.sheetType = type;
        this.httpClient = httpClient;
    }

    public GoogleSheetParser(ConfigManager configManager, SheetType type, Consumer<Map<String, String>> configCallback, boolean isConfigSheet, OkHttpClient httpClient) {
        this.configManager = configManager;
        this.configCallback = configCallback;
        this.sheetType = type;
        this.httpClient = httpClient;
    }

    private String makeSheetsApiRequest(String range) throws IOException {
        HttpUrl url = HttpUrl.parse("https://sheets.googleapis.com/v4/spreadsheets/"
                        + SPREADSHEET_ID
                        + "/values/"
                        + range)
                .newBuilder()
                .addQueryParameter("key", API_KEY)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Request failed with status code: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            return body.string();
        }
    }

    public JsonArray getValues(String range) throws IOException {
        String jsonResponse = makeSheetsApiRequest(range);

        JsonObject jsonObject = new JsonParser()
                .parse(jsonResponse)
                .getAsJsonObject();

        if (!jsonObject.has("values")) {
            log.warn("[Sheets] No 'values' field found for range {}", range);
            return new JsonArray();
        }

        return jsonObject.getAsJsonArray("values");
    }


    public JsonArray parseTop10Leaderboard() {
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

            if (!values.isEmpty()) {
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

    public Map<String, Integer> parseHuntScores() {
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

            if (!values.isEmpty()) {
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

    public Map<String, String> parseConfigValues() {
        Map<String, String> configValues = new HashMap<>();

        try {
            JsonArray jsonArray = getValues("Config");
            log.info("[Config] Raw rows count: {}", jsonArray.size());

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray row = jsonArray.get(i).getAsJsonArray();

                if (row.size() < 2) {
                    log.debug("[Config] Skipping row {} (less than 2 columns)", i);
                    continue;
                }

                String key = row.get(0).getAsString().trim();
                String value = row.get(1).getAsString().trim();

                if (key.isEmpty() || key.equalsIgnoreCase("key") || key.equalsIgnoreCase("config")) {
                    log.debug("[Config] Skipping header/empty key: {}", key);
                    continue;
                }

                configValues.put(key.toUpperCase(), value);
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
                huntScoreThread = new Thread(this::pollHuntScores);
                huntScoreThread.start();
            } else if (sheetType == SheetType.CONFIG) {
                configThread = new Thread(this::pollConfigValues);
                configThread.start();
            } else {
                leaderboardThread = new Thread(this::pollLeaderboard);
                leaderboardThread.start();
            }
        }
    }

    private void pollLeaderboard() {
        Runnable leaderboardTask = () -> {
            if (!isRunning.get()) {
                return;
            }

            JsonArray leaderboard = parseTop10Leaderboard();
            if (leaderboardCallback != null) {
                leaderboardCallback.accept(leaderboard);
            }
        };

        scheduler.scheduleAtFixedRate(leaderboardTask, 0, 7, TimeUnit.MINUTES);
    }

    private void pollHuntScores() {
        Runnable huntScoresTask = () -> {
            if (!isRunning.get()) {
                return;
            }

            Map<String, Integer> scores = parseHuntScores();
            if (huntScoreCallback != null && !scores.isEmpty()) {
                huntScoreCallback.accept(scores);
            }
        };

        scheduler.scheduleAtFixedRate(huntScoresTask, 0, 7, TimeUnit.MINUTES);
    }

    private void pollConfigValues() {
        Runnable configValuesTask = () -> {
            if (!isRunning.get()) {
                log.debug("[Config] Poll skipped (not running)");
                return;
            }

            Map<String, String> configValues = parseConfigValues();

            if (configCallback == null) {
                log.warn("[Config] configCallback is NULL");
                return;
            }

            if (configValues.isEmpty()) {
                log.warn("[Config] No config values parsed");
                return;
            }

            configCallback.accept(configValues);
        };

        scheduler.scheduleAtFixedRate(configValuesTask, 0, 2, TimeUnit.MINUTES);
    }

    public void stop() {
        isRunning.set(false);
    }

    public void shutdown() {
        stop();
        scheduler.shutdownNow();

        if (leaderboardThread != null) {
            leaderboardThread.interrupt();
        }
        if (huntScoreThread != null) {
            huntScoreThread.interrupt();
        }
        if (configThread != null) {
            configThread.interrupt();
        }
    }
}
