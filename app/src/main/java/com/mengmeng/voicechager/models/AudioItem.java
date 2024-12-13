package com.mengmeng.voicechager.models;

public class AudioItem {
    private String id;
    private String name;
    private String path;
    private String date;
    private boolean isConverted;
    private String voiceType;

    public AudioItem(String id, String name, String path, String date, boolean isConverted) {
        this(id, name, path, date, isConverted, null);
    }

    public AudioItem(String id, String name, String path, String date, boolean isConverted, String voiceType) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.date = date;
        this.isConverted = isConverted;
        this.voiceType = voiceType;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getDate() { return date; }
    public boolean isConverted() { return isConverted; }
    public String getVoiceType() { return voiceType; }
    public String getDisplayName() {
        if (isConverted && voiceType != null) {
            return name + " (" + voiceType + ")";
        }
        return name;
    }
} 