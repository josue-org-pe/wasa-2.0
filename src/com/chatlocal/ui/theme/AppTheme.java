package com.chatlocal.ui.theme;

import java.awt.Color;

// colores de los temas de la app
public class AppTheme {

    private final String id;
    private final String name;

    public final Color bgDark;
    public final Color bgSidebar;
    public final Color bgCard;
    public final Color bgCardHover;
    public final Color bgInput;
    public final Color borderSubtle;
    public final Color borderActive;
    public final Color primary;
    public final Color primaryHover;
    public final Color primaryPressed;
    public final Color accent;
    public final Color success;
    public final Color warning;
    public final Color danger;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color textMuted;
    public final Color bubbleSelf;
    public final Color bubblePeer;
    public final Color bubbleSystem;
    public final boolean isDark;

    public AppTheme(String id, String name, Color bgDark, Color bgSidebar, Color bgCard, Color bgCardHover,
                    Color bgInput, Color borderSubtle, Color borderActive, Color primary, Color primaryHover,
                    Color primaryPressed, Color accent, Color success, Color warning, Color danger,
                    Color textPrimary, Color textSecondary, Color textMuted, Color bubbleSelf, Color bubblePeer,
                    Color bubbleSystem, boolean isDark) {
        this.id = id;
        this.name = name;
        this.bgDark = bgDark;
        this.bgSidebar = bgSidebar;
        this.bgCard = bgCard;
        this.bgCardHover = bgCardHover;
        this.bgInput = bgInput;
        this.borderSubtle = borderSubtle;
        this.borderActive = borderActive;
        this.primary = primary;
        this.primaryHover = primaryHover;
        this.primaryPressed = primaryPressed;
        this.accent = accent;
        this.success = success;
        this.warning = warning;
        this.danger = danger;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textMuted = textMuted;
        this.bubbleSelf = bubbleSelf;
        this.bubblePeer = bubblePeer;
        this.bubbleSystem = bubbleSystem;
        this.isDark = isDark;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // --- Paletas Predefinidas ---

    public static final AppTheme NEON_CYBER = new AppTheme(
            "neon_cyber", "🟣 Neon Cyber (Midnight)",
            new Color(0x0C, 0x0A, 0x15), // bgDark
            new Color(0x13, 0x0F, 0x22), // bgSidebar
            new Color(0x1B, 0x15, 0x31), // bgCard
            new Color(0x24, 0x1D, 0x41), // bgCardHover
            new Color(0x15, 0x10, 0x25), // bgInput
            new Color(0x2D, 0x23, 0x4D), // borderSubtle
            new Color(0x8B, 0x5C, 0xF6), // borderActive
            new Color(0x8B, 0x5C, 0xF6), // primary
            new Color(0x7C, 0x3A, 0xED), // primaryHover
            new Color(0x6D, 0x28, 0xD9), // primaryPressed
            new Color(0x06, 0xB6, 0xD4), // accent
            new Color(0x10, 0xB9, 0x81), // success
            new Color(0xF5, 0x9E, 0x0B), // warning
            new Color(0xEF, 0x44, 0x44), // danger
            new Color(0xF8, 0xFA, 0xFC), // textPrimary
            new Color(0xC4, 0xB5, 0xFD), // textSecondary
            new Color(0x7C, 0x6E, 0xA2), // textMuted
            new Color(0x7C, 0x3A, 0xED), // bubbleSelf
            new Color(0x1E, 0x18, 0x35), // bubblePeer
            new Color(0x16, 0x12, 0x26), // bubbleSystem
            true
    );

    public static final AppTheme EMERALD_MATRIX = new AppTheme(
            "emerald_matrix", "🟢 Emerald Matrix",
            new Color(0x08, 0x10, 0x0D),
            new Color(0x0E, 0x1A, 0x15),
            new Color(0x14, 0x27, 0x20),
            new Color(0x1B, 0x34, 0x2B),
            new Color(0x0C, 0x17, 0x13),
            new Color(0x1F, 0x3C, 0x31),
            new Color(0x10, 0xB9, 0x81),
            new Color(0x10, 0xB9, 0x81),
            new Color(0x05, 0x96, 0x69),
            new Color(0x04, 0x78, 0x57),
            new Color(0x34, 0xD3, 0x99),
            new Color(0x10, 0xB9, 0x81),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0xEF, 0x44, 0x44),
            new Color(0xF0, 0xFD, 0xF4),
            new Color(0xA7, 0xF3, 0xD0),
            new Color(0x56, 0x7A, 0x6C),
            new Color(0x05, 0x96, 0x69),
            new Color(0x15, 0x29, 0x21),
            new Color(0x10, 0x1E, 0x18),
            true
    );

