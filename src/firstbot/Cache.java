package firstbot;

import battlecode.common.*;

public class Cache {
    public static RobotController controller;
    public static Team OUR_TEAM;
    public static Team OPPONENT_TEAM;
    public static RobotInfo[] ALL_NEARBY_ROBOTS;
    public static RobotInfo[] ALL_NEARBY_FRIENDLY_ROBOTS;
    public static RobotInfo[] ALL_NEARBY_ENEMY_ROBOTS;
    public static MapLocation CURRENT_LOCATION;
    public static RobotType ROBOT_TYPE;
    public static int ID;
    public static int VISION_RADIUS_SQUARED;

//    public static MapLocation myArchonLocation;

    public static MapLocation START_LOCATION;

    public static int LEVEL;


    public static void init(RobotController controller) throws GameActionException {
        Cache.controller = controller;
        OUR_TEAM = controller.getTeam();
        OPPONENT_TEAM = OUR_TEAM.opponent();
        ROBOT_TYPE = controller.getType();
        CURRENT_LOCATION = controller.getLocation();
        START_LOCATION = CURRENT_LOCATION;
        ID = controller.getID();
        VISION_RADIUS_SQUARED = Cache.ROBOT_TYPE.visionRadiusSquared;

//        myArchonLocation =
    }

    public static void loop() {
        ALL_NEARBY_ROBOTS = controller.senseNearbyRobots();
        ALL_NEARBY_FRIENDLY_ROBOTS = controller.senseNearbyRobots(-1, OUR_TEAM);
        ALL_NEARBY_ENEMY_ROBOTS = controller.senseNearbyRobots(-1, OPPONENT_TEAM);
        CURRENT_LOCATION = controller.getLocation();
        LEVEL = controller.getLevel();
    }
}
