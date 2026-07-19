package com.megaiptv.eltv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static final String PREF_NAME  = "eltv_prefs";
    private static final String PREF_THEME = "theme";

    public static final int THEME_MIDNIGHT = 0;
    public static final int THEME_FOREST   = 1;
    public static final int THEME_PURPLE   = 2;

    public interface ThemeChangeListener {
        void onThemeChanged(int theme);
    }

    private static ThemeManager instance;
    private int currentTheme = THEME_MIDNIGHT;
    private final List<ThemeChangeListener> listeners = new ArrayList<>();

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentTheme = prefs.getInt(PREF_THEME, THEME_MIDNIGHT);
    }

    public void setTheme(Context context, int theme) {
        currentTheme = theme;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(PREF_THEME, theme).apply();
        for (ThemeChangeListener l : new ArrayList<>(listeners)) l.onThemeChanged(theme);
    }

    public int getCurrentTheme() { return currentTheme; }

    public int getBrandColor() {
        switch (currentTheme) {
            case THEME_FOREST: return Color.parseColor("#1B4332");
            case THEME_PURPLE: return Color.parseColor("#2D1B69");
            default:           return Color.parseColor("#1A1A2E");
        }
    }

    public int getSearchAffordanceColor() {
        switch (currentTheme) {
            case THEME_FOREST: return Color.parseColor("#52B788");
            case THEME_PURPLE: return Color.parseColor("#9B59B6");
            default:           return Color.parseColor("#E94560");
        }
    }

    public String getThemeName(int theme) {
        switch (theme) {
            case THEME_FOREST: return "Forest";
            case THEME_PURPLE: return "Purple";
            default:           return "Midnight";
        }
    }

    public void addListener(ThemeChangeListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(ThemeChangeListener l) {
        listeners.remove(l);
    }
}

