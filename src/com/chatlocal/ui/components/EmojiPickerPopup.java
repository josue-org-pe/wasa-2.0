package com.chatlocal.ui.components;

import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Popover emergente para selección de Emojis y Stickers expresivos.
 */
public class EmojiPickerPopup extends JPopupMenu {

    public interface EmojiCallback {
        void onEmojiSelected(String emoji);
        void onStickerSelected(String stickerText);
    }

    private static final String[] POPULAR_EMOJIS = {
            "😀", "😂", "🤣", "😍", "🥰", "😎", "🔥", "👍", "👏", "❤️",
            "🚀", "💻", "🎮", "☕", "🎉", "💯", "⚡", "💡", "🔒", "🌐",
            "🍕", "🌮", "🥑", "🍿", "🎧", "📸", "📱", "🕹️", "🏆", "🎯",
            "🥇", "🏖️", "🌙", "☀️", "🎨", "🧪", "✨", "🙌", "🤝", "🥳"
    };

    private static final String[] EXPRESSIVE_STICKERS = {
            "¡Hola a todos! 👋",
            "¡Aprobado con 20! 💯",
            "Código funcionando 💻",
            "En camino 🚀",
            "Pausa café ☕",
            "Bug solucionado 🐛",
            "GG Buen juego 🎮",
            "En reunión local 🤫",
            "¡Excelente trabajo! 👏"
    };

    private final EmojiCallback callback;

    public EmojiPickerPopup(EmojiCallback callback) {
        this.callback = callback;
        setOpaque(true);
        setBackground(ThemeManager.getTheme().bgCard);
        setBorder(BorderFactory.createLineBorder(ThemeManager.getTheme().borderSubtle, 1));

        buildUI();
    }

    private void buildUI() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(true);
        container.setBackground(ThemeManager.getTheme().bgCard);
        container.setPreferredSize(new Dimension(320, 260));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(ThemeManager.getTheme().bgCard);
        tabbedPane.setForeground(ThemeManager.getTheme().textPrimary);

        // Pestaña 1: Emojis
        JPanel emojiPanel = new JPanel(new GridLayout(5, 8, 4, 4));
        emojiPanel.setOpaque(true);
        emojiPanel.setBackground(ThemeManager.getTheme().bgCard);
        emojiPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (String emoji : POPULAR_EMOJIS) {
            JButton btn = createEmojiButton(emoji);
            emojiPanel.add(btn);
        }

        // Pestaña 2: Stickers
        JPanel stickerPanel = new JPanel(new GridLayout(5, 2, 6, 6));
        stickerPanel.setOpaque(true);
        stickerPanel.setBackground(ThemeManager.getTheme().bgCard);
        stickerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (String sticker : EXPRESSIVE_STICKERS) {
            JButton btn = createStickerButton(sticker);
            stickerPanel.add(btn);
        }

        tabbedPane.addTab("😀 Emojis", new JScrollPane(emojiPanel));
        tabbedPane.addTab("✨ Stickers", new JScrollPane(stickerPanel));

        container.add(tabbedPane, BorderLayout.CENTER);
        add(container);
    }

    private JButton createEmojiButton(String emoji) {
        JButton btn = new JButton(emoji);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(ThemeManager.getTheme().bgCardHover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        btn.addActionListener(e -> {
            setVisible(false);
            if (callback != null) callback.onEmojiSelected(emoji);
        });
        return btn;
    }

    private JButton createStickerButton(String sticker) {
        JButton btn = new JButton(sticker);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(ThemeManager.getTheme().textPrimary);
        btn.setBackground(ThemeManager.getTheme().bgInput);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(ThemeManager.getTheme().borderSubtle, 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            setVisible(false);
            if (callback != null) callback.onStickerSelected(sticker);
        });
        return btn;
    }
}
