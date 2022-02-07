package soldiermicronew.utils;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

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
        updateOnTurn();
    }

    public static class PerTurn {
        public static int ROUND_NUM;
        public static int ROUNDS_ALIVE;
        public static RobotInfo[] ALL_NEARBY_ROBOTS;
        public static RobotInfo[] ALL_NEARBY_FRIENDLY_ROBOTS;
        public static RobotInfo[] ALL_NEARBY_ENEMY_ROBOTS;
        public static MapLocation CURRENT_LOCATION;
        public static int LEVEL;
        public static int HEALTH;

        public static void whenMoved() {
            // don't need to update
            if (PerTurn.CURRENT_LOCATION != null && Global.rc.getLocation().equals(PerTurn.CURRENT_LOCATION)) {
                return;
            }

            // do the update
            updateForMovement();
        }

        private static void updateForMovement() {
            PerTurn.CURRENT_LOCATION = Global.rc.getLocation();
            PerTurn.ALL_NEARBY_ROBOTS = Global.rc.senseNearbyRobots();
            PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS = Global.rc.senseNearbyRobots(-1, Permanent.OUR_TEAM);
            PerTurn.ALL_NEARBY_ENEMY_ROBOTS = Global.rc.senseNearbyRobots(-1, Permanent.OPPONENT_TEAM);
        }
    }

    public static void updateOnTurn() {
        PerTurn.ROUND_NUM = Global.rc.getRoundNum();
        PerTurn.ROUNDS_ALIVE = PerTurn.ROUND_NUM - Permanent.ROUND_SPAWNED;
        PerTurn.LEVEL = Global.rc.getLevel();
        PerTurn.HEALTH = Global.rc.getHealth();
        PerTurn.updateForMovement();
    }
}
