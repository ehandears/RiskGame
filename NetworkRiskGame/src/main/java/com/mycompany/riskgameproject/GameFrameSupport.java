package com.mycompany.riskgameproject;

import com.mycompany.riskgameproject.common.GameState;

final class GameFrameSupport {
    private static final java.awt.Color PANEL_BG = new java.awt.Color(28, 35, 42);
    private static final java.awt.Color TEXT = new java.awt.Color(245, 238, 220);
    private static final java.awt.Color GOLD = new java.awt.Color(218, 174, 75);
    private GameFrameSupport() {}

    static long showSharedPopupsFromState(GameState state, long lastSharedPopupId, java.util.function.Consumer<String> eventHandler) {
        if (state.getSharedPopupId() == 0L || state.getSharedPopupId() == lastSharedPopupId) return lastSharedPopupId;
        String events = state.getSharedPopupEvents();
        if (events == null || events.isBlank()) return state.getSharedPopupId();
        String[] parts = events.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            String event = parts[i];
            javax.swing.Timer timer = new javax.swing.Timer(i * 1500, evt -> eventHandler.accept(event));
            timer.setRepeats(false);
            timer.start();
        }
        return state.getSharedPopupId();
    }

    static void showSharedPopupEvent(String event, java.util.function.IntConsumer placementTurn,
            java.util.function.IntConsumer placementFinished, java.util.function.IntConsumer turnEnded,
            java.util.function.BiConsumer<Integer, Integer> reinforcement, java.util.function.IntConsumer actionPhase) {
        String[] fields = event.split(":");
        if (fields.length < 3) return;
        String type = fields[0];
        int player = parseNumber(fields[1]);
        int amount = parseNumber(fields[2]);
        if ("PLACEMENT_TURN".equals(type)) placementTurn.accept(player);
        else if ("PLACEMENT_FINISHED".equals(type)) placementFinished.accept(player);
        else if ("TURN_ENDED".equals(type)) turnEnded.accept(player);
        else if ("REINFORCEMENT".equals(type)) reinforcement.accept(player, amount);
        else if ("ACTION_PHASE".equals(type)) actionPhase.accept(player);
    }

    static GameState createGameState(int currentPlayer, int[] territoryOwner, int[] territoryArmies,
            int[] remainingArmies, boolean placementPhase, boolean initialPlacementPhase, String currentPhaseName,
            int[] highlightedTerritories, StringBuilder pendingPopupEvents, long sharedPopupSequence,
            boolean battleInProgress, int pendingAttackFrom, int pendingAttackTarget, int pendingDefender,
            int attackerDiceCount, int defenderDiceCount, int[] attackerDiceValues, int[] defenderDiceValues,
            int attackerRolledCount, int defenderRolledCount, boolean attackerRolled, boolean defenderRolled,
            javax.swing.JLabel battleInfoLabel) {
        long popupId = pendingPopupEvents.length() == 0 ? 0L : sharedPopupSequence;
        return new GameState(currentPlayer, territoryOwner, territoryArmies, remainingArmies, placementPhase,
                initialPlacementPhase, currentPhaseName, highlightedTerritories, popupId, pendingPopupEvents.toString(),
                battleInProgress, pendingAttackFrom, pendingAttackTarget, pendingDefender, attackerDiceCount,
                defenderDiceCount, attackerDiceValues == null ? new int[0] : attackerDiceValues,
                defenderDiceValues == null ? new int[0] : defenderDiceValues, attackerRolledCount, defenderRolledCount,
                attackerRolled, defenderRolled, battleInfoLabel == null ? "" : battleInfoLabel.getText());
    }

    static void applyMapStateToButtons(int[] territoryOwner, int[] territoryArmies, javax.swing.JButton[] mapButtons,
            java.awt.Color[] playerColors, java.util.function.BiConsumer<javax.swing.JButton, Integer> armyRenderer) {
        for (int territoryId = 1; territoryId <= mapButtons.length; territoryId++) {
            int owner = territoryOwner[territoryId];
            if (owner >= 1 && owner <= playerColors.length) mapButtons[territoryId - 1].setBackground(playerColors[owner - 1]);
            mapButtons[territoryId - 1].setOpaque(true);
            armyRenderer.accept(mapButtons[territoryId - 1], territoryArmies[territoryId]);
        }
    }

    static int[] toIntArray(java.util.List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    static void applyRemoteHighlights(int[] highlightedTerritories, javax.swing.JButton[] mapButtons,
            java.util.List<Integer> highlightedAttackTargets, java.util.function.IntConsumer highlighter) {
        highlightedAttackTargets.clear();
        for (int territoryId : highlightedTerritories) if (territoryId >= 1 && territoryId <= mapButtons.length) highlighter.accept(territoryId);
    }

    static void highlightTerritoryWhite(int territoryId, javax.swing.JButton[] mapButtons, java.util.List<Integer> highlightedAttackTargets) {
        if (territoryId >= 1 && territoryId <= mapButtons.length) {
            mapButtons[territoryId - 1].setBackground(java.awt.Color.WHITE);
            if (!highlightedAttackTargets.contains(territoryId)) highlightedAttackTargets.add(territoryId);
        }
    }

    static void restoreHighlightedTerritories(java.util.List<Integer> highlightedAttackTargets, int[] territoryOwner,
            javax.swing.JButton[] mapButtons, java.awt.Color[] playerColors) {
        for (Integer territoryId : highlightedAttackTargets) {
            int owner = territoryOwner[territoryId];
            if (owner >= 1 && owner <= playerColors.length) mapButtons[territoryId - 1].setBackground(playerColors[owner - 1]);
        }
        highlightedAttackTargets.clear();
    }

    static javax.swing.JButton[] createMapButtonArray(Object owner) {
        javax.swing.JButton[] buttons = new javax.swing.JButton[RiskGameLogic.TERRITORY_COUNT];
        Class<?> type = owner.getClass();
        for (int i = 1; i <= buttons.length; i++) buttons[i - 1] = findButton(type, owner, "jButton" + i);
        return buttons;
    }
    static javax.swing.JTextField createArmyInputField() {
        javax.swing.JTextField field = new javax.swing.JTextField("1");
        field.setVisible(false);
        return field;
    }

    static void styleRightPanel(javax.swing.JPanel panel, javax.swing.JLabel[] labels, javax.swing.JLabel[] oldPlayerLabels,
            javax.swing.JLabel phaseLabel, javax.swing.JLabel turnLabel, javax.swing.JLabel remainingLabel) {
        panel.setBackground(new java.awt.Color(33, 43, 52));
        java.awt.Color textColor = new java.awt.Color(235, 226, 206);
        for (javax.swing.JLabel label : labels) label.setForeground(textColor);
        labels[0].setVisible(false);
        labels[3].setVisible(false);
        turnLabel.setText("Turn: Player 1");
        turnLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 17));
        panel.remove(turnLabel);
        panel.add(turnLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 25, 260, 28));
        phaseLabel.setText("Phase: Placement Phase");
        phaseLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        panel.remove(phaseLabel);
        panel.add(phaseLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 62, 260, 24));
        remainingLabel.setText("Remaining Armies: 0");
        remainingLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        panel.remove(remainingLabel);
        panel.add(remainingLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 92, 260, 22));
        for (javax.swing.JLabel label : oldPlayerLabels) label.setVisible(false);
    }

    static PlayerStatsView addPlayerStatsList(javax.swing.JPanel panel, javax.swing.DefaultListModel<String> model) {
        javax.swing.JList<String> list = new javax.swing.JList<>(model);
        list.setBackground(new java.awt.Color(23, 27, 31));
        list.setForeground(new java.awt.Color(235, 226, 206));
        list.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        list.setFixedCellHeight(28);
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);
        list.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> source, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                javax.swing.JLabel label = (javax.swing.JLabel) super.getListCellRendererComponent(source, value, index, false, false);
                label.setOpaque(true);
                label.setBackground(new java.awt.Color(23, 27, 31));
                label.setForeground(new java.awt.Color(235, 226, 206));
                label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 6));
                return label;
            }
        });
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(list);
        scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(70, 82, 92), 1));
        panel.add(scrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 145, 205, 180));
        panel.repaint();
        return new PlayerStatsView(list, scrollPane);
    }

    static DiceView addDiceDisplay(javax.swing.JPanel panel, Runnable rollAttacker, Runnable rollDefender) {
        java.awt.Color textColor = new java.awt.Color(235, 226, 206);
        javax.swing.JLabel battleInfoLabel = new javax.swing.JLabel("Battle: -");
        javax.swing.JLabel attackerDiceLabel = new javax.swing.JLabel("Attacker");
        javax.swing.JLabel defenderDiceLabel = new javax.swing.JLabel("Defender");
        attackerDiceLabel.setVisible(false);
        defenderDiceLabel.setVisible(false);
        battleInfoLabel.setForeground(textColor);
        attackerDiceLabel.setForeground(textColor);
        defenderDiceLabel.setForeground(textColor);
        battleInfoLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        attackerDiceLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        defenderDiceLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        attackerDiceLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        defenderDiceLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        attackerDiceLabel.addMouseListener(clickListener(rollAttacker));
        defenderDiceLabel.addMouseListener(clickListener(rollDefender));
        javax.swing.JLabel[] attackerDiceSlots = createDiceSlots(panel, 3, true, rollAttacker, rollDefender);
        javax.swing.JLabel[] defenderDiceSlots = createDiceSlots(panel, 2, false, rollAttacker, rollDefender);
        panel.add(battleInfoLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(8, 425, 228, 110));
        panel.add(attackerDiceLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(16, 360, 100, 22));
        panel.add(defenderDiceLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(135, 360, 100, 22));
        panel.repaint();
        return new DiceView(battleInfoLabel, attackerDiceLabel, defenderDiceLabel, attackerDiceSlots, defenderDiceSlots);
    }

    static void prepareDiceSlots(javax.swing.JLabel[] slots, int activeCount, Class<?> imageAnchor) {
        for (int i = 0; i < slots.length; i++) {
            slots[i].setVisible(i < activeCount);
            slots[i].setIcon(i < activeCount ? loadPictureIcon(imageAnchor, "/pictures/dice1.png", 28, 28) : null);
        }
    }

    static void showDiceSlots(javax.swing.JLabel[] slots, int[] diceValues, int rolledCount, Class<?> imageAnchor) {
        for (int i = 0; i < slots.length && i < rolledCount && i < diceValues.length; i++) {
            slots[i].setIcon(loadPictureIcon(imageAnchor, "/pictures/dice" + diceValues[i] + ".png", 28, 28));
        }
    }

    static void hideDiceSlots(javax.swing.JLabel[] slots) {
        for (javax.swing.JLabel slot : slots) {
            slot.setIcon(null);
            slot.setVisible(false);
        }
    }

    static void showRolledDice(javax.swing.JLabel label, int player, int[] dice, int rolledCount, int diceCount) {
        int[] visibleDice = new int[rolledCount];
        for (int i = 0; i < rolledCount; i++) visibleDice[i] = dice[i];
        label.setText("P" + player + ": " + DiceRoller.formatDice(visibleDice) + " (" + rolledCount + "/" + diceCount + ")");
    }

    static javax.swing.ImageIcon loadPictureIcon(Class<?> anchor, String path, int width, int height) {
        java.net.URL imageUrl = anchor.getResource(path);
        if (imageUrl == null) return null;
        java.awt.Image image = new javax.swing.ImageIcon(imageUrl).getImage();
        return new javax.swing.ImageIcon(image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
    }
    static void showTurnEndedPopup(javax.swing.JFrame owner, int localPlayerNumber, boolean onlineMode, int endedPlayer) {
        javax.swing.JPanel panel = createPanel(new java.awt.BorderLayout(), new java.awt.Color(120, 150, 170), 2, new java.awt.Insets(14, 24, 14, 24));
        javax.swing.JLabel message = new javax.swing.JLabel("Player " + endedPlayer + " turn ended", javax.swing.SwingConstants.CENTER);
        message.setForeground(TEXT);
        message.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        panel.add(message, java.awt.BorderLayout.CENTER);
        showPopup(owner, localPlayerNumber, onlineMode, panel, 1000);
    }

    static void showGameNoticePopup(javax.swing.JFrame owner, int localPlayerNumber, boolean onlineMode,
            String titleText, String detailText, java.awt.Color borderColor, int durationMillis) {
        javax.swing.JPanel panel = createPanel(new java.awt.BorderLayout(0, 6), borderColor, 3, new java.awt.Insets(18, 32, 18, 32));
        javax.swing.JLabel title = new javax.swing.JLabel(titleText, javax.swing.SwingConstants.CENTER);
        title.setForeground(borderColor);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        javax.swing.JLabel detail = new javax.swing.JLabel(detailText, javax.swing.SwingConstants.CENTER);
        detail.setForeground(TEXT);
        detail.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        panel.add(title, java.awt.BorderLayout.CENTER);
        panel.add(detail, java.awt.BorderLayout.SOUTH);
        showPopup(owner, localPlayerNumber, onlineMode, panel, durationMillis);
    }

    static void showPlacementFinishedPopup(javax.swing.JFrame owner, int localPlayerNumber, boolean onlineMode, String text) {
        javax.swing.JPanel panel = createPanel(new java.awt.BorderLayout(), GOLD, 2, new java.awt.Insets(14, 24, 14, 24));
        javax.swing.JLabel message = new javax.swing.JLabel(text, javax.swing.SwingConstants.CENTER);
        message.setForeground(TEXT);
        message.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        panel.add(message, java.awt.BorderLayout.CENTER);
        showPopup(owner, localPlayerNumber, onlineMode, panel, 1200);
    }

    static void showPlacementTurnPopupFor(javax.swing.JFrame owner, int localPlayerNumber, boolean onlineMode, int player) {
        javax.swing.JPanel panel = createPanel(new java.awt.BorderLayout(0, 4), GOLD, 2, new java.awt.Insets(14, 24, 14, 24));
        javax.swing.JLabel title = new javax.swing.JLabel("Player " + player, javax.swing.SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        javax.swing.JLabel phase = new javax.swing.JLabel("Placement Phase", javax.swing.SwingConstants.CENTER);
        phase.setForeground(GOLD);
        phase.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        panel.add(title, java.awt.BorderLayout.CENTER);
        panel.add(phase, java.awt.BorderLayout.SOUTH);
        showPopup(owner, localPlayerNumber, onlineMode, panel, 1200);
    }

    static void showReinforcementBonusPopup(javax.swing.JFrame owner, int localPlayerNumber, boolean onlineMode, int player, int reinforcementArmies) {
        javax.swing.JPanel panel = createPanel(new java.awt.BorderLayout(0, 4), GOLD, 2, new java.awt.Insets(14, 24, 14, 24));
        javax.swing.JLabel title = new javax.swing.JLabel("Reinforcement Bonus", javax.swing.SwingConstants.CENTER);
        title.setForeground(GOLD);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        javax.swing.JLabel amount = new javax.swing.JLabel("Player " + player + " +" + reinforcementArmies + " armies", javax.swing.SwingConstants.CENTER);
        amount.setForeground(TEXT);
        amount.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        panel.add(title, java.awt.BorderLayout.NORTH);
        panel.add(amount, java.awt.BorderLayout.CENTER);
        showPopup(owner, localPlayerNumber, onlineMode, panel, 1300);
    }

    static void showArmyCountOnButton(javax.swing.JButton button, int armyCount) {
        button.setText(String.valueOf(armyCount));
        button.setForeground(java.awt.Color.BLACK);
        button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 8));
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        button.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        button.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        button.setVerticalTextPosition(javax.swing.SwingConstants.CENTER);
    }

    static void updatePlacementLabels(javax.swing.JLabel phaseLabel, javax.swing.JLabel turnLabel,
            javax.swing.JLabel remainingLabel, javax.swing.JLabel battleInfoLabel, javax.swing.JPanel sidePanel,
            boolean onlineMode, int localPlayerNumber, int currentPlayer, boolean placementPhase,
            int remainingArmies, String currentPhaseName, boolean battleInProgress, Runnable updatePlayerStats) {
        if (onlineMode) {
            turnLabel.setText("You: Player " + localPlayerNumber + " | Turn: Player " + currentPlayer);
            remainingLabel.setText(placementPhase ? "Player " + currentPlayer + " to place: " + remainingArmies : "Player " + currentPlayer + " to act");
            if (battleInfoLabel != null && !battleInProgress) battleInfoLabel.setText("Player " + currentPlayer + " turn - " + currentPhaseName);
        } else {
            turnLabel.setText("Turn: Player " + currentPlayer);
            remainingLabel.setText("Remaining Armies: " + remainingArmies);
        }
        phaseLabel.setText("Phase: " + currentPhaseName);
        turnLabel.setVisible(true);
        phaseLabel.setVisible(true);
        remainingLabel.setVisible(true);
        updatePlayerStats.run();
        sidePanel.revalidate();
        sidePanel.repaint();
    }

    static void updatePlayerStatsLabels(javax.swing.DefaultListModel<String> model, int playerCount,
            java.util.function.IntUnaryOperator countTerritories, java.util.function.IntUnaryOperator countArmies) {
        if (model == null) return;
        model.clear();
        for (int player = 1; player <= playerCount; player++) {
            java.awt.Color color = player == 1 ? new java.awt.Color(255, 80, 80) : new java.awt.Color(80, 170, 255);
            String colorStyle = "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
            model.addElement("<html><span style='color:" + colorStyle + ";'>Player " + player + "</span>: "
                    + countTerritories.applyAsInt(player) + " countries, " + countArmies.applyAsInt(player) + " armies</html>");
        }
    }
    private static javax.swing.JLabel[] createDiceSlots(javax.swing.JPanel panel, int count, boolean attacker, Runnable rollAttacker, Runnable rollDefender) {
        javax.swing.JLabel[] slots = new javax.swing.JLabel[count];
        int startX = attacker ? 20 : 130;
        Runnable action = attacker ? rollAttacker : rollDefender;
        for (int i = 0; i < count; i++) {
            javax.swing.JLabel slot = new javax.swing.JLabel();
            slot.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            slot.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            slot.setVisible(false);
            slot.addMouseListener(clickListener(action));
            panel.add(slot, new org.netbeans.lib.awtextra.AbsoluteConstraints(startX + (i * 31), 388, 28, 28));
            slots[i] = slot;
        }
        return slots;
    }

    private static java.awt.event.MouseAdapter clickListener(Runnable action) {
        return new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent evt) { action.run(); }
        };
    }

    private static javax.swing.JPanel createPanel(java.awt.LayoutManager layout, java.awt.Color borderColor, int borderSize, java.awt.Insets padding) {
        javax.swing.JPanel panel = new javax.swing.JPanel(layout);
        panel.setBackground(PANEL_BG);
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(borderColor, borderSize),
                javax.swing.BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)));
        return panel;
    }

    private static void showPopup(javax.swing.JFrame owner, int localPlayerNumber, boolean onlineMode, javax.swing.JPanel panel, int durationMillis) {
        javax.swing.JWindow popup = new javax.swing.JWindow(owner);
        popup.add(panel);
        popup.pack();
        popup.setAlwaysOnTop(true);
        java.awt.Point ownerLocation;
        try { ownerLocation = owner.getLocationOnScreen(); } catch (java.awt.IllegalComponentStateException ex) { ownerLocation = owner.getLocation(); }
        int x = ownerLocation.x + Math.max(0, (owner.getWidth() - popup.getWidth()) / 2);
        int y = ownerLocation.y + Math.max(0, (owner.getHeight() - popup.getHeight()) / 2);
        if (onlineMode && localPlayerNumber > 0) { x += (localPlayerNumber - 1) * 36; y += (localPlayerNumber - 1) * 36; }
        popup.setLocation(x, y);
        popup.setVisible(true);
        popup.toFront();
        javax.swing.Timer timer = new javax.swing.Timer(durationMillis, evt -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private static javax.swing.JButton findButton(Class<?> type, Object owner, String fieldName) {
        try {
            java.lang.reflect.Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (javax.swing.JButton) field.get(owner);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Map button not found: " + fieldName, ex);
        }
    }

    private static int parseNumber(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return 0; }
    }

    static final class PlayerStatsView {
        final javax.swing.JList<String> list;
        final javax.swing.JScrollPane scrollPane;
        PlayerStatsView(javax.swing.JList<String> list, javax.swing.JScrollPane scrollPane) { this.list = list; this.scrollPane = scrollPane; }
    }

    static final class DiceView {
        final javax.swing.JLabel battleInfoLabel;
        final javax.swing.JLabel attackerDiceLabel;
        final javax.swing.JLabel defenderDiceLabel;
        final javax.swing.JLabel[] attackerDiceSlots;
        final javax.swing.JLabel[] defenderDiceSlots;
        DiceView(javax.swing.JLabel battleInfoLabel, javax.swing.JLabel attackerDiceLabel, javax.swing.JLabel defenderDiceLabel,
                javax.swing.JLabel[] attackerDiceSlots, javax.swing.JLabel[] defenderDiceSlots) {
            this.battleInfoLabel = battleInfoLabel;
            this.attackerDiceLabel = attackerDiceLabel;
            this.defenderDiceLabel = defenderDiceLabel;
            this.attackerDiceSlots = attackerDiceSlots;
            this.defenderDiceSlots = defenderDiceSlots;
        }
    }

    static final class ResponsiveLayout {
        private static final int GAME_BASE_WIDTH = 982;
        private static final int GAME_BASE_HEIGHT = 551;
        private final javax.swing.JFrame frame;
        private final javax.swing.JLayeredPane mapPane;
        private final javax.swing.JPanel sidePanel;
        private final javax.swing.JLabel mapLabel;
        private final java.util.Map<java.awt.Component, java.awt.Rectangle> baseBounds = new java.util.HashMap<>();
        private final java.util.Map<java.awt.Component, java.awt.Font> baseFonts = new java.util.HashMap<>();
        private javax.swing.JPanel responsiveGamePanel;
        private javax.swing.ImageIcon originalMapIcon;

        ResponsiveLayout(javax.swing.JFrame frame, javax.swing.JLayeredPane mapPane, javax.swing.JPanel sidePanel, javax.swing.JLabel mapLabel) {
            this.frame = frame; this.mapPane = mapPane; this.sidePanel = sidePanel; this.mapLabel = mapLabel;
        }

        void configure() {
            if (responsiveGamePanel != null) return;
            mapPane.doLayout(); sidePanel.doLayout();
            mapPane.setBounds(6, 6, 707, 539); sidePanel.setBounds(731, 0, 245, 551);
            responsiveGamePanel = new javax.swing.JPanel(null);
            responsiveGamePanel.setBackground(frame.getContentPane().getBackground());
            frame.getContentPane().removeAll();
            frame.getContentPane().setLayout(new java.awt.BorderLayout());
            frame.getContentPane().add(responsiveGamePanel, java.awt.BorderLayout.CENTER);
            responsiveGamePanel.add(mapPane); responsiveGamePanel.add(sidePanel);
            rememberBounds(mapPane); rememberBounds(sidePanel);
            if (mapLabel.getIcon() instanceof javax.swing.ImageIcon) originalMapIcon = (javax.swing.ImageIcon) mapLabel.getIcon();
            mapPane.setLayout(null); sidePanel.setLayout(null);
            responsiveGamePanel.addComponentListener(new java.awt.event.ComponentAdapter() { @Override public void componentResized(java.awt.event.ComponentEvent evt) { resizeGame(); } });
            frame.setMinimumSize(new java.awt.Dimension(520, 310));
            frame.setSize(new java.awt.Dimension(1000, 590));
            frame.revalidate(); frame.repaint();
            javax.swing.SwingUtilities.invokeLater(this::resizeGame);
        }

        private void rememberBounds(java.awt.Container parent) {
            for (java.awt.Component component : parent.getComponents()) { baseBounds.put(component, component.getBounds()); baseFonts.put(component, component.getFont()); }
        }

        private void resizeGame() {
            if (responsiveGamePanel == null || responsiveGamePanel.getWidth() <= 0 || responsiveGamePanel.getHeight() <= 0) return;
            double scale = Math.min(responsiveGamePanel.getWidth() / (double) GAME_BASE_WIDTH, responsiveGamePanel.getHeight() / (double) GAME_BASE_HEIGHT);
            scale = Math.max(0.45, Math.min(1.0, scale));
            int scaledWidth = (int) Math.round(GAME_BASE_WIDTH * scale);
            int scaledHeight = (int) Math.round(GAME_BASE_HEIGHT * scale);
            int offsetX = Math.max(0, (responsiveGamePanel.getWidth() - scaledWidth) / 2);
            int offsetY = Math.max(0, (responsiveGamePanel.getHeight() - scaledHeight) / 2);
            mapPane.setBounds(offsetX + scaleValue(6, scale), offsetY + scaleValue(6, scale), scaleValue(707, scale), scaleValue(539, scale));
            sidePanel.setBounds(offsetX + scaleValue(731, scale), offsetY, scaleValue(245, scale), scaleValue(551, scale));
            scaleChildren(mapPane, scale); scaleChildren(sidePanel, scale); scaleMapImage();
            responsiveGamePanel.revalidate(); responsiveGamePanel.repaint();
        }

        private void scaleChildren(java.awt.Container parent, double scale) {
            for (java.awt.Component component : parent.getComponents()) {
                java.awt.Rectangle base = baseBounds.get(component);
                if (base == null) continue;
                component.setBounds(scaleValue(base.x, scale), scaleValue(base.y, scale), scaleValue(base.width, scale), scaleValue(base.height, scale));
                java.awt.Font baseFont = baseFonts.get(component);
                if (baseFont != null) component.setFont(baseFont.deriveFont(Math.max(7f, (float) (baseFont.getSize2D() * scale))));
            }
        }

        private int scaleValue(int value, double scale) { return Math.max(1, (int) Math.round(value * scale)); }

        private void scaleMapImage() {
            if (originalMapIcon == null) return;
            int width = Math.max(1, mapLabel.getWidth());
            int height = Math.max(1, mapLabel.getHeight());
            java.awt.Image image = originalMapIcon.getImage().getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
            mapLabel.setIcon(new javax.swing.ImageIcon(image));
        }
    }
}