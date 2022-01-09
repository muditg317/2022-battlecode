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
    if (myBuilding == null) moveRandomly();
//    rc.disintegrate();
    if (rc.isActionReady()) {
      if (myBuilding != null) {
        int healthNeeded = RobotType.WATCHTOWER.getMaxHealth(1) - rc.senseRobotAtLocation(myBuilding).health;
        if (healthNeeded > 0 && rc.canRepair(myBuilding)) {
           rc.repair(myBuilding);
           if (healthNeeded <= -creationStats.type.damage) {
             myBuilding = null;
           }
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
