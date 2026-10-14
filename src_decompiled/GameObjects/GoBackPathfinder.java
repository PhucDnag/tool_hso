package GameObjects;

/** Four-way shortest paths using the same collision tiles as the game. */
public final class GoBackPathfinder {
    private GoBackPathfinder() { }

    public static short[] find(int[] tiles, int width, int height,
            int sx, int sy, int tx, int ty) {
        if (width > 128 || height > 128) return null;
        int[] cells = findCells(tiles, width, height, sx, sy, tx, ty, 0);
        if (cells == null) return null;
        short[] road = new short[cells.length];
        for (int i=0; i<cells.length; i++) road[i]=(short)(((cells[i]%width)<<8)|(cells[i]/width));
        return road;
    }

    // Radius is in tiles, for reaching a walkable position beside a blocked NPC.
    public static int[] findCells(int[] tiles, int width, int height,
            int sx, int sy, int tx, int ty, int radius) {
        if (tiles == null || width <= 0 || height <= 0 || width > 512 || height > 512
                || tiles.length < width * height || sx < 0 || sy < 0
                || tx < 0 || ty < 0 || sx >= width || tx >= width
                || sy >= height || ty >= height) return null;
        int start = sy * width + sx;
        int target = ty * width + tx;
        if (tiles[start] == 1 || tiles[start] == -1 || radius < 0) return null;
        if (radius == 0 && (tiles[target] == 1 || tiles[target] == -1)) return null;
        int[] parent = new int[width * height];
        int[] queue = new int[parent.length];
        for (int i = 0; i < parent.length; i++) parent[i] = -1;
        parent[start] = start;
        int head = 0, tail = 0;
        queue[tail++] = start;
        boolean found = false;
        while (head < tail) {
            int current = queue[head++];
            if (Math.abs(current % width - tx) + Math.abs(current / width - ty) <= radius
                    && tiles[current] != 1 && tiles[current] != -1) {
                target = current; found = true; break;
            }
            for (int direction = 0; direction < 4; direction++) {
                int x = current % width, y = current / width;
                if (direction == 0) x++;
                else if (direction == 1) x--;
                else if (direction == 2) y++;
                else y--;
                if (x < 0 || y < 0 || x >= width || y >= height) continue;
                int next = y * width + x;
                if (parent[next] != -1 || tiles[next] == 1 || tiles[next] == -1) continue;
                parent[next] = current;
                queue[tail++] = next;
            }
        }
        if (!found) return null;
        int length = 1;
        for (int p = target; p != start; p = parent[p]) length++;
        // Include the start tile: align with its centre before taking a turn.
        int[] road = new int[length];
        int p = target;
        for (int i = length - 1; i >= 0; i--) {
            road[i] = p;
            p = parent[p];
        }
        return road;
    }
}
