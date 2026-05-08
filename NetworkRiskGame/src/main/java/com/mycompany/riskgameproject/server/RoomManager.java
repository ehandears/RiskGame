package com.mycompany.riskgameproject.server;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {

    private final List<Room> rooms = new ArrayList<>();
    private int nextRoomNumber = 1;

    public synchronized Room assignRoom(ClientHandler client) {
        Room room = findOpenRoom();
        if (room == null) {
            room = new Room("Room " + nextRoomNumber++);
            rooms.add(room);
        }
        room.addClient(client);
        return room;
    }

    public synchronized void remove(ClientHandler client) {
        Room room = client.getRoom();
        if (room != null) {
            room.removeClient(client);
            if (!room.isFull() && room.toRoomInfo().getPlayers().isEmpty()) {
                rooms.remove(room);
            }
        }
    }

    private Room findOpenRoom() {
        for (Room room : rooms) {
            if (!room.isFull()) {
                return room;
            }
        }
        return null;
    }
}