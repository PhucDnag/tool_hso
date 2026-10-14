package GameObjects;

/** Complete world atlas and teleport stone category database
 * learned from KnightAge280 v3.0 (toolmau) and Knight Online client world map.
 * Java ME CLDC-1.1 / Java 1.4 compatible.
 */
public final class GoBackAtlas {
    public static final String[] MAP_NAMES = new String[]{
        "Ng\u00f4i L\u00e0ng Nh\u1ecf",
        "L\u00e0ng S\u00f3i Tr\u1eafng",
        "Khu m\u1ecf",
        "B\u00eca R\u1eebng",
        "Hang L\u1eeda",
        "R\u1eebng \u1ea2o Gi\u00e1c",
        "Khe V\u1ef1c",
        "C\u00e1nh \u0110\u1ed3ng S\u00f3i",
        "Thung L\u0169ng K\u1ef3 B\u00ed",
        "H\u1ed3 K\u00fd \u1ee8c",
        "B\u00e3i \u0110\u1ea5t Tr\u1ed1ng",
        "B\u1edd Bi\u1ec3n",
        "V\u1ef1c \u0110\u00e1",
        "R\u1eb7ng \u0110\u00e1 Ng\u1ea7m",
        "Ngh\u0129a \u0110\u1ecba T\u00e0u \u0110\u1eafm",
        "\u0110\u1ea7m L\u1ea7y",
        "\u0110\u1ec1n C\u1ed5",
        "Hang D\u01a1i",
        "Hang S\u00f3i Qu\u1ef7",
        "C\u1eeda Bi\u1ec3n",
        "Sa M\u1ea1c",
        "\u0110\u1ed3i C\u00e1t",
        "V\u1ef1c L\u00fan",
        "H\u1ed1 T\u1eed Th\u1ea7n",
        "Ngh\u0129a \u0111\u1ecba c\u00e1t",
        "R\u1eebng Ch\u1ebft",
        "Su\u1ed1i Ma",
        "Thung L\u0169ng \u0110\u00e1",
        "Boss Guardian",
        "H\u1ea7m M\u1ed9 T\u1ea7ng 1",
        "H\u1ea7m M\u1ed9 T\u1ea7ng 2",
        "H\u1ea7m M\u1ed9 T\u1ea7ng 3",
        "H\u1ea7m m\u1ed9 qu\u00e1i v\u1eadt",
        "Th\u00e0nh Ph\u1ed1 Kho B\u00e1u",
        "Khu ph\u00eda T\u00e2y",
        "Khu ph\u00eda \u0110\u00f4ng",
        "\u0110\u1ea5u Tr\u01b0\u1eddng",
        "R\u1eebng cao nguy\u00ean",
        "Con \u0111\u01b0\u1eddng hi\u1ec3m tr\u1edf",
        "V\u00e1ch \u0111\u00e1 cheo leo",
        "N\u00fai C\u1ea7u V\u00f2ng",
        "L\u1ed1i l\u00ean Th\u01b0\u1ee3ng gi\u1edbi",
        "\u0110\u1ea3o hoang s\u01a1",
        "\u0110\u01b0\u1eddng xu\u1ed1ng l\u00f2ng \u0111\u1ea5t",
        "C\u00e2y c\u1ea7u ma \u00e1m",
        "C\u1ed5ng v\u00e0o H\u1ea1 gi\u1edbi",
        "\u0110\u1ea5u Tr\u01b0\u1eddng",
        "\u0110\u1ed3i x\u00e1c ch\u1ebft",
        "Ng\u00e3 t\u01b0 t\u1eed th\u1ea7n",
        "Ph\u00f3 b\u1ea3n m\u1edbi",
        "Khu v\u01b0\u1eddn",
        "C\u1ed5ng thi\u00ean \u0111\u00e0ng",
        "C\u1ed5ng \u0111\u1ecba ng\u1ee5c",
        "Chu\u1ea9n b\u1ecb",
        "L\u00e0ng \u00e1nh s\u00e1ng",
        "Chu\u1ea9n b\u1ecb",
        "L\u00e0ng gi\u00f3",
        "Chu\u1ea9n b\u1ecb",
        "L\u00e0ng s\u00e9t",
        "Chu\u1ea9n b\u1ecb",
        "L\u00e0ng l\u1eeda",
        "Chi\u1ebfn tr\u01b0\u1eddng",
        "\u0110\u1ecba ng\u1ee5c t\u1ea7ng 1",
        "R\u1eebng medusa",
        "R\u1eebng Chimera",
        "R\u1eebng qu\u00e1i v\u1eadt",
        "Th\u00e1c reo",
        "Th\u00e0nh ph\u1ed1 c\u1ea3ng",
        "Khu b\u1edd nam",
        "Khu b\u1edd b\u1eafc",
        "Khu b\u1edd t\u00e2y",
        "R\u1eebng chu\u1ed9t",
        "R\u1eebng hoa \u0111\u1ecf",
        "V\u1ecbnh Caribe",
        "M\u00ea cung",
        "M\u00ea cung t\u1ea7ng 1",
        "M\u00ea cung t\u1ea7ng 2",
        "M\u00ea cung t\u1ea7ng 3",
        "M\u00ea cung t\u1ea7ng 4",
        "M\u00ea cung t\u1ea7ng cu\u1ed1i",
        "Thi \u0111\u1ea5u",
        "Map 81",
        "Khu mua b\u00e1n",
        "C\u1eeda \u0111\u00f4ng",
        "C\u1eeda t\u00e2y",
        "C\u1eeda nam",
        "C\u1eeda b\u1eafc",
        "\u0110\u1ea5u tr\u01b0\u1eddng",
        "Map 88",
        "Map 89",
        "Map 90",
        "Map 91",
        "C\u1ed5ng tr\u1eafng",
        "Th\u1ecb tr\u1ea5n m\u00f9a \u0111\u00f4ng",
        "Thung l\u0169ng b\u0103ng gi\u00e1",
        "Ch\u00e2n n\u00fai tuy\u1ebft",
        "\u0110\u1ea3o b\u0103ng gi\u00e1",
        "V\u1ef1c th\u1eb3m s\u01b0\u01a1ng m\u00f9",
        "Tr\u1ea1m n\u00fai tuy\u1ebft"
    };

    // Category 1: Map Lv 1 - 39
    public static final short[] CAT_1_MAPS = new short[]{
        0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 50
    };

    // Category 2: Map Lv 40 - 69
    public static final short[] CAT_2_MAPS = new short[]{
        33, 34, 35, 36, 25, 26, 27, 28, 29, 30, 31, 32, 20, 21, 22, 23, 24, 19, 42, 47, 48
    };

    // Category 3: Map Lv 70 - 89
    public static final short[] CAT_3_MAPS = new short[]{
        67, 68, 69, 70, 37, 38, 39, 40, 41, 43, 44, 45, 2
    };

    // Category 4: Map Lv 90 - 109
    public static final short[] CAT_4_MAPS = new short[]{
        63, 64, 65, 66, 71, 72, 73, 51, 52, 62
    };

    // Category 5: Map Lv 110+
    public static final short[] CAT_5_MAPS = new short[]{
        74, 75, 76, 77, 78, 79, 92, 93, 94, 95, 96, 97, 98
    };

    // Category 6: Chiến Trường & Khác
    public static final short[] CAT_6_MAPS = new short[]{
        54, 56, 58, 60, 61, 82, 88, 89, 90, 91, 102
    };

    // Complete in-game walking connections (length 103)
    public static final short[][] MAP_LINKS = new short[][]{
        /*  0 */ new short[]{1},
        /*  1 */ new short[]{2, 3, 0, 7, 50},
        /*  2 */ new short[]{1},
        /*  3 */ new short[]{4, 1},
        /*  4 */ new short[]{3, 5},
        /*  5 */ new short[]{4, 6},
        /*  6 */ new short[]{5},
        /*  7 */ new short[]{1, 11, 15, 18, 8},
        /*  8 */ new short[]{9, 7},
        /*  9 */ new short[]{8, 10},
        /* 10 */ new short[]{9},
        /* 11 */ new short[]{7, 12},
        /* 12 */ new short[]{11, 13},
        /* 13 */ new short[]{12, 14},
        /* 14 */ new short[]{13},
        /* 15 */ new short[]{16, 7},
        /* 16 */ new short[]{15, 17},
        /* 17 */ new short[]{16},
        /* 18 */ new short[]{7, 25},
        /* 19 */ new short[]{24, 47},
        /* 20 */ new short[]{34, 23},
        /* 21 */ new short[]{24, 37},
        /* 22 */ new short[]{24, 42},
        /* 23 */ new short[]{20, 24},
        /* 24 */ new short[]{22, 21, 23, 19},
        /* 25 */ new short[]{18, 26, 33},
        /* 26 */ new short[]{25, 27},
        /* 27 */ new short[]{28, 26},
        /* 28 */ new short[]{27},
        /* 29 */ new short[]{35, 30},
        /* 30 */ new short[]{31, 29},
        /* 31 */ new short[]{30, 32},
        /* 32 */ new short[]{31},
        /* 33 */ new short[]{25, 34, 35, 36},
        /* 34 */ new short[]{33, 20},
        /* 35 */ new short[]{33, 29},
        /* 36 */ new short[]{33},
        /* 37 */ new short[]{21, 38},
        /* 38 */ new short[]{37, 39},
        /* 39 */ new short[]{38, 40},
        /* 40 */ new short[]{39, 41},
        /* 41 */ new short[]{40, 51},
        /* 42 */ new short[]{22, 43},
        /* 43 */ new short[]{42, 44},
        /* 44 */ new short[]{43, 45},
        /* 45 */ new short[]{44, 52},
        /* 46 */ null,
        /* 47 */ new short[]{19},
        /* 48 */ null,
        /* 49 */ null,
        /* 50 */ new short[]{1},
        /* 51 */ new short[]{41},
        /* 52 */ new short[]{45, 62},
        /* 53 */ new short[]{54},
        /* 54 */ new short[]{53},
        /* 55 */ new short[]{56},
        /* 56 */ new short[]{55},
        /* 57 */ new short[]{58},
        /* 58 */ new short[]{57},
        /* 59 */ new short[]{60},
        /* 60 */ new short[]{59},
        /* 61 */ null,
        /* 62 */ new short[]{52},
        /* 63 */ new short[]{64, 70, 65, 72},
        /* 64 */ new short[]{63, 66, 70, 71},
        /* 65 */ new short[]{68, 63},
        /* 66 */ new short[]{64, 69},
        /* 67 */ new short[]{69, 68, 70},
        /* 68 */ new short[]{67, 65},
        /* 69 */ new short[]{66, 67},
        /* 70 */ new short[]{67, 64, 63},
        /* 71 */ new short[]{64, 73},
        /* 72 */ new short[]{63, 73},
        /* 73 */ new short[]{72, 71, 74},
        /* 74 */ new short[]{73, 75},
        /* 75 */ new short[]{74, 76},
        /* 76 */ new short[]{75, 77},
        /* 77 */ new short[]{76, 78},
        /* 78 */ new short[]{77, 79},
        /* 79 */ new short[]{78, 92},
        /* 80 */ null,
        /* 81 */ null,
        /* 82 */ null,
        /* 83 */ null,
        /* 84 */ null,
        /* 85 */ null,
        /* 86 */ null,
        /* 87 */ null,
        /* 88 */ null,
        /* 89 */ null,
        /* 90 */ null,
        /* 91 */ null,
        /* 92 */ new short[]{79, 93},
        /* 93 */ new short[]{92, 94},
        /* 94 */ new short[]{93, 95},
        /* 95 */ new short[]{94, 96},
        /* 96 */ new short[]{97, 95},
        /* 97 */ new short[]{96, 98},
        /* 98 */ new short[]{97}
    };

