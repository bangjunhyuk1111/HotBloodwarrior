import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;

class HotBloodWarriorClient {
    static int inPort = 9999;
    static String address = "172.20.10.2"; // 서버 주소
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
    static int turn = 1;
    static String filePath; // 파일 경로 변수 추가
    static Socket socket; // 소켓을 static으로 변경하여 다른 메서드에서도 접근 가능하도록 함

    public static void main(String[] args) {
        connectToServer();

        // 종료 시 서버에 알리고 소켓 닫기
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (out != null) {
                    out.println("종료");
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public static void connectToServer() {
        try {
            socket = new Socket(address, inPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            gui = new ClientGUI(socket);
            SwingUtilities.invokeLater(() -> gui.createAndShowGUI());

            out.println(userName);
            playerId = Integer.parseInt(in.readLine());

            // BGM 재생
            String userHomeDir = System.getProperty("user.home");
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                filePath = "C:/HotBloodWarrior/";
                playBGM(filePath + "sound/bgm.wav");
            } else if (osName.contains("mac")) {
                System.out.println("성공");
                filePath = userHomeDir + "/Desktop/HotBloodwarrior/CUSTOM/";
                playBGM(filePath + "sound/bgm.wav");
            } else {
                System.out.println("Unsupported operating system");
                return;
            }

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
                if (hp <= 0) {
                    int option = JOptionPane.showConfirmDialog(null, "상대에게 패배했다...  수련을 통해 더욱 정진하자... ", "게임 종료", JOptionPane.DEFAULT_OPTION);
                    if (option == JOptionPane.OK_OPTION) {
                        out.println("종료");
                        System.exit(0);
                    }
                }

                if (opponentHp <= 0) {
                    int option = JOptionPane.showConfirmDialog(null, "무자비하게 상대를 박살냈다!!!  이 세상에 나를 막을 자는 없다!!! ", "게임 종료", JOptionPane.DEFAULT_OPTION);
                    if (option == JOptionPane.OK_OPTION) {
                        out.println("종료");
                        System.exit(0);
                    }
                }

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
                gui.updateSelectedInfo(x, y, value); // 선택된 칸 정보 업데이트
            });
        } else if (message.startsWith("opponentValue,")) {
            String[] arr = message.split(",");
            int value = Integer.parseInt(arr[3]);
            SwingUtilities.invokeLater(() -> {
                gui.updateOpponentInfo(value); // 상대가 선택한 칸 정보 업데이트
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
        JLabel selectedInfoLabel; // 선택된 칸 정보 라벨
        JLabel opponentInfoLabel; // 상대가 선택한 칸 정보 라벨
        JButton[][] buttons;
        Socket socket;

        public ClientGUI(Socket socket) {
            this.socket = socket;
        }

        public void createAndShowGUI() {
            frame = new JFrame("HotBloodWarrior Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 780);  // 프레임 크기를 적절히 조정

            mainPanel = new JPanel(new BorderLayout());

            // 상단에 HP 바 패널
            JPanel hpPanel = new JPanel(new GridLayout(2, 2));
            JLabel playerHpTextLabel = new JLabel("Your HP", SwingConstants.CENTER);
            JLabel opponentHpTextLabel = new JLabel("Opponent's HP", SwingConstants.CENTER);

            // 플레이어 HP 바와 라벨을 포함하는 패널
            JPanel playerHpPanel = new JPanel(new BorderLayout());
            playerHpLabel = new JLabel(String.valueOf(hp), SwingConstants.CENTER);
            playerHpBar = new JProgressBar(0, 50);
            playerHpBar.setValue(hp);
            playerHpBar.setForeground(Color.GREEN);
            playerHpPanel.add(playerHpLabel, BorderLayout.NORTH);
            playerHpPanel.add(playerHpBar, BorderLayout.CENTER);

            // 상대방 HP 바와 라벨을 포함하는 패널
            JPanel opponentHpPanel = new JPanel(new BorderLayout());
            opponentHpLabel = new JLabel(String.valueOf(opponentHp), SwingConstants.CENTER);
            opponentHpBar = new JProgressBar(0, 50);
            opponentHpBar.setValue(opponentHp);
            opponentHpBar.setForeground(Color.RED);
            opponentHpPanel.add(opponentHpLabel, BorderLayout.NORTH);
            opponentHpPanel.add(opponentHpBar, BorderLayout.CENTER);

            hpPanel.add(playerHpTextLabel);
            hpPanel.add(opponentHpTextLabel);
            hpPanel.add(playerHpPanel);
            hpPanel.add(opponentHpPanel);

            mainPanel.add(hpPanel, BorderLayout.NORTH);

            // 중앙에 상태 정보 패널
            JPanel statusPanel = new JPanel(new GridLayout(1, 2, 10, 10));
            JPanel playerStatusPanel = new JPanel(new BorderLayout());
            JPanel opponentStatusPanel = new JPanel(new BorderLayout());

            // Player Info
            playerImageLabel = new JLabel(resizeImageIcon(filePath + "images/me.jpg", 450, 150));
            selectedInfoLabel = new JLabel("", SwingConstants.CENTER);
            playerStatusPanel.add(playerImageLabel, BorderLayout.NORTH);
            playerStatusPanel.add(selectedInfoLabel, BorderLayout.SOUTH);

            // Opponent Info
            opponentImageLabel = new JLabel(resizeImageIcon(filePath + "images/enemy.jpg", 450, 150));
            opponentInfoLabel = new JLabel("", SwingConstants.CENTER);
            opponentStatusPanel.add(opponentImageLabel, BorderLayout.NORTH);
            opponentStatusPanel.add(opponentInfoLabel, BorderLayout.SOUTH);

            statusPanel.add(playerStatusPanel);
            statusPanel.add(opponentStatusPanel);

            mainPanel.add(statusPanel, BorderLayout.CENTER);

            // 게임 맵 패널
            JPanel mapPanel = new JPanel(new GridLayout(width, width));
            buttons = new JButton[width][width];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    final int x = i;
                    final int y = j;
                    buttons[i][j] = new JButton("");
                    buttons[i][j].setPreferredSize(new Dimension(40, 40));  // 버튼 크기 조정
                    buttons[i][j].addActionListener(e -> {
                        if (myTurn && !selected[x][y]) {
                            selected[x][y] = true;
                            out.println(x + "," + y);
                            myTurn = false;
                            turnLabel.setText("상대가 나를 공격하기 위한 무기를 찾고 있다... 제발 함정에 걸려라...");
                            turn++;
                        }
                    });
                    mapPanel.add(buttons[i][j]);
                }
            }

            // 시스템 메시지와 턴 인디케이터 패널
            JPanel messagePanel = new JPanel();
            turnLabel = new JLabel("waiting", SwingConstants.CENTER);
            turnLabel.setPreferredSize(new Dimension(800, 30)); // 박스 크기 조정
            messagePanel.add(turnLabel);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(messagePanel, BorderLayout.NORTH);
            bottomPanel.add(new JScrollPane(mapPanel), BorderLayout.CENTER);

            mainPanel.add(bottomPanel, BorderLayout.SOUTH);

            frame.add(mainPanel);
            frame.setVisible(true);
        }

        public void updateHp() {
            playerHpLabel.setText(String.valueOf(hp));
            opponentHpLabel.setText(String.valueOf(opponentHp));
            playerHpBar.setValue(hp);
            opponentHpBar.setValue(opponentHp);
        }

        public void updateTurnIndicator() {
            Random randx = new Random();
            int x = randx.nextInt(10);

            if (myTurn) {
                turnLabel.setText("상대를 베어내기 위한 " + turn + "번째 검을 골라야한다. 과연 어떤 검이 나올까..? (소문으로는 함정도 있다는데...)");
            } else {
                switch (x) {
                    case 0:
                        turnLabel.setText("알고 계셨나요? 학생복지관 (하나은행 건물) 2층의 안경점에서는 아이스크림을 판매하고 있습니다.");
                        break;
                    case 1:
                        turnLabel.setText("알고 계셨나요? 기숙사 명현관 5층은 1인실입니다.");
                        break;
                    case 2:
                        turnLabel.setText("알고 계셨나요? 명지대학교는 자연캠퍼스가 본교입니다.");
                        break;
                    case 3:
                        turnLabel.setText("알고 계셨나요? 도서관 열람실은 자리 예약이 필수입니다 ^^ 연장을 놓치지 마세요!");
                        break;
                    case 4:
                        turnLabel.setText("알고 계셨나요? 밤이 되면 ECC 옆에서 개구리가 울기 시작합니다...");
                        break;
                    case 5:
                        turnLabel.setText("알고 계셨나요? 새벽에 기숙사에서 들리는 비명 소리는 대부분 귀여운 고라니의 짝을 찾기 위한 울음소리입니다.");
                        break;
                    case 6:
                        turnLabel.setText("알고 계셨나요? 2023년까지 기숙사 3동 지하 1층 문의 개방 시간은 09:00 ~ 22:00 이었습니다.");
                        break;
                    case 7:
                        turnLabel.setText("알고 계셨나요? 이 게임의 개발자 중 한 명인 윤동주는 개발 기간 중 바디프로필을 준비하고 있었습니다.");
                        break;
                    case 8:
                        turnLabel.setText("알고 계셨나요? 딸기 바나나맛 밀키스 제로는 굉장히 맛있습니다...");
                        break;
                    case 9:
                        turnLabel.setText("알고 계셨나요? 대부분 역북의 밥집은 즐삼 선에서 컷입니다.");
                        break;
                }

            }

        }

        public void updateButton(int x, int y, int value) {
            buttons[x][y].setText(""); // 텍스트 제거
            buttons[x][y].setIcon(resizeImageIcon(getImagePathForValue(value), 40, 40)); // 이미지 설정
            playSoundForValue(value); // 효과음 재생
        }

        public void updateSelectedInfo(int x, int y, int value) {
            String imagePath = getImagePathForValue(value);
            selectedInfoLabel.setIcon(resizeImageIcon(imagePath, 40, 40));
            selectedInfoLabel.setText("위치: (" + x + ", " + y + "), 데미지: " + value);
            playSoundForValue(value); // 효과음 재생
        }

        public void updateOpponentInfo(int value) {
            String imagePath = getImagePathForValue(value);
            opponentInfoLabel.setIcon(resizeImageIcon(imagePath, 40, 40));
            opponentInfoLabel.setText("상대가 선택한 데미지: " + value);
            playSoundForValue(value); // 효과음 재생
        }

        private String getImagePathForValue(int value) {
            return filePath + "images/damage" + value + ".jpg";
        }

        private ImageIcon resizeImageIcon(String path, int width, int height) {
            ImageIcon icon = new ImageIcon(path);
            Image image = icon.getImage();
            Image newImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImage);
        }
        
        private void playSoundForValue(int value) {
            String soundFilePath = getSoundPathForValue(value);
            playSound(soundFilePath);
        }
        
        private String getSoundPathForValue(int value) {
            switch (value) {
                case -5:
                    return filePath + "sound/damage_teemo.wav";
                case 10:
                    return filePath + "sound/damage_10.wav";
                case 20:
                    return filePath + "sound/damage_20.wav";
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
    }

    // BGM 재생 메서드 추가
    public static void playBGM(String filePath) {
        try {
            File bgmFile = new File(filePath);
            if (bgmFile.exists()) {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bgmFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
                clip.loop(Clip.LOOP_CONTINUOUSLY); // BGM 반복 재생
            } else {
                System.out.println("BGM 파일을 찾을 수 없습니다: " + filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}