package edu.bupt.ta.model;

import edu.bupt.ta.repository.Identifiable;

public class UserSettings implements Identifiable<String> {

    public static final String DEFAULT_ID = "global";

    private String id = DEFAULT_ID;
    private String language = "en";
    private double fontSize = 14.0;

    public UserSettings() {
    }

    public UserSettings(String id, String language, double fontSize) {
        this.id = id;
        this.language = language;
        this.fontSize = fontSize;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }
}
