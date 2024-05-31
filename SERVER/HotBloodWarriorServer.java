import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;

public class HotBloodWarriorServer {
    public static int inPort = 9999;
    public static Vector<Client> clients = new Vector<Client>();
    public static int maxPlayer = 2;
    public static int numPlayer = 0;
    public static int width = 10;
    public static Map map;
    public static int[] hp = {50, 50};  // 각 플레이어의 초기 HP
    public static int currentPlayerTurn = 0; // 현재 턴인 플레이어
    boolean[] adrenalineState = new boolean[maxPlayer]; // 모든 플레이어에 대한 아드레날린 상태

    private ServerGUI gui;
    public static String filePath; // 파일 경로 변수 추가

    public static void main(String[] args) throws Exception {
        // 운영 체제 감지 및 filePath 설정
        String userHomeDir = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            filePath = "C:/HotBloodWarrior/";
        } else if (osName.contains("mac")) {
            filePath = userHomeDir + "/Desktop/HotBloodwarrior/CUSTOM/";
        } else {
            System.out.println("Unsupported operating system");
            return;
        }

        new HotBloodWarriorServer().createServer();
    }

    public void createServer() throws Exception {
        System.out.println("전사 출격 대기 중 ...");
        ServerSocket server = new ServerSocket(inPort);

        numPlayer = 0;
        while (numPlayer < maxPlayer) {
            Socket socket = server.accept();
            Client c = new Client(socket, numPlayer);
            clients.add(c);
            numPlayer++;
        }
        System.out.println("\n" + numPlayer + " players join");
        for (Client c : clients) {
            System.out.println("  - " + c.userName);
        }

        map = new Map(width);

        // 서버 GUI 초기화
        SwingUtilities.invokeLater(() -> {
            gui = new ServerGUI();
            gui.createAndShowGUI();
        });

        // 첫 번째 플레이어에게 턴 시작 메시지 전송
        clients.get(currentPlayerTurn).send("choose!");
    }

    public void sendToAll(String msg) {
        for (Client c : clients) {
            c.send(msg);
        }
    }

    class Client extends Thread {
        Socket socket;
        PrintWriter out = null;
        BufferedReader in = null;
        int playerId;
        String userName = null;
        int x, y;
        public boolean turn = false;
        int opponentId;

        public Client(Socket socket, int playerId) throws Exception {
            this.playerId = playerId;
            this.opponentId = (playerId + 1) % maxPlayer;
            initial(socket);
            start();
        }

        public void initial(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            userName = in.readLine();
            System.out.println(userName + " joins from " + socket.getInetAddress());
            send(String.valueOf(playerId));
            send("waiting");
        }

        @Override
        public void run() {
            String msg;

            try {
                while (true) {
                    msg = in.readLine();
                    
                    // 채팅 메시지 처리
                    if (msg.startsWith("chat:")) {
                        String chatMsg = msg.substring(5);
                        sendToAll("chat:" + userName + ": " + chatMsg);
                        continue;
                    }

                    if (msg.equals("아드레날린") && this.playerId == currentPlayerTurn) {
                        adrenalineState[currentPlayerTurn] = true; // 아드레날린 상태 활성
                        sendToAll("skill:" + userName + ",Adrenaline");
                    } else if (msg != null && this.playerId == currentPlayerTurn) {
                        String[] arr = msg.split(",");
                        x = Integer.parseInt(arr[0]);
                        y = Integer.parseInt(arr[1]);

                        int value = map.checkCell(x, y);

                        if (adrenalineState[currentPlayerTurn]) {
                            value *= 2; // 값을 2배로 조정
                            adrenalineState[currentPlayerTurn] = false; // 아드레날린 상태 해제
                        }

                        if (value < 0) {
                            hp[currentPlayerTurn] += value; // 자신의 HP 감소
                        } else {
                            hp[(currentPlayerTurn + 1) % maxPlayer] -= value; // 상대방의 HP 감소
                        }

                        send("value," + x + "," + y + "," + value);
                        sendToAll("HP " + hp[0] + " " + hp[1]);
                        clients.get(opponentId).send("opponentValue," + x + "," + y + "," + value + (adrenalineState[currentPlayerTurn] ? ",skill" : ""));
                        
                        // GUI 업데이트
                        gui.updateHp();
                        playSoundForValue(value); // 효과음 재생
                        nextPlayerTurn();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(String msg) {
            out.println(msg);
        }

        private void playSoundForValue(int value) {
            String soundFilePath = getSoundPathForValue(value);
            playSound(soundFilePath);
        }

        private String getSoundPathForValue(int value) {
            switch (value) {
                case -10:
                    return filePath + "sound/damage_teemo.wav";
                case -5:
                    return filePath + "sound/damage_teemo.wav";
                case 10:
                    return filePath + "sound/damage_10.wav";
                case 20:
                    return filePath + "sound/damage_20.wav";
                case 40:
                    return filePath + "sound/damage_40.wav";
                default:
                    return filePath + "sound/damage_normal.wav";
            }
        }

        private void playSound(String filePath) {
            try {
                File soundFile = new File(filePath);
                if (soundFile.exists()) {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInputStream);
                    clip.start();
                } else {
                    System.out.println("Sound file not found: " + filePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void nextPlayerTurn() {
            currentPlayerTurn = (currentPlayerTurn + 1) % maxPlayer; // 다음 플레이어로 순환
            HotBloodWarriorServer.this.clients.get(currentPlayerTurn).send("choose!");
        }
    }

    // Server GUI class
    class ServerGUI {
        JFrame frame;
        JPanel mainPanel;
        JLabel[] playerHpLabels;
        JButton[][] buttons;

        public void createAndShowGUI() {
            frame = new JFrame("HotBloodWarrior Server");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 800);  // 프레임 크기를 키움

            mainPanel = new JPanel(new BorderLayout());

            // Player HP Panel
            JPanel hpPanel = new JPanel();
            playerHpLabels = new JLabel[maxPlayer];
            for (int i = 0; i < maxPlayer; i++) {
                playerHpLabels[i] = new JLabel("Player " + (i + 1) + " HP: " + hp[i]);
                hpPanel.add(playerHpLabels[i]);
            }

            mainPanel.add(hpPanel, BorderLayout.NORTH);

            // Game Map Panel
            JPanel mapPanel = new JPanel(new GridLayout(width, width));
            buttons = new JButton[width][width];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    buttons[i][j] = new JButton("");
                    buttons[i][j].setPreferredSize(new Dimension(60, 60));  // 버튼 크기 조정
                    buttons[i][j].setEnabled(false); // 서버에서는 버튼을 클릭할 수 없음
                    mapPanel.add(buttons[i][j]);
                }
            }

            mainPanel.add(mapPanel, BorderLayout.CENTER);
            frame.add(mainPanel);
            frame.setVisible(true);

            // 맵 초기화
            updateMap();
        }

        public void updateMap() {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    int value = map.getCellValue(i, j);
                    buttons[i][j].setText(value == Integer.MIN_VALUE ? "" : String.valueOf(value));
                }
            }
        }

        public void updateHp() {
            for (int i = 0; i < maxPlayer; i++) {
                playerHpLabels[i].setText("Player " + (i + 1) + " HP: " + hp[i]);
            }
        }
    }
}