    public static int getCategoryForMap(int mapId) {
        if (mapId < 0) return 0;
        for (int i = 0; i < CAT_1_MAPS.length; i++) if (CAT_1_MAPS[i] == mapId) return 1;
        for (int i = 0; i < CAT_2_MAPS.length; i++) if (CAT_2_MAPS[i] == mapId) return 2;
        for (int i = 0; i < CAT_3_MAPS.length; i++) if (CAT_3_MAPS[i] == mapId) return 3;
        for (int i = 0; i < CAT_4_MAPS.length; i++) if (CAT_4_MAPS[i] == mapId) return 4;
        for (int i = 0; i < CAT_5_MAPS.length; i++) if (CAT_5_MAPS[i] == mapId) return 5;
        for (int i = 0; i < CAT_6_MAPS.length; i++) if (CAT_6_MAPS[i] == mapId) return 6;
        return 0;
    }

    public static boolean isTeleportable(int mapId) {
        return getCategoryForMap(mapId) > 0;
    }

    public static boolean matchesCategory(String label, int cat) {
        if (label == null || cat <= 0) return false;
        String l = MenuUP.stripAccents(label).toLowerCase();
        if (cat == 6) {
            return l.indexOf("chien truong") >= 0 || l.indexOf("khac") >= 0;
        }
        if (cat > 5 || l.indexOf("map") < 0 && l.indexOf("lv") < 0 && l.indexOf("level") < 0) return false;
        int first=-1;
        for(int i=0;i<l.length();i++) {
            char c=l.charAt(i);
            if(c>='0' && c<='9') {
                if(first<0) first=0;
                first=first*10+c-'0';
            } else if(first>=0) break;
        }
        int[] starts={1,40,70,90,110};
        return first==starts[cat-1];
    }

    public static int mapIdForName(String name) {
        int found=-1;
        for(int i=0;i<MAP_NAMES.length;i++) if(MenuUP.stripAccents(MAP_NAMES[i]).equals(name)) {
            if(found>=0) return -1;
            found=i;
        }
        return found;
    }

