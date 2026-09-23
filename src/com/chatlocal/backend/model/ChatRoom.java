package com.chatlocal.backend.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa una sala de chat, grupo temático o reunión local.
 */
public class ChatRoom implements Serializable {

    private final String id;
    private final String name;
    private final String topic;
    private final String accessCode;
    private final boolean isMeeting;
    private final String createdBy;
    private int unreadCount = 0;
    private int participantCount = 1;

    public ChatRoom(String name, String topic, String accessCode, boolean isMeeting, String createdBy) {
        this(UUID.randomUUID().toString(), name, topic, accessCode, isMeeting, createdBy);
    }

    public ChatRoom(String id, String name, String topic, String accessCode, boolean isMeeting, String createdBy) {
        this.id = id;
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Sala General";
        this.topic = topic != null ? topic.trim() : "";
        this.accessCode = accessCode != null ? accessCode.trim() : "";
        this.isMeeting = isMeeting;
        this.createdBy = createdBy != null ? createdBy : "Sistema";
    }

    public static ChatRoom createDefaultGeneralRoom() {
        return new ChatRoom("general", "Sala General", "Canal principal para todos los conectados", "", false, "Sistema");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public boolean isMeeting() {
        return isMeeting;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void incrementUnread() {
        this.unreadCount++;
    }

    public void resetUnread() {
        this.unreadCount = 0;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public boolean hasPassword() {
        return accessCode != null && !accessCode.isEmpty();
    }
}
