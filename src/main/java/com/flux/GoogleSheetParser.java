package com.flux;

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
    private static final String API_KEY = "AIzaSyBu-qDCAFvD_z00uohkfD_ub0sZj-H8s1E"; // Your API Key
    private static final String SPREADSHEET_ID = "1qqkjx4YjuQ9FIBDgAGzSpmoKcDow3yEa9lYFmc-JeDA"; // Your Spreadsheet ID
    private static final List<String> RANGES = Collections.unmodifiableList(Arrays.asList("BOTM"));
    private Consumer<JSONArray> leaderboardCallback;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public GoogleSheetParser(ConfigManager configManager, Consumer<JSONArray> leaderboardCallback) {
        this.leaderboardCallback = leaderboardCallback;
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
                    // Fetch headers dynamically
                    List<Object> headers = values.get(startRow - 1); // Row above startRow is the header row
                    Map<String, Integer> headerIndexMap = mapHeaders(headers);

                    // Parse leaderboard data starting from the identified row
                    for (int i = startRow; i < values.size(); i++) {
                        List<Object> row = values.get(i);
                        if (row.size() >= headerIndexMap.size()) {
                            // Extract data dynamically based on header index map
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

        // Ensure we only return the top 10 players
        if (leaderboard.length() > 10) {
            leaderboard = new JSONArray(leaderboard.toList().subList(0, 10));
        }

        return leaderboard;
    }

    private static int findLeaderboardStartRow(List<List<Object>> values) {
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 4) { // Look for at least 4 columns
                // Try to find the header row by checking common header keywords
                // (case-insensitive)
                if (row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("Rank")) &&
                        row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("Points")) &&
                        row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("Players")) &&
                        row.stream().anyMatch(cell -> cell.toString().equalsIgnoreCase("KC"))) {
                    return i + 1; // Return row index after header row
                }
            }
        }
        return -1; // Return -1 if the header isn't found
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
            new Thread(this::pollLeaderboard).start();
        }
    }

    // Inside GoogleSheetParser class
    public void setLeaderboardCallback(Consumer<JSONArray> leaderboardCallback) {
        this.leaderboardCallback = leaderboardCallback;
    }

    private void pollLeaderboard() {
        while (isRunning.get()) {
            try {
                // Fetch the leaderboard and trigger the callback
                JSONArray leaderboard = parseTop10Leaderboard();
                leaderboardCallback.accept(leaderboard);

                // Sleep for a while before polling again (you can adjust the delay as needed)
                Thread.sleep(5000); // 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        isRunning.set(false);
    }
}
