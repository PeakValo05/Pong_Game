package com.pong;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int COURT_MARGIN = 28;
    private static final int PADDLE_WIDTH = 12;
    private static final int PADDLE_HEIGHT = 92;
    private static final int BALL_SIZE = 16;
    private static final int PADDLE_SPEED = 6;
    private static final int START_COUNTDOWN = 3;
    private static final Color BACKGROUND = new Color(8, 13, 27);
    private static final Color COURT = new Color(13, 21, 40);
    private static final Color CYAN = new Color(75, 229, 255);
    private static final Color CORAL = new Color(255, 111, 145);
    private static final Color MUTED = new Color(143, 160, 190);

    private final Timer gameTimer;
    private final Timer countdownTimer;
    private int ballX;
    private int ballY;
    private int ballSpeedX = 5;
    private int ballSpeedY = 3;
    private int leftPaddleY;
    private int rightPaddleY;
    private int leftPoints;
    private int rightPoints;
    private int countdown = START_COUNTDOWN;
    private boolean leftUpPressed;
    private boolean leftDownPressed;
    private boolean rightUpPressed;
    private boolean rightDownPressed;
    private boolean positionsInitialized;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(560, 400));
        setBackground(BACKGROUND);
        setFocusable(true);
        installControls();
        resetPositions();

        gameTimer = new Timer(16, event -> updateGame());
        gameTimer.start();
        countdownTimer = new Timer(1000, event -> {
            if (countdown > 0) {
                countdown--;
                repaint();
            }
        });
        countdownTimer.start();
    }

    private void installControls() {
        bindKey(KeyEvent.VK_W, "left-up", () -> leftUpPressed = true, () -> leftUpPressed = false);
        bindKey(KeyEvent.VK_S, "left-down", () -> leftDownPressed = true, () -> leftDownPressed = false);
        bindKey(KeyEvent.VK_UP, "right-up", () -> rightUpPressed = true, () -> rightUpPressed = false);
        bindKey(KeyEvent.VK_DOWN, "right-down", () -> rightDownPressed = true, () -> rightDownPressed = false);
    }

    private void bindKey(int keyCode, String name, Runnable onPress, Runnable onRelease) {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), name + "-pressed");
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), name + "-released");
        actionMap.put(name + "-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onPress.run();
            }
        });
        actionMap.put(name + "-released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onRelease.run();
            }
        });
    }

    private void updateGame() {
        if (!positionsInitialized && getWidth() > 0 && getHeight() > 0) {
            resetPositions();
        }
        if (countdown <= 0) {
            movePaddles();
            ballX += ballSpeedX;
            ballY += ballSpeedY;
            bounceOffWalls();
            bounceOffPaddles();
            updateScore();
        }
        repaint();
    }

    private void movePaddles() {
        if (leftUpPressed) {
            leftPaddleY -= PADDLE_SPEED;
        }
        if (leftDownPressed) {
            leftPaddleY += PADDLE_SPEED;
        }
        if (rightUpPressed) {
            rightPaddleY -= PADDLE_SPEED;
        }
        if (rightDownPressed) {
            rightPaddleY += PADDLE_SPEED;
        }

        int maxY = Math.max(COURT_MARGIN, getHeight() - COURT_MARGIN - PADDLE_HEIGHT);
        leftPaddleY = Math.max(COURT_MARGIN, Math.min(leftPaddleY, maxY));
        rightPaddleY = Math.max(COURT_MARGIN, Math.min(rightPaddleY, maxY));
    }

    private void bounceOffWalls() {
        int top = COURT_MARGIN + 1;
        int bottom = getHeight() - COURT_MARGIN - BALL_SIZE - 1;
        if (ballY <= top) {
            ballY = top;
            ballSpeedY = Math.abs(ballSpeedY);
        } else if (ballY >= bottom) {
            ballY = bottom;
            ballSpeedY = -Math.abs(ballSpeedY);
        }
    }

    private void bounceOffPaddles() {
        int leftPaddleX = COURT_MARGIN + 12;
        int rightPaddleX = getWidth() - COURT_MARGIN - 12 - PADDLE_WIDTH;
        if (ballSpeedX < 0
                && ballX <= leftPaddleX + PADDLE_WIDTH
                && ballX + BALL_SIZE >= leftPaddleX
                && ballY + BALL_SIZE >= leftPaddleY
                && ballY <= leftPaddleY + PADDLE_HEIGHT) {
            ballX = leftPaddleX + PADDLE_WIDTH;
            bounceFromPaddle(leftPaddleY, 1);
        } else if (ballSpeedX > 0
                && ballX + BALL_SIZE >= rightPaddleX
                && ballX <= rightPaddleX + PADDLE_WIDTH
                && ballY + BALL_SIZE >= rightPaddleY
                && ballY <= rightPaddleY + PADDLE_HEIGHT) {
            ballX = rightPaddleX - BALL_SIZE;
            bounceFromPaddle(rightPaddleY, -1);
        }
    }

    
    private void bounceFromPaddle(int paddleY, int horizontalDirection) {
        double impact = (ballY + BALL_SIZE / 2.0 - (paddleY + PADDLE_HEIGHT / 2.0))
                / (PADDLE_HEIGHT / 2.0);
        ballSpeedX = horizontalDirection * Math.min(Math.abs(ballSpeedX) + 1, 9);
        ballSpeedY = (int) Math.round(Math.max(-7, Math.min(7, impact * 6)));
        if (ballSpeedY == 0) {
            ballSpeedY = impact < 0 ? -1 : 1;
        }
    }

    private void updateScore() {
        if (ballX + BALL_SIZE < 0) {
            rightPoints++;
            startNextRound(-1);
        } else if (ballX > getWidth()) {
            leftPoints++;
            startNextRound(1);
        }
    }

    private void startNextRound(int serveDirection) {
        resetPositions();
        ballSpeedX = serveDirection * 5;
        ballSpeedY = 3;
        countdown = START_COUNTDOWN;
    }

    private void resetPositions() {
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);
        ballX = (width - BALL_SIZE) / 2;
        ballY = (height - BALL_SIZE) / 2;
        leftPaddleY = (height - PADDLE_HEIGHT) / 2;
        rightPaddleY = leftPaddleY;
        positionsInitialized = getWidth() > 0 && getHeight() > 0;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        paintCourt(g, width, height);
        paintHud(g, width);
        paintPaddle(g, COURT_MARGIN + 12, leftPaddleY, CYAN);
        paintPaddle(g, width - COURT_MARGIN - 12 - PADDLE_WIDTH, rightPaddleY, CORAL);
        paintBall(g);
        if (countdown > 0) {
            paintCountdown(g, width, height);
        }
        g.dispose();
    }

    private void paintCourt(Graphics2D g, int width, int height) {
        g.setPaint(new GradientPaint(0, 0, new Color(11, 19, 37), 0, height, BACKGROUND));
        g.fillRect(0, 0, width, height);

        int courtX = COURT_MARGIN;
        int courtY = COURT_MARGIN;
        int courtWidth = Math.max(0, width - COURT_MARGIN * 2);
        int courtHeight = Math.max(0, height - COURT_MARGIN * 2);
        g.setColor(COURT);
        g.fill(new RoundRectangle2D.Float(courtX, courtY, courtWidth, courtHeight, 20, 20));
        g.setColor(new Color(83, 108, 151, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(courtX, courtY, courtWidth, courtHeight, 20, 20));

        g.setColor(new Color(143, 160, 190, 85));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{7, 12}, 0));
        int centerX = width / 2;
        g.drawLine(centerX, courtY + 16, centerX, courtY + courtHeight - 16);
        g.setColor(new Color(75, 229, 255, 24));
        g.drawOval(centerX - 68, height / 2 - 68, 136, 136);
        g.setColor(new Color(75, 229, 255, 40));
        g.drawOval(centerX - 42, height / 2 - 42, 84, 84);
    }

    private void paintHud(Graphics2D g, int width) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(CYAN);
        g.drawString("PLAYER 1", COURT_MARGIN + 18, COURT_MARGIN + 25);
        g.setColor(CORAL);
        String playerTwo = "PLAYER 2";
        g.drawString(playerTwo, width - COURT_MARGIN - 18 - g.getFontMetrics().stringWidth(playerTwo),
                COURT_MARGIN + 25);

        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.setColor(new Color(228, 238, 255));
        String leftScore = String.valueOf(leftPoints);
        String rightScore = String.valueOf(rightPoints);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(leftScore, width / 2 - 32 - metrics.stringWidth(leftScore), COURT_MARGIN + 65);
        g.drawString(rightScore, width / 2 + 32, COURT_MARGIN + 65);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(MUTED);
        String controls = "W / S  MOVE                              ↑ / ↓  MOVE";
        int controlsWidth = g.getFontMetrics().stringWidth(controls);
        g.drawString(controls, Math.max(12, (width - controlsWidth) / 2), getHeight() - 10);
    }

    private void paintPaddle(Graphics2D g, int x, int y, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
        g.fillRoundRect(x - 5, y - 5, PADDLE_WIDTH + 10, PADDLE_HEIGHT + 10, 13, 13);
        g.setColor(color);
        g.fillRoundRect(x, y, PADDLE_WIDTH, PADDLE_HEIGHT, 9, 9);
    }

    private void paintBall(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 40));
        g.fillOval(ballX - 8, ballY - 8, BALL_SIZE + 16, BALL_SIZE + 16);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(ballX - 3, ballY - 3, BALL_SIZE + 6, BALL_SIZE + 6);
        g.setColor(Color.WHITE);
        g.fillRoundRect(ballX, ballY, BALL_SIZE, BALL_SIZE, 5, 5);
    }

    private void paintCountdown(Graphics2D g, int width, int height) {
        g.setColor(new Color(5, 10, 22, 150));
        g.fillRoundRect(width / 2 - 72, height / 2 - 66, 144, 132, 24, 24);
        g.setColor(new Color(255, 255, 255, 225));
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        String text = String.valueOf(countdown);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (width - metrics.stringWidth(text)) / 2, height / 2 + metrics.getAscent() / 3);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(MUTED);
        String label = "GET READY";
        g.drawString(label, (width - g.getFontMetrics().stringWidth(label)) / 2, height / 2 + 36);
    }
}
