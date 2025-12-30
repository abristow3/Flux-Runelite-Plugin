package com.flux.services.wom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
public class WiseOldManApiClient {
    private static final String BASE_API_URL = "https://api.wiseoldman.net/v2";
    private static final String GROUP_ID = "141";
    private static final JsonParser jsonParser = new JsonParser();

    public JsonArray fetchGroupCompetitions() throws Exception {
        String urlString = BASE_API_URL + "/groups/" + GROUP_ID + "/competitions";
        String response = makeHttpRequest(urlString);
        return jsonParser.parse(response).getAsJsonArray();
    }

    public JsonObject fetchCompetitionDetails(int competitionId) throws Exception {
        String urlString = BASE_API_URL + "/competitions/" + competitionId;
        String response = makeHttpRequest(urlString);
        return jsonParser.parse(response).getAsJsonObject();
    }

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
