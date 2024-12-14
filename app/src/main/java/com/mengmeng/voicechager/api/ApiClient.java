package com.mengmeng.voicechager.api;

import com.mengmeng.voicechager.utils.LogUtils;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String WS_URL = "wss://cn-huadong-1.xf-yun.com/v1/private/s5e668773";
    private static final String APP_ID = "e57180ee";
    private static final String API_KEY = "b0e1787a1677ade1b9def9d49125e764";
    private static final String API_SECRET = "YWM0OTI5MzQwNWFiNzExNzhhY2FhMzkw";

    private static OkHttpClient client = null;

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .hostnameVerifier((hostname, session) -> true)
                .build();
        }
        return client;
    }

    public static String getAppId() {
        return APP_ID;
    }

    public static String getApiKey() {
        return API_KEY;
    }

    public static String getApiSecret() {
        return API_SECRET;
    }

    public static String getWsUrl() {
        return WS_URL;
    }
} 