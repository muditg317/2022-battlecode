package noarchonsaving.pathfinding;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
    mapTracker.add(rc.getLocation());
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

  private void exploreMove(Direction dir) {
    try{
      if (!rc.canMove(dir)) return;
      rc.move(dir);
//      lastDirMoved = dir;
    } catch (Exception e){
      e.printStackTrace();
    }
  }
//  }

  abstract Direction getBestDir(MapLocation target);


}
