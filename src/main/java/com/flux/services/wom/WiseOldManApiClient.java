package com.flux.services.wom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import javax.inject.Inject;
import java.io.IOException;


@Slf4j
public class WiseOldManApiClient {
    private static final String BASE_API_URL = "https://api.wiseoldman.net/v2";
    private static final String GROUP_ID = "141";
    private static final JsonParser jsonParser = new JsonParser();

    @Inject
    private OkHttpClient httpClient;

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

    private String makeHttpRequest(String urlString) throws IOException {
        Request request = new Request.Builder()
                .url(urlString)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Request failed with status code: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }

            return response.body().string();
        }
    }
}
