package bfspathing.pathfinding;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import bfspathing.utils.Cache;

public abstract class BFS {

  final int BYTECODE_REMAINING = 1000;
  final int BYTECODE_REMAINING_NON_SLAND = 2500;
  //static final int BYTECODE_BFS = 5000;
  final int GREEDY_TURNS = 4;

  public Pathfinding path;
  //  Explore explore;
  static RobotController rc;
  MapTracker mapTracker = new MapTracker();

  int turnsGreedy = 0;

  MapLocation currentTarget = null;



  BFS(RobotController rc){
    BFS.rc = rc;
//    this.explore = explore;
    this.path = new Pathfinding(rc);
  }

  void reset(){
    turnsGreedy = 0;
    mapTracker.reset();
  }

  void update(MapLocation target){
    if (currentTarget == null || target.distanceSquaredTo(currentTarget) > 0){
      reset();
    } else --turnsGreedy;
    currentTarget = target;
    mapTracker.add(Cache.PerTurn.CURRENT_LOCATION);
  }

  void activateGreedy(){
    turnsGreedy = GREEDY_TURNS;
  }

  public void initTurn(){
    path.initTurn();
  }

  public void move(MapLocation target){
    move(target, false);
  }

  public void move(MapLocation target, boolean greedy){
    if (target == null) return;
    if (!rc.isMovementReady()) return;
    if (rc.getLocation().distanceSquaredTo(target) == 0) return;

    update(target);

    if (!greedy && turnsGreedy <= 0){

      //System.err.println("Using bfs");
      Direction dir = getBestDir(target);
      if (dir != null && !mapTracker.check(rc.getLocation().add(dir))){
        exploreMove(dir);
        return;
      } else activateGreedy();
    }

//    if (rc.getType() == RobotType.SLANDERER) {
    if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING) {
      //System.err.println("Using greedy");
      ////System.out.println("Before pathfinding " + Clock.getBytecodeNum());
      path.move(target);
      ////System.out.println("After pathfinding " + Clock.getBytecodeNum());
      --turnsGreedy;
    }
//    } else{
//      if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING_NON_SLAND) {
//        //System.err.println("Using greedy");
//        ////System.out.println("Before pathfinding " + Clock.getBytecodeNum());
//        path.move(target);
//        ////System.out.println("After pathfinding " + Clock.getBytecodeNum());
//        --turnsGreedy;
//      }
//    }
  }

  public Direction bestDir(MapLocation target, boolean greedy){
    if (target == null) return null;
    if (!rc.isMovementReady()) return null;
    if (Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(target) == 0) return null;

    update(target);

    if (!greedy && turnsGreedy <= 0){

      //System.err.println("Using bfs");
      Direction dir = getBestDir(target);
      if (dir != null && !mapTracker.check(Cache.PerTurn.CURRENT_LOCATION.add(dir))){
        return exploreMoveDirOnly(dir);
      } else activateGreedy();
    }

//    if (rc.getType() == RobotType.SLANDERER) {
    if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING) {
      //System.err.println("Using greedy");
      ////System.out.println("Before pathfinding " + Clock.getBytecodeNum());
      Direction dir = path.moveDirOnly(target);
      ////System.out.println("After pathfinding " + Clock.getBytecodeNum());
      --turnsGreedy;
      return dir;
    }
//    } else{
//      if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING_NON_SLAND) {
//        //System.err.println("Using greedy");
//        ////System.out.println("Before pathfinding " + Clock.getBytecodeNum());
//        path.move(target);
//        ////System.out.println("After pathfinding " + Clock.getBytecodeNum());
//        --turnsGreedy;
//      }
//    }
    return null;
  }

  private void exploreMove(Direction dir) {
    try{
      if (!rc.canMove(dir)) return;
      rc.move(dir);
//      lastDirMoved = dir;
    } catch (Exception e){
      e.printStackTrace();
    }
  }
  private Direction exploreMoveDirOnly(Direction dir) {
    try{
      if (!rc.canMove(dir)) return null;
//      rc.move(dir);
      return dir;
//      lastDirMoved = dir;
    } catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }
//  }

  abstract Direction getBestDir(MapLocation target);


}