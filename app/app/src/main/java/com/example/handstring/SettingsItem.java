package com.example.handstring;

public class SettingsItem {
    private String title;
    private String subtitle;

    public SettingsItem(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
} 