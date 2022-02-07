package minerregulation.utils;

import battlecode.common.*;

public class Cache {
    public static class Permanent {
        public static int ROUND_SPAWNED;
        public static Team OUR_TEAM;
        public static Team OPPONENT_TEAM;
        public static RobotType ROBOT_TYPE;
        public static int ID;
        public static int VISION_RADIUS_SQUARED;
        public static int ACTION_RADIUS_SQUARED;
        public static MapLocation START_LOCATION;
        public static int MAP_WIDTH;
        public static int MAP_HEIGHT;
        public static int MAP_AREA;

        public static int MAX_HEALTH;

        public static int CHUNK_WIDTH;
        public static int NUM_HORIZONTAL_CHUNKS;
        public static int CHUNK_HEIGHT;
        public static int NUM_VERTICAL_CHUNKS;
        public static int CHUNK_EXPLORATION_RADIUS_SQUARED;
        public static int NUM_CHUNKS;

        private static void setupChunkBounds() {
            final int minAreaPerChunk = (int) Math.ceil(MAP_AREA / (double) Utils.MAX_MAP_CHUNKS);
            // int minMapArea = 20x20 = 400 / 100 == 4
//            int bestArea = (GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT / Utils.NUM_MAP_CHUNKS) + 1;
            switch (minAreaPerChunk) {
                case 4:
                    CHUNK_WIDTH = 2;
                    CHUNK_HEIGHT = 2;
                    CHUNK_EXPLORATION_RADIUS_SQUARED = 10; // 13 slightly too big
                    break;
                case 5:
//                    CHUNK_WIDTH = 1;
//                    CHUNK_HEIGHT = 5;
//                    break;
                case 6:
//                    CHUNK_WIDTH = 2;
//                    CHUNK_HEIGHT = 3;
//                    break;
                case 7:
                case 8:
//                    CHUNK_WIDTH = 2;
//                    CHUNK_HEIGHT = 4;
//                    break;
                case 9:
                    CHUNK_WIDTH = 3;
                    CHUNK_HEIGHT = 3;
                    CHUNK_EXPLORATION_RADIUS_SQUARED = 13; // must see > 6/9
                    break;
                case 10:
//                    CHUNK_WIDTH = 2;
//                    CHUNK_HEIGHT = 5;
//                    break;
                case 11:
                case 12:
//                    CHUNK_WIDTH = 3;
//                    CHUNK_HEIGHT = 4;
//                    break;
                case 13:
                case 14:
                case 15:
//                    CHUNK_WIDTH = 3;
//                    CHUNK_HEIGHT = 5;
//                    break;
                case 16:
                    CHUNK_WIDTH = 4;
                    CHUNK_HEIGHT = 4;
                    CHUNK_EXPLORATION_RADIUS_SQUARED = 8; // 9/10 leave too much space in lower left
                    break;
                case 17:
                case 18:
//                    CHUNK_WIDTH = 3;
//                    CHUNK_HEIGHT = 6;
//                    break;
                case 19:
                case 20:
//                    CHUNK_WIDTH = 4;
//                    CHUNK_HEIGHT = 5;
//                    break;
                case 21:
                case 22:
                case 23:
                case 24:
//                    CHUNK_WIDTH = 4;
//                    CHUNK_HEIGHT = 6;
//                    break;
                case 25:
                    CHUNK_WIDTH = 5;
                    CHUNK_HEIGHT = 5;
                    CHUNK_EXPLORATION_RADIUS_SQUARED = 8; // 9/10 leave whole row
                    break;
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
//                    CHUNK_WIDTH = 5;
//                    CHUNK_HEIGHT = 6;
//                    break;
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                    CHUNK_WIDTH = 6;
                    CHUNK_HEIGHT = 6;
                    CHUNK_EXPLORATION_RADIUS_SQUARED = 2; // 4/5 leave too much gap
                    break;
            }

            NUM_HORIZONTAL_CHUNKS = MAP_WIDTH / CHUNK_WIDTH;
            if (MAP_WIDTH % CHUNK_WIDTH > 0 && MAP_WIDTH % CHUNK_WIDTH < Math.sqrt(CHUNK_EXPLORATION_RADIUS_SQUARED)) { // only allow chunk if more than 1 column of it's locs are on the map
                NUM_HORIZONTAL_CHUNKS++;
            }

            NUM_VERTICAL_CHUNKS = MAP_HEIGHT / CHUNK_HEIGHT;
            if (MAP_HEIGHT % CHUNK_HEIGHT > 0) { // only allow chunk if more than 1 row of it's locs are on the map
                NUM_VERTICAL_CHUNKS++;
            }

            NUM_CHUNKS = NUM_HORIZONTAL_CHUNKS * NUM_VERTICAL_CHUNKS;

//            System.out.println("MAP WIDTH = " + MAP_WIDTH + " MAP HEIGHT = " + MAP_HEIGHT);
//            System.out.println("CHUNK_WIDTH: " + Cache.Permanent.CHUNK_WIDTH + " CHUNK_HEIGHT: " + Cache.Permanent.CHUNK_HEIGHT);
//            System.out.println("NUM_HORIZONTAL_CHUNKS: " + Cache.Permanent.NUM_HORIZONTAL_CHUNKS + " NUM_VERTICAL_CHUNKS: " + Cache.Permanent.NUM_VERTICAL_CHUNKS);

        }
    }

