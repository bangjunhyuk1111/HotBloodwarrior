import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

class HotBloodWarriorClient {
    static int inPort = 9999;
    static String address = "192.168.40.133"; // 서버 주소
    static public PrintWriter out;
    static public BufferedReader in;
    static String userName = "Ikjae";
    static int hp = 50;
    static int opponentHp = 50;
    static int width = 10;
    static int[][] map = new int[width][width];
    static boolean[][] selected = new boolean[width][width];  // 선택한 칸을 추적
    static ClientGUI gui;
    static int playerId;
    static boolean myTurn = false;

    public static void main(String[] args) {
        try (Socket socket = new Socket(address, inPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            gui = new ClientGUI(socket);
            SwingUtilities.invokeLater(() -> gui.createAndShowGUI());

            out.println(userName);
            playerId = Integer.parseInt(in.readLine());

            // 서버로부터 초기 메시지 수신
            while (true) {
                String serverMessage = in.readLine();
                if (serverMessage != null) {
                    handleServerMessage(serverMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleServerMessage(String message) {
        if (message.startsWith("HP")) {
            String[] hpValues = message.split(" ");
            hp = Integer.parseInt(hpValues[1 + playerId]);
            opponentHp = Integer.parseInt(hpValues[2 - playerId]);
            SwingUtilities.invokeLater(() -> {
                gui.updateHp();
            });
        } else if (message.equals("choose!")) {
            myTurn = true;
            SwingUtilities.invokeLater(() -> {
                gui.updateTurnIndicator();
            });
        } else if (message.equals("waiting")) {
            myTurn = false;
            SwingUtilities.invokeLater(() -> {
                gui.updateTurnIndicator();
            });
        } else if (message.startsWith("value,")) {
            String[] arr = message.split(",");
            int x = Integer.parseInt(arr[1]);
            int y = Integer.parseInt(arr[2]);
            int value = Integer.parseInt(arr[3]);
            SwingUtilities.invokeLater(() -> {
                gui.updateButton(x, y, value);
            });
        } else {
            try {
                int value = Integer.parseInt(message);
                if (value >= 0) {
                    opponentHp -= value;
                } else {
                    hp += value;
                }
                SwingUtilities.invokeLater(() -> {
                    gui.updateHp();
                });
            } catch (NumberFormatException e) {
                System.out.println("Invalid message from server: " + message);
            }
        }
    }

    static class ClientGUI {
        JFrame frame;
        JPanel mainPanel;
        JLabel playerHpLabel;
        JLabel opponentHpLabel;
        JLabel turnLabel;
        JProgressBar playerHpBar;
        JProgressBar opponentHpBar;
        JLabel playerImageLabel;
        JLabel opponentImageLabel;
        JButton[][] buttons;
        Socket socket;

        public ClientGUI(Socket socket) {
            this.socket = socket;
        }

        public void createAndShowGUI() {
            frame = new JFrame("HotBloodWarrior Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 800);  // 프레임 크기를 키움

            mainPanel = new JPanel(new BorderLayout());

            // Player HP and Image Panel
            JPanel playerPanel = new JPanel(new BorderLayout());
            playerHpLabel = new JLabel("Your HP: " + hp);
            playerHpBar = new JProgressBar(0, 50);
            playerHpBar.setValue(hp);
            playerHpBar.setForeground(Color.GREEN);
            playerImageLabel = new JLabel(resizeImageIcon("C:/HotBloodWarrior/images/me.jpg", 400, 100));
            playerPanel.add(playerHpLabel, BorderLayout.NORTH);
            playerPanel.add(playerHpBar, BorderLayout.CENTER);
            playerPanel.add(playerImageLabel, BorderLayout.SOUTH);

            // Opponent HP and Image Panel
            JPanel opponentPanel = new JPanel(new BorderLayout());
            opponentHpLabel = new JLabel("Opponent's HP: " + opponentHp);
            opponentHpBar = new JProgressBar(0, 50);
            opponentHpBar.setValue(opponentHp);
            opponentHpBar.setForeground(Color.RED);
            opponentImageLabel = new JLabel(resizeImageIcon("C:/HotBloodWarrior/images/enemy.jpg", 400, 100));
            opponentPanel.add(opponentHpLabel, BorderLayout.NORTH);
            opponentPanel.add(opponentHpBar, BorderLayout.CENTER);
            opponentPanel.add(opponentImageLabel, BorderLayout.SOUTH);

            // Combine player and opponent panels
            JPanel battlePanel = new JPanel(new GridLayout(1, 2));
            battlePanel.add(playerPanel);
            battlePanel.add(opponentPanel);

            mainPanel.add(battlePanel, BorderLayout.NORTH);

            // Turn Indicator
            turnLabel = new JLabel("waiting");
            turnLabel.setHorizontalAlignment(SwingConstants.CENTER);
            turnLabel.setPreferredSize(new Dimension(800, 30)); // 박스 크기 조정
            mainPanel.add(turnLabel, BorderLayout.CENTER);

            // Game Map Panel
            JPanel mapPanel = new JPanel(new GridLayout(width, width));
            buttons = new JButton[width][width];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    final int x = i;
                    final int y = j;
                    buttons[i][j] = new JButton("");
                    buttons[i][j].setPreferredSize(new Dimension(60, 60));  // 버튼 크기 조정
                    buttons[i][j].addActionListener(e -> {
                        if (myTurn && !selected[x][y]) {
                            selected[x][y] = true;
                            out.println(x + "," + y);
                            myTurn = false;
                            turnLabel.setText("waiting");
                            buttons[x][y].setEnabled(false);
                        }
                    });
                    mapPanel.add(buttons[i][j]);
                }
            }

            mainPanel.add(new JScrollPane(mapPanel), BorderLayout.SOUTH);

            frame.add(mainPanel);
            frame.setVisible(true);
        }

        public void updateHp() {
            playerHpLabel.setText("Your HP: " + hp);
            opponentHpLabel.setText("Opponent's HP: " + opponentHp);
            playerHpBar.setValue(hp);
            opponentHpBar.setValue(opponentHp);
        }

        public void updateTurnIndicator() {
            if (myTurn) {
                turnLabel.setText("choose!");
            } else {
                turnLabel.setText("waiting");
            }
        }

        public void updateButton(int x, int y, int value) {
            buttons[x][y].setText(String.valueOf(value));
        }

        private ImageIcon resizeImageIcon(String path, int width, int height) {
            ImageIcon icon = new ImageIcon(path);
            Image image = icon.getImage();
            Image newImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImage);
        }
    }
}
