package noarchonsaving.robots.droids;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import noarchonsaving.robots.Robot;

public abstract class Droid extends Robot {

  protected MapLocation parentArchonLoc;

  public Droid(RobotController rc) {
    super(rc);
    for (RobotInfo info : rc.senseNearbyRobots(2, creationStats.myTeam)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        break;
      }
    }
  }
}
