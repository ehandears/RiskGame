package com.mycompany.riskgameproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RiskGameLogic {

    public static final int TERRITORY_COUNT = 42;

    private final int playerCount;
    private int currentPlayer = 1;
    private final int[] territoryOwner = new int[TERRITORY_COUNT + 1];
    private final int[] territoryArmies = new int[TERRITORY_COUNT + 1];
    private final int[] remainingArmies;
    private final List<Integer>[] neighbors;

    public RiskGameLogic(int playerCount) {
        this.playerCount = playerCount;
        this.remainingArmies = new int[playerCount + 1];
        this.neighbors = createNeighbors();
        assignInitialTerritories();
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int[] getTerritoryOwner() {
        return territoryOwner;
    }

    public int[] getTerritoryArmies() {
        return territoryArmies;
    }

    public int[] getRemainingArmies() {
        return remainingArmies;
    }

    public List<Integer>[] getNeighbors() {
        return neighbors;
    }

    public boolean canPlaceArmy(int territoryId, int player, int armyCount) {
        return isTerritory(territoryId)
                && territoryOwner[territoryId] == player
                && armyCount > 0
                && remainingArmies[player] >= armyCount;
    }

    public void placeArmies(int territoryId, int player, int armyCount) {
        territoryArmies[territoryId] += armyCount;
        remainingArmies[player] -= armyCount;
    }

    public boolean canAttack(int fromTerritoryId, int targetTerritoryId, int player) {
        return isTerritory(fromTerritoryId)
                && isTerritory(targetTerritoryId)
                && territoryOwner[fromTerritoryId] == player
                && territoryArmies[fromTerritoryId] >= 2
                && territoryOwner[targetTerritoryId] != player
                && neighbors[fromTerritoryId].contains(targetTerritoryId);
    }

    public boolean canFortify(int fromTerritoryId, int targetTerritoryId, int player) {
        return isTerritory(fromTerritoryId)
                && isTerritory(targetTerritoryId)
                && territoryOwner[fromTerritoryId] == player
                && territoryOwner[targetTerritoryId] == player
                && territoryArmies[fromTerritoryId] > 1
                && neighbors[fromTerritoryId].contains(targetTerritoryId);
    }

    public void fortifyOneArmy(int fromTerritoryId, int targetTerritoryId) {
        territoryArmies[fromTerritoryId]--;
        territoryArmies[targetTerritoryId]++;
    }

    public int countTerritories(int player) {
        int total = 0;
        for (int territoryId = 1; territoryId < territoryOwner.length; territoryId++) {
            if (territoryOwner[territoryId] == player) {
                total++;
            }
        }
        return total;
    }

    public int getReinforcementArmies(int player) {
        return countTerritories(player) / 3;
    }

    public int countArmies(int player) {
        int total = 0;
        for (int territoryId = 1; territoryId < territoryArmies.length; territoryId++) {
            if (territoryOwner[territoryId] == player) {
                total += territoryArmies[territoryId];
            }
        }
        return total;
    }

    private void assignInitialTerritories() {
        int[] territoryCounts = getTerritoryCounts(playerCount);
        int[] startingArmies = getStartingArmies(playerCount);

        List<Integer> territories = new ArrayList<>();
        for (int territoryId = 1; territoryId <= TERRITORY_COUNT; territoryId++) {
            territories.add(territoryId);
        }
        Collections.shuffle(territories);

        int territoryIndex = 0;
        for (int player = 1; player <= playerCount; player++) {
            remainingArmies[player] = startingArmies[player - 1] - territoryCounts[player - 1];
            for (int i = 0; i < territoryCounts[player - 1]; i++) {
                int territoryId = territories.get(territoryIndex);
                territoryOwner[territoryId] = player;
                territoryArmies[territoryId] = 1;
                territoryIndex++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Integer>[] createNeighbors() {
        List<Integer>[] createdNeighbors = new ArrayList[TERRITORY_COUNT + 1];
        for (int i = 1; i <= TERRITORY_COUNT; i++) {
            createdNeighbors[i] = new ArrayList<>();
        }

        connect(createdNeighbors, 1, 2);
        connect(createdNeighbors, 1, 3);
        connect(createdNeighbors, 2, 3);
        connect(createdNeighbors, 2, 4);
        connect(createdNeighbors, 3, 4);
        connect(createdNeighbors, 4, 5);
        connect(createdNeighbors, 5, 6);
        connect(createdNeighbors, 5, 13);
        connect(createdNeighbors, 5, 14);
        connect(createdNeighbors, 6, 7);
        connect(createdNeighbors, 6, 11);
        connect(createdNeighbors, 6, 13);
        connect(createdNeighbors, 7, 8);
        connect(createdNeighbors, 7, 11);
        connect(createdNeighbors, 8, 9);
        connect(createdNeighbors, 8, 11);
        connect(createdNeighbors, 9, 10);
        connect(createdNeighbors, 9, 11);
        connect(createdNeighbors, 10, 11);
        connect(createdNeighbors, 10, 12);
        connect(createdNeighbors, 10, 15);
        connect(createdNeighbors, 11, 12);
        connect(createdNeighbors, 12, 13);
        connect(createdNeighbors, 12, 15);
        connect(createdNeighbors, 12, 16);
        connect(createdNeighbors, 13, 14);
        connect(createdNeighbors, 13, 16);
        connect(createdNeighbors, 14, 16);
        connect(createdNeighbors, 15, 16);
        connect(createdNeighbors, 15, 17);
        connect(createdNeighbors, 16, 17);
        connect(createdNeighbors, 16, 20);
        connect(createdNeighbors, 16, 21);
        connect(createdNeighbors, 17, 18);
        connect(createdNeighbors, 17, 19);
        connect(createdNeighbors, 17, 20);
        connect(createdNeighbors, 18, 19);
        connect(createdNeighbors, 18, 29);
        connect(createdNeighbors, 18, 30);
        connect(createdNeighbors, 19, 20);
        connect(createdNeighbors, 19, 29);
        connect(createdNeighbors, 20, 21);
        connect(createdNeighbors, 20, 28);
        connect(createdNeighbors, 20, 29);
        connect(createdNeighbors, 21, 22);
        connect(createdNeighbors, 21, 28);
        connect(createdNeighbors, 22, 23);
        connect(createdNeighbors, 22, 25);
        connect(createdNeighbors, 22, 28);
        connect(createdNeighbors, 23, 24);
        connect(createdNeighbors, 23, 25);
        connect(createdNeighbors, 24, 25);
        connect(createdNeighbors, 25, 26);
        connect(createdNeighbors, 26, 27);
        connect(createdNeighbors, 26, 28);
        connect(createdNeighbors, 26, 36);
        connect(createdNeighbors, 27, 28);
        connect(createdNeighbors, 27, 29);
        connect(createdNeighbors, 27, 36);
        connect(createdNeighbors, 28, 29);
        connect(createdNeighbors, 30, 31);
        connect(createdNeighbors, 31, 32);
        connect(createdNeighbors, 31, 37);
        connect(createdNeighbors, 32, 30);
        connect(createdNeighbors, 38, 33);
        connect(createdNeighbors, 22, 26);
        connect(createdNeighbors, 32, 33);
        connect(createdNeighbors, 32, 37);
        connect(createdNeighbors, 33, 34);
        connect(createdNeighbors, 33, 35);
        connect(createdNeighbors, 33, 37);
        connect(createdNeighbors, 34, 35);
        connect(createdNeighbors, 34, 37);
        connect(createdNeighbors, 35, 38);
        connect(createdNeighbors, 36, 40);
        connect(createdNeighbors, 36, 27);
        connect(createdNeighbors, 36, 26);
        connect(createdNeighbors, 37, 31);
        connect(createdNeighbors, 37, 32);
        connect(createdNeighbors, 37, 33);
        connect(createdNeighbors, 37, 34);
        connect(createdNeighbors, 38, 39);
        connect(createdNeighbors, 38, 40);
        connect(createdNeighbors, 39, 40);
        connect(createdNeighbors, 39, 41);
        connect(createdNeighbors, 39, 42);
        connect(createdNeighbors, 40, 41);
        connect(createdNeighbors, 40, 36);
        connect(createdNeighbors, 41, 42);

        return createdNeighbors;
    }

    private void connect(List<Integer>[] createdNeighbors, int first, int second) {
        createdNeighbors[first].add(second);
        createdNeighbors[second].add(first);
    }

    private boolean isTerritory(int territoryId) {
        return territoryId >= 1 && territoryId <= TERRITORY_COUNT;
    }

    private int[] getTerritoryCounts(int playerCount) {
        return new int[]{21, 21};
    }

    private int[] getStartingArmies(int playerCount) {
        return new int[]{40, 40};
    }
}
