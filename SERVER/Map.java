import java.util.*;

public class Map {
    int width;    
    int[][] map;

    public Map(int width) {
        this.width = width;
        this.map = new int[width][width];
        initializeMap();
    }

    private void initializeMap() {
        Random rand = new Random();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                map[i][j] = rand.nextInt(11) - 5; // -5 ~ 5 사이의 랜덤한 숫자
            }
        }
    }

    public int checkCell(int x, int y) {
        return map[x][y];
    }

    public int getCellValue(int x, int y) {
        return map[x][y];
    }

    public void updateMap(int x, int y) {
        // 여기에 필요하다면 특정한 업데이트 로직을 추가하세요
    }
}
