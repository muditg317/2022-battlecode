package pnayminingmicro.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import pnayminingmicro.utils.Cache;
import pnayminingmicro.utils.Utils;

public class Builder extends Droid {
  private static final int DIST_TO_WALL_THRESH = 6;

  MapLocation myBuilding;
  Direction dirToBuild;
  boolean readyToBuild;
  boolean hasSpaceToBuild;

  public Builder(RobotController rc) throws GameActionException {
    super(rc);
    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
    dirToBuild = parentArchonLoc.directionTo(Cache.PerTurn.CURRENT_LOCATION);
    if ((myLoc.x < DIST_TO_WALL_THRESH && dirToBuild.dx < 0) || (Cache.Permanent.MAP_WIDTH - myLoc.x < DIST_TO_WALL_THRESH && dirToBuild.dx > 0)) {
      dirToBuild = Utils.flipDirX(dirToBuild);
    }
    if ((myLoc.y < DIST_TO_WALL_THRESH && dirToBuild.dy < 0) || (Cache.Permanent.MAP_HEIGHT - myLoc.y < DIST_TO_WALL_THRESH && dirToBuild.dy > 0)) {
      dirToBuild = Utils.flipDirY(dirToBuild);
    }
  }

  @Override
  protected void runTurn() throws GameActionException {
    if (myBuilding == null && (moveInDirRandom(dirToBuild) || moveRandomly())) {
      if (!rc.onTheMap(Cache.PerTurn.CURRENT_LOCATION.add(dirToBuild)) || offensiveEnemiesNearby()) { // gone to map edge
        dirToBuild = dirToBuild.rotateRight();
      } else if (!readyToBuild && Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(parentArchonLoc) >= Cache.Permanent.VISION_RADIUS_SQUARED) {
        // can only be ready to build if not on edge
        readyToBuild = true;
        hasSpaceToBuild = checkSpaceToBuild();
      } else if (readyToBuild) {
        hasSpaceToBuild = checkSpaceToBuild();
      }
    }
//    rc.disintegrate();
    if (readyToBuild && rc.isActionReady()) {
      if (myBuilding != null) {
        if (repairBuilding()) myBuilding = null;
      } else if (hasSpaceToBuild) {
        Direction dir = Utils.randomDirection();
        if (rc.canBuildRobot(RobotType.WATCHTOWER, dir)) {
          rc.buildRobot(RobotType.WATCHTOWER, dir);
          myBuilding = Cache.PerTurn.CURRENT_LOCATION.add(dir);
        }
      }
    }
  }

  /**
   * check around the miner if it has space to build
   *    currently just checks no other buildings within 2x2 square
   * @return true if enough space to build around self
   */
  private boolean checkSpaceToBuild() throws GameActionException {
    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      if (friend.type.isBuilding() && friend.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_1by1)) {
        return false;
      }
    }
    return true;
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
