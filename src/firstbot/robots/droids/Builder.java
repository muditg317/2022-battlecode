package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import firstbot.Utils;

public class Builder extends Droid {
  MapLocation myBuilding;

  public Builder(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    moveRandomly();
//    rc.disintegrate();
    if (rc.isActionReady()) {
      if (myBuilding != null) {
        if (rc.senseRobotAtLocation(myBuilding).health < RobotType.WATCHTOWER.getMaxHealth(1) && rc.canRepair(myBuilding)) {
           rc.repair(myBuilding);
        }
      } else {
        Direction dir = Utils.randomDirection();
        if (rc.canBuildRobot(RobotType.WATCHTOWER, dir)) {
          rc.buildRobot(RobotType.WATCHTOWER, dir);
          myBuilding = rc.getLocation().add(dir);
        }
      }
    }
  }
}
