package chunking.robots.droids;

import battlecode.common.*;
import chunking.robots.Robot;
import chunking.utils.Cache;
import chunking.utils.Utils;

public abstract class Droid extends Robot {

  protected MapLocation parentArchonLoc;

  protected MapLocation explorationTarget;
  protected int turnsExploring;

  public Droid(RobotController rc) throws GameActionException {
    super(rc);
    for (RobotInfo info : rc.senseNearbyRobots(2, Cache.Permanent.OUR_TEAM)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        break;
      }
    }
    randomizeExplorationTarget();
  }

  protected void randomizeExplorationTarget() throws GameActionException {
//    int b = Clock.getBytecodeNum();
    explorationTarget = communicator.chunkInfo.centerOfClosestUnexploredChunk(Cache.PerTurn.CURRENT_LOCATION);
//    //System.out.println("randomizeExplorationTarget - " + explorationTarget + " - " + (Clock.getBytecodeNum() - b));
    if (explorationTarget == null) {
      int attempts = 0;
      //rc.setIndicatorString("no unexplored local chunks");
      do {
        explorationTarget = Utils.randomMapLocation();
      } while (++attempts <= 10 && communicator.chunkInfo.chunkHasBeenExplored(explorationTarget));
//      if (attempts == 11) failedAttempts++;
//      else failedAttempts = 0;
    }
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
    MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(desired);
    int rubbleThere = rc.senseRubble(newLoc);
    if ((this instanceof Soldier && rubbleThere >= 25 && rubbleThere > 1.5 * rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION))
//    || (this instanceof Miner && rubbleThere >= 50 && rubbleThere > 1.5 * rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION))
    ) {
      timesTriedEnterHighRubble++;
      if (timesTriedEnterHighRubble < 3) {
        randomizeExplorationTarget();
      } else {
        justGoThrough = true;
      }
    } else if (justGoThrough) {
      timesTriedEnterHighRubble = 0;
      justGoThrough = false;
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
    //rc.setIndicatorString("doExploration - " + explorationTarget);
    if (communicator.chunkInfo.chunkHasBeenExplored(explorationTarget)) {
      randomizeExplorationTarget();
    }
    if (goToExplorationTarget()) {
      communicator.chunkInfo.markExplored(explorationTarget);
      randomizeExplorationTarget();
      return true;
    }
    return false;
  }
}
