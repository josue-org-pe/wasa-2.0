package com.chatlocal.ui.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// cambia el tema actual y avisa a los componentes para repintar
public final class ThemeManager {

    private static AppTheme currentTheme = AppTheme.NEON_CYBER;
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ThemeManager() {}

    public static AppTheme getTheme() {
        return currentTheme;
    }

    public static void setTheme(AppTheme theme) {
        if (theme != null && theme != currentTheme) {
            currentTheme = theme;
            for (Runnable r : listeners) {
                try {
                    r.run();
                } catch (Exception e) {
                    System.err.println("Error al aplicar nuevo tema: " + e.getMessage());
                }
            }
        }
    }

    public static void addThemeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void removeThemeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static List<AppTheme> getAvailableThemes() {
        List<AppTheme> list = new ArrayList<>();
        list.add(AppTheme.NEON_CYBER);
        list.add(AppTheme.EMERALD_MATRIX);
        list.add(AppTheme.OCEAN_DISCORD);
        list.add(AppTheme.CRIMSON_DARK);
        list.add(AppTheme.SOLAR_LIGHT);
        return list;
    }
}
