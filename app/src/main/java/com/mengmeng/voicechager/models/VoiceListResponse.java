package com.mengmeng.voicechager.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class VoiceListResponse {
    @SerializedName("code")
    private int code;

    @SerializedName("msg")
    private String message;

    @SerializedName("data")
    private List<Speaker> data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public List<Speaker> getData() { return data; }

    public static class Speaker {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("preview_url")
        private String previewUrl;

        @SerializedName("language")
        private String language;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPreviewUrl() { return previewUrl; }
        public String getLanguage() { return language; }
    }
} 