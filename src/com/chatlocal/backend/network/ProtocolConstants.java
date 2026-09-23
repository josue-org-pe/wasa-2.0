package com.chatlocal.backend.network;

/**
 * Constantes del protocolo binario de sockets para ChatLocal.
 */
public final class ProtocolConstants {

    private ProtocolConstants() {}

    public static final byte TYPE_TEXT = 1;
    public static final byte TYPE_FILE = 2;
    public static final byte TYPE_PING = 3;
    public static final byte TYPE_PONG = 4;
    public static final byte TYPE_HANDSHAKE = 5;
    public static final byte TYPE_USER_LIST = 6;
    public static final byte TYPE_AUDIO = 7;
    public static final byte TYPE_STICKER = 8;
    public static final byte TYPE_ACK = 9;
    public static final byte TYPE_ROOM_EVENT = 10;

    public static final int DEFAULT_PORT = 5000;
    public static final int BUFFER_SIZE = 8192;
    public static final int PING_TIMEOUT_MS = 3500;
}
