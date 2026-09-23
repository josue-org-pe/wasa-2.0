package com.chatlocal.frontend.controller;

import com.chatlocal.backend.event.ConnectionListener;
import com.chatlocal.backend.event.MessageListener;
import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.backend.service.ChatService;
import com.chatlocal.frontend.view.ChatPanel;
import com.chatlocal.frontend.view.ConfigPanel;
import com.chatlocal.frontend.view.MainFrame;
import com.chatlocal.frontend.view.StatusPanel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Controlador principal (Patrón MVC / MVP).
 * Coordina las operaciones del servicio de backend y la actualización de las vistas en el hilo EDT de Swing.
 */
public class ChatController implements ConnectionListener, MessageListener,
        ConfigPanel.ConfigListener, StatusPanel.StatusActionCallback, ChatPanel.ChatActionListener {

    private final ChatService chatService;
    private final MainFrame mainFrame;
    private final ConfigPanel configPanel;
    private final StatusPanel statusPanel;
    private final ChatPanel chatPanel;

    public ChatController(ChatService chatService, MainFrame mainFrame) {
        this.chatService = chatService;
        this.mainFrame = mainFrame;
        this.configPanel = mainFrame.getConfigPanel();
        this.statusPanel = mainFrame.getStatusPanel();
        this.chatPanel = mainFrame.getChatPanel();

        // Suscribir listeners
        this.chatService.addConnectionListener(this);
        this.chatService.addMessageListener(this);

        this.configPanel.setConfigListener(this);
        this.statusPanel.setActionCallback(this);
        this.chatPanel.setChatActionListener(this);
    }

    // --- Manejo de ConfigPanel.ConfigListener ---

    @Override
    public void onStartHost(int port) {
        mainFrame.showView(MainFrame.VIEW_STATUS);
        statusPanel.updateStatus(ConnectionState.LISTENING, "Iniciando servidor anfitrión en el puerto " + port + "...");
        chatService.startHost(port);
    }

    @Override
    public void onConnectToHost(String hostIp, int port) {
        mainFrame.showView(MainFrame.VIEW_STATUS);
        statusPanel.updateStatus(ConnectionState.CONNECTING, "Estableciendo conexión uwu con " + hostIp + ":" + port + "...");
        chatService.connectToHost(hostIp, port);
    }

    // --- Manejo de StatusPanel.StatusActionCallback ---

    @Override
    public void onCancelOrBackRequested() {
        chatService.disconnect();
        mainFrame.showView(MainFrame.VIEW_CONFIG);
    }

    // --- Manejo de ChatPanel.ChatActionListener ---

    @Override
    public void onSendMessage(String text) {
        new Thread(() -> {
            try {
                chatService.sendTextMessage(text);
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        chatPanel.addMessage(ChatMessage.createSystemMessage("Error al enviar mensaje: " + e.getMessage()))
                );
            }
        }, "send-text-thread").start();
    }

    @Override
    public void onSendFile(File file) {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() ->
                        chatPanel.addMessage(ChatMessage.createSystemMessage("Enviando archivo '" + file.getName() + "'..."))
                );
                chatService.sendFile(file);
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        chatPanel.addMessage(ChatMessage.createSystemMessage("Error al enviar archivo: " + e.getMessage()))
                );
            }
        }, "send-file-thread").start();
    }

    @Override
    public void onDisconnectRequested() {
        chatService.disconnect();
        chatPanel.clearMessages();
        mainFrame.showView(MainFrame.VIEW_CONFIG);
    }

    // --- Callbacks de ConnectionListener (Backend -> Frontend) ---

    @Override
    public void onConnectionStateChanged(ConnectionState newState, String details) {
        SwingUtilities.invokeLater(() -> {
            switch (newState) {
                case LISTENING, CONNECTING, VERIFYING -> {
                    mainFrame.showView(MainFrame.VIEW_STATUS);
                    statusPanel.updateStatus(newState, details);
                }
                case CONNECTED -> {
                    chatPanel.clearMessages();
                    chatPanel.setConnectionInfo(chatService.getPeerAddress(), chatService.getActivePort());
                    chatPanel.addMessage(ChatMessage.createSystemMessage("¡Conexión segura establecida!"));
                    mainFrame.showView(MainFrame.VIEW_CHAT);
                }
                case ERROR -> {
                    statusPanel.updateStatus(newState, details);
                }
                case DISCONNECTED -> {
                    chatPanel.setConnectionStatus(ConnectionState.DISCONNECTED, "Desconectado");
                    chatPanel.addMessage(ChatMessage.createSystemMessage(details));
                }
            }
        });
    }

    @Override
    public void onConnectionError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            statusPanel.updateStatus(ConnectionState.ERROR, errorMessage);
        });
    }

    // --- Callbacks de MessageListener (Backend -> Frontend) ---

    @Override
    public void onMessageReceived(ChatMessage message) {
        chatPanel.addMessage(message);
    }

    @Override
    public void onFileReceived(ChatMessage message) {
        chatPanel.addMessage(message);
    }

    @Override
    public void onMessageSent(ChatMessage message) {
        chatPanel.addMessage(message);
    }
}
