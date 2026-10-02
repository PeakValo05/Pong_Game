package com.pong;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("PONG");
            GamePanel gamePanel = new GamePanel();

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(gamePanel);
            window.pack();
            window.setMinimumSize(gamePanel.getMinimumSize());
            window.setLocationRelativeTo(null);
            window.setResizable(true);
            window.setVisible(true);
            gamePanel.requestFocusInWindow();
        });
    }
}