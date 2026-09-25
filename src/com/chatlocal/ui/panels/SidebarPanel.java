package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.ChatRoom;
import com.chatlocal.backend.model.UserProfile;
import com.chatlocal.backend.service.ChatService;
import com.chatlocal.ui.components.ContactListItem;
import com.chatlocal.ui.components.ModernTextField;
import com.chatlocal.ui.dialogs.CreateRoomDialog;
import com.chatlocal.ui.dialogs.SettingsDialog;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/**
 * Barra lateral (Sidebar) estilo WhatsApp Desktop / Discord:
 * - Tarjeta superior de perfil con estado editable y botón de configuración ⚙️.
 * - Buscador rápido en tiempo real.
 * - Lista de Salas y Grupos con botón de nueva sala (+).
 * - Lista de contactos en línea en la red local.
 */
public class SidebarPanel extends JPanel {

    public interface SidebarCallback {
        void onRoomSelected(ChatRoom room);
        void onUserSelected(UserProfile user);
        void onSettingsChanged();
    }

    private final ChatService chatService;
    private final Frame parentFrame;
    private final SidebarCallback callback;

    private JLabel lblUserAvatar;
    private JLabel lblUserName;
    private JLabel lblUserStatus;

    private ModernTextField txtSearch;
    private JPanel listContainer;
    private String filterQuery = "";

