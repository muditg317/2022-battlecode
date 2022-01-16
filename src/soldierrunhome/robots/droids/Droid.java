package soldierrunhome.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import soldierrunhome.robots.Robot;
import soldierrunhome.utils.Cache;
import soldierrunhome.utils.Utils;

public abstract class Droid extends Robot {

  protected MapLocation parentArchonLoc;

  protected MapLocation explorationTarget;
  protected int turnsExploring;
  /** true if the exploration target is set to random location instead of unexplored lands */
  protected boolean exploringRandomly = false;

  public Droid(RobotController rc) throws GameActionException {
    super(rc);
    for (RobotInfo info : rc.senseNearbyRobots(2, Cache.Permanent.OUR_TEAM)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        break;
      }
    }
    randomizeExplorationTarget(false);
  }

  protected void randomizeExplorationTarget(boolean forceNotSelf) throws GameActionException {
//    int b = Clock.getBytecodeNum();
//    explorationTarget = communicator.chunkInfo.centerOfClosestOptimalChunkForMiners(Cache.PerTurn.CURRENT_LOCATION, forceNotSelf);
//    //System.out.println("new target - " + explorationTarget + " - " + (Clock.getBytecodeNum() - b));
//    if (explorationTarget == null) {
//      exploringRandomly = true;
//      int attempts = 10;
//      //rc.setIndicatorString("no unexplored local chunks");
//      do {
//        explorationTarget = Utils.randomMapLocation();
//      } while (--attempts >= 0 && !communicator.chunkInfo.chunkIsGoodForMinerExploration(explorationTarget));
////      if (attempts == 11) failedAttempts++;
////      else failedAttempts = 0;
//    } else {
//      exploringRandomly = false;
//    }
    explorationTarget = Utils.randomMapLocation();
    exploringRandomly = true;
  }

  private int timesTriedEnterHighRubble = 0;
  private boolean justGoThrough = false;
  /**
   * assuming there is a explorationTarget for the miner, approach it
   *    currently very naive -- should use path finding!
   * @return if the miner is within the action radius of the explorationTarget
   * @throws GameActionException if movement or line indication fails
   */
  protected boolean goToExplorationTarget() throws GameActionException {
    if (!rc.isMovementReady()) return false;
    turnsExploring++;
//    Direction goal = Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget);
    Direction desired = getOptimalDirectionTowards(explorationTarget);
    if (desired == null) desired = getLeastRubbleDirAroundDir(Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget));
    if (desired == null) {
      //rc.setIndicatorString("Cannot reach exploreTarget: " + explorationTarget);
//      //System.out.println("Desired direction (from " + Cache.PerTurn.CURRENT_LOCATION + ") (explorationTarget " + explorationTarget + ") is null!!");
      return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, Cache.Permanent.CHUNK_EXPLORATION_RADIUS_SQUARED); // set explorationTarget to null if found!
    }
    boolean changed = false;
    MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(desired);
    int rubbleThere = rc.senseRubble(newLoc);
    int myRubble1p5 = (int) (1.5 * rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION));
    if (((this instanceof Soldier && rubbleThere >= 25 && rubbleThere > myRubble1p5)
    || (this instanceof Miner && rubbleThere >= 50 && rubbleThere > myRubble1p5)
    )) {
      //System.out.println("Rubble to high to enter " + explorationTarget + " from " + Cache.PerTurn.CURRENT_LOCATION + " via " + newLoc);
      //System.out.println("Rubble: " + rubbleThere);
      //System.out.println("times tried: " + timesTriedEnterHighRubble);
      //System.out.println("Just go through: " + justGoThrough);
      timesTriedEnterHighRubble++;
      if (timesTriedEnterHighRubble < 3) {
        randomizeExplorationTarget(true);
        changed = true;
      } else {
        justGoThrough = true;
      }
    } else {
      MapLocation directPathLoc = Cache.PerTurn.CURRENT_LOCATION.add(Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget));
      MapLocation directFromDesiredLoc = newLoc.add(newLoc.directionTo(explorationTarget));
      int rubbleDirect = rc.senseRubble(directPathLoc);
      int rubbleDesired = rc.senseRubble(directFromDesiredLoc);

      if (((this instanceof Soldier && rubbleDirect >= 25 && rubbleDirect > myRubble1p5)
          || (this instanceof Miner && rubbleDirect >= 50 && rubbleDirect > myRubble1p5)
      ) && ((this instanceof Soldier && rubbleDesired >= 25 && rubbleDesired > myRubble1p5)
          || (this instanceof Miner && rubbleDesired >= 50 && rubbleDesired > myRubble1p5)
      )) {
        //System.out.println("Rubble to high to enter " + explorationTarget + " from " + Cache.PerTurn.CURRENT_LOCATION + " via " + newLoc);
        //System.out.println("Rubble: " + rubbleThere);
        //System.out.println("times tried: " + timesTriedEnterHighRubble);
        //System.out.println("Just go through: " + justGoThrough);
        timesTriedEnterHighRubble++;
        if (timesTriedEnterHighRubble < 3) {
          randomizeExplorationTarget(true);
          changed = true;
        } else {
          justGoThrough = true;
        }
      } else if (justGoThrough) {
        timesTriedEnterHighRubble = 0;
        justGoThrough = false;
      }
    }
    if (changed) {
      desired = getOptimalDirectionTowards(explorationTarget);
      if (desired == null) desired = getLeastRubbleDirAroundDir(Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget));
      if (desired == null) {
        //rc.setIndicatorString("Cannot reach exploreTarget: " + explorationTarget);
//      //System.out.println("Desired direction (from " + Cache.PerTurn.CURRENT_LOCATION + ") (explorationTarget " + explorationTarget + ") is null!!");
        return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, Cache.Permanent.CHUNK_EXPLORATION_RADIUS_SQUARED); // set explorationTarget to null if found!
      }
    }
    if (move(desired)) {
      //rc.setIndicatorString("Approaching explorationTarget" + explorationTarget);
//    moveInDirLoose(goal);
      //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 255, 10, 10);
      //rc.setIndicatorDot(explorationTarget, 0, 255, 0);
    }
    return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, Cache.Permanent.CHUNK_EXPLORATION_RADIUS_SQUARED); // set explorationTarget to null if found!
  }

  /**
   * generic exploration process for droids to see the rest of the map
   * @return true if the target was reached & updated
   * @throws GameActionException if exploring/moving fails
   */
  protected boolean doExploration() throws GameActionException {
//    if (!exploringRandomly && !communicator.chunkInfo.chunkIsGoodForMinerExploration(explorationTarget)) {
//      //System.out.printf("TARGET IS BAD\n\tmyLoc:%s\n\ttarget:%s\n\texplRndm:%s\n\tbits:%d\n",Cache.PerTurn.CURRENT_LOCATION,explorationTarget,exploringRandomly,communicator.chunkInfo.chunkInfoBits(Utils.locationToChunkIndex(explorationTarget)));
//      randomizeExplorationTarget(true);
//      //System.out.println("Reset to " + explorationTarget);
//      //rc.setIndicatorString("bad target... now go to - " + explorationTarget);
//    }
    if (goToExplorationTarget()) {
//      updateVisibleChunks();
      MapLocation oldTarget = explorationTarget;
      randomizeExplorationTarget(true);
      //rc.setIndicatorString("explored " + oldTarget + " -- now: " + explorationTarget);
      timesTriedEnterHighRubble = 0;
      justGoThrough = false;
      return true;
    }
    return false;
  }
}
