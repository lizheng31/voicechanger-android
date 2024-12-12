package com.mengmeng.voicechager.models;

import com.google.gson.annotations.SerializedName;

public class VoiceResponse {
    @SerializedName("code")
    private int code;

    @SerializedName("msg")
    private String message;

    @SerializedName("data")
    private AudioData data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public AudioData getData() { return data; }

    public static class AudioData {
        @SerializedName("audio_url")
        private String audioUrl;

        @SerializedName("task_id")
        private String taskId;

        public String getAudioUrl() { return audioUrl; }
        public String getTaskId() { return taskId; }
    }
} 