    public SidebarPanel(Frame parentFrame, ChatService chatService, SidebarCallback callback) {
        this.parentFrame = parentFrame;
        this.chatService = chatService;
        this.callback = callback;

        setPreferredSize(new Dimension(280, 600));
        setOpaque(true);
        setBackground(ThemeManager.getTheme().bgSidebar);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 1. Tarjeta de Perfil
        topPanel.add(createProfileHeader());
        topPanel.add(Box.createVerticalStrut(10));

        // 2. Buscador
        topPanel.add(createSearchBar());
        topPanel.add(Box.createVerticalStrut(10));

        add(topPanel, BorderLayout.NORTH);

        // 3. Contenedor de listas con scroll
        listContainer = new JPanel();
        listContainer.setOpaque(false);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    private JPanel createProfileHeader() {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ThemeManager.getTheme().borderSubtle);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        card.setOpaque(true);
        card.setBackground(ThemeManager.getTheme().bgSidebar);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        UserProfile profile = chatService.getLocalUserProfile();

        lblUserAvatar = new JLabel(Icons.avatar(profile != null ? profile.getUsername() : "U", 38,
                new Color(profile != null ? profile.getAvatarColorHex() : 0x8B5CF6), Color.WHITE,
                profile != null ? profile.getAvatarImagePath() : null));

        JPanel namePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        namePanel.setOpaque(false);

        lblUserName = new JLabel(profile != null ? profile.getUsername() : "Usuario");
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUserName.setForeground(ThemeManager.getTheme().textPrimary);

        lblUserStatus = new JLabel(profile != null ? profile.getStatusMessage() : "En línea");
        lblUserStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblUserStatus.setForeground(ThemeManager.getTheme().success);

        namePanel.add(lblUserName);
        namePanel.add(lblUserStatus);

        // Botón de configuración
        JButton btnSettings = new JButton(Icons.gear(18, ThemeManager.getTheme().textSecondary));
        btnSettings.setContentAreaFilled(false);
        btnSettings.setBorderPainted(false);
        btnSettings.setFocusPainted(false);
        btnSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSettings.setToolTipText("Configuración y Personalización");
        btnSettings.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(parentFrame, chatService, () -> {
                updateProfileLabels();
                refresh();
                if (callback != null) callback.onSettingsChanged();
            });
            dialog.setVisible(true);
        });

        card.add(lblUserAvatar, BorderLayout.WEST);
        card.add(namePanel, BorderLayout.CENTER);
        card.add(btnSettings, BorderLayout.EAST);
        return card;
    }

    private JPanel createSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        txtSearch = new ModernTextField("Buscar sala o contacto...", 15);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        panel.add(txtSearch, BorderLayout.CENTER);
        return panel;
    }

    private void filter() {
        this.filterQuery = txtSearch.getText().trim().toLowerCase();
        refresh();
    }

    public void updateProfileLabels() {
        UserProfile profile = chatService.getLocalUserProfile();
        if (profile != null) {
            lblUserName.setText(profile.getUsername());
            lblUserStatus.setText(profile.getStatusMessage());
            lblUserAvatar.setIcon(Icons.avatar(profile.getUsername(), 38,
                    new Color(profile.getAvatarColorHex()), Color.WHITE, profile.getAvatarImagePath()));
        }
        setBackground(ThemeManager.getTheme().bgSidebar);
        revalidate();
        repaint();
    }

    public void refresh() {
        listContainer.removeAll();

        // 1. Sección de Salas
        JPanel roomsHeader = createSectionHeader("SALAS Y GRUPOS", true, () -> {
            CreateRoomDialog dialog = new CreateRoomDialog(parentFrame, chatService, () -> {
                refresh();
                ChatRoom active = chatService.getActiveRoom();
                if (callback != null) callback.onRoomSelected(active);
            });
            dialog.setVisible(true);
        });
        listContainer.add(roomsHeader);

        ChatRoom activeRoom = chatService.getActiveRoom();
        UserProfile activePrivate = chatService.getActivePrivateUser();
        List<ChatRoom> rooms = chatService.getRooms();

        for (ChatRoom room : rooms) {
            if (filterQuery.isEmpty() || room.getName().toLowerCase().contains(filterQuery)) {
                boolean isSelected = (activePrivate == null) && activeRoom != null && activeRoom.getId().equals(room.getId());
                ContactListItem item = new ContactListItem(room, isSelected, new ContactListItem.ContactActionCallback() {
                    @Override
                    public void onSelected() {
                        chatService.setActiveRoom(room.getId());
                        refresh();
                        if (callback != null) callback.onRoomSelected(room);
                    }
                    @Override
                    public void onBlockRequested() {}
                });
                listContainer.add(item);
            }
        }

        listContainer.add(Box.createVerticalStrut(14));

        // 2. Sección de Contactos en Línea
        List<UserProfile> users = chatService.getConnectedUsers();
        int onlineCount = users.size();
        JPanel usersHeader = createSectionHeader("EN LÍNEA (" + onlineCount + ")", false, null);
        listContainer.add(usersHeader);

        if (users.isEmpty()) {
            JLabel lblEmpty = new JLabel("  Sin otros usuarios activos");
            lblEmpty.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblEmpty.setForeground(ThemeManager.getTheme().textMuted);
            listContainer.add(lblEmpty);
        } else {
            for (UserProfile user : users) {
                if (filterQuery.isEmpty() || user.getUsername().toLowerCase().contains(filterQuery)) {
                    boolean isPrivateSelected = (activePrivate != null) &&
                            activePrivate.getUsername().equalsIgnoreCase(user.getUsername());

                    ContactListItem item = new ContactListItem(user, isPrivateSelected, new ContactListItem.ContactActionCallback() {
                        @Override
                        public void onSelected() {
                            chatService.setActivePrivateUser(user);
                            refresh();
                            if (callback != null) callback.onUserSelected(user);
                        }
                        @Override
                        public void onBlockRequested() {
                            chatService.blockUser(user.getUsername());
                            JOptionPane.showMessageDialog(SidebarPanel.this, "Usuario " + user.getUsername() + " bloqueado.");
                            refresh();
                        }
                    });
                    listContainer.add(item);
                }
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createSectionHeader(String title, boolean showAddButton, Runnable onAdd) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 14, 4, 10));

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(ThemeManager.getTheme().textMuted);
        panel.add(label, BorderLayout.WEST);

        if (showAddButton) {
            JButton btnAdd = new JButton(Icons.plus(12, ThemeManager.getTheme().primary));
            btnAdd.setContentAreaFilled(false);
            btnAdd.setBorderPainted(false);
            btnAdd.setFocusPainted(false);
            btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAdd.setToolTipText("Crear nueva sala o reunión");
            btnAdd.addActionListener(e -> {
                if (onAdd != null) onAdd.run();
            });
            panel.add(btnAdd, BorderLayout.EAST);
        }

        return panel;
    }
}
