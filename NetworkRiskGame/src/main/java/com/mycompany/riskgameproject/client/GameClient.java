package com.mycompany.riskgameproject.client;

import com.mycompany.riskgameproject.common.Message;
import com.mycompany.riskgameproject.common.MessageType;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GameClient {

    public static final String DEFAULT_HOST = "13.51.204.239";
    public static final int DEFAULT_PORT = 5000;

    private final String host;
    private final int port;
    private final MessageListener listener;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public GameClient(MessageListener listener) {
        this(DEFAULT_HOST, DEFAULT_PORT, listener);
    }

    public GameClient(String host, int port, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect(String playerName) throws IOException {
        // Client burada server'a baglaniyor, bundan sonrasi object stream ile mesajlasma.
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
        Thread listenerThread = new Thread(this::listen, "server-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        send(new Message(MessageType.CONNECT, null, playerName, "connect"));
    }

    public synchronized void send(Message message) throws IOException {
        if (output == null) {
            throw new IOException("Client is not connected.");
        }
        output.writeObject(message);
        output.flush();
    }

    public void sendMessage(MessageType type, String roomCode, String senderName, String text) throws IOException {
        send(new Message(type, roomCode, senderName, text));
    }

    public void sendMessage(MessageType type, String roomCode, String senderName, String text, java.io.Serializable payload) throws IOException {
        send(new Message(type, roomCode, senderName, text, payload));
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            // Already closed.
        }
    }

    private void listen() {
        try {
            while (socket != null && !socket.isClosed()) {
                Object received = input.readObject();
                if (received instanceof Message message && listener != null) {
                    listener.onMessage(message);
                }
            }
        } catch (EOFException ex) {
            notifyDisconnected("Server closed the connection.");
        } catch (IOException | ClassNotFoundException ex) {
            notifyDisconnected(ex.getMessage());
        }
    }

    private void notifyDisconnected(String detail) {
        if (listener != null) {
            listener.onMessage(new Message(MessageType.DISCONNECT, null, "SERVER", detail));
        }
    }

    public interface MessageListener {
        void onMessage(Message message);
    }
}
