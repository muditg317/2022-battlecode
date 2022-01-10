package firstbot.robots.droids;

import battlecode.common.*;
import firstbot.robots.Robot;

public abstract class Droid extends Robot {

  protected MapLocation parentArchonLoc;

  public Droid(RobotController rc) throws GameActionException {
    super(rc);
    for (RobotInfo info : rc.senseNearbyRobots(2, creationStats.myTeam)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        break;
      }
    }
  }
}
