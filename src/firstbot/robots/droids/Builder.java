package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import firstbot.Utils;

public class Builder extends Droid {
  private static final int DIST_TO_WALL_THRESH = 5;

  MapLocation myBuilding;
  Direction dirToBuild;
  boolean readyToBuild;

  public Builder(RobotController rc) {
    super(rc);
    MapLocation myLoc = rc.getLocation();
    dirToBuild = parentArchonLoc.directionTo(rc.getLocation());
    if ((myLoc.x < DIST_TO_WALL_THRESH && dirToBuild.dx < 0) || (rc.getMapWidth() - myLoc.x < DIST_TO_WALL_THRESH && dirToBuild.dx > 0)) {
      dirToBuild = Utils.flipDirX(dirToBuild);
    }
    if ((myLoc.y < DIST_TO_WALL_THRESH && dirToBuild.dy < 0) || (rc.getMapHeight() - myLoc.y < DIST_TO_WALL_THRESH && dirToBuild.dy > 0)) {
      dirToBuild = Utils.flipDirY(dirToBuild);
    }
  }

  @Override
  protected void runTurn() throws GameActionException {
    if (myBuilding == null) {
      moveInDirRandom(dirToBuild);
      if (!readyToBuild && rc.getLocation().distanceSquaredTo(parentArchonLoc) >= creationStats.type.visionRadiusSquared) {
        readyToBuild = true;
      }
    }
//    rc.disintegrate();
    if (readyToBuild && rc.isActionReady()) {
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
