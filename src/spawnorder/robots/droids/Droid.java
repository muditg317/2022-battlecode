package spawnorder.robots.droids;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import spawnorder.utils.Cache;
import spawnorder.robots.Robot;

public abstract class Droid extends Robot {

  protected MapLocation parentArchonLoc;

  public Droid(RobotController rc) throws GameActionException {
    super(rc);
    for (RobotInfo info : rc.senseNearbyRobots(2, Cache.Permanent.OUR_TEAM)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        break;
      }
    }
  }
}
