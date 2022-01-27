package sagerunning.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotMode;
import sagerunning.utils.Cache;
import sagerunning.utils.Utils;

public class Laboratory extends Building {

  private int rate;

  public Laboratory(RobotController rc) throws GameActionException {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    rate = rc.getTransmutationRate();

    // I am now spawned,
    // if I am in portiable
    if (rc.getMode() == RobotMode.PROTOTYPE) {
      // maybe do some calculations while I can't do anything
      runPrototype();
      return;
    }

//    if (rate > 2) {
//      //System.out.println("Not optimal rate: " + rate);
//    } else {
//      //System.out.println("Transmuting optimally");
//    }


    if (!moving && rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) > 0) shouldMoveToBetterLocation(); // only run if we aren't moving yet

    if (whereToGo != null && Cache.PerTurn.CURRENT_LOCATION.equals(whereToGo) && rc.getMode() == RobotMode.PORTABLE) {
      if (rc.canTransform()) {
        rc.transform();
      }
    }

    if (!moving && rc.canTransmute()) {
      rc.transmute();
    }

    // we decided to move, let's go to target!
    if (moving) {
      if (rc.getMode() != RobotMode.PORTABLE) {
        if (rc.canTransform()) rc.transform();
      } else {
        moveOptimalTowards(whereToGo);
        if (Cache.PerTurn.CURRENT_LOCATION.equals(whereToGo)) {
          moving = false;
//          if (rc.canTransform())
        }

        // some bot took the spot...
        if (moving && rc.canSenseLocation(whereToGo) && rc.isLocationOccupied(whereToGo)) {
          shouldMoveToBetterLocation();
        }
      }


    }

  }


  private boolean moving;
  private MapLocation whereToGo;

  // only called if we are not movings
  private boolean shouldMoveToBetterLocation() throws GameActionException {
    MapLocation findBetterRubbleSquare = findBetterRubbleSquare();
    if (findBetterRubbleSquare != null) {
      //rc.setIndicatorString("I want to move to better rubble square " + findBetterRubbleSquare);
      int currentGold = rc.getTeamGoldAmount(Cache.Permanent.OUR_TEAM);
      if (currentGold <= 15 || currentGold >= 20) {
        whereToGo = findBetterRubbleSquare;
        moving = true;
      }
    }
    return moving;
  }

  // find best location to spawn at such that:
  // 1) locations must be closer to corner than archon
  // 2) location has least rubble
  // 3) location is closest to corner
  private MapLocation findBetterRubbleSquare() throws GameActionException {
    //todo: maybe bound the locations to some distance from corner
    MapLocation corner = new MapLocation(
            Cache.PerTurn.CURRENT_LOCATION.x <= Cache.Permanent.MAP_WIDTH / 2 ? 0 : (Cache.Permanent.MAP_WIDTH-1),
            Cache.PerTurn.CURRENT_LOCATION.y <= Cache.Permanent.MAP_HEIGHT / 2 ? 0 : (Cache.Permanent.MAP_HEIGHT-1));

    int currentRubbleSquare = rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION);

    MapLocation bestLabLocation = null;
    int bestRubbleAtLocation = 101;
    int bestDistance = 101;
    int bestDistanceToCorner = 101;

    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, 34)) {
      if (!rc.canSenseLocation(loc) || rc.isLocationOccupied(loc)) continue;

      int candidateDistance = Utils.maxSingleAxisDist(loc, Cache.PerTurn.CURRENT_LOCATION);
      int candidateRubble = rc.senseRubble(loc);
      int candidateDistanceToCorner = loc.distanceSquaredTo(corner);//.maxSingleAxisDist(loc, corner);
      if (currentRubbleSquare <= candidateRubble) continue;
      if (candidateRubble < bestRubbleAtLocation) {
        bestLabLocation = loc;
        bestRubbleAtLocation = candidateRubble;
        bestDistance = candidateDistance;
        bestDistanceToCorner = candidateDistanceToCorner;
      } else if (candidateRubble == bestRubbleAtLocation && candidateDistance < bestDistance) {
        bestLabLocation = loc;
        bestDistance = candidateDistance;
        bestDistanceToCorner = candidateDistanceToCorner;
      } else if (candidateRubble == bestRubbleAtLocation && candidateDistance == bestDistance && candidateDistanceToCorner < bestDistanceToCorner) {
        bestLabLocation = loc;
        bestDistanceToCorner = candidateDistanceToCorner;
      }
    }
    return bestLabLocation;
  }

  // If there are lots of enemies near me, RUN BOI

  private void runPrototype() {
  }
}
