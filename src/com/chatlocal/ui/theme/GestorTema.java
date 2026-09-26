package com.chatlocal.ui.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// cambia el tema actual y avisa a los componentes para repintar
public final class GestorTema {

    private static TemaApp currentTheme = TemaApp.NEON_CYBER;
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GestorTema() {}

    public static TemaApp getTheme() {
        return currentTheme;
    }

    public static void setTheme(TemaApp theme) {
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

    public static List<TemaApp> getAvailableThemes() {
        List<TemaApp> list = new ArrayList<>();
        list.add(TemaApp.NEON_CYBER);
        list.add(TemaApp.EMERALD_MATRIX);
        list.add(TemaApp.OCEAN_DISCORD);
        list.add(TemaApp.CRIMSON_DARK);
        list.add(TemaApp.SOLAR_LIGHT);
        return list;
    }
}
