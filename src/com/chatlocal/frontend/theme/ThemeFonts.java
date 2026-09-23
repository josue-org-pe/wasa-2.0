package com.chatlocal.frontend.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Gestión de tipografías modernas y consistentes para la interfaz.
 */
public final class ThemeFonts {

    private static final String FONT_FAMILY;

    static {
        // Detectar si Segoe UI (Windows standard moderno), Inter o Roboto están disponibles
        String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String selected = "SansSerif";

        for (String f : availableFonts) {
            if (f.equalsIgnoreCase("Segoe UI")) {
                selected = "Segoe UI";
                break;
            } else if (f.equalsIgnoreCase("Inter")) {
                selected = "Inter";
                break;
            } else if (f.equalsIgnoreCase("Roboto")) {
                selected = "Roboto";
            }
        }
        FONT_FAMILY = selected;
    }

    private ThemeFonts() {}

    public static Font title(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    public static Font subtitle(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    public static Font medium(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font bold(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    public static Font regular(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font code(int size) {
        return new Font("Consolas", Font.PLAIN, size);
    }
}
