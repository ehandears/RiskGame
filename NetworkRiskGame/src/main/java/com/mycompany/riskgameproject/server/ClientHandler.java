package com.mycompany.riskgameproject.server;

import com.mycompany.riskgameproject.common.Message;
import com.mycompany.riskgameproject.common.MessageType;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RoomManager roomManager;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String playerName = "Player";
    private Room room;
    private int playerNumber;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            listen();
        } catch (EOFException ex) {
            // Client closed normally.
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Client connection closed: " + ex.getMessage());
        } finally {
            close();
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public Room getRoom() {
        return room;
    }

    public void joinRoom(Room room, int playerNumber) {
        this.room = room;
        this.playerNumber = playerNumber;
        send(new Message(MessageType.ROOM_ASSIGNED, room.getRoomCode(), "SERVER", String.valueOf(playerNumber), room.toRoomInfo()));
    }

    public synchronized void send(Message message) {
        if (output == null) {
            return;
        }
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException ex) {
            close();
        }
    }

    private void listen() throws IOException, ClassNotFoundException {
        while (!socket.isClosed()) {
            Object received = input.readObject();
            if (received instanceof Message message) {
                handle(message);
            }
        }
    }

    private void handle(Message message) {
        if (message.getType() == MessageType.CONNECT) {
            playerName = cleanName(message.getSenderName());
            roomManager.assignRoom(this);
            return;
        }
        if (room == null) {
            send(new Message(MessageType.ERROR, null, "SERVER", "You are not in a room yet."));
            return;
        }
        if (message.getType() == MessageType.DISCONNECT) {
            close();
            return;
        }
        room.relay(this, message);
    }

    private String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Player";
        }
        return name.trim();
    }

    private void close() {
        roomManager.remove(this);
        try {
            socket.close();
        } catch (IOException ex) {
            // Already closed.
        }
    }
}