    public static final AppTheme OCEAN_DISCORD = new AppTheme(
            "ocean_discord", "🔵 Ocean Discord",
            new Color(0x0F, 0x17, 0x2A),
            new Color(0x1E, 0x29, 0x3B),
            new Color(0x27, 0x35, 0x49),
            new Color(0x33, 0x41, 0x55),
            new Color(0x18, 0x22, 0x34),
            new Color(0x33, 0x41, 0x55),
            new Color(0x3B, 0x82, 0xF6),
            new Color(0x3B, 0x82, 0xF6),
            new Color(0x25, 0x63, 0xEB),
            new Color(0x1D, 0x4E, 0xD8),
            new Color(0x38, 0xBD, 0xF8),
            new Color(0x10, 0xB9, 0x81),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0xEF, 0x44, 0x44),
            new Color(0xF8, 0xFA, 0xFC),
            new Color(0x94, 0xA3, 0xB8),
            new Color(0x64, 0x74, 0x8B),
            new Color(0x25, 0x63, 0xEB),
            new Color(0x1E, 0x29, 0x3B),
            new Color(0x19, 0x22, 0x32),
            true
    );

    public static final AppTheme CRIMSON_DARK = new AppTheme(
            "crimson_dark", "🔴 Crimson Velvet",
            new Color(0x12, 0x08, 0x0C),
            new Color(0x1B, 0x0D, 0x13),
            new Color(0x26, 0x13, 0x1B),
            new Color(0x33, 0x1A, 0x24),
            new Color(0x1A, 0x0C, 0x12),
            new Color(0x3C, 0x1F, 0x2B),
            new Color(0xF4, 0x3F, 0x5E),
            new Color(0xF4, 0x3F, 0x5E),
            new Color(0xE1, 0x1D, 0x48),
            new Color(0xBE, 0x12, 0x3C),
            new Color(0xFB, 0x71, 0x85),
            new Color(0x10, 0xB9, 0x81),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0xEF, 0x44, 0x44),
            new Color(0xFF, 0xF1, 0xF2),
            new Color(0xFE, 0xCD, 0xD3),
            new Color(0x8C, 0x4F, 0x60),
            new Color(0xE1, 0x1D, 0x48),
            new Color(0x26, 0x13, 0x1B),
            new Color(0x1C, 0x0E, 0x14),
            true
    );

    public static final AppTheme SOLAR_LIGHT = new AppTheme(
            "solar_light", "☀️ Solar Light",
            new Color(0xF8, 0xFA, 0xFC),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0xF1, 0xF5, 0xF9),
            new Color(0xE2, 0xE8, 0xF0),
            new Color(0xCB, 0xD5, 0xE1),
            new Color(0x63, 0x66, 0xF1),
            new Color(0x63, 0x66, 0xF1),
            new Color(0x4F, 0x46, 0xE5),
            new Color(0x43, 0x38, 0xCA),
            new Color(0x02, 0x84, 0xC7),
            new Color(0x10, 0xB9, 0x81),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0xEF, 0x44, 0x44),
            new Color(0x0F, 0x17, 0x2A),
            new Color(0x47, 0x55, 0x69),
            new Color(0x94, 0xA3, 0xB8),
            new Color(0x63, 0x66, 0xF1),
            new Color(0xE2, 0xE8, 0xF0),
            new Color(0xED, 0xF2, 0xF7),
            false
    );
}
