package com.mycompany.riskgameproject.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String roomCode;
    private final List<PlayerInfo> players = new ArrayList<>();

    public RoomInfo(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public List<PlayerInfo> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void addPlayer(PlayerInfo player) {
        players.add(player);
    }
}
