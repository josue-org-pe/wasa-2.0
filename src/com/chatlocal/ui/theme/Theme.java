package com.chatlocal.ui.theme;

import java.awt.*;

/**
 * Sistema central de diseño y tokens visuales para ChatLocal.
 * Define la paleta de colores Dark Slate, tipografías escalables,
 * curvaturas y configuraciones de renderizado anti-aliasing.
 */
public final class Theme {

    private Theme() {}

    // --- Paleta de Fondos ---
    public static final Color BG_DARK = new Color(0x11, 0x13, 0x18);       // Fondo de la ventana
    public static final Color BG_PANEL = new Color(0x18, 0x1A, 0x22);      // Paneles y contenedores principales
    public static final Color BG_CARD = new Color(0x22, 0x25, 0x30);       // Tarjetas e ítems de lista
    public static final Color BG_CARD_HOVER = new Color(0x2A, 0x2E, 0x3C); // Hover en tarjetas
    public static final Color BG_INPUT = new Color(0x14, 0x16, 0x1E);      // Campos de texto
    public static final Color BG_BADGE = new Color(0x1E, 0x22, 0x2D);      // Píldoras de estado

    // --- Bordes y Separadores ---
    public static final Color BORDER_SUBTLE = new Color(0x2E, 0x32, 0x41);
    public static final Color BORDER_ACTIVE = new Color(0x63, 0x66, 0xF1);

    // --- Colores de Acento ---
    public static final Color PRIMARY = new Color(0x63, 0x66, 0xF1);       // Índigo moderno
    public static final Color PRIMARY_HOVER = new Color(0x4F, 0x46, 0xE5);
    public static final Color PRIMARY_PRESSED = new Color(0x43, 0x38, 0xCA);

    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);       // Verde Esmeralda (En línea)
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);       // Ámbar (Esperando)
    public static final Color DANGER = new Color(0xEF, 0x44, 0x44);        // Rojo (Error / Desconectar)
    public static final Color DANGER_HOVER = new Color(0xDC, 0x26, 0x26);
    public static final Color SECONDARY = new Color(0x33, 0x37, 0x46);     // Botones neutros
    public static final Color SECONDARY_HOVER = new Color(0x3E, 0x43, 0x54);

    // --- Colores de Texto ---
    public static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);
    public static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);
    public static final Color TEXT_ACCENT = new Color(0x81, 0x8C, 0xF8);

    // --- Burbujas de Chat ---
    public static final Color BUBBLE_SELF = new Color(0x4F, 0x46, 0xE5);
    public static final Color BUBBLE_PEER = new Color(0x27, 0x2A, 0x37);
    public static final Color BUBBLE_SYSTEM = new Color(0x1E, 0x22, 0x2D);

    // --- Tipografías ---
    public static final String FONT_FAMILY = "Segoe UI";

    public static final Font FONT_HERO = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_CAPTION = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_CODE = new Font("Consolas", Font.PLAIN, 12);

    /**
     * Habilita anti-aliasing de gráficos y texto con calidad visual LCD/Subpixel.
     */
    public static void enableQualityRendering(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
