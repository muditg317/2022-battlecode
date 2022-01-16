package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import firstbot.robots.Robot;
import firstbot.utils.Cache;
import firstbot.utils.Utils;

public abstract class Droid extends Robot {

  private static final double HEALTH_FACTOR_TO_RUN_HOME = 0.25;
  private static final double HEALTH_FACTOR_TO_GO_BACK_OUT = 0.9;
  protected MapLocation parentArchonLoc;

  protected MapLocation explorationTarget;
  protected int turnsExploring;
  /** true if the exploration target is set to random location instead of unexplored lands */
  protected boolean exploringRandomly = false;

  protected boolean needToRunHomeForSaving;

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

  @Override
  protected void runTurnTypeWrapper() throws GameActionException {

//    needToRunHomeForSaving = false;
    if (this instanceof Soldier) {
      rc.setIndicatorString(Cache.PerTurn.HEALTH + "/" + Cache.Permanent.MAX_HEALTH);
      if (Cache.PerTurn.HEALTH < Cache.Permanent.MAX_HEALTH * this.HEALTH_FACTOR_TO_RUN_HOME) {
        needToRunHomeForSaving = true;
      }

      if (needToRunHomeForSaving && Cache.PerTurn.HEALTH > Cache.Permanent.MAX_HEALTH * this.HEALTH_FACTOR_TO_GO_BACK_OUT) {
        needToRunHomeForSaving = false;
      }
    }

    runTurn();

    if (needToRunHomeForSaving) {
      runHome(Cache.Permanent.START_LOCATION);
    }
  }

  /**
   * Create lattice structure of slanderers centered around the EC location
   * Potential Bugs:
   *       if two seperate ECs collide slanderers with each other (big problem I think), not sure best way to fix... maybe each slanderer communicates in its flag distance from closest EC and we greedily make it accordingly to closest EC?
   *       if one side of the EC is overproduced and bots can't get further away... is this really a bug tho or a feature? I think feature
   * @return true if moved
   */
  public boolean runHome(MapLocation archonLocation) throws GameActionException {
    if (!Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(archonLocation, Utils.DSQ_3by3plus)) {
      return moveOptimalTowards(archonLocation);
    } else {
      boolean isMyCurrentSquareGood = checkIfGoodSquare(Cache.PerTurn.CURRENT_LOCATION);
      if (isMyCurrentSquareGood) {
        return currentSquareIsGoodExecute(archonLocation);
      } else {
        return currentSquareIsBadExecute(archonLocation);
      }
    }
  }

  /* Behavior =>
    Good Square => not blocking the EC AND an odd distance away
    Bad Square => blocking the EC or an even distance away
  Return: true if and only if the square is good
  */
  private boolean checkIfGoodSquare(MapLocation location) {
    return (location.x % 2 == location.y % 2) && !location.isAdjacentTo(Cache.Permanent.START_LOCATION);
  }

  /* Execute behavior if current square is a "bad" square
   * Behavior: perform a moving action to square in the following priority ->
   *          If there exists a good square that the bot can move to regardless of distance, then move to the one that is closest to the EC
   *          If there exists a bad square that the bot can move to that is further from the EC than the current square, then move to the one that is furthest to the EC
   *          Else => do nothing
   * */
  public boolean currentSquareIsBadExecute(MapLocation archonLocation) throws GameActionException {

    if (!rc.isMovementReady()) return false;

    int badSquareMaximizedDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(archonLocation);
    Direction badSquareMaximizedDirection = null;

    // try to find a good square

    // move further or equal to EC

    int goodSquareMinimizedDistance = (int) 1E9;
    Direction goodSquareMinimizedDirection = null;

    Direction[] directions = Utils.directions;
    for (int i = 0, directionsLength = directions.length; i < directionsLength; i++) {
      Direction direction = directions[i];
      if (rc.canMove(direction)) {
        MapLocation candidateLocation = Cache.PerTurn.CURRENT_LOCATION.add(direction);
        int candidateDistance = candidateLocation.distanceSquaredTo(archonLocation);
        boolean isGoodSquare = checkIfGoodSquare(candidateLocation);

        if (candidateLocation.isAdjacentTo(archonLocation)) continue;

        if (isGoodSquare) {
          if (goodSquareMinimizedDistance > candidateDistance) {
            goodSquareMinimizedDistance = candidateDistance;
            goodSquareMinimizedDirection = direction;
          }
        } else {
          if (badSquareMaximizedDistance <= candidateDistance) {
            badSquareMaximizedDistance = candidateDistance;
            badSquareMaximizedDirection = direction;
          }
        }
      }
    }

    if (goodSquareMinimizedDirection != null) {
      return move(goodSquareMinimizedDirection);
    } else if (badSquareMaximizedDirection != null) {
      return move(badSquareMaximizedDirection);
    }
    return false;
  }

