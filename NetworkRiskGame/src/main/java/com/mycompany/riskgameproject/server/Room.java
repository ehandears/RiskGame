package com.mycompany.riskgameproject.server;

import com.mycompany.riskgameproject.common.GameState;
import com.mycompany.riskgameproject.common.Message;
import com.mycompany.riskgameproject.common.MessageType;
import com.mycompany.riskgameproject.model.PlayerInfo;
import com.mycompany.riskgameproject.model.RoomInfo;
import java.util.ArrayList;
import java.util.List;

public class Room {

    public static final int MAX_PLAYERS = 2;

    private final String roomCode;
    private final List<ClientHandler> clients = new ArrayList<>();
    private GameState gameState;

    public Room(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public synchronized boolean isFull() {
        return clients.size() >= MAX_PLAYERS;
    }

    public synchronized int addClient(ClientHandler client) {
        if (isFull()) {
            throw new IllegalStateException("Room is full");
        }
        clients.add(client);
        int playerNumber = clients.size();
        client.joinRoom(this, playerNumber);
        broadcastLobbyUpdate();
        if (isFull()) {
            gameState = GameState.newTwoPlayerGame();
            broadcast(new Message(MessageType.START_GAME, roomCode, "SERVER", "Room is full. Game can start.", gameState));
        }
        return playerNumber;
    }

    public synchronized void removeClient(ClientHandler client) {
        int leftPlayer = client.getPlayerNumber();
        if (!clients.remove(client)) {
            return;
        }
        if (!clients.isEmpty()) {
            broadcast(new Message(MessageType.PLAYER_LEFT, roomCode, "SERVER", String.valueOf(leftPlayer)));
            return;
        }
        broadcastLobbyUpdate();
    }

    public synchronized RoomInfo toRoomInfo() {
        RoomInfo info = new RoomInfo(roomCode);
        for (ClientHandler client : clients) {
            info.addPlayer(new PlayerInfo(client.getPlayerNumber(), client.getPlayerName()));
        }
        return info;
    }

    public synchronized void relay(ClientHandler sender, Message message) {
        if (message.getType() == MessageType.REPLAY_REQUEST) {
            restartGame();
            return;
        }
        if (message.getType() == MessageType.PLAYER_LEFT) {
            handlePlayerLeft(sender, message);
            return;
        }
        if (message.getType() == MessageType.GAME_STATE && message.getPayload() instanceof GameState state) {
            gameState = state;
        }
        // Server burada gelen oyun mesajini odadaki diger oyuncuya dagitiyor.
        Message relayed = new Message(message.getType(), roomCode, sender.getPlayerName(), message.getText(), message.getPayload());
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send(relayed);
            }
        }
    }

    private void restartGame() {
        if (!isFull()) {
            return;
        }
        gameState = GameState.newTwoPlayerGame();
        broadcast(new Message(MessageType.START_GAME, roomCode, "SERVER", "Replay started.", gameState));
    }

    private void handlePlayerLeft(ClientHandler sender, Message message) {
        Message playerLeft = new Message(MessageType.PLAYER_LEFT, roomCode, sender.getPlayerName(), message.getText(), message.getPayload());
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send(playerLeft);
            }
        }
        clients.remove(sender);
    }

    public synchronized void broadcast(Message message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    private void broadcastLobbyUpdate() {
        broadcast(new Message(MessageType.LOBBY_UPDATE, roomCode, "SERVER", "Room updated", toRoomInfo()));
    }
}