    public static void setup() throws GameActionException {
        Permanent.ROUND_SPAWNED = Global.rc.getRoundNum();
        Permanent.OUR_TEAM = Global.rc.getTeam();
        Permanent.OPPONENT_TEAM = Permanent.OUR_TEAM.opponent();
        Permanent.ROBOT_TYPE = Global.rc.getType();
        Permanent.START_LOCATION = Global.rc.getLocation();
        Permanent.ID = Global.rc.getID();
        Permanent.VISION_RADIUS_SQUARED = Permanent.ROBOT_TYPE.visionRadiusSquared;
        Permanent.ACTION_RADIUS_SQUARED = Permanent.ROBOT_TYPE.actionRadiusSquared;
        Permanent.MAP_WIDTH = Global.rc.getMapWidth();
        Permanent.MAP_HEIGHT = Global.rc.getMapHeight();
        Permanent.MAP_AREA = Permanent.MAP_WIDTH * Permanent.MAP_HEIGHT;
        Permanent.MAX_HEALTH = Global.rc.getType().health;
        Permanent.setupChunkBounds();
        updateOnTurn();
    }

    public static class PerTurn {
        public static int ROUND_NUM;
        public static int ROUNDS_ALIVE;
        public static RobotInfo[] ALL_NEARBY_ROBOTS;
        public static RobotInfo[] ALL_NEARBY_FRIENDLY_ROBOTS;
        public static RobotInfo[] ALL_NEARBY_ENEMY_ROBOTS;
        public static RobotInfo ENEMY_0;
        public static RobotInfo ENEMY_1;
        public static RobotInfo ENEMY_2;
        public static RobotInfo ENEMY_3;
        public static RobotInfo ENEMY_4;
        public static RobotInfo ENEMY_5;
        public static RobotInfo ENEMY_6;
        public static RobotInfo ENEMY_7;
        public static RobotInfo ENEMY_8;
        public static RobotInfo ENEMY_9;
        public static MapLocation CURRENT_LOCATION;
        public static int LEVEL;
        public static int HEALTH;
        //        public static MapLocation[] NEARBY_LEAD_MIN_2;
//        public static MapLocation[] NEARBY_LEAD_2;

        public static int cacheState;

        public static void whenMoved() throws GameActionException {
            // don't need to update
            if (PerTurn.CURRENT_LOCATION != null && Global.rc.getLocation().equals(PerTurn.CURRENT_LOCATION)) {
                return;
            }
            // do the update
            updateForMovement();
        }

        private static void updateForMovement() throws GameActionException {
            PerTurn.cacheState++;
            PerTurn.CURRENT_LOCATION = Global.rc.getLocation();
            PerTurn.ALL_NEARBY_ROBOTS = Global.rc.senseNearbyRobots();
            PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS = Global.rc.senseNearbyRobots(-1, Permanent.OUR_TEAM);
            PerTurn.ALL_NEARBY_ENEMY_ROBOTS = Global.rc.senseNearbyRobots(-1, Permanent.OPPONENT_TEAM);
//            PerTurn.NEARBY_LEAD_MIN_2 = Global.rc.senseNearbyLocationsWithLead(-1, 2);
        }
    }

    public static void updateOnTurn() throws GameActionException {
        PerTurn.ROUND_NUM = Global.rc.getRoundNum();
        PerTurn.ROUNDS_ALIVE = PerTurn.ROUND_NUM - Permanent.ROUND_SPAWNED;
        PerTurn.LEVEL = Global.rc.getLevel();
        PerTurn.HEALTH = Global.rc.getHealth();
        PerTurn.updateForMovement();
    }
}
