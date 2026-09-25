package com.chatlocal.backend.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa el perfil de identidad de un usuario en el chat.
 */
public class UserProfile implements Serializable {

    private final String id;
    private String username;
    private final ConnectionRole role;
    private final String ipAddress;
    private final int port;
    private int avatarColorHex;
    private String statusMessage = "En línea";
    private boolean isBlocked = false;
    private String avatarImagePath = null;

    public UserProfile(String username, ConnectionRole role, String ipAddress, int port) {
        this(UUID.randomUUID().toString(), username, role, ipAddress, port, generateColor(username));
    }

    public UserProfile(String id, String username, ConnectionRole role, String ipAddress, int port, int avatarColorHex) {
        this.id = id;
        this.username = (username != null && !username.trim().isEmpty()) ? username.trim() : "Usuario";
        this.role = role;
        this.ipAddress = ipAddress;
        this.port = port;
        this.avatarColorHex = avatarColorHex;
    }

    public String getAvatarImagePath() {
        return avatarImagePath;
    }

    public void setAvatarImagePath(String avatarImagePath) {
        this.avatarImagePath = avatarImagePath;
    }

    public boolean hasCustomAvatar() {
        return avatarImagePath != null && new java.io.File(avatarImagePath).exists();
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatarColorHex(int colorHex) {
        this.avatarColorHex = colorHex;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public ConnectionRole getRole() {
        return role;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getAvatarColorHex() {
        return avatarColorHex;
    }

    public String getInitials() {
        if (username == null || username.trim().isEmpty()) return "?";
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private static int generateColor(String name) {
        if (name == null || name.isEmpty()) return 0x6366F1;
        int[] palette = {
                0x6366F1, // Indigo
                0x3B82F6, // Blue
                0x10B981, // Emerald
                0xF59E0B, // Amber
                0x8B5CF6, // Purple
                0xEC4899, // Pink
                0x14B8A6, // Teal
                0xF97316  // Orange
        };
        int hash = Math.abs(name.hashCode());
        return palette[hash % palette.length];
    }
}
