package com.mengmeng.voicechager.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "http://direct.virtaicloud.com:29896/";
    private static final String API_KEY = "your_api_key_here"; // 替换为实际的API密钥

    private static Retrofit retrofit = null;
    private static VoiceService voiceService = null;

    public static VoiceService getVoiceService() {
        if (voiceService == null) {
            voiceService = getClient().create(VoiceService.class);
        }
        return voiceService;
    }

    private static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    public static String getAuthorizationHeader() {
        return "Bearer " + API_KEY;
    }
} 