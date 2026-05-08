package com.mycompany.riskgameproject.client;

import com.mycompany.riskgameproject.GameFrame;
import com.mycompany.riskgameproject.EndFrame;
import com.mycompany.riskgameproject.common.GameState;
import com.mycompany.riskgameproject.common.Message;
import com.mycompany.riskgameproject.common.MessageType;
import com.mycompany.riskgameproject.model.PlayerInfo;
import com.mycompany.riskgameproject.model.RoomInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ClientFrame extends JFrame {

    private final DefaultListModel<String> roomListModel = new DefaultListModel<>();
    private final JLabel statusLabel = new JLabel("Not connected");
    private final JTextField nameField = new JTextField(defaultPlayerName());
    private final JButton connectButton = new JButton("Joining...");
    private GameClient client;
    private String roomCode;
    private int playerNumber;
    private GameFrame gameFrame;
    private EndFrame endFrame;

    public ClientFrame() {
        initLobbyComponents();
    }

    private void initLobbyComponents() {
        setTitle("NetworkRiskGame Lobby");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                disconnectAndExit();
            }
        });
        setPreferredSize(new Dimension(420, 300));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(new JLabel("Name:"), BorderLayout.WEST);
        topPanel.add(nameField, BorderLayout.CENTER);
        topPanel.add(connectButton, BorderLayout.EAST);

        JList<String> roomList = new JList<>(roomListModel);
        JScrollPane scrollPane = new JScrollPane(roomList);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(topPanel, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);

        connectButton.addActionListener(evt -> connectToServer());
        pack();
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(this::connectToServer);
    }

    private void connectToServer() {
        connectButton.setEnabled(false);
        statusLabel.setText("Connecting to " + GameClient.DEFAULT_HOST + ":" + GameClient.DEFAULT_PORT + "...");
        client = new GameClient(this::handleServerMessage);
        try {
            client.connect(nameField.getText());
        } catch (IOException ex) {
            connectButton.setEnabled(true);
            statusLabel.setText("Connection failed");
            JOptionPane.showMessageDialog(this, "Could not connect to the game server.");
        }
    }

    private void handleServerMessage(Message message) {
        // Serverdan gelen cevaplari arayuz kilitlenmesin diye Swing tarafinda isliyorum.
        SwingUtilities.invokeLater(() -> applyServerMessage(message));
    }

    private void applyServerMessage(Message message) {
        if (message.getType() == MessageType.ROOM_ASSIGNED) {
            roomCode = message.getRoomCode();
            playerNumber = Integer.parseInt(message.getText());
            statusLabel.setText("Joined " + roomCode + " as Player " + playerNumber + ". Waiting for opponent...");
            updateRoomList(message);
            return;
        }
        if (message.getType() == MessageType.LOBBY_UPDATE) {
            updateRoomList(message);
            return;
        }
        if (message.getType() == MessageType.START_GAME) {
            statusLabel.setText(roomCode + " ready. You are Player " + playerNumber + ".");
            openGameFrame(message);
            return;
        }
        if (message.getType() == MessageType.GAME_OVER) {
            showGameOver(message);
            return;
        }
        if (message.getType() == MessageType.PLAYER_LEFT) {
            showPlayerLeft(message);
            return;
        }
        if (message.getType() == MessageType.GAME_STATE) {
            applyGameState(message);
            return;
        }
        if (message.getType() == MessageType.DISCONNECT) {
            statusLabel.setText("Disconnected: " + message.getText());
            connectButton.setEnabled(true);
            return;
        }
        if (message.getType() == MessageType.ERROR) {
            JOptionPane.showMessageDialog(this, message.getText());
        }
    }


    private void openGameFrame(Message message) {
        if (!(message.getPayload() instanceof GameState state)) {
            JOptionPane.showMessageDialog(this, "Game could not start: missing game state.");
            return;
        }
        if (gameFrame == null) {
            gameFrame = new GameFrame(state, playerNumber, this::sendGameState, this::sendGameOver, this::sendReplayRequest, this::returnToLobby, this::sendPlayerLeft);
            gameFrame.setVisible(true);
            gameFrame.showInitialPlacementPopup();
            setVisible(false);
        } else {
            gameFrame.restartGame(state);
        }
    }

    private void showGameOver(Message message) {
        int winner = parsePlayerNumber(message.getText());
        if (gameFrame != null && winner > 0) {
            gameFrame.showEndFrame(winner);
        }
    }

    private void showPlayerLeft(Message message) {
        int leftPlayer = parsePlayerNumber(message.getText());
        if (leftPlayer <= 0) {
            return;
        }
        if (gameFrame != null) {
            gameFrame.showPlayerLeftFrame(leftPlayer);
            closeCurrentConnection();
            return;
        }
        if (endFrame != null && endFrame.isDisplayable()) {
            endFrame.toFront();
            return;
        }
        endFrame = new EndFrame("PLAYER " + leftPlayer + " LEFT", "Game over", this::returnToLobby);
        endFrame.setLocationRelativeTo(this);
        endFrame.setVisible(true);
        endFrame.toFront();
        closeCurrentConnection();
    }
    private void applyGameState(Message message) {
        if (gameFrame != null && message.getPayload() instanceof GameState state) {
            gameFrame.applyRemoteState(state);
        }
    }

    private void sendGameOver(int winnerPlayer) {
        try {
            client.sendMessage(MessageType.GAME_OVER, roomCode, nameField.getText(), String.valueOf(winnerPlayer));
        } catch (IOException ex) {
            statusLabel.setText("Could not send game over: " + ex.getMessage());
        }
    }

    private void sendReplayRequest() {
        try {
            client.sendMessage(MessageType.REPLAY_REQUEST, roomCode, nameField.getText(), "replay");
        } catch (IOException ex) {
            statusLabel.setText("Could not request replay: " + ex.getMessage());
        }
    }

    private void sendPlayerLeft(int leftPlayer) {
        try {
            client.sendMessage(MessageType.PLAYER_LEFT, roomCode, nameField.getText(), String.valueOf(leftPlayer));
        } catch (IOException ex) {
            statusLabel.setText("Could not send player left: " + ex.getMessage());
        } finally {
            closeCurrentConnection();
        }
    }

    private void returnToLobby() {
        if (endFrame != null) {
            endFrame.dispose();
            endFrame = null;
        }
        if (gameFrame != null) {
            gameFrame.dispose();
            gameFrame = null;
        }
        closeCurrentConnection();
        roomCode = null;
        playerNumber = 0;
        roomListModel.clear();
        setVisible(true);
        connectToServer();
    }

    private void closeCurrentConnection() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private void disconnectAndExit() {
        try {
            if (client != null) {
                client.sendMessage(MessageType.DISCONNECT, roomCode, nameField.getText(), String.valueOf(playerNumber));
            }
        } catch (IOException ex) {
            // Closing anyway.
        } finally {
            closeCurrentConnection();
        }
        dispose();
    }
    private void sendGameState(GameState state) {
        try {
            // Oyun ekrandaki hamleden sonra server uzerinden diger client'a gonderiliyor.
            client.sendMessage(MessageType.GAME_STATE, roomCode, nameField.getText(), "state", state);
        } catch (IOException ex) {
            statusLabel.setText("Could not send game state: " + ex.getMessage());
        }
    }

    private void updateRoomList(Message message) {
        if (!(message.getPayload() instanceof RoomInfo info)) {
            return;
        }
        roomListModel.clear();
        roomListModel.addElement(info.getRoomCode());
        for (PlayerInfo player : info.getPlayers()) {
            roomListModel.addElement("Player " + player.getPlayerNumber() + ": " + player.getPlayerName());
        }
        int waitingCount = RoomInfo.class.cast(info).getPlayers().size();
        if (waitingCount < 2) {
            roomListModel.addElement("Waiting for Player 2...");
        }
    }

    private int parsePlayerNumber(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    private static String defaultPlayerName() {
        return "Player" + ((int) (Math.random() * 900) + 100);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientFrame().setVisible(true));
    }
}
