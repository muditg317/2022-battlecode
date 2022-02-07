package firstbot.robots.droids;

import battlecode.common.*;
import firstbot.communications.messages.LabBuiltMessage;
import firstbot.utils.Cache;
import firstbot.utils.Utils;

public class Builder extends Droid {
  private static final int DIST_TO_WALL_THRESH = 2;
  private static final int MIN_LAB_CORNER_DIST = 7;

  private boolean IS_FARMER;

  MapLocation myBuilding;
  Direction dirToBuild;
  boolean readyToBuild;
  boolean hasSpaceToBuild;

  MapLocation bestLocationToSpawnLab;
  boolean labBuilt;
  LabBuiltMessage labBuiltMessage;

  MapLocation corner;
  int archonDistanceToCorner;

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
    corner = new MapLocation(
            parentArchonLoc.x <= Cache.Permanent.MAP_WIDTH / 2 ? 0 : (Cache.Permanent.MAP_WIDTH-1),
            parentArchonLoc.y <= Cache.Permanent.MAP_HEIGHT / 2 ? 0 : (Cache.Permanent.MAP_HEIGHT-1));
    archonDistanceToCorner = Utils.maxSingleAxisDist(parentArchonLoc, corner);
    if (archonDistanceToCorner < MIN_LAB_CORNER_DIST) archonDistanceToCorner = MIN_LAB_CORNER_DIST;
    bestLocationToSpawnLab = findBestLabSpawnLocation();

    IS_FARMER = communicator.spawnInfo.getMinerMainCount() >= communicator.spawnInfo.getNumMinersNeeded()
            && rc.getTeamGoldAmount(Cache.Permanent.OUR_TEAM) >= 5
            && Utils.rng.nextInt(15) < 14;
  }

  // find best location to spawn at such that:
  // 1) locations must be closer to corner than archon
  // 2) location has least rubble
  // 3) location is closest to corner
  private MapLocation findBestLabSpawnLocation() throws GameActionException {
//    Printer.cleanPrint();
    MapLocation bestLabLocation = null;
    int bestRubbleAtLocation = 101;
    int bestDistanceToCorner = 101;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED)) {
//      Printer.submitPrint();
//      Printer.print("loc: " + loc);
      if (!rc.canSenseLocation(loc) || (rc.isLocationOccupied(loc) && !Cache.PerTurn.CURRENT_LOCATION.equals(loc))) continue;
      int candidateDistanceToCorner = Utils.maxSingleAxisDist(loc, corner);
//      Printer.print("candidateDistanceToCorner " + candidateDistanceToCorner, "archonDistanceToCorner: " + archonDistanceToCorner);
      if (candidateDistanceToCorner <= archonDistanceToCorner) {
        int candidateRubble = rc.senseRubble(loc);
//        Printer.print("candidateRubble " + candidateRubble, "bestRubbleAtLocation: " + bestRubbleAtLocation);
        if (candidateRubble < bestRubbleAtLocation) {
          bestLabLocation = loc;
          bestRubbleAtLocation = candidateRubble;
          bestDistanceToCorner = candidateDistanceToCorner;
//          Printer.print("updating to " + bestLabLocation);
        } else if (candidateRubble == bestRubbleAtLocation && candidateDistanceToCorner < bestDistanceToCorner) {
          bestLabLocation = loc;
          bestDistanceToCorner = candidateDistanceToCorner;
//          Printer.print("updating to " + bestLabLocation);
        }
      }
    }
    return bestLabLocation;
  }

  private MapLocation findBestBuildSpot(MapLocation spawnLocation) throws GameActionException {
    MapLocation bestBuildSpot = null;
    int rubbleAtBest = 9999;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(spawnLocation, Utils.DSQ_1by1)) {
      if (!rc.canSenseLocation(loc) || (rc.isLocationOccupied(loc) && !Cache.PerTurn.CURRENT_LOCATION.equals(loc)) || spawnLocation.equals(loc)) continue;
      int rubble = rc.senseRubble(loc);
      if (rubble < rubbleAtBest) {
        bestBuildSpot = loc;
        rubbleAtBest = rubble;
      }
    }
    return bestBuildSpot;
  }

  private MapLocation findBestRepairSpot(MapLocation buildingToRepair) throws GameActionException {
    MapLocation bestRepairSpot = null;
    int rubbleAtBest = 9999;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(buildingToRepair, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
      if (!rc.canSenseLocation(loc) || (rc.isLocationOccupied(loc) && !Cache.PerTurn.CURRENT_LOCATION.equals(loc))) continue;
      int rubble = rc.senseRubble(loc);
      if (rubble < rubbleAtBest) {
        bestRepairSpot = loc;
        rubbleAtBest = rubble;
      }
    }
    return bestRepairSpot;
  }

  private MapLocation bestLeadFarmLocation() throws GameActionException {
    MapLocation bestFarmLocation = null;
    int bestRubbleAtLocation = 101;
    int bestDistance = 101;
    for (MapLocation location : rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED)) {
      // if there is an unoccupied location with 0 lead
      if (rc.canSenseLocation(location) && !rc.isLocationOccupied(location) && rc.senseLead(location) == 0) {
        int candidateRubble = rc.senseRubble(location);
        int candidateDistance = Utils.maxSingleAxisDist(location, corner);
        if (candidateRubble < bestRubbleAtLocation) {
          bestFarmLocation = location;
          bestRubbleAtLocation = candidateRubble;
          bestDistance = candidateDistance;
        } else if (candidateRubble == bestRubbleAtLocation && candidateDistance < bestDistance) {
          bestFarmLocation = location;
          bestDistance = candidateDistance;
        }
      }
    }
    return bestFarmLocation;
  }

  @Override
  protected void runTurn() throws GameActionException {

//    if ((IS_FARMER || hasBuildersNearby()) && myBuilding == null && communicator.spawnInfo.getMinerMainCount() >= communicator.spawnInfo.getNumMinersNeeded() && rc.getTeamGoldAmount(Cache.Permanent.OUR_TEAM) >= 5) {
//      MapLocation leadFarmLoc = bestLeadFarmLocation();
//      if (leadFarmLoc != null) {
//        moveOptimalTowards(leadFarmLoc);
//      } else {
//        moveOptimalAway(parentArchonLoc);
//      }
//      if (rc.senseLead(Cache.PerTurn.CURRENT_LOCATION) == 0) {
//        rc.disintegrate();
//      }
//    }

    if (!labBuilt) {
      if (myBuilding != null) {
        MapLocation bestRepairSpot = findBestRepairSpot(myBuilding);
        if (bestRepairSpot != null) {
          moveOptimalTowards(bestRepairSpot);
        }
        if (repairBuilding()) myBuilding = null;
      }
      if (bestLocationToSpawnLab != null && Cache.PerTurn.CURRENT_LOCATION.equals(bestLocationToSpawnLab)) {
//        System.out.println("Sorry im in the way! moving...");
        moveOptimalAway(bestLocationToSpawnLab);
      }
      if (bestLocationToSpawnLab == null) {
//        System.out.println("no best location to spawn lab!!");
      } else {
        MapLocation bestBuildSpot = findBestBuildSpot(bestLocationToSpawnLab);
        if (bestBuildSpot == null) bestBuildSpot = bestLocationToSpawnLab;

        rc.setIndicatorString("lab loc: " + bestLocationToSpawnLab + " build loc: " + bestBuildSpot);
        if (moveOptimalTowards(bestBuildSpot) && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(bestLocationToSpawnLab, Utils.DSQ_1by1) && Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(bestLocationToSpawnLab, Utils.DSQ_2by2)) {
          bestLocationToSpawnLab = findBestLabSpawnLocation();
        }

        Direction dirToBuildLab = getLeastRubbleUnoccupiedDir(); //Cache.PerTurn.CURRENT_LOCATION.directionTo(bestLocationToSpawnLab);
        if (dirToBuildLab != null && buildRobot(RobotType.LABORATORY, dirToBuildLab)) {
//          System.out.println("spawning lab at " + bestLocationToSpawnLab);
          myBuilding = Cache.PerTurn.CURRENT_LOCATION.add(dirToBuildLab);
          IS_FARMER = false;
          broadcastLabBuilt();
//          labBuilt = true;
        }
      }
    } else if (myBuilding == null && (moveInDirAvoidRubble(dirToBuild) || moveRandomly())) {
      if (!rc.onTheMap(Cache.PerTurn.CURRENT_LOCATION.add(dirToBuild)) || offensiveEnemiesNearby()) { // gone to map edge
        dirToBuild = dirToBuild.rotateRight();
      } else if (!readyToBuild && Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(parentArchonLoc) >= 0) {
        // can only be ready to build if not on edge
        readyToBuild = true;
        hasSpaceToBuild = checkSpaceToBuild();
      } else if (readyToBuild) {
        hasSpaceToBuild = checkSpaceToBuild();
      }
    }

    if (labBuiltMessage != null && labBuiltMessage.writeInfo == null) {
      communicator.enqueueMessage(labBuiltMessage);
    }

    if (rc.isActionReady()) {
      if (myBuilding != null) {
        MapLocation bestRepairSpot = findBestRepairSpot(myBuilding);
        if (bestRepairSpot != null) {
          moveOptimalTowards(bestRepairSpot);
        }
        if (repairBuilding()) myBuilding = null;
      } else if (labBuilt && readyToBuild && hasSpaceToBuild) {
//        for (RobotInfo )
        Direction dir = getLeastRubbleUnoccupiedDir();
//        RobotType typeToBuild = labBuilt ? RobotType.WATCHTOWER : RobotType.LABORATORY;
        RobotType typeToBuild = RobotType.LABORATORY;
        if (buildRobot(typeToBuild, dir)) {
          myBuilding = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//          if (typeToBuild == RobotType.LABORATORY) {
//            labBuilt = true;
          IS_FARMER = false;
            broadcastLabBuilt();
//          }
        }
      }
    }

    if (Clock.getBytecodesLeft() > 4000 && myBuilding == null) {
      for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
        if (friend.type.isBuilding() && friend.mode == RobotMode.PROTOTYPE) {
          myBuilding = friend.location;
          IS_FARMER = false;
          break;
        }
      }
    }
  }

  private boolean hasBuildersNearby() {
    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      if (friend.type == RobotType.BUILDER) return true;
    }
    return false;
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
    if (!rc.canSenseLocation(myBuilding)) moveOptimalTowards(myBuilding);
    if (!rc.canSenseLocation(myBuilding)) return false;
    RobotInfo toRepair = rc.senseRobotAtLocation(myBuilding);
    if (toRepair == null) {
      System.out.println("NOBODY TO REPAIR!! UH OH -- " + myBuilding + " is empty...");
      return true; // stop repairing here
    }
    int healthNeeded = toRepair.type.getMaxHealth(1) - toRepair.health;
    boolean needsRepair = healthNeeded > 0;
    if (needsRepair && rc.canRepair(myBuilding)) {
      rc.repair(myBuilding);
      return healthNeeded <= -Cache.Permanent.ROBOT_TYPE.damage;
    }
    return needsRepair;
  }

  private void broadcastLabBuilt() throws GameActionException {
    communicator.enqueueMessage(labBuiltMessage = new LabBuiltMessage(myBuilding));
  }
}
