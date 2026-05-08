package com.mycompany.riskgameproject;

final class FortifySupport {
    private FortifySupport() {}

    static void showFortifyPreviewFrom(int territoryId, boolean fortifySelectionMode, int currentPlayer,
            int[] territoryOwner, int[] territoryArmies, java.util.List<Integer>[] neighbors,
            javax.swing.JButton[] mapButtons, java.util.List<Integer> highlightedAttackTargets,
            Runnable restoreHighlightedAttackTargets) {
        restoreHighlightedAttackTargets.run();
        if (!fortifySelectionMode || territoryOwner[territoryId] != currentPlayer || territoryArmies[territoryId] < 2) return;
        highlightFriendlyNeighbors(territoryId, currentPlayer, territoryOwner, neighbors, mapButtons, highlightedAttackTargets);
    }

    static boolean hasOwnNeighbor(int territoryId, int currentPlayer, int[] territoryOwner, java.util.List<Integer>[] neighbors) {
        for (Integer neighborId : neighbors[territoryId]) if (territoryOwner[neighborId] == currentPlayer) return true;
        return false;
    }

    static void showFortifyableNeighborsFrom(int territoryId, int currentPlayer, int[] territoryOwner,
            java.util.List<Integer>[] neighbors, javax.swing.JButton[] mapButtons,
            java.util.List<Integer> highlightedAttackTargets, Runnable restoreHighlightedAttackTargets) {
        restoreHighlightedAttackTargets.run();
        highlightFriendlyNeighbors(territoryId, currentPlayer, territoryOwner, neighbors, mapButtons, highlightedAttackTargets);
    }

    static int getNextActivePlayer(int fromPlayer, int playerCount, java.util.function.IntUnaryOperator countTerritories) {
        for (int step = 1; step <= playerCount; step++) {
            int player = ((fromPlayer + step - 1) % playerCount) + 1;
            if (countTerritories.applyAsInt(player) > 0) return player;
        }
        return fromPlayer;
    }

    private static void highlightFriendlyNeighbors(int territoryId, int currentPlayer, int[] territoryOwner,
            java.util.List<Integer>[] neighbors, javax.swing.JButton[] mapButtons,
            java.util.List<Integer> highlightedAttackTargets) {
        for (Integer neighborId : neighbors[territoryId]) {
            if (territoryOwner[neighborId] == currentPlayer) {
                mapButtons[neighborId - 1].setBackground(java.awt.Color.WHITE);
                highlightedAttackTargets.add(neighborId);
            }
        }
    }
}