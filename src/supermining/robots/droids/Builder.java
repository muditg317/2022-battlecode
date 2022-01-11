package supermining.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import supermining.utils.Cache;
import supermining.utils.Utils;

public class Builder extends Droid {
  private static final int DIST_TO_WALL_THRESH = 6;

  MapLocation myBuilding;
  Direction dirToBuild;
  boolean readyToBuild;

  public Builder(RobotController rc) throws GameActionException {
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
    if (myBuilding == null && (moveInDirRandom(dirToBuild) || moveRandomly())) {
      if (!rc.onTheMap(rc.getLocation().add(dirToBuild))) { // gone to map edge
        dirToBuild = dirToBuild.opposite();
      } else if (!readyToBuild && rc.getLocation().distanceSquaredTo(parentArchonLoc) >= Cache.Permanent.VISION_RADIUS_SQUARED) {
        // can only be ready to build if not on edge
        readyToBuild = true;
      }
    }
//    rc.disintegrate();
    if (readyToBuild && rc.isActionReady()) {
      if (myBuilding != null) {
        if (repairBuilding()) myBuilding = null;
      } else {
        Direction dir = Utils.randomDirection();
        if (rc.canBuildRobot(RobotType.WATCHTOWER, dir)) {
          rc.buildRobot(RobotType.WATCHTOWER, dir);
          myBuilding = rc.getLocation().add(dir);
        }
      }
    }
  }

  /**
   * repair the building at myBuilding
   * @return true if done repairing
   */
  private boolean repairBuilding() throws GameActionException {
    RobotInfo toRepair = rc.senseRobotAtLocation(myBuilding);
    if (toRepair == null) return true; // stop repairing here
    int healthNeeded = RobotType.WATCHTOWER.getMaxHealth(1) - toRepair.health;
    boolean needsRepair = healthNeeded > 0;
    if (needsRepair && rc.canRepair(myBuilding)) {
      rc.repair(myBuilding);
      return healthNeeded <= -Cache.Permanent.ROBOT_TYPE.damage;
    }
    return needsRepair;
  }
}
