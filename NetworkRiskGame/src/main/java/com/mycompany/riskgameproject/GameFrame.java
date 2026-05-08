package com.mycompany.riskgameproject;

import com.mycompany.riskgameproject.common.GameState;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class GameFrame extends javax.swing.JFrame {

    private int playerCount;
    private int currentPlayer = 1;
    private int[] territoryOwner;
    private int[] territoryArmies;
    private int[] remainingArmies;
    private RiskGameLogic gameLogic;
    private javax.swing.JButton[] mapButtons;
    private javax.swing.JTextField armyInputField;
    private java.util.List<Integer>[] neighbors;
    private java.util.List<Integer> highlightedAttackTargets = new java.util.ArrayList<>();
    private boolean placementPhase = true;
    private boolean initialPlacementPhase = true;
    private boolean attackSelectionMode = false;
    private boolean fortifySelectionMode = false;
    private String currentPhaseName = "Placement Phase";
    private int selectedAttackFrom = 0;
    private int selectedFortifyFrom = 0;
    private int selectedFortifyTarget = 0;
    private javax.swing.JLabel attackerDiceLabel;
    private javax.swing.JLabel defenderDiceLabel;
    private javax.swing.JLabel battleInfoLabel;
    private javax.swing.JLabel[] attackerDiceSlots;
    private javax.swing.JLabel[] defenderDiceSlots;
    private javax.swing.DefaultListModel<String> playerListModel;
    private javax.swing.JList<String> playerStatsList;
    private javax.swing.JScrollPane playerStatsScrollPane;
    private int pendingAttackFrom = 0;
    private int pendingAttackTarget = 0;
    private int pendingDefender = 0;
    private int attackerDiceCount = 0;
    private int defenderDiceCount = 0;
    private int[] attackerDiceValues;
    private int[] defenderDiceValues;
    private int attackerRolledCount = 0;
    private int defenderRolledCount = 0;
    private boolean attackerRolled = false;
    private boolean defenderRolled = false;
    private boolean diceRolling = false;
    private boolean battleInProgress = false;
    private boolean keepAttackTargetsHighlighted = false;
    private int localPlayerNumber = 0;
    private Consumer<GameState> stateSender;
    private IntConsumer gameOverSender;
    private Runnable replayRequester;
    private Runnable returnToLobbyRequester;
    private IntConsumer playerLeftSender;
    private EndFrame endFrame;
    private boolean applyingRemoteState = false;
    private GameFrameSupport.ResponsiveLayout responsiveGameLayout;
    private long sharedPopupSequence = 0L;
    private long lastSharedPopupId = 0L;
    private GameState initialPopupState;
    private final StringBuilder pendingSharedPopupEvents = new StringBuilder();
    private final java.awt.Color[] playerColors = {
        java.awt.Color.RED,
        java.awt.Color.BLUE
    };

    public GameFrame(GameState initialState, int localPlayerNumber, Consumer<GameState> stateSender, IntConsumer gameOverSender, Runnable replayRequester, Runnable returnToLobbyRequester, IntConsumer playerLeftSender) {
        initComponents();
        this.localPlayerNumber = localPlayerNumber;
        this.stateSender = stateSender;
        this.gameOverSender = gameOverSender;
        this.replayRequester = replayRequester;
        this.returnToLobbyRequester = returnToLobbyRequester;
        this.playerLeftSender = playerLeftSender;
        initializeFromState(initialState);
        configureDisconnectOnClose();
        setTitle("NetworkRiskGame - Player " + localPlayerNumber);
    }

    private void configureDisconnectOnClose() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                if (playerLeftSender != null) {
                    playerLeftSender.accept(localPlayerNumber);
                }
                dispose();
            }
        });
    }
    private void initializeFromState(GameState state) {
        playerCount = 2;
        gameLogic = new RiskGameLogic(playerCount);
        currentPlayer = state.getCurrentPlayer();
        currentPhaseName = state.getCurrentPhaseName();
        placementPhase = state.isPlacementPhase();
        initialPlacementPhase = state.isInitialPlacementPhase();
        territoryOwner = gameLogic.getTerritoryOwner();
        territoryArmies = gameLogic.getTerritoryArmies();
        remainingArmies = gameLogic.getRemainingArmies();
        copyInto(state.getTerritoryOwner(), territoryOwner);
        copyInto(state.getTerritoryArmies(), territoryArmies);
        copyInto(state.getRemainingArmies(), remainingArmies);
        gameLogic.setCurrentPlayer(currentPlayer);
        neighbors = gameLogic.getNeighbors();
        mapButtons = createMapButtonArray();
        applyMapStateToButtons();
        addArmyInputField();
        styleRightPanel();
        addPlayerStatsList();
        addDiceDisplayLabels();
        addPlacementClickListeners();
        addAttackHoverListeners();
        addEndTurnListener();
        updatePlacementLabels();
        configureResponsiveGameLayout();
        initialPopupState = state;
    }

    public void showInitialPlacementPopup() {
        javax.swing.Timer timer = new javax.swing.Timer(350, evt -> {
            if (initialPopupState != null && initialPopupState.getSharedPopupId() != 0L) {
                showSharedPopupsFromState(initialPopupState);
                initialPopupState = null;
            } else {
                showPlacementTurnPopup();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void restartGame(GameState state) {
        if (endFrame != null) {
            endFrame.dispose();
            endFrame = null;
        }
        lastSharedPopupId = 0L;
        pendingSharedPopupEvents.setLength(0);
        applyRemoteState(state);
        setVisible(true);
        showInitialPlacementPopup();
    }

    public void showEndFrame(int winnerPlayer) {
        showEndFrame(new EndFrame(winnerPlayer, () -> {
            if (replayRequester != null) {
                replayRequester.run();
            }
        }));
    }

    public void showPlayerLeftFrame(int leftPlayer) {
        if (endFrame != null && endFrame.isDisplayable()) {
            endFrame.toFront();
            return;
        }
        endFrame = new EndFrame("PLAYER " + leftPlayer + " LEFT", "Game over", () -> {
            if (endFrame != null) {
                endFrame.dispose();
                endFrame = null;
            }
            if (returnToLobbyRequester != null) {
                returnToLobbyRequester.run();
            }
        });
        showEndFrame(endFrame);
    }

    private void showEndFrame(EndFrame frame) {
        if (endFrame != null && endFrame.isDisplayable() && endFrame != frame) {
            endFrame.toFront();
            return;
        }
        endFrame = frame;
        endFrame.setLocationRelativeTo(this);
        endFrame.setVisible(true);
        endFrame.toFront();
    }
    public void applyRemoteState(GameState state) {
        applyingRemoteState = true;
        int previousPlayer = currentPlayer;
        String previousPhase = currentPhaseName;
        currentPlayer = state.getCurrentPlayer();
        currentPhaseName = state.getCurrentPhaseName();
        placementPhase = state.isPlacementPhase();
        initialPlacementPhase = state.isInitialPlacementPhase();
        copyInto(state.getTerritoryOwner(), territoryOwner);
        copyInto(state.getTerritoryArmies(), territoryArmies);
        copyInto(state.getRemainingArmies(), remainingArmies);
        gameLogic.setCurrentPlayer(currentPlayer);
        attackSelectionMode = false;
        fortifySelectionMode = false;
        selectedAttackFrom = 0;
        selectedFortifyFrom = 0;
        selectedFortifyTarget = 0;
        keepAttackTargetsHighlighted = false;
        restoreHighlightedAttackTargets();
        applyMapStateToButtons();
        applyRemoteHighlights(state.getHighlightedTerritories());
        applyBattleState(state);
        updatePlacementLabels();
        applyBattleInfoText(state.getBattleInfoText());
        showSharedPopupsFromState(state);
        applyingRemoteState = false;
    }

    private void queueSharedPopup(String type, int player, int amount) {
        if (applyingRemoteState) {
            return;
        }
        sharedPopupSequence = System.nanoTime();
        if (pendingSharedPopupEvents.length() > 0) {
            pendingSharedPopupEvents.append("|");
        }
        pendingSharedPopupEvents.append(type).append(":").append(player).append(":").append(amount);
    }

    private void showSharedPopupsFromState(GameState state) {
        lastSharedPopupId = GameFrameSupport.showSharedPopupsFromState(state, lastSharedPopupId, this::showSharedPopupEvent);
    }
    private void showSharedPopupEvent(String event) {
        GameFrameSupport.showSharedPopupEvent(event, this::showPlacementTurnPopupFor,
                player -> showPlacementFinishedPopup(false), this::showTurnEndedPopup,
                this::showReinforcementMessageFor, this::showActionPhasePopup);
    }

    private void applyBattleInfoText(String text) {
        if (text != null && !text.isBlank() && battleInfoLabel != null) {
            battleInfoLabel.setText(text);
        }
    }

    private void applyBattleState(GameState state) {
        battleInProgress = state.isBattleInProgress();
        pendingAttackFrom = state.getPendingAttackFrom();
        pendingAttackTarget = state.getPendingAttackTarget();
        pendingDefender = state.getPendingDefender();
        attackerDiceCount = state.getAttackerDiceCount();
        defenderDiceCount = state.getDefenderDiceCount();
        attackerDiceValues = state.getAttackerDiceValues();
        defenderDiceValues = state.getDefenderDiceValues();
        attackerRolledCount = state.getAttackerRolledCount();
        defenderRolledCount = state.getDefenderRolledCount();
        attackerRolled = state.isAttackerRolled();
        defenderRolled = state.isDefenderRolled();

        if (battleInProgress && pendingAttackFrom > 0 && pendingAttackTarget > 0) {
            showBattleDiceState();
        } else if (!diceRolling) {
            attackerDiceLabel.setVisible(false);
            defenderDiceLabel.setVisible(false);
            hideDiceSlots(attackerDiceSlots);
            hideDiceSlots(defenderDiceSlots);
        }
    }

    private void showBattleDiceState() {
        battleInfoLabel.setText("Player" + currentPlayer + " vs Player" + pendingDefender);
        attackerDiceLabel.setVisible(true);
        defenderDiceLabel.setVisible(true);
        prepareDiceSlots(attackerDiceSlots, attackerDiceCount);
        prepareDiceSlots(defenderDiceSlots, defenderDiceCount);
        showDiceSlots(attackerDiceSlots, attackerDiceValues, attackerRolledCount);
        showDiceSlots(defenderDiceSlots, defenderDiceValues, defenderRolledCount);
        if (attackerRolledCount > 0) {
            showRolledDice(attackerDiceLabel, currentPlayer, attackerDiceValues, attackerRolledCount, attackerDiceCount);
        } else {
            attackerDiceLabel.setText("P" + currentPlayer + ": Roll " + attackerDiceCount + " dice");
        }
        if (defenderRolledCount > 0) {
            showRolledDice(defenderDiceLabel, pendingDefender, defenderDiceValues, defenderRolledCount, defenderDiceCount);
        } else {
            defenderDiceLabel.setText("P" + pendingDefender + ": Roll " + defenderDiceCount + " dice");
        }}
 
    private void showDiceSlots(javax.swing.JLabel[] slots, int[] diceValues, int rolledCount) {
        GameFrameSupport.showDiceSlots(slots, diceValues, rolledCount, getClass());
    }
    private boolean canRollDice(boolean attacker) {
        return localPlayerNumber == (attacker ? currentPlayer : pendingDefender);
    }
    private void copyInto(int[] source, int[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    private void configureResponsiveGameLayout() {
        if (responsiveGameLayout == null) {
            responsiveGameLayout = new GameFrameSupport.ResponsiveLayout(this, jLayeredPane1, jPanel1, jLabel2);
        }
        responsiveGameLayout.configure();
    }

    private void applyMapStateToButtons() {
        GameFrameSupport.applyMapStateToButtons(territoryOwner, territoryArmies, mapButtons, playerColors, this::showArmyCountOnButton);
    }
    private GameState createCurrentGameState() {
        return GameFrameSupport.createGameState(currentPlayer, territoryOwner, territoryArmies, remainingArmies,
                placementPhase, initialPlacementPhase, currentPhaseName, getHighlightedTerritoryArray(),
                pendingSharedPopupEvents, sharedPopupSequence, battleInProgress, pendingAttackFrom,
                pendingAttackTarget, pendingDefender, attackerDiceCount, defenderDiceCount,
                attackerDiceValues, defenderDiceValues, attackerRolledCount, defenderRolledCount,
                attackerRolled, defenderRolled, battleInfoLabel);
    }
    private int[] getHighlightedTerritoryArray() {
        return GameFrameSupport.toIntArray(highlightedAttackTargets);
    }
    private void applyRemoteHighlights(int[] highlightedTerritories) {
        GameFrameSupport.applyRemoteHighlights(highlightedTerritories, mapButtons, highlightedAttackTargets, this::highlightTerritoryWhite);
    }
    private void sendGameStateToServer() {
        if (!applyingRemoteState && stateSender != null) {
            // Kendi hamlemden sonra son durumu server'a yolluyorum, remote state tekrar gonderilmiyor.
            stateSender.accept(createCurrentGameState());
            pendingSharedPopupEvents.setLength(0);
        }
    }

    private boolean canLocalPlayerAct() {
        return currentPlayer == localPlayerNumber;
    }

    private void showNotYourTurnMessage() {
        javax.swing.JOptionPane.showMessageDialog(this, "It is not your turn. Player " + currentPlayer + " is playing.");
    }

    private void highlightTerritoryWhite(int territoryId) {
        GameFrameSupport.highlightTerritoryWhite(territoryId, mapButtons, highlightedAttackTargets);
    }
    private javax.swing.JButton[] createMapButtonArray() {
        return GameFrameSupport.createMapButtonArray(this);
    }
    private void addArmyInputField() {
        armyInputField = GameFrameSupport.createArmyInputField();
    }
    private void styleRightPanel() {
        GameFrameSupport.styleRightPanel(jPanel1,
                new javax.swing.JLabel[]{jLabel1, jLabel3, jLabel4, jLabel5, jLabel6, jLabel13},
                new javax.swing.JLabel[]{jLabel7, jLabel8, jLabel9, jLabel10, jLabel11, jLabel12, jLabel13},
                jLabel3, jLabel4, jLabel6);
    }
    private void addPlayerStatsList() {
        playerListModel = new javax.swing.DefaultListModel<>();
        GameFrameSupport.PlayerStatsView statsView = GameFrameSupport.addPlayerStatsList(jPanel1, playerListModel);
        playerStatsList = statsView.list;
        playerStatsScrollPane = statsView.scrollPane;
    }
    private void addPlacementClickListeners() {
        for (int i = 0; i < mapButtons.length; i++) {
            int territoryId = i + 1;
            mapButtons[i].addActionListener(evt -> handleMapButtonClick(territoryId));
        }
    }
    private void addDiceDisplayLabels() {
        GameFrameSupport.DiceView diceView = GameFrameSupport.addDiceDisplay(jPanel1,
                () -> rollBattleDice(true), () -> rollBattleDice(false));
        battleInfoLabel = diceView.battleInfoLabel;
        attackerDiceLabel = diceView.attackerDiceLabel;
        defenderDiceLabel = diceView.defenderDiceLabel;
        attackerDiceSlots = diceView.attackerDiceSlots;
        defenderDiceSlots = diceView.defenderDiceSlots;
    }

    private void handleMapButtonClick(int territoryId) {
        if (!canLocalPlayerAct()) {
            showNotYourTurnMessage();
            return;
        }
        if (battleInProgress) {
            javax.swing.JOptionPane.showMessageDialog(this, "Wait for the dice result first.");
            return;
        }
        if (placementPhase) {
            placeArmyOnTerritory(territoryId);
        } else if (fortifySelectionMode) {
            handleFortifySelectionClick(territoryId);
        } else if (attackSelectionMode) {
            handleAttackSelectionClick(territoryId);
        }
    }
    private void addEndTurnListener() {
        jButton43.addActionListener(evt -> endCurrentTurn());
    }
    private void addAttackHoverListeners() {
        for (int i = 0; i < mapButtons.length; i++) {
            int territoryId = i + 1;
            mapButtons[i].addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (battleInProgress) {
                        return;
                    }
                    if (fortifySelectionMode && selectedFortifyFrom == 0) {
                        showFortifyPreviewFrom(territoryId);
                    } else {
                        showAttackableNeighborsFrom(territoryId);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (battleInProgress) {
                        return;
                    }
                    if (fortifySelectionMode && selectedFortifyFrom == 0) {
                        restoreHighlightedAttackTargets();
                        return;
                    }
                    if (!keepAttackTargetsHighlighted) {
                        restoreHighlightedAttackTargets();
                    }
                }
            });
        }
    }

    private void showFortifyPreviewFrom(int territoryId) {
        FortifySupport.showFortifyPreviewFrom(territoryId, fortifySelectionMode, currentPlayer,
                territoryOwner, territoryArmies, neighbors, mapButtons, highlightedAttackTargets,
                this::restoreHighlightedAttackTargets);
    }
    private void showAttackableNeighborsFrom(int territoryId) {
        AttackBattleSupport.showAttackableNeighborsFrom(territoryId, currentPlayer, placementPhase,
                attackSelectionMode, keepAttackTargetsHighlighted, selectedAttackFrom, territoryOwner,
                territoryArmies, neighbors, mapButtons, highlightedAttackTargets,
                this::restoreHighlightedAttackTargets);
    }
    private void restoreHighlightedAttackTargets() {
        GameFrameSupport.restoreHighlightedTerritories(highlightedAttackTargets, territoryOwner, mapButtons, playerColors);
    }
    private void startFortifySelection() {
        if (!canLocalPlayerAct()) {
            showNotYourTurnMessage();
            return;
        }
        if (placementPhase) {
            javax.swing.JOptionPane.showMessageDialog(this, "Finish army placement first.");
            return;
        }
        if (battleInProgress) {
            javax.swing.JOptionPane.showMessageDialog(this, "Wait for the dice result first.");
            return;
        }
        if (fortifySelectionMode) {
            javax.swing.JOptionPane.showMessageDialog(this, "Continue fortifying or press End Tour.");
            return;
        }
        attackSelectionMode = false;
        fortifySelectionMode = true;
        selectedAttackFrom = 0;
        selectedFortifyFrom = 0;
        selectedFortifyTarget = 0;
        keepAttackTargetsHighlighted = false;
        restoreHighlightedAttackTargets();
        currentPhaseName = "Fortify Phase";
        updatePlacementLabels();
        sendGameStateToServer();
        javax.swing.JOptionPane.showMessageDialog(this, "Choose the territory to move armies from.");
    }

    private void handleFortifySelectionClick(int territoryId) {
        if (selectedFortifyFrom == 0) {
            selectFortifySource(territoryId);
            return;
        }
        moveFortifyArmyTo(territoryId);
    }

    private void selectFortifySource(int territoryId) {
        if (territoryOwner[territoryId] != currentPlayer) {
            javax.swing.JOptionPane.showMessageDialog(this, "Choose Player" + currentPlayer + " territory first.");
            return;
        }
        if (territoryArmies[territoryId] < 2) {
            javax.swing.JOptionPane.showMessageDialog(this, "Source territory must keep at least 1 army.");
            return;
        }
        if (!hasOwnNeighbor(territoryId)) {
            javax.swing.JOptionPane.showMessageDialog(this, "Choose a territory with a friendly neighbor.");
            return;
        }
        selectedFortifyFrom = territoryId;
        selectedFortifyTarget = 0;
        keepAttackTargetsHighlighted = true;
        restoreHighlightedAttackTargets();
        battleInfoLabel.setText("Player " + currentPlayer + " fortifying");
        showFortifyableNeighborsFrom(territoryId);
        highlightTerritoryWhite(territoryId);
        sendGameStateToServer();
    }

    private boolean hasOwnNeighbor(int territoryId) {
        return FortifySupport.hasOwnNeighbor(territoryId, currentPlayer, territoryOwner, neighbors);
    }
    private void showFortifyableNeighborsFrom(int territoryId) {
        FortifySupport.showFortifyableNeighborsFrom(territoryId, currentPlayer, territoryOwner,
                neighbors, mapButtons, highlightedAttackTargets, this::restoreHighlightedAttackTargets);
    }
    private void moveFortifyArmyTo(int targetTerritoryId) {
        if (!gameLogic.canFortify(selectedFortifyFrom, targetTerritoryId, currentPlayer)) {
            javax.swing.JOptionPane.showMessageDialog(this, "You can move armies only to a friendly neighbor territory.");
            return;
        }
        if (selectedFortifyTarget == 0) {
            selectedFortifyTarget = targetTerritoryId;
        }
        if (targetTerritoryId != selectedFortifyTarget) {
            javax.swing.JOptionPane.showMessageDialog(this, "Fortify can move armies to one selected territory only.");
            return;
        }

        gameLogic.fortifyOneArmy(selectedFortifyFrom, targetTerritoryId);
        restoreHighlightedAttackTargets();
        showArmyCountOnButton(mapButtons[selectedFortifyFrom - 1], territoryArmies[selectedFortifyFrom]);
        showArmyCountOnButton(mapButtons[targetTerritoryId - 1], territoryArmies[targetTerritoryId]);
        highlightTerritoryWhite(selectedFortifyFrom);
        highlightTerritoryWhite(targetTerritoryId);
        battleInfoLabel.setText("<html>Player " + currentPlayer + " moving armies </html>");
        updatePlayerStatsLabels();
        sendGameStateToServer();

        if (territoryArmies[selectedFortifyFrom] <= 1) {
            endCurrentTurn();
        }
    }

    private void endCurrentTurn() {
        if (!canLocalPlayerAct()) {
            showNotYourTurnMessage();
            return;
        }
        if (placementPhase) {
            javax.swing.JOptionPane.showMessageDialog(this, "Finish army placement first.");
            return;
        }
        if (battleInProgress) {
            javax.swing.JOptionPane.showMessageDialog(this, "Wait for the dice result first.");
            return;
        }
        attackSelectionMode = false;
        fortifySelectionMode = false;
        selectedAttackFrom = 0;
        selectedFortifyFrom = 0;
        selectedFortifyTarget = 0;
        keepAttackTargetsHighlighted = false;
        restoreHighlightedAttackTargets();
        int endedPlayer = currentPlayer;
        int nextPlayer = getNextActivePlayer(currentPlayer);
        queueSharedPopup("TURN_ENDED", endedPlayer, 0);
        showTurnEndedPopup(endedPlayer);
        sendGameStateToServer();
        javax.swing.Timer timer = new javax.swing.Timer(1400, evt -> {
            startReinforcementPhase(nextPlayer);
            sendGameStateToServer();
        });
        timer.setRepeats(false);
        timer.start();
    }
    private void showTurnEndedPopup(int endedPlayer) {
        GameFrameSupport.showTurnEndedPopup(this, localPlayerNumber, true, endedPlayer);
    }
    private void startAttackSelection() {
        if (!canLocalPlayerAct()) {
            showNotYourTurnMessage();
            return;
        }
        if (placementPhase) {
            javax.swing.JOptionPane.showMessageDialog(this, "Finish army placement first.");
            return;
        }
        if (battleInProgress) {
            javax.swing.JOptionPane.showMessageDialog(this, "Wait for the dice result first.");
            return;
        }
        if (fortifySelectionMode) {
            javax.swing.JOptionPane.showMessageDialog(this, "Finish fortifying or press End Tour.");
            return;
        }
        selectedAttackFrom = 0;
        keepAttackTargetsHighlighted = false;
        currentPhaseName = "Attack Phase";
        updatePlacementLabels();
        fortifySelectionMode = false;
        attackSelectionMode = true;
        javax.swing.JOptionPane.showMessageDialog(this, "Click the territory you want to attack from.");
    }
    private void handleAttackSelectionClick(int territoryId) {
        if (selectedAttackFrom == 0) {
            selectAttackingTerritory(territoryId);
            return;
        }
        attackSelectedTarget(territoryId);
    }

    private void selectAttackingTerritory(int territoryId) {
        if (territoryOwner[territoryId] != currentPlayer) {
            javax.swing.JOptionPane.showMessageDialog(this, "Choose Player" + currentPlayer + " territory first.");
            return;
        }
        if (territoryArmies[territoryId] < 2) {
            javax.swing.JOptionPane.showMessageDialog(this, "Attacking territory needs at least 2 armies.");
            return;
        }
        selectedAttackFrom = territoryId;
        keepAttackTargetsHighlighted = true;
        restoreHighlightedAttackTargets();
        battleInfoLabel.setText("Player " + currentPlayer + " attacking");
        showAttackableNeighborsFrom(territoryId);
        
        mapButtons[territoryId - 1].setBackground(java.awt.Color.WHITE);
        highlightedAttackTargets.add(territoryId);
    }
    private void attackSelectedTarget(int targetTerritoryId) {
        if (!canAttack(selectedAttackFrom, targetTerritoryId)) {
            javax.swing.JOptionPane.showMessageDialog(this, "You can attack only enemy neighbor territories.");
            return;
        }

        pendingAttackFrom = selectedAttackFrom;
        pendingAttackTarget = targetTerritoryId;
        pendingDefender = territoryOwner[targetTerritoryId];
        battleInProgress = true;
        attackerDiceCount = DiceRoller.getAttackerDiceCount(territoryArmies[pendingAttackFrom]);
        defenderDiceCount = DiceRoller.getDefenderDiceCount(territoryArmies[pendingAttackTarget]);
        attackerDiceValues = new int[attackerDiceCount];
        defenderDiceValues = new int[defenderDiceCount];
        attackerRolledCount = 0;
        defenderRolledCount = 0;
        attackerRolled = false;
        defenderRolled = false;
        selectedAttackFrom = 0;
        keepAttackTargetsHighlighted = false;
        restoreHighlightedAttackTargets();
        battleInfoLabel.setText("Player" + currentPlayer + " vs Player" + pendingDefender);
        attackerDiceLabel.setText("P" + currentPlayer + ": Roll " + attackerDiceCount + " dice");
        defenderDiceLabel.setText("P" + pendingDefender + ": Roll " + defenderDiceCount + " dice");
        attackerDiceLabel.setVisible(true);
        defenderDiceLabel.setVisible(true);
        prepareDiceSlots(attackerDiceSlots, attackerDiceCount);
        prepareDiceSlots(defenderDiceSlots, defenderDiceCount);
        highlightedAttackTargets.clear();
        mapButtons[pendingAttackFrom - 1].setBackground(java.awt.Color.WHITE);
        mapButtons[pendingAttackTarget - 1].setBackground(java.awt.Color.WHITE);
        highlightedAttackTargets.add(pendingAttackFrom);
        highlightedAttackTargets.add(pendingAttackTarget);
        applyRemoteHighlights(getHighlightedTerritoryArray());
        sendGameStateToServer();
    }

    private boolean canAttack(int fromTerritoryId, int targetTerritoryId) {
        return gameLogic.canAttack(fromTerritoryId, targetTerritoryId, currentPlayer);
    }

    private void prepareDiceSlots(javax.swing.JLabel[] slots, int activeCount) {
        GameFrameSupport.prepareDiceSlots(slots, activeCount, getClass());
    }
    private void rollBattleDice(boolean attacker) {
        if (pendingAttackFrom == 0 || diceRolling) {
            return;
        }
        if (!canRollDice(attacker)) {
            javax.swing.JOptionPane.showMessageDialog(this, "You can only roll your own dice.");
            return;
        }

        javax.swing.JLabel label = attacker ? attackerDiceLabel : defenderDiceLabel;
        javax.swing.JLabel[] slots = attacker ? attackerDiceSlots : defenderDiceSlots;
        int diceCount = attacker ? attackerDiceCount : defenderDiceCount;
        int rolledCount = attacker ? attackerRolledCount : defenderRolledCount;
        if (rolledCount >= diceCount) {
            return;
        }

        diceRolling = true;
        int slotIndex = rolledCount;
        int[] frame = {0};
        javax.swing.Timer timer = new javax.swing.Timer(150, null);
        timer.addActionListener(evt -> {
            int die = DiceRoller.rollDie();
            slots[slotIndex].setIcon(loadPictureIcon("/pictures/dice" + die + ".png", 28, 28));
            label.setText((attacker ? "P" + currentPlayer : "P" + pendingDefender) + ": rolling " + (slotIndex + 1) + "/" + diceCount);
            frame[0]++;
            if (frame[0] >= 10) {
                timer.stop();
                int result = DiceRoller.rollDie();
                slots[slotIndex].setIcon(loadPictureIcon("/pictures/dice" + result + ".png", 28, 28));
                if (attacker) {
                    attackerDiceValues[attackerRolledCount] = result;
                    attackerRolledCount++;
                    attackerRolled = attackerRolledCount == attackerDiceCount;
                    showRolledDice(label, currentPlayer, attackerDiceValues, attackerRolledCount, attackerDiceCount);
                } else {
                    defenderDiceValues[defenderRolledCount] = result;
                    defenderRolledCount++;
                    defenderRolled = defenderRolledCount == defenderDiceCount;
                    showRolledDice(label, pendingDefender, defenderDiceValues, defenderRolledCount, defenderDiceCount);
                }
                diceRolling = false;
                sendGameStateToServer();
                finishBattleIfReady();
            }
        });
        timer.start();
    }

    private void showRolledDice(javax.swing.JLabel label, int player, int[] dice, int rolledCount, int diceCount) {
        GameFrameSupport.showRolledDice(label, player, dice, rolledCount, diceCount);
    }
    private void finishBattleIfReady() {
        if (!attackerRolled || !defenderRolled) {
            return;
        }

        DiceRoller.BattleResult result = DiceRoller.compare(attackerDiceValues, defenderDiceValues);
        int attackerPlayer = currentPlayer;
        int defenderPlayer = pendingDefender;
        territoryArmies[pendingAttackFrom] -= result.getAttackerLosses();
        territoryArmies[pendingAttackTarget] -= result.getDefenderLosses();
        boolean captured = territoryArmies[pendingAttackTarget] <= 0;
        if (territoryArmies[pendingAttackTarget] <= 0) {
            captureTerritory(pendingAttackFrom, pendingAttackTarget, attackerDiceCount);
        }
        boolean defenderEliminated = captured && countTerritories(defenderPlayer) == 0;
        boolean gameWon = countTerritories(attackerPlayer) == RiskGameLogic.TERRITORY_COUNT;
        showArmyCountOnButton(mapButtons[pendingAttackFrom - 1], territoryArmies[pendingAttackFrom]);
        showArmyCountOnButton(mapButtons[pendingAttackTarget - 1], territoryArmies[pendingAttackTarget]);
        battleInfoLabel.setText(createBattleResultText(result));
        updatePlayerStatsLabels();
        sendGameStateToServer();
        hideDiceResultAfterDelay(attackerPlayer, defenderPlayer, captured, defenderEliminated, gameWon, result);
        pendingAttackFrom = 0;
        pendingAttackTarget = 0;
        pendingDefender = 0;
    }

    private void hideDiceResultAfterDelay(int attackerPlayer, int defenderPlayer, boolean captured,
            boolean defenderEliminated, boolean gameWon, DiceRoller.BattleResult result) {
        javax.swing.Timer timer = new javax.swing.Timer(2500, evt -> {
            attackerDiceLabel.setVisible(false);
            defenderDiceLabel.setVisible(false);
            hideDiceSlots(attackerDiceSlots);
            hideDiceSlots(defenderDiceSlots);
            battleInfoLabel.setText(createShortBattleWinnerText(attackerPlayer, defenderPlayer, captured, result));
            battleInProgress = false;
            pendingAttackFrom = 0;
            pendingAttackTarget = 0;
            pendingDefender = 0;
            sendGameStateToServer();
            showGameStatePopup(attackerPlayer, defenderPlayer, defenderEliminated, gameWon);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showGameStatePopup(int attackerPlayer, int defenderPlayer, boolean defenderEliminated, boolean gameWon) {
        if (defenderEliminated) {
            showGameNoticePopup("Player " + defenderPlayer + " lost!", "No territories left.", new java.awt.Color(190, 70, 70), 1800);
        }
        if (gameWon) {
            javax.swing.Timer timer = new javax.swing.Timer(defenderEliminated ? 1900 : 0, evt -> {
                showGameNoticePopup("PLAYER " + attackerPlayer + " WON!", "The whole world is yours!", new java.awt.Color(218, 174, 75), 3200);
                battleInfoLabel.setText("Player " + attackerPlayer + " conquered the world!");
                showEndFrame(attackerPlayer);
                if (gameOverSender != null) {
                    gameOverSender.accept(attackerPlayer);
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void showGameNoticePopup(String titleText, String detailText, java.awt.Color borderColor, int durationMillis) {
        GameFrameSupport.showGameNoticePopup(this, localPlayerNumber, true, titleText, detailText, borderColor, durationMillis);
    }
    private void hideDiceSlots(javax.swing.JLabel[] slots) {
        GameFrameSupport.hideDiceSlots(slots);
    }
    private String createShortBattleWinnerText(int attackerPlayer, int defenderPlayer, boolean captured, DiceRoller.BattleResult result) {
        return AttackBattleSupport.createShortBattleWinnerText(attackerPlayer, defenderPlayer, captured, result);
    }
    private void captureTerritory(int fromTerritoryId, int targetTerritoryId, int minimumMoveArmies) {
        AttackBattleSupport.captureTerritory(fromTerritoryId, targetTerritoryId, minimumMoveArmies,
                currentPlayer, territoryOwner, territoryArmies);
        mapButtons[targetTerritoryId - 1].setBackground(playerColors[currentPlayer - 1]);
    }
    private String createBattleResultText(DiceRoller.BattleResult result) {
        return AttackBattleSupport.createBattleResultText(result, currentPlayer, pendingDefender,
                pendingAttackTarget, territoryOwner, attackerDiceValues, defenderDiceValues);
    }
    private javax.swing.ImageIcon loadPictureIcon(String path, int width, int height) {
        return GameFrameSupport.loadPictureIcon(getClass(), path, width, height);
    }
    private void placeArmyOnTerritory(int territoryId) {
        if (remainingArmies == null || !placementPhase) {
            return;
        }
        if (territoryOwner[territoryId] != currentPlayer) {
            javax.swing.JOptionPane.showMessageDialog(this, "You can only place armies on Player" + currentPlayer + " territories.");
            return;
        }
        if (remainingArmies[currentPlayer] <= 0) {
            if (!initialPlacementPhase) {
                startAttackPhase();
            }
            return;
        }

        int armyCount = getArmyCountFromInput();
        if (armyCount <= 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Enter a positive army count.");
            return;
        }
        if (armyCount > remainingArmies[currentPlayer]) {
            javax.swing.JOptionPane.showMessageDialog(this, "Not enough remaining armies.");
            return;
        }
        gameLogic.placeArmies(territoryId, currentPlayer, armyCount);
        showArmyCountOnButton(mapButtons[territoryId - 1], territoryArmies[territoryId]);

        if (remainingArmies[currentPlayer] == 0) {
            if (initialPlacementPhase) {
                moveToNextPlayerWithArmies();
            } else {
                queueSharedPopup("PLACEMENT_FINISHED", currentPlayer, 0);
                showPlacementFinishedPopup(false);
                startAttackPhase();
            }
        }
        updatePlacementLabels();
        sendGameStateToServer();
    }

    private void showPlacementFinishedPopup(boolean includePlayer) {
        String text = includePlayer ? "Placement finished for Player " + currentPlayer : "Placement finished";
        GameFrameSupport.showPlacementFinishedPopup(this, localPlayerNumber, true, text);
    }
    private void showPlacementTurnPopup() {
        showPlacementTurnPopupFor(currentPlayer);
    }

    private void showPlacementTurnPopupFor(int player) {
        GameFrameSupport.showPlacementTurnPopupFor(this, localPlayerNumber, true, player);
    }
    private int getArmyCountFromInput() {
        try {
            return Integer.parseInt(armyInputField.getText().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    private void showArmyCountOnButton(javax.swing.JButton button, int armyCount) {
        GameFrameSupport.showArmyCountOnButton(button, armyCount);
    }
    private void moveToNextPlayerWithArmies() {
        for (int player = currentPlayer + 1; player <= playerCount; player++) {
            if (remainingArmies[player] > 0) {
                currentPlayer = player;
                currentPhaseName = "Placement Phase";
                updatePlacementLabels();
                queueSharedPopup("PLACEMENT_TURN", currentPlayer, 0);
                showPlacementTurnPopup();
                return;
            }
        }
        initialPlacementPhase = false;
        startReinforcementPhase(1);
    }

    private void startReinforcementPhase(int player) {
        if (countTerritories(player) == 0) {
            player = getNextActivePlayer(player);
        }
        currentPlayer = player;
        placementPhase = true;
        attackSelectionMode = false;
        selectedAttackFrom = 0;
        keepAttackTargetsHighlighted = false;
        restoreHighlightedAttackTargets();
        remainingArmies[currentPlayer] = gameLogic.getReinforcementArmies(currentPlayer);
        if (remainingArmies[currentPlayer] <= 0) {
            startAttackPhase();
            return;
        }
        currentPhaseName = "Reinforcement Phase";
        updatePlacementLabels();
        queueSharedPopup("REINFORCEMENT", currentPlayer, remainingArmies[currentPlayer]);
        showReinforcementMessage(remainingArmies[currentPlayer]);
    }
    private int getNextActivePlayer(int fromPlayer) {
        return FortifySupport.getNextActivePlayer(fromPlayer, playerCount, this::countTerritories);
    }
    private void showReinforcementMessage(int reinforcementArmies) {
        showReinforcementMessageFor(currentPlayer, reinforcementArmies);
    }

    private void showReinforcementMessageFor(int player, int reinforcementArmies) {
        battleInfoLabel.setText("<html>Player " + player + " receives<br>" + reinforcementArmies + " reinforcement armies</html>");
        showReinforcementBonusPopup(player, reinforcementArmies);
        javax.swing.Timer timer = new javax.swing.Timer(1300, evt -> {
            if ("Reinforcement Phase".equals(currentPhaseName)) {
                battleInfoLabel.setText("");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    private void showReinforcementBonusPopup(int player, int reinforcementArmies) {
        GameFrameSupport.showReinforcementBonusPopup(this, localPlayerNumber, true, player, reinforcementArmies);
    }
    private void startAttackPhase() {
        boolean waitForPlacementPopup = pendingSharedPopupEvents.toString().contains("PLACEMENT_FINISHED");
        placementPhase = false;
        attackSelectionMode = false;
        currentPhaseName = "Action Phase";
        restoreHighlightedAttackTargets();
        updatePlacementLabels();
        queueSharedPopup("ACTION_PHASE", currentPlayer, 0);
        if (waitForPlacementPopup) {
            javax.swing.Timer timer = new javax.swing.Timer(1500, evt -> showActionPhasePopup(currentPlayer));
            timer.setRepeats(false);
            timer.start();
        } else {
            showActionPhasePopup(currentPlayer);
        }
    }
    private void showActionPhasePopup(int player) {
        showGameNoticePopup("Player " + player, "Action Phase", new java.awt.Color(218, 174, 75), 1200);
    }
    private void updatePlacementLabels() {
        GameFrameSupport.updatePlacementLabels(jLabel3, jLabel4, jLabel6, battleInfoLabel, jPanel1,
                true, localPlayerNumber, currentPlayer, placementPhase, remainingArmies[currentPlayer],
                currentPhaseName, battleInProgress, this::updatePlayerStatsLabels);
    }
    private void updatePlayerStatsLabels() {
        GameFrameSupport.updatePlayerStatsLabels(playerListModel, playerCount, this::countTerritories, this::countArmies);
    }

    private int countTerritories(int player) {
        return gameLogic.countTerritories(player);
    }
    private int countArmies(int player) {
        return gameLogic.countArmies(player);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jButton24 = new javax.swing.JButton();
        jButton25 = new javax.swing.JButton();
        jButton26 = new javax.swing.JButton();
        jButton27 = new javax.swing.JButton();
        jButton28 = new javax.swing.JButton();
        jButton29 = new javax.swing.JButton();
        jButton30 = new javax.swing.JButton();
        jButton31 = new javax.swing.JButton();
        jButton32 = new javax.swing.JButton();
        jButton33 = new javax.swing.JButton();
        jButton34 = new javax.swing.JButton();
        jButton35 = new javax.swing.JButton();
        jButton36 = new javax.swing.JButton();
        jButton37 = new javax.swing.JButton();
        jButton38 = new javax.swing.JButton();
        jButton39 = new javax.swing.JButton();
        jButton40 = new javax.swing.JButton();
        jButton41 = new javax.swing.JButton();
        jButton42 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        Attack = new javax.swing.JButton();
        jButton43 = new javax.swing.JButton();
        Fortify = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLayeredPane1.setBackground(new java.awt.Color(102, 102, 102));
        jLayeredPane1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jButton1.setBackground(new java.awt.Color(255, 255, 255));
        jButton1.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 330, 20, 20));

        jButton2.setBackground(new java.awt.Color(255, 255, 255));
        jButton2.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 300, 20, 20));

        jButton3.setBackground(new java.awt.Color(255, 255, 255));
        jButton3.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 310, 20, 20));

        jButton4.setBackground(new java.awt.Color(255, 255, 255));
        jButton4.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton4, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 250, 20, 20));

        jButton5.setBackground(new java.awt.Color(255, 255, 255));
        jButton5.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton5, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 200, 20, 20));

        jButton6.setBackground(new java.awt.Color(255, 255, 255));
        jButton6.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton6, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 160, 20, 20));

        jButton7.setBackground(new java.awt.Color(255, 255, 255));
        jButton7.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton7, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 130, 20, 20));

        jButton8.setBackground(new java.awt.Color(255, 255, 255));
        jButton8.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton8, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 80, 20, 20));

        jButton9.setBackground(new java.awt.Color(255, 255, 255));
        jLayeredPane1.add(jButton9, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 60, 20, 20));

        jButton10.setBackground(new java.awt.Color(255, 255, 255));
        jButton10.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton10, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 70, 20, 20));

        jButton11.setBackground(new java.awt.Color(255, 255, 255));
        jButton11.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 110, 20, 20));

        jButton12.setBackground(new java.awt.Color(255, 255, 255));
        jButton12.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton12, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 130, 20, 20));

        jButton13.setBackground(new java.awt.Color(255, 255, 255));
        jButton13.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton13, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 170, 20, 20));

        jButton14.setBackground(new java.awt.Color(255, 255, 255));
        jButton14.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton14, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 200, 20, 20));

        jButton15.setBackground(new java.awt.Color(255, 255, 255));
        jButton15.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton15, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 97, 20, 20));

        jButton16.setBackground(new java.awt.Color(255, 255, 255));
        jButton16.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton16, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 170, 20, 20));

        jButton17.setBackground(new java.awt.Color(255, 255, 255));
        jButton17.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton17, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 100, 20, 20));

        jButton18.setBackground(new java.awt.Color(255, 255, 255));
        jButton18.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton18, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 80, 20, 20));

        jButton19.setBackground(new java.awt.Color(255, 255, 255));
        jButton19.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton19, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 120, 20, 20));

        jButton20.setBackground(new java.awt.Color(255, 255, 255));
        jButton20.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton20, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 160, 20, 20));

        jButton21.setBackground(new java.awt.Color(255, 255, 255));
        jButton21.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton21, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 200, 20, 20));

        jButton22.setBackground(new java.awt.Color(255, 255, 255));
        jButton22.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton22, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 240, 20, 20));

        jButton23.setBackground(new java.awt.Color(255, 255, 255));
        jButton23.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton23, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 290, 20, 20));

        jButton24.setBackground(new java.awt.Color(255, 255, 255));
        jButton24.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton24, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 330, 20, 20));

        jButton25.setBackground(new java.awt.Color(255, 255, 255));
        jButton25.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton25, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 270, 20, 20));

        jButton26.setBackground(new java.awt.Color(255, 255, 255));
        jButton26.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton26, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 240, 20, 20));

        jButton27.setBackground(new java.awt.Color(255, 255, 255));
        jButton27.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton27, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 200, 20, 20));

        jButton28.setBackground(new java.awt.Color(255, 255, 255));
        jButton28.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton28, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 200, 20, 20));

        jButton29.setBackground(new java.awt.Color(255, 255, 255));
        jButton29.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton29, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 137, 20, 20));

        jButton30.setBackground(new java.awt.Color(255, 255, 255));
        jButton30.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton30, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 50, 20, 20));

        jButton31.setBackground(new java.awt.Color(255, 255, 255));
        jButton31.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton31, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 60, 20, 20));

        jButton32.setBackground(new java.awt.Color(255, 255, 255));
        jButton32.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton32, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 120, 20, 20));

        jButton33.setBackground(new java.awt.Color(255, 255, 255));
        jButton33.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton33, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 150, 20, 20));

        jButton34.setBackground(new java.awt.Color(255, 255, 255));
        jButton34.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton34, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 140, 20, 20));

        jButton35.setBackground(new java.awt.Color(255, 255, 255));
        jButton35.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton35, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 190, 20, 20));

        jButton36.setBackground(new java.awt.Color(255, 255, 255));
        jButton36.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton36, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 230, 20, 20));

        jButton37.setBackground(new java.awt.Color(255, 255, 255));
        jButton37.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton37, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 100, 20, 20));

        jButton38.setBackground(new java.awt.Color(255, 255, 255));
        jButton38.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton38, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 240, 20, 20));

        jButton39.setBackground(new java.awt.Color(255, 255, 255));
        jButton39.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton39, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 280, 20, 20));

        jButton40.setBackground(new java.awt.Color(255, 255, 255));
        jButton40.setForeground(new java.awt.Color(0, 0, 0));
        jLayeredPane1.add(jButton40, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 270, 20, 20));

        jButton41.setBackground(new java.awt.Color(255, 255, 255));
        jLayeredPane1.add(jButton41, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 300, 20, 20));

        jButton42.setBackground(new java.awt.Color(255, 255, 255));
        jLayeredPane1.add(jButton42, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 340, 20, 20));

        jLabel2.setForeground(new java.awt.Color(0, 0, 0));
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pictures/map.png"))); // NOI18N
        jLayeredPane1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 20, 600, -1));

        Attack.setBackground(new java.awt.Color(255, 255, 255));
        Attack.setForeground(new java.awt.Color(0, 0, 51));
        Attack.setText("Attack");
        Attack.addActionListener(evt -> startAttackSelection());
        jLayeredPane1.add(Attack, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 440, 130, 30));

        jButton43.setBackground(new java.awt.Color(255, 255, 255));
        jButton43.setForeground(new java.awt.Color(0, 0, 51));
        jButton43.setText("End Tour");
        jLayeredPane1.add(jButton43, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 440, 150, 30));

        Fortify.setBackground(new java.awt.Color(255, 255, 255));
        Fortify.setForeground(new java.awt.Color(0, 0, 51));
        Fortify.setText("Fortify");
        Fortify.addActionListener(evt -> startFortifySelection());
        jLayeredPane1.add(Fortify, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 440, 140, 30));

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Player1");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 20, 70, -1));

        jLabel4.setText("Player Turn : ");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        jLabel3.setText("Attack");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 50, 100, 20));

        jLabel5.setText("Phase :");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 50, 50, 20));

        jLabel6.setText("Player2");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 140, -1, -1));

        jLabel7.setText("Player1");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, -1, -1));

        jLabel8.setText("");
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 180, -1, -1));

        jLabel9.setText("");
        jPanel1.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 220, -1, -1));

        jLabel10.setText("");
        jPanel1.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 260, -1, -1));

        jLabel11.setText("");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 300, -1, -1));

        jLabel12.setText("attackplayer");
        jPanel1.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 340, -1, -1));

        jLabel13.setText("attackedplayer");
        jPanel1.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 340, -1, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 707, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Attack;
    private javax.swing.JButton Fortify;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton24;
    private javax.swing.JButton jButton25;
    private javax.swing.JButton jButton26;
    private javax.swing.JButton jButton27;
    private javax.swing.JButton jButton28;
    private javax.swing.JButton jButton29;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton30;
    private javax.swing.JButton jButton31;
    private javax.swing.JButton jButton32;
    private javax.swing.JButton jButton33;
    private javax.swing.JButton jButton34;
    private javax.swing.JButton jButton35;
    private javax.swing.JButton jButton36;
    private javax.swing.JButton jButton37;
    private javax.swing.JButton jButton38;
    private javax.swing.JButton jButton39;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton40;
    private javax.swing.JButton jButton41;
    private javax.swing.JButton jButton42;
    private javax.swing.JButton jButton43;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}


