package com.mycompany.riskgameproject;

final class AttackBattleSupport {
    private AttackBattleSupport() {}

    static void showAttackableNeighborsFrom(int territoryId, int currentPlayer, boolean placementPhase,
            boolean attackSelectionMode, boolean keepAttackTargetsHighlighted, int selectedAttackFrom,
            int[] territoryOwner, int[] territoryArmies, java.util.List<Integer>[] neighbors,
            javax.swing.JButton[] mapButtons, java.util.List<Integer> highlightedAttackTargets,
            Runnable restoreHighlightedAttackTargets) {
        if (!keepAttackTargetsHighlighted) restoreHighlightedAttackTargets.run();
        if (keepAttackTargetsHighlighted && selectedAttackFrom != 0 && territoryId != selectedAttackFrom) return;
        if (placementPhase || !attackSelectionMode || territoryOwner[territoryId] != currentPlayer || territoryArmies[territoryId] < 2) return;
        for (Integer neighborId : neighbors[territoryId]) {
            if (territoryOwner[neighborId] != currentPlayer) {
                mapButtons[neighborId - 1].setBackground(java.awt.Color.WHITE);
                highlightedAttackTargets.add(neighborId);
            }
        }
    }

    static void captureTerritory(int fromTerritoryId, int targetTerritoryId, int minimumMoveArmies,
            int currentPlayer, int[] territoryOwner, int[] territoryArmies) {
        int movingArmies = Math.min(minimumMoveArmies, territoryArmies[fromTerritoryId] - 1);
        territoryOwner[targetTerritoryId] = currentPlayer;
        territoryArmies[targetTerritoryId] = movingArmies;
        territoryArmies[fromTerritoryId] -= movingArmies;
    }

    static String createShortBattleWinnerText(int attackerPlayer, int defenderPlayer, boolean captured,
            DiceRoller.BattleResult result) {
        if (captured) return "Player " + attackerPlayer + " won and captured the territory.";
        if (result.getDefenderLosses() > result.getAttackerLosses()) return "Player " + attackerPlayer + " won the attack.";
        if (result.getAttackerLosses() > result.getDefenderLosses()) return "Player " + defenderPlayer + " won the defense.";
        return "Both sides lost armies.";
    }

    static String createBattleResultText(DiceRoller.BattleResult result, int currentPlayer, int pendingDefender,
            int pendingAttackTarget, int[] territoryOwner, int[] attackerDiceValues, int[] defenderDiceValues) {
        String attackerName = "Player" + currentPlayer;
        String defenderName = "Player" + pendingDefender;
        String resultMessage;
        if (territoryOwner[pendingAttackTarget] == currentPlayer) {
            resultMessage = defenderName + " lost " + result.getDefenderLosses() + " armies. Territory captured!";
        } else if (result.getDefenderLosses() > 0 && result.getAttackerLosses() > 0) {
            resultMessage = defenderName + " lost " + result.getDefenderLosses() + " armies, " + attackerName + " lost " + result.getAttackerLosses() + " armies.";
        } else if (result.getDefenderLosses() > 0) {
            resultMessage = defenderName + " lost " + result.getDefenderLosses() + " armies.";
        } else {
            resultMessage = attackerName + " lost " + result.getAttackerLosses() + " armies.";
        }
        return "<html>" + attackerName + " rolled " + DiceRoller.formatDice(attackerDiceValues) + "<br>"
                + defenderName + " rolled " + DiceRoller.formatDice(defenderDiceValues) + "<br>Result: " + resultMessage + "</html>";
    }
}