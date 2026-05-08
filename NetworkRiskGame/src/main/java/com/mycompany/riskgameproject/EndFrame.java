package com.mycompany.riskgameproject;

public class EndFrame extends javax.swing.JFrame {

    public EndFrame(int winnerPlayer, Runnable replayAction) {
        this("PLAYER " + winnerPlayer + " WON", "Game over", replayAction);
    }

    public EndFrame(String titleText, String detailText, Runnable replayAction) {
        initComponents(titleText, detailText, replayAction);
    }

    private void initComponents(String titleText, String detailText, Runnable replayAction) {
        setTitle("Game Over");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(12, 18));
        panel.setBackground(new java.awt.Color(28, 35, 42));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(28, 36, 28, 36));

        javax.swing.JLabel title = new javax.swing.JLabel(titleText, javax.swing.SwingConstants.CENTER);
        title.setForeground(new java.awt.Color(218, 174, 75));
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));

        javax.swing.JLabel detail = new javax.swing.JLabel(detailText, javax.swing.SwingConstants.CENTER);
        detail.setForeground(new java.awt.Color(245, 238, 220));
        detail.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));

        javax.swing.JButton replayButton = new javax.swing.JButton("Replay");
        replayButton.addActionListener(evt -> {
            replayButton.setEnabled(false);
            replayButton.setText("Starting...");
            replayAction.run();
        });

        javax.swing.JButton exitButton = new javax.swing.JButton("Exit");
        exitButton.addActionListener(evt -> System.exit(0));

        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(replayButton);
        buttonPanel.add(exitButton);

        javax.swing.JPanel textPanel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        textPanel.setOpaque(false);
        textPanel.add(title, java.awt.BorderLayout.CENTER);
        textPanel.add(detail, java.awt.BorderLayout.SOUTH);

        panel.add(textPanel, java.awt.BorderLayout.CENTER);
        panel.add(buttonPanel, java.awt.BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setSize(360, 210);
        setLocationRelativeTo(null);
    }
}
