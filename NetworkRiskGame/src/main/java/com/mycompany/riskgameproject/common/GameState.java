package com.mycompany.riskgameproject.common;

import com.mycompany.riskgameproject.RiskGameLogic;
import java.io.Serializable;
import java.util.Arrays;

public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int currentPlayer;
    private final int[] territoryOwner;
    private final int[] territoryArmies;
    private final int[] remainingArmies;
    private final int[] highlightedTerritories;
    private final boolean placementPhase;
    private final boolean initialPlacementPhase;
    private final String currentPhaseName;
    private final long sharedPopupId;
    private final String sharedPopupEvents;
    private final boolean battleInProgress;
    private final int pendingAttackFrom;
    private final int pendingAttackTarget;
    private final int pendingDefender;
    private final int attackerDiceCount;
    private final int defenderDiceCount;
    private final int[] attackerDiceValues;
    private final int[] defenderDiceValues;
    private final int attackerRolledCount;
    private final int defenderRolledCount;
    private final boolean attackerRolled;
    private final boolean defenderRolled;
    private final String battleInfoText;

    public GameState(int currentPlayer, int[] territoryOwner, int[] territoryArmies, int[] remainingArmies,
            boolean placementPhase, boolean initialPlacementPhase, String currentPhaseName) {
        this(currentPlayer, territoryOwner, territoryArmies, remainingArmies, placementPhase,
                initialPlacementPhase, currentPhaseName, new int[0], 0L, "");
    }

    public GameState(int currentPlayer, int[] territoryOwner, int[] territoryArmies, int[] remainingArmies,
            boolean placementPhase, boolean initialPlacementPhase, String currentPhaseName, int[] highlightedTerritories) {
        this(currentPlayer, territoryOwner, territoryArmies, remainingArmies, placementPhase,
                initialPlacementPhase, currentPhaseName, highlightedTerritories, 0L, "");
    }

    public GameState(int currentPlayer, int[] territoryOwner, int[] territoryArmies, int[] remainingArmies,
            boolean placementPhase, boolean initialPlacementPhase, String currentPhaseName, int[] highlightedTerritories,
            long sharedPopupId, String sharedPopupEvents) {
        this(currentPlayer, territoryOwner, territoryArmies, remainingArmies, placementPhase,
                initialPlacementPhase, currentPhaseName, highlightedTerritories, sharedPopupId, sharedPopupEvents,
                false, 0, 0, 0, 0, 0, new int[0], new int[0], 0, 0, false, false, "");
    }

    public GameState(int currentPlayer, int[] territoryOwner, int[] territoryArmies, int[] remainingArmies,
            boolean placementPhase, boolean initialPlacementPhase, String currentPhaseName, int[] highlightedTerritories,
            long sharedPopupId, String sharedPopupEvents, boolean battleInProgress, int pendingAttackFrom,
            int pendingAttackTarget, int pendingDefender, int attackerDiceCount, int defenderDiceCount,
            int[] attackerDiceValues, int[] defenderDiceValues, int attackerRolledCount, int defenderRolledCount,
            boolean attackerRolled, boolean defenderRolled, String battleInfoText) {
        this.currentPlayer = currentPlayer;
        this.territoryOwner = Arrays.copyOf(territoryOwner, territoryOwner.length);
        this.territoryArmies = Arrays.copyOf(territoryArmies, territoryArmies.length);
        this.remainingArmies = Arrays.copyOf(remainingArmies, remainingArmies.length);
        this.highlightedTerritories = Arrays.copyOf(highlightedTerritories, highlightedTerritories.length);
        this.placementPhase = placementPhase;
        this.initialPlacementPhase = initialPlacementPhase;
        this.currentPhaseName = currentPhaseName;
        this.sharedPopupId = sharedPopupId;
        this.sharedPopupEvents = sharedPopupEvents == null ? "" : sharedPopupEvents;
        this.battleInProgress = battleInProgress;
        this.pendingAttackFrom = pendingAttackFrom;
        this.pendingAttackTarget = pendingAttackTarget;
        this.pendingDefender = pendingDefender;
        this.attackerDiceCount = attackerDiceCount;
        this.defenderDiceCount = defenderDiceCount;
        this.attackerDiceValues = Arrays.copyOf(attackerDiceValues, attackerDiceValues.length);
        this.defenderDiceValues = Arrays.copyOf(defenderDiceValues, defenderDiceValues.length);
        this.attackerRolledCount = attackerRolledCount;
        this.defenderRolledCount = defenderRolledCount;
        this.attackerRolled = attackerRolled;
        this.defenderRolled = defenderRolled;
        this.battleInfoText = battleInfoText == null ? "" : battleInfoText;
    }

    public static GameState newTwoPlayerGame() {
        RiskGameLogic logic = new RiskGameLogic(2);
        return new GameState(logic.getCurrentPlayer(), logic.getTerritoryOwner(), logic.getTerritoryArmies(),
                logic.getRemainingArmies(), true, true, "Placement Phase");
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int[] getTerritoryOwner() {
        return Arrays.copyOf(territoryOwner, territoryOwner.length);
    }

    public int[] getTerritoryArmies() {
        return Arrays.copyOf(territoryArmies, territoryArmies.length);
    }

    public int[] getRemainingArmies() {
        return Arrays.copyOf(remainingArmies, remainingArmies.length);
    }

    public int[] getHighlightedTerritories() {
        return Arrays.copyOf(highlightedTerritories, highlightedTerritories.length);
    }

    public boolean isPlacementPhase() {
        return placementPhase;
    }

    public boolean isInitialPlacementPhase() {
        return initialPlacementPhase;
    }

    public String getCurrentPhaseName() {
        return currentPhaseName;
    }

    public long getSharedPopupId() {
        return sharedPopupId;
    }

    public String getSharedPopupEvents() {
        return sharedPopupEvents;
    }

    public boolean isBattleInProgress() {
        return battleInProgress;
    }

    public int getPendingAttackFrom() {
        return pendingAttackFrom;
    }

    public int getPendingAttackTarget() {
        return pendingAttackTarget;
    }

    public int getPendingDefender() {
        return pendingDefender;
    }

    public int getAttackerDiceCount() {
        return attackerDiceCount;
    }

    public int getDefenderDiceCount() {
        return defenderDiceCount;
    }

    public int[] getAttackerDiceValues() {
        return Arrays.copyOf(attackerDiceValues, attackerDiceValues.length);
    }

    public int[] getDefenderDiceValues() {
        return Arrays.copyOf(defenderDiceValues, defenderDiceValues.length);
    }

    public int getAttackerRolledCount() {
        return attackerRolledCount;
    }

    public int getDefenderRolledCount() {
        return defenderRolledCount;
    }

    public boolean isAttackerRolled() {
        return attackerRolled;
    }

    public boolean isDefenderRolled() {
        return defenderRolled;
    }

    public String getBattleInfoText() {
        return battleInfoText;
    }
}