  /* Execute behavior if current square is a "good" square
   * Behavior:
   *           perform a moving action to square if and only if the square is a good square AND it is closer to the EC AND if we are ready
   *           else: do nothing
   * */
  //TODO: check why sometimes we have a bug with the units not moving closer to EC? -- not that big of a deal I think
  public boolean currentSquareIsGoodExecute(MapLocation archonLocation) throws GameActionException {
    // try to move towards EC with any ordinal directions that decreases distance (NE, SE, SW, NW)

    if (!rc.isMovementReady()) return false;

    int moveTowardsDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(archonLocation);
    Direction moveTowardsDirection = null;

    for (Direction direction : Utils.ordinal_directions) {
      if (rc.canMove(direction)) {
        MapLocation candidateLocation = Cache.PerTurn.CURRENT_LOCATION.add(direction);
        int candidateDistance = candidateLocation.distanceSquaredTo(archonLocation);
        boolean isGoodSquare = checkIfGoodSquare(candidateLocation);
        if (isGoodSquare && candidateDistance < moveTowardsDistance) {
          moveTowardsDistance = candidateDistance;
          moveTowardsDirection = direction;
        }
      }
    }

    return moveTowardsDirection != null && move(moveTowardsDirection);
  }

  protected void randomizeExplorationTarget(boolean forceNotSelf) throws GameActionException {
//    int b = Clock.getBytecodeNum();
//    explorationTarget = communicator.chunkInfo.centerOfClosestOptimalChunkForMiners(Cache.PerTurn.CURRENT_LOCATION, forceNotSelf);
//    System.out.println("new target - " + explorationTarget + " - " + (Clock.getBytecodeNum() - b));
//    if (explorationTarget == null) {
      int attempts = 10;
//      rc.setIndicatorString("no unexplored local chunks");
      do {
        explorationTarget = Utils.randomMapLocation();
      } while (--attempts >= 0 && !communicator.chunkInfo.chunkIsGoodForMinerExploration(explorationTarget));
      if (attempts == -1) exploringRandomly = true;
////      if (attempts == 11) failedAttempts++;
////      else failedAttempts = 0;
//    } else {
//      exploringRandomly = false;
//    }
//    explorationTarget = Utils.randomMapLocation();
//    exploringRandomly = true;
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
      rc.setIndicatorString("Cannot reach exploreTarget: " + explorationTarget);
//      System.out.println("Desired direction (from " + Cache.PerTurn.CURRENT_LOCATION + ") (explorationTarget " + explorationTarget + ") is null!!");
      return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, Cache.Permanent.CHUNK_EXPLORATION_RADIUS_SQUARED); // set explorationTarget to null if found!
    }
    boolean changed = false;
    MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(desired);
    int rubbleThere = rc.senseRubble(newLoc);
    int myRubble1p5 = (int) (1.5 * rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION));
    if (((this instanceof Soldier && rubbleThere >= 25 && rubbleThere > myRubble1p5)
    || (this instanceof Miner && rubbleThere >= 50 && rubbleThere > myRubble1p5)
    )) {
      System.out.println("Rubble to high to enter " + explorationTarget + " from " + Cache.PerTurn.CURRENT_LOCATION + " via " + newLoc);
      System.out.println("Rubble: " + rubbleThere);
      System.out.println("times tried: " + timesTriedEnterHighRubble);
      System.out.println("Just go through: " + justGoThrough);
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
        System.out.println("Rubble to high to enter " + explorationTarget + " from " + Cache.PerTurn.CURRENT_LOCATION + " via " + newLoc);
        System.out.println("Rubble: " + rubbleThere);
        System.out.println("times tried: " + timesTriedEnterHighRubble);
        System.out.println("Just go through: " + justGoThrough);
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
        rc.setIndicatorString("Cannot reach exploreTarget: " + explorationTarget);
//      System.out.println("Desired direction (from " + Cache.PerTurn.CURRENT_LOCATION + ") (explorationTarget " + explorationTarget + ") is null!!");
        return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, Cache.Permanent.CHUNK_EXPLORATION_RADIUS_SQUARED); // set explorationTarget to null if found!
      }
    }
    if (move(desired)) {
      rc.setIndicatorString("Approaching explorationTarget" + explorationTarget);
//    moveInDirLoose(goal);
      rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 255, 10, 10);
      rc.setIndicatorDot(explorationTarget, 0, 255, 0);
    }
    return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, Cache.Permanent.CHUNK_EXPLORATION_RADIUS_SQUARED); // set explorationTarget to null if found!
  }

  /**
   * generic exploration process for droids to see the rest of the map
   * @return true if the target was reached & updated
   * @throws GameActionException if exploring/moving fails
   */
  protected boolean doExploration() throws GameActionException {
    if (!exploringRandomly && !communicator.chunkInfo.chunkIsGoodForMinerExploration(explorationTarget)) {
      System.out.printf("TARGET IS BAD\n\tmyLoc:%s\n\ttarget:%s\n\texplRndm:%s\n\tbits:%d\n",Cache.PerTurn.CURRENT_LOCATION,explorationTarget,exploringRandomly,communicator.chunkInfo.chunkInfoBits(Utils.locationToChunkIndex(explorationTarget)));
      randomizeExplorationTarget(true);
      System.out.println("Reset to " + explorationTarget);
      rc.setIndicatorString("bad target... now go to - " + explorationTarget);
    }
    if (goToExplorationTarget()) {
      updateVisibleChunks();
      MapLocation oldTarget = explorationTarget;
      randomizeExplorationTarget(true);
      rc.setIndicatorString("explored " + oldTarget + " -- now: " + explorationTarget);
      timesTriedEnterHighRubble = 0;
      justGoThrough = false;
      return true;
    }
    return false;
  }
}