    /** Candidate destinations only: the planner still requires a complete path
     * from the landing map to the saved target, using observed edges first. */
    public static void addStoneRoutes(GoBackRoutes graph,int from,int x,int y,int npc) {
        for(int id=0;id<MAP_NAMES.length;id++) {
            if(!isTeleportable(id)) continue;
            int to=graph.byName(MenuUP.stripAccents(MAP_NAMES[id]));
            if(to<0) to=graph.byId(id);
            if(to>=0 && to!=from) graph.exit(from,to,x,y,npc);
        }
    }

    public static int getNearestTeleportMap(int targetMapId) {
        if (isTeleportable(targetMapId)) {
            return targetMapId;
        }
        if (targetMapId < 0 || targetMapId >= MAP_LINKS.length) {
            return 1;
        }
        int[] queue = new int[MAP_LINKS.length];
        boolean[] visited = new boolean[MAP_LINKS.length];
        int head = 0, tail = 0;
        queue[tail++] = targetMapId;
        visited[targetMapId] = true;
        while (head < tail) {
            int u = queue[head++];
            if (isTeleportable(u)) {
                return u;
            }
            short[] neighbors = MAP_LINKS[u];
            if (neighbors != null) {
                for (int i = 0; i < neighbors.length; i++) {
                    int v = neighbors[i];
                    if (v >= 0 && v < MAP_LINKS.length && !visited[v]) {
                        visited[v] = true;
                        queue[tail++] = v;
                    }
                }
            }
        }
        return 1;
    }

    public static int getAlternateTeleportMap(int targetMapId, int failedMapId) {
        if (targetMapId < 0 || targetMapId >= MAP_LINKS.length) return 1;
        short[] neighbors = MAP_LINKS[targetMapId];
        if (neighbors != null) {
            for (int i = 0; i < neighbors.length; i++) {
                int n = neighbors[i];
                if (n != failedMapId && isTeleportable(n)) {
                    return n;
                }
            }
            for (int i = 0; i < neighbors.length; i++) {
                short[] n2 = neighbors[i] >= 0 && neighbors[i] < MAP_LINKS.length ? MAP_LINKS[neighbors[i]] : null;
                if (n2 != null) {
                    for (int j = 0; j < n2.length; j++) {
                        int m = n2[j];
                        if (m != failedMapId && m != targetMapId && isTeleportable(m)) {
                            return m;
                        }
                    }
                }
            }
        }
        int nearest = getNearestTeleportMap(targetMapId);
        if (nearest != failedMapId && nearest != targetMapId) return nearest;
        return 1;
    }

    public static String getMapName(int mapId) {
        if (mapId >= 0 && mapId < MAP_NAMES.length) {
            return MAP_NAMES[mapId];
        }
        return "";
    }

    /** Add teleport stone destination exit for the specific target map */
    public static void addStoneExitForTarget(GoBackRoutes graph, int currentNode, int targetMapId, int stoneX, int stoneY, int npcId) {
        if (graph == null || currentNode < 0 || targetMapId < 0) return;
        int destId = isTeleportable(targetMapId) ? targetMapId : getNearestTeleportMap(targetMapId);
        if (destId >= 0) {
            int toNode = graph.byId(destId);
            if (toNode < 0) {
                toNode = graph.node(MenuUP.stripAccents(getMapName(destId)), destId);
            }
            if (toNode >= 0 && toNode != currentNode) {
                graph.exit(currentNode, toNode, stoneX, stoneY, npcId);
            }
        }
    }

    public static void seed(GoBackRoutes graph) {
        int[] nodes = new int[MAP_NAMES.length];
        for (int i = 0; i < MAP_NAMES.length; i++) {
            String key = MenuUP.stripAccents(MAP_NAMES[i]);
            int id = i;
            if (id == 53 || id == 55 || id == 57 || id == 59 || id == 36 || id == 46 || id == 87) {
                nodes[i] = graph.distinctNode(key, id);
            } else {
                nodes[i] = graph.node(key, -1);
            }
        }
        for (int from = 0; from < MAP_LINKS.length; from++) {
            short[] dests = MAP_LINKS[from];
            if (dests == null || from >= nodes.length) continue;
            int a = nodes[from];
            if (a < 0) continue;
            for (int j = 0; j < dests.length; j++) {
                int to = dests[j];
                if (to >= 0 && to < nodes.length) {
                    int b = nodes[to];
                    if (b >= 0 && a != b) {
                        graph.atlasLink(a, b);
                    }
                }
            }
        }
    }
}
