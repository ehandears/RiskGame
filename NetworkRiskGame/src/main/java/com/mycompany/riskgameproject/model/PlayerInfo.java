package com.mycompany.riskgameproject.model;

import java.io.Serializable;

public class PlayerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int playerNumber;
    private final String playerName;

    public PlayerInfo(int playerNumber, String playerName) {
        this.playerNumber = playerNumber;
        this.playerName = playerName;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public String getPlayerName() {
        return playerName;
    }
}
