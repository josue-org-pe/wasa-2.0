package com.chatlocal.ui.components;

import com.chatlocal.backend.model.SalaChat;
import com.chatlocal.backend.model.UsuarioPerfil;
import com.chatlocal.ui.theme.Iconos;
import com.chatlocal.ui.theme.GestorTema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// item de cada contacto o sala en la barra lateral izquierda
public class ItemListaContacto extends JPanel {

    public interface ContactActionCallback {
        void onSelected();
        void onBlockRequested();
    }

    private final String title;
    private final String subtitle;
    private final int colorHex;
    private final boolean isRoom;
    private final boolean isMeeting;
    private final boolean isOnline;
    private final int unreadCount;
    private boolean selected = false;
    private boolean hovered = false;
    private String avatarImagePath = null;

    public ItemListaContacto(SalaChat room, boolean selected, ContactActionCallback callback) {
        this(room.getName(), room.getTopic(), 0x6366F1, true, room.isMeeting(), true, room.getUnreadCount(), selected, callback);
    }

    public ItemListaContacto(UsuarioPerfil user, boolean selected, ContactActionCallback callback) {
        this(user.getUsername(), user.getStatusMessage(), user.getAvatarColorHex(), false, false, true, 0, selected, callback);
        this.avatarImagePath = user.getAvatarImagePath();
    }

    public ItemListaContacto(String title, String subtitle, int colorHex, boolean isRoom,
                           boolean isMeeting, boolean isOnline, int unreadCount,
                           boolean selected, ContactActionCallback callback) {
        this.title = title;
        this.subtitle = subtitle;
        this.colorHex = colorHex;
        this.isRoom = isRoom;
        this.isMeeting = isMeeting;
        this.isOnline = isOnline;
        this.unreadCount = unreadCount;
        this.selected = selected;

        setOpaque(false);
        setPreferredSize(new Dimension(250, 56));
        setMinimumSize(new Dimension(100, 56));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && !isRoom) {
                    showContextMenu(e.getX(), e.getY(), callback);
                } else if (SwingUtilities.isLeftMouseButton(e) && callback != null) {
                    callback.onSelected();
                }
            }
        });
    }

    private void showContextMenu(int x, int y, ContactActionCallback callback) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(GestorTema.getTheme().bgCard);
        menu.setBorder(BorderFactory.createLineBorder(GestorTema.getTheme().borderSubtle, 1));

        JMenuItem itemBlock = new JMenuItem("🚫 Bloquear Contacto");
        itemBlock.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        itemBlock.setForeground(new Color(0xF8, 0x71, 0x71));
        itemBlock.setBackground(GestorTema.getTheme().bgCard);
        itemBlock.addActionListener(e -> {
            if (callback != null) callback.onBlockRequested();
        });

        menu.add(itemBlock);
        menu.show(this, x, y);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int r = 10;

        // Fondo activo o hover
        if (selected) {
            g2.setColor(GestorTema.getTheme().bgCardHover);
            g2.fillRoundRect(4, 3, w - 8, h - 6, r, r);
            g2.setColor(GestorTema.getTheme().primary);
            g2.fillRect(4, 12, 3, h - 24);
        } else if (hovered) {
            g2.setColor(new Color(GestorTema.getTheme().bgCardHover.getRed(),
                    GestorTema.getTheme().bgCardHover.getGreen(),
                    GestorTema.getTheme().bgCardHover.getBlue(), 120));
            g2.fillRoundRect(4, 3, w - 8, h - 6, r, r);
        }

        // Avatar o icono de sala
        int avSize = 36;
        int avX = 14;
        int avY = (h - avSize) / 2;

        if (isRoom) {
            Icon icon = isMeeting ? Iconos.group(22, GestorTema.getTheme().primary) : Iconos.group(20, GestorTema.getTheme().textSecondary);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(avX, avY, avSize, avSize);
            icon.paintIcon(this, g2, avX + 7, avY + 8);
        } else {
            Icon avatarIcon = Iconos.avatar(title, avSize, new Color(colorHex), Color.WHITE, avatarImagePath);
            avatarIcon.paintIcon(this, g2, avX, avY);

            // Punto de estado verde
            if (isOnline) {
                g2.setColor(GestorTema.getTheme().success);
                g2.fillOval(avX + avSize - 9, avY + avSize - 9, 9, 9);
                g2.setColor(GestorTema.getTheme().bgSidebar);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(avX + avSize - 9, avY + avSize - 9, 9, 9);
            }
        }

        // Título
        int textX = avX + avSize + 10;
        int maxTextW = w - textX - 35;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(selected ? GestorTema.getTheme().textPrimary : GestorTema.getTheme().textPrimary);
        String displayTitle = truncate(g2, title, maxTextW);
        g2.drawString(displayTitle, textX, 24);

        // Subtítulo
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(GestorTema.getTheme().textMuted);
        String displaySubtitle = truncate(g2, subtitle != null ? subtitle : "", maxTextW);
        g2.drawString(displaySubtitle, textX, 42);

        // Badge de no leídos
        if (unreadCount > 0) {
            g2.setColor(GestorTema.getTheme().primary);
            g2.fillRoundRect(w - 28, (h - 18) / 2, 20, 18, 9, 9);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString(String.valueOf(unreadCount), w - 21, (h + 10) / 2);
        }

        g2.dispose();
    }

    private String truncate(Graphics2D g2, String text, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String s = text;
        while (s.length() > 3 && fm.stringWidth(s + "...") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }
}
