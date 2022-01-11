package spreadtowers.utils;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Cache {
    public static class Permanent {
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

    public static void setup(RobotController rc) throws GameActionException {
        Permanent.OUR_TEAM = rc.getTeam();
        Permanent.OPPONENT_TEAM = Permanent.OUR_TEAM.opponent();
        Permanent.ROBOT_TYPE = rc.getType();
        Permanent.START_LOCATION = rc.getLocation();
        Permanent.ID = rc.getID();
        Permanent.VISION_RADIUS_SQUARED = Permanent.ROBOT_TYPE.visionRadiusSquared;
        Permanent.ACTION_RADIUS_SQUARED = Permanent.ROBOT_TYPE.actionRadiusSquared;
        Permanent.MAP_WIDTH = rc.getMapWidth();
        Permanent.MAP_HEIGHT = rc.getMapHeight();
    }

    public static class PerTurn {

        public static RobotInfo[] ALL_NEARBY_ROBOTS;
        public static RobotInfo[] ALL_NEARBY_FRIENDLY_ROBOTS;
        public static RobotInfo[] ALL_NEARBY_ENEMY_ROBOTS;
        public static MapLocation CURRENT_LOCATION;
        public static int LEVEL;
    }

    public static void updateOnTurn() {
        PerTurn.ALL_NEARBY_ROBOTS = Global.rc.senseNearbyRobots();
        PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS = Global.rc.senseNearbyRobots(-1, Permanent.OUR_TEAM);
        PerTurn.ALL_NEARBY_ENEMY_ROBOTS = Global.rc.senseNearbyRobots(-1, Permanent.OPPONENT_TEAM);
        PerTurn.CURRENT_LOCATION = Global.rc.getLocation();
        PerTurn.LEVEL = Global.rc.getLevel();
    }
}
