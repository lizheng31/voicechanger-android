package com.mengmeng.voicechager.models;

public class AudioItem {
    private String id;
    private String name;
    private String path;
    private String date;
    private boolean isConverted;

    public AudioItem(String id, String name, String path, String date, boolean isConverted) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.date = date;
        this.isConverted = isConverted;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getDate() { return date; }
    public boolean isConverted() { return isConverted; }
} 