package noarchonsaving.pathfinding;


import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import noarchonsaving.Utils;
import java.util.HashSet;

public class Pathfinding {

  RobotController rc;
  MapLocation target = null;
  double avgImpassabilityInv = 10;

  BugNav bugNav = new BugNav();
//  Explore explore;


  boolean[] impassable = null;

  public void setImpassable(boolean[] impassable){
    this.impassable = impassable;
  }

  void initTurn(){
    impassable = new boolean[Utils.directions.length];
  }

  boolean canMove(Direction dir){
    if (!rc.canMove(dir)) return false;
    if (impassable[dir.ordinal()]) return false;
    return true;
  }


  Pathfinding(RobotController rc){
    this.rc = rc;
//    this.explore = explore;
  }

  double getEstimation (MapLocation loc){
    try {
      if (loc.distanceSquaredTo(target) == 0) return 0;
      int d = Utils.maxSingleAxisDist(target, loc);
//      double p = rc.sensePassability(loc);
      return rc.senseRubble(loc) + (d - 1)*avgImpassabilityInv;
    } catch (Throwable e){
      e.printStackTrace();
    }
    return 1e9;
  }

  public void move(MapLocation loc){
    if (!rc.isMovementReady()) return;
    target = loc;

    ////rc.setIndicatorLine(rc.getLocation(), target, 255, 0, 0);

    if (!bugNav.move()) greedyPath();
    bugNav.move();
  }

  final double eps = 1e-5;

  void greedyPath(){
    try {
      MapLocation myLoc = rc.getLocation();
      Direction bestDir = null;
      double bestEstimation = 0;
      double firstStep = rc.senseRubble(myLoc);
      int contPassability = 0;
      int bestEstimationDist = 0;
      double avgP = 0;
      for (Direction dir : Utils.directions) {
        MapLocation newLoc = myLoc.add(dir);
        if (!rc.onTheMap(newLoc)) continue;

        //pass
        avgP += rc.senseRubble(newLoc);
        ++contPassability;


        if (!canMove(dir)) continue;
        if (!strictlyCloser(newLoc, myLoc, target)) continue;

        int newDist = newLoc.distanceSquaredTo(target);

        double estimation = firstStep + getEstimation(newLoc);
        if (bestDir == null || estimation < bestEstimation - eps || (Math.abs(estimation - bestEstimation) <= 2*eps && newDist < bestEstimationDist)) {
          bestEstimation = estimation;
          bestDir = dir;
          bestEstimationDist = newDist;
        }
      }
      if (contPassability != 0) {
        avgImpassabilityInv = avgP / contPassability;
      }
      if (bestDir != null) rc.move(bestDir);
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  boolean strictlyCloser(MapLocation newLoc, MapLocation oldLoc, MapLocation target){
    int dOld = Utils.maxSingleAxisDist(target, oldLoc), dNew = Utils.maxSingleAxisDist(target, newLoc);
    if (dOld < dNew) return false;
    if (dNew < dOld) return true;
    return target.distanceSquaredTo(newLoc) < target.distanceSquaredTo(oldLoc);

  }

  class BugNav{

    BugNav(){}

    final int INF = 1000000;
    final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;

    boolean rotateRight = true; //if I should rotate right or left
    MapLocation lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    MapLocation prevTarget = null; //previous target
    HashSet<Integer> visited = new HashSet<>();

    boolean move() {
      try{

        //different target? ==> previous data does not help!
        if (prevTarget == null || target.distanceSquaredTo(prevTarget) > 0) resetPathfinding();

        //If I'm at a minimum distance to the target, I'm free!
        MapLocation myLoc = rc.getLocation();
        int d = myLoc.distanceSquaredTo(target);
        if (d <= minDistToEnemy) resetPathfinding();

        int code = getCode();

        if (visited.contains(code)) resetPathfinding();
        visited.add(code);

        //Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);
        if (canMove(dir)){
          resetPathfinding();
//          if (rc.getType() != RobotType.SLANDERER) return false;
        }

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        for (int i = 8; i-- > 0;) {
          if (canMove(dir)) {
            rc.move(dir);
            return true;
          }
          MapLocation newLoc = myLoc.add(dir);
          if (!rc.onTheMap(newLoc)) rotateRight = !rotateRight;
            //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
          else lastObstacleFound = myLoc.add(dir);
          if (rotateRight) dir = dir.rotateRight();
          else dir = dir.rotateLeft();
        }

        if (canMove(dir)) rc.move(dir);
      } catch (Exception e){
        e.printStackTrace();
      }
      return true;
    }

    //clear some of the previous data
    void resetPathfinding(){
      lastObstacleFound = null;
      minDistToEnemy = INF;
      visited.clear();
    }

    int getCode(){
      int x = rc.getLocation().x % MAX_MAP_SIZE;
      int y = rc.getLocation().y % MAX_MAP_SIZE;
      Direction obstacleDir = rc.getLocation().directionTo(target);
      if (lastObstacleFound != null) obstacleDir = rc.getLocation().directionTo(lastObstacleFound);
      int bit = rotateRight ? 1 : 0;
      return (((((x << 6) | y) << 4) | obstacleDir.ordinal()) << 1) | bit;
    }
  }


}