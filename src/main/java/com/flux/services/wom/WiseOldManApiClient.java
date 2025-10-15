package com.flux.services.wom;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Client for interacting with the Wise Old Man API.
 */
public class WiseOldManApiClient {
    private static final String BASE_API_URL = "https://api.wiseoldman.net/v2";
    private static final String GROUP_ID = "141";

    /**
     * Fetches the list of competitions for the configured group.
     */
    public JSONArray fetchGroupCompetitions() throws Exception {
        String urlString = BASE_API_URL + "/groups/" + GROUP_ID + "/competitions";
        String response = makeHttpRequest(urlString);
        return new JSONArray(response);
    }

    /**
     * Fetches detailed information for a specific competition.
     */
    public JSONObject fetchCompetitionDetails(int competitionId) throws Exception {
        String urlString = BASE_API_URL + "/competitions/" + competitionId;
        String response = makeHttpRequest(urlString);
        return new JSONObject(response);
    }

    /**
     * Generates the WOM URL for a competition.
     */
    public String getCompetitionUrl(int competitionId) {
        return "https://wiseoldman.net/competitions/" + competitionId;
    }

    private String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } finally {
            conn.disconnect();
        }
    }
}