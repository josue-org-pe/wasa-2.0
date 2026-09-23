package com.chatlocal.frontend.theme;

import java.awt.Color;

/**
 * Paleta de colores moderna inspirada en interfaces oscuras premium (Discord, Linear, Telegram).
 */
public final class ThemeColors {

    private ThemeColors() {}

    // Fondos principales
    public static final Color BG_DARK = new Color(11, 15, 25);
    public static final Color BG_SURFACE = new Color(21, 29, 44);
    public static final Color BG_CARD = new Color(27, 38, 59);
    public static final Color BG_INPUT = new Color(15, 22, 35);

    // Bordes y separadores
    public static final Color BORDER = new Color(42, 57, 80);
    public static final Color BORDER_FOCUS = new Color(99, 102, 241);

    // Colores primarios y acentos
    public static final Color PRIMARY = new Color(99, 102, 241);
    public static final Color PRIMARY_HOVER = new Color(79, 70, 229);
    public static final Color PRIMARY_PRESSED = new Color(67, 56, 202);
    public static final Color ACCENT_CYAN = new Color(6, 182, 212);

    // Estados
    public static final Color SUCCESS = new Color(16, 185, 129);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color DANGER_HOVER = new Color(220, 38, 38);

    // Tipografía
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color TEXT_HINT = new Color(100, 116, 139);

    // Burbujas de chat
    public static final Color BUBBLE_SELF = new Color(79, 70, 229);
    public static final Color BUBBLE_OTHER = new Color(30, 41, 59);
    public static final Color BUBBLE_SYSTEM = new Color(24, 33, 47);
}
