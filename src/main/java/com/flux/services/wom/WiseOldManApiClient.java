package com.flux.services.wom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;


@Slf4j
public class WiseOldManApiClient {
    private final String BASE_API_URL = "https://api.wiseoldman.net/v2";
    private final String GROUP_ID = "141";
    private final JsonParser jsonParser = new JsonParser();
    private final OkHttpClient httpClient;
    private static final String EMPTY_STRING = "";

    public WiseOldManApiClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public JsonArray fetchGroupCompetitions() {
        String urlString = BASE_API_URL + "/groups/" + GROUP_ID + "/competitions";
        String response = makeHttpRequest(urlString);
        return jsonParser.parse(response).getAsJsonArray();
    }

    public JsonObject fetchCompetitionDetails(int competitionId){
        String urlString = BASE_API_URL + "/competitions/" + competitionId;
        String response = makeHttpRequest(urlString);
        return jsonParser.parse(response).getAsJsonObject();
    }

    public String getCompetitionUrl(int competitionId) {
        // Non-API URL for button links
        return "https://wiseoldman.net/competitions/" + competitionId;
    }

    private String makeHttpRequest(String urlString) {
        Request request = new Request.Builder()
                .url(urlString)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            log.info("Wise old man request failed response: {}", response);
            return EMPTY_STRING;
        } catch (IOException e) {
            log.error("Request failed: {}", e.getMessage());
            return EMPTY_STRING;
        }
    }
}
