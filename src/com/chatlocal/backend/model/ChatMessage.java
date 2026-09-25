package com.chatlocal.backend.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Representa un mensaje intercambiado (Texto, Archivo, Nota de Voz, Sticker, Sistema).
 * Contiene metadatos de confirmación de entrega (MessageStatus), duración de audios
 * y color de remitente para renderizado de alta calidad.
 */
public class ChatMessage {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String id;
    private final String sender;
    private final int senderColorHex;
    private final String content;
    private final String timestamp;
    private final MessageType type;
    private final boolean isSelf;
    private MessageStatus status;
    private final String roomId;

    // Campos para archivos y notas de voz
    private final String fileName;
    private final long fileSize;
    private final String localFilePath;
    private final int audioDurationSeconds;
    private String senderAvatarPath = null;
    private String recipient = null; // null o "general" para sala, o nombre de usuario/IP para privado

    public ChatMessage(String sender, int senderColorHex, String content, MessageType type,
                       boolean isSelf, String fileName, long fileSize, String localFilePath,
                       int audioDurationSeconds, String roomId) {
        this(UUID.randomUUID().toString(), sender, senderColorHex, content, type, isSelf, fileName, fileSize, localFilePath, audioDurationSeconds, roomId, null, null);
    }

    public ChatMessage(String id, String sender, int senderColorHex, String content, MessageType type,
                       boolean isSelf, String fileName, long fileSize, String localFilePath,
                       int audioDurationSeconds, String roomId, String senderAvatarPath, String recipient) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.sender = (sender != null && !sender.isEmpty()) ? sender : "Usuario";
        this.senderColorHex = senderColorHex;
        this.content = content != null ? content : "";
        this.timestamp = LocalTime.now().format(TIME_FORMATTER);
        this.type = type;
        this.isSelf = isSelf;
        this.status = isSelf ? MessageStatus.SENT : MessageStatus.DELIVERED;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.localFilePath = localFilePath;
        this.audioDurationSeconds = audioDurationSeconds;
        this.roomId = (roomId != null && !roomId.isEmpty()) ? roomId : "general";
        this.senderAvatarPath = senderAvatarPath;
        this.recipient = recipient;
    }

    public static ChatMessage createTextMessage(String sender, String text, boolean isSelf) {
        return createTextMessage(sender, generateColor(sender), text, isSelf, "general");
    }

    public static ChatMessage createTextMessage(String sender, int senderColorHex, String text, boolean isSelf, String roomId) {
        return new ChatMessage(sender, senderColorHex, text, MessageType.TEXT, isSelf, null, 0, null, 0, roomId);
    }

    public static ChatMessage createAudioMessage(String sender, int senderColorHex, String localFilePath, int durationSecs, boolean isSelf, String roomId) {
        String desc = "Nota de voz (" + durationSecs + "s)";
        return new ChatMessage(sender, senderColorHex, desc, MessageType.AUDIO, isSelf, "audio_nota.wav", 0, localFilePath, durationSecs, roomId);
    }

    public static ChatMessage createStickerMessage(String sender, int senderColorHex, String stickerText, boolean isSelf, String roomId) {
        return new ChatMessage(sender, senderColorHex, stickerText, MessageType.STICKER, isSelf, null, 0, null, 0, roomId);
    }

    public static ChatMessage createFileMessage(String sender, String fileName, long fileSize, String localFilePath, boolean isSelf) {
        return createFileMessage(sender, generateColor(sender), fileName, fileSize, localFilePath, isSelf, "general");
    }

    public static ChatMessage createFileMessage(String sender, int senderColorHex, String fileName, long fileSize, String localFilePath, boolean isSelf, String roomId) {
        String desc = isSelf ? "Has enviado un archivo" : "Has recibido un archivo";
        return new ChatMessage(sender, senderColorHex, desc, MessageType.FILE, isSelf, fileName, fileSize, localFilePath, 0, roomId);
    }

    public static ChatMessage createSystemMessage(String text) {
        return new ChatMessage("Sistema", 0x94A3B8, text, MessageType.SYSTEM, false, null, 0, null, 0, "general");
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public int getSenderColorHex() {
        return senderColorHex;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public int getAudioDurationSeconds() {
        return audioDurationSeconds;
    }

    public String getSenderAvatarPath() {
        return senderAvatarPath;
    }

    public void setSenderAvatarPath(String path) {
        this.senderAvatarPath = path;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public boolean isPrivate() {
        return recipient != null && !recipient.isEmpty() && !recipient.equalsIgnoreCase("general");
    }

    public String getFormattedAudioDuration() {
        int minutes = audioDurationSeconds / 60;
        int seconds = audioDurationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    private static int generateColor(String name) {
        if (name == null || name.isEmpty()) return 0x6366F1;
        int[] palette = {
                0x6366F1, 0x3B82F6, 0x10B981, 0xF59E0B,
                0x8B5CF6, 0xEC4899, 0x14B8A6, 0xF97316
        };
        int hash = Math.abs(name.hashCode());
        return palette[hash % palette.length];
    }
}
