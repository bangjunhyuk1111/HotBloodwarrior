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
        int value = map[x][y];
        map[x][y] = Integer.MIN_VALUE; // 이 칸은 이미 선택되었음을 표시
        return value;
    }

    public int getCellValue(int x, int y) {
        return map[x][y];
    }

    public void updateMap(int x, int y) {
        // 필요에 따라 맵을 업데이트합니다. 
        // 예를 들어, 이미 선택된 칸을 다른 색상으로 표시할 수 있습니다.
    }
}
