package firstbot.robots.droids;

import battlecode.common.*;
import firstbot.robots.Robot;
import firstbot.utils.Cache;
import firstbot.utils.Printer;
import firstbot.utils.Utils;

public abstract class Droid extends Robot {

  private static final double DISTANCE_FACTOR_TO_RUN_HOME = 2;
  private static final double HEALTH_FACTOR_TO_RUN_HOME = 0.4;
  private static double HEALTH_FACTOR_TO_GO_BACK_OUT = 0.95;
  private static final double HEALTH_FACTOR_TO_SUICIDE_SOLDIER = 0.07;
  private static final double HEALTH_FACTOR_TO_SUICIDE_OTHER = 0.1;
  private static final double HEALTH_FACTOR_TO_CANCEL_SUICIDE = 0.2;

  protected final int EXPLORATION_REACHED_RADIUS;

  protected MapLocation parentArchonLoc;

  protected MapLocation explorationTarget;
  protected int turnsExploring;
  /** true if the exploration target is set to random location instead of unexplored lands */
  protected boolean exploringRandomly = false;

  protected boolean needToRunHomeForSaving;
  protected boolean needToRunHomeForSuicide;
  protected boolean isMovementDisabled;
  protected boolean leaveArchon;
  protected int leaveArchonRound;

  public Droid(RobotController rc) throws GameActionException {
    super(rc);
    for (RobotInfo info : rc.senseNearbyRobots(Utils.DSQ_1by1, Cache.Permanent.OUR_TEAM)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        break;
      }
    }
    if (parentArchonLoc == null) {
      parentArchonLoc = Cache.Permanent.START_LOCATION;
    }
    randomizeExplorationTarget(false);
    EXPLORATION_REACHED_RADIUS = Cache.Permanent.ACTION_RADIUS_SQUARED;
  }

  @Override
  protected void runTurnTypeWrapper() throws GameActionException {

//    needToRunHomeForSaving = false;
    int distance = Utils.maxSingleAxisDist(Cache.PerTurn.CURRENT_LOCATION, communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION));

    if (this instanceof Soldier) {
//      rc.setIndicatorString(Cache.PerTurn.HEALTH + "/" + Cache.Permanent.MAX_HEALTH);

      // go back if health is less than half, unless you are closer to the archon already (then its equal to distance from archon?)
      if (Cache.PerTurn.HEALTH < Math.min(4 + distance * DISTANCE_FACTOR_TO_RUN_HOME, Cache.Permanent.MAX_HEALTH * HEALTH_FACTOR_TO_RUN_HOME)) {
        needToRunHomeForSaving = true;
      }

      // once suicidal, always suicidal (no matter what) :)
      if (Cache.PerTurn.HEALTH < Cache.Permanent.MAX_HEALTH * HEALTH_FACTOR_TO_SUICIDE_SOLDIER) {
        needToRunHomeForSaving = true;
        needToRunHomeForSuicide = true;
      }

    } else {
      // once suicidal, always suicidal (no matter what) :)
      if (Cache.PerTurn.HEALTH < Cache.Permanent.MAX_HEALTH * HEALTH_FACTOR_TO_SUICIDE_OTHER) {
        needToRunHomeForSaving = true;
        needToRunHomeForSuicide = true;
      }
    }

    if (Cache.PerTurn.HEALTH > Cache.Permanent.MAX_HEALTH * HEALTH_FACTOR_TO_GO_BACK_OUT) {
      needToRunHomeForSaving = false;
    }

    if (Cache.PerTurn.HEALTH > Cache.Permanent.MAX_HEALTH * HEALTH_FACTOR_TO_CANCEL_SUICIDE) {
      needToRunHomeForSuicide = false;
    }

    if (leaveArchon && Cache.PerTurn.ROUND_NUM - leaveArchonRound <= 150) {
      needToRunHomeForSaving = false;
    } else {
      leaveArchon = false;
    }

    isMovementDisabled = (needToRunHomeForSaving || needToRunHomeForSuicide) && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), RobotType.ARCHON.actionRadiusSquared/2);

//    Printer.print("needToRunHomeForSaving: " + needToRunHomeForSaving, "needToRunHomeForSuicide: " + needToRunHomeForSuicide);
//    Printer.print("parentArchonLoc: " + parentArchonLoc, "distance: " + distance);
    runTurn();
//    Printer.print("aCD: " + rc.getActionCooldownTurns(), "mCD: " + rc.getMovementCooldownTurns());
    if (needToRunHomeForSaving || needToRunHomeForSuicide) {
      MapLocation whereToRun = communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION);
      whereToRun = checkMovingArchonToRunTowards(whereToRun);
      runHome(whereToRun);
    }

  }

  protected MapLocation checkMovingArchonToRunTowards(MapLocation currentTarget) {
    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      if (friend.type == RobotType.ARCHON && friend.mode == RobotMode.PORTABLE) return friend.location;
    }
    return currentTarget;
  }

  /**
   * Create lattice structure of slanderers centered around the EC location
   * Potential Bugs:
   *       if two seperate ECs collide slanderers with each other (big problem I think), not sure best way to fix... maybe each slanderer communicates in its flag distance from closest EC and we greedily make it accordingly to closest EC?
   *       if one side of the EC is overproduced and bots can't get further away... is this really a bug tho or a feature? I think feature
   * @return true if moved
   */
  public boolean runHome(MapLocation archonLocation) throws GameActionException {
    if (rc.canSenseRobotAtLocation(archonLocation) && rc.senseRobotAtLocation(archonLocation).type != RobotType.ARCHON) {
      Printer.print("ERROR: archonLocation is not an archon " + archonLocation, "archon probably dead, replacing wit nearest...");
      archonLocation = communicator.archonInfo.replaceOurArchon(archonLocation);
    }
//    Printer.print("RUNNING runHome():", "archonLocation: " + archonLocation);
    //todo: run home better (more smart, avoid enemies)
    if (!Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(archonLocation, Utils.DSQ_3by3plus)) {
      if (USE_STOLEN_BFS) {
        return moveBFS(archonLocation);
      }
      return moveOptimalTowards(archonLocation);
    } else {
      if (needToRunHomeForSuicide && rc.senseLead(Cache.PerTurn.CURRENT_LOCATION) == 0) rc.disintegrate();

      if (!rc.isMovementReady()) return false;

      boolean shouldLeave = checkIfTooManySoldiers(archonLocation);
      if (shouldLeave) {
        leaveArchon = true;
        leaveArchonRound = Cache.PerTurn.ROUND_NUM;
      }

      boolean isMyCurrentSquareGood = checkIfGoodSquare(Cache.PerTurn.CURRENT_LOCATION);
      if (isMyCurrentSquareGood) {
        return currentSquareIsGoodExecute(archonLocation);
      } else {
        return currentSquareIsBadExecute(archonLocation);
      }
    }
  }

  public boolean checkIfTooManySoldiers(MapLocation archonLocation) throws GameActionException {
    int myHealth = Cache.PerTurn.HEALTH;
    int soldierCount = 1;
    boolean highestHealthSoldier = true;


    for (RobotInfo info : rc.senseNearbyRobots(archonLocation, RobotType.ARCHON.visionRadiusSquared, Cache.Permanent.OUR_TEAM)) {
      if (info.type == RobotType.SOLDIER && info.health < 49) { //only count soldiers that are not at max health - 1
        ++soldierCount;
        if (myHealth < info.health) { // if I am not the highest health soldier
          highestHealthSoldier = false;
        }
      }
    }


    switch (soldierCount) {
      case 1:
      case 2:
        HEALTH_FACTOR_TO_GO_BACK_OUT = 0.95;
        break;
      case 3:
      case 4:
      case 5:
        HEALTH_FACTOR_TO_GO_BACK_OUT = 0.85;
        break;
      default:
        HEALTH_FACTOR_TO_GO_BACK_OUT = 0.75;
    }

    return soldierCount >= 10 && highestHealthSoldier;
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
//    Printer.print("RUNNING randomizeExplorationTarget(): ");
    switch (Cache.Permanent.ROBOT_TYPE) {
      case MINER:
        if (Utils.rng.nextInt(5)<2) {
//          MapLocation oldTarget = explorationTarget;
//          int tries = 10;
//          do {
          int xRand = Utils.rng.nextInt(4);
          int yRand = Utils.rng.nextInt(4);
            explorationTarget = new MapLocation(
                1 + (xRand == 3 ? 1 : xRand)*(Cache.Permanent.MAP_WIDTH/2-1),
                1 + (yRand == 3 ? 1 : yRand)*(Cache.Permanent.MAP_HEIGHT/2-1)
            );
//          } while (--tries > 0 && explorationTarget.equals(oldTarget));
        } else {
          explorationTarget = Utils.randomMapLocation();
        }
        break;
      case SOLDIER:
      case SAGE:
        explorationTarget = communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION);
        if (rc.canSenseRobotAtLocation(explorationTarget) && rc.senseRobotAtLocation(explorationTarget).type != RobotType.ARCHON) {
          Printer.print("ERROR: archonLocation is not an archon " + explorationTarget, "archon probably dead, replacing with nearest...");
          explorationTarget = communicator.archonInfo.replaceEnemyArchon(explorationTarget);
        }
        if (((Soldier)this).checkNeedToStayOnSafeSide()) {
          MapLocation friendly = communicator.archonInfo.getNearestFriendlyArchon(explorationTarget);
          MapLocation enemy = communicator.archonInfo.getNearestEnemyArchon(explorationTarget);
          Direction backHome = enemy.directionTo(friendly);
          int tries = 20;
          while (tries-- > 0 && friendly.distanceSquaredTo(explorationTarget) > enemy.distanceSquaredTo(explorationTarget)) {
            explorationTarget = explorationTarget.add(backHome);
          }
//      if (tries < 19) return;
          tries = 20;
          backHome = backHome.opposite();
          while (tries-- > 0 && friendly.distanceSquaredTo(explorationTarget) < enemy.distanceSquaredTo(explorationTarget)) {
            explorationTarget = explorationTarget.add(backHome);
          }
        }
        break;
      default:
        explorationTarget = Utils.randomMapLocation();
        break;
    }
    exploringRandomly = true;
  }

  private final int RUBBLY_EXPLORATIONS_BEFORE_GO_THROUGH = this instanceof Miner ? 0 : 5;
  private int timesTriedEnterHighRubble = 0;
  private boolean justGoThrough = false;
  /**
   * assuming there is a explorationTarget for the droid, approach it
   * @return if the droid has reached (actionRadius) the explorationTarget
   * @throws GameActionException if movement or line indication fails
   */
  protected boolean goToExplorationTarget() throws GameActionException {
    if (!rc.isMovementReady()) {
      if (explorationTarget != null) {
        rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 255, 0, 0);
      }
    }
    if (USE_STOLEN_BFS && Cache.PerTurn.ROUNDS_ALIVE >= 1) {
      moveBFS(explorationTarget);
      return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, EXPLORATION_REACHED_RADIUS);
    }
    turnsExploring++;
    if (Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, EXPLORATION_REACHED_RADIUS)) return true;
//    Direction goal = Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget);
    Direction desired = getOptimalDirectionTowards(explorationTarget);
    if (desired == null) desired = getLeastRubbleMoveableDirAroundDir(Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget));
    if (desired == null) {
      rc.setIndicatorString("Cannot reach exploreTarget: " + explorationTarget);
//      System.out.println("Desired direction (from " + Cache.PerTurn.CURRENT_LOCATION + ") (explorationTarget " + explorationTarget + ") is null!!");
      return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, EXPLORATION_REACHED_RADIUS);
    }
    boolean changed = checkTooMuchRubbleOnPathToExploration(desired);
    if (changed) {
      desired = getOptimalDirectionTowards(explorationTarget);
      if (desired == null) desired = getLeastRubbleMoveableDirAroundDir(Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget));
      if (desired == null) {
        rc.setIndicatorString("Cannot reach new (changed for lead) exploreTarget: " + explorationTarget);
//      System.out.println("Desired direction (from " + Cache.PerTurn.CURRENT_LOCATION + ") (explorationTarget " + explorationTarget + ") is null!!");
        return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, EXPLORATION_REACHED_RADIUS);
      }
    }
    if (this instanceof Soldier && ((Soldier) this).checkNeedToStayOnSafeSide()) {
      if (Utils.tooCloseToEnemyArchon(Cache.PerTurn.CURRENT_LOCATION) && Utils.tooCloseToEnemyArchon(Cache.PerTurn.CURRENT_LOCATION.add(desired))) {
        return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, EXPLORATION_REACHED_RADIUS);
      }
    }
    if (move(desired)) {
      rc.setIndicatorString("Approaching explorationTarget" + explorationTarget);
//    moveInDirLoose(goal);
//      rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 255, 10, 10);
    }
//    if (explorationTarget != null) {
      rc.setIndicatorDot(explorationTarget, 0, 255, 0);
//      if (communicator.chunkInfo.chunkHasDanger(Utils.locationToChunkIndex(explorationTarget))) {
        rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 255, 0, 0);
//      } else if (communicator.chunkInfo.chunkHasPassiveUnits(Utils.locationToChunkIndex(explorationTarget))) {
//        rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 0, 255, 0);
//      } else {
//        rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, explorationTarget, 0, 0, 255);
//      }
//    }
    return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(explorationTarget, EXPLORATION_REACHED_RADIUS); // set explorationTarget to null if found!
  }

  /**
   *
   * @param desired
   * @return if target was changed
   * @throws GameActionException
   */
  protected boolean checkTooMuchRubbleOnPathToExploration(Direction desired) throws GameActionException {
    if (!exploringRandomly) return false;
    MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(desired);
    int rubbleThere = rc.senseRubble(newLoc);
    int myRubble1p5 = (int) (1.5 * rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION));
    if (((this instanceof Soldier && rubbleThere >= 25 && rubbleThere > myRubble1p5)
            || (this instanceof Miner && rubbleThere >= 50 && rubbleThere > myRubble1p5)
    )) {
//      System.out.println("Rubble too high to enter " + explorationTarget + " from " + Cache.PerTurn.CURRENT_LOCATION + " via " + newLoc);
//      System.out.println("Rubble: " + rubbleThere);
//      System.out.println("times tried already: " + timesTriedEnterHighRubble);
//      System.out.println("Just go through: " + justGoThrough);
      timesTriedEnterHighRubble++;
      if (timesTriedEnterHighRubble < RUBBLY_EXPLORATIONS_BEFORE_GO_THROUGH) {
        randomizeExplorationTarget(true);
        return true;
      } else {
        justGoThrough = true;
      }
    } else {
//      MapLocation directPathLoc = Cache.PerTurn.CURRENT_LOCATION.add(Cache.PerTurn.CURRENT_LOCATION.directionTo(explorationTarget));
//      if (!rc.onTheMap(directPathLoc)) return false;
//      MapLocation directFromDesiredLoc = newLoc.add(newLoc.directionTo(explorationTarget));
//      if (!rc.onTheMap(directFromDesiredLoc)) return false;
//
//      int rubbleDirect = rc.senseRubble(directPathLoc);
//      int rubbleDesired = rc.senseRubble(directFromDesiredLoc);
//
//      if (((this instanceof Soldier && rubbleDirect >= 25 && rubbleDirect > myRubble1p5)
//              || (this instanceof Miner && rubbleDirect >= 50 && rubbleDirect > myRubble1p5)
//      ) && ((this instanceof Soldier && rubbleDesired >= 25 && rubbleDesired > myRubble1p5)
//              || (this instanceof Miner && rubbleDesired >= 50 && rubbleDesired > myRubble1p5)
//      )) {
//        System.out.println("Rubble to high to enter " + explorationTarget + " from " + Cache.PerTurn.CURRENT_LOCATION + " via " + newLoc);
//        System.out.println("Rubble: " + rubbleThere);
//        System.out.println("times tried: " + timesTriedEnterHighRubble);
//        System.out.println("Just go through: " + justGoThrough);
//        timesTriedEnterHighRubble++;
//        if (timesTriedEnterHighRubble < RUBBLY_EXPLORATIONS_BEFORE_GO_THROUGH) {
//          randomizeExplorationTarget(true);
//          return true;
//        } else {
//          justGoThrough = true;
//        }
//      } else if (justGoThrough) {
//        timesTriedEnterHighRubble = 0;
//        justGoThrough = false;
//      }
    }
    return false;
  }

  /**
   * generic exploration process for droids to see the rest of the map
   * @return true if the target was reached & updated
   * @throws GameActionException if exploring/moving fails
   */
  protected boolean doExploration() throws GameActionException {
//    Printer.print("RUNNING doExploration(): ", "old explorationTarget: " + explorationTarget);
    // if we are explorating smartly and the chunk has been explored already
//    System.out.println("  " + Cache.PerTurn.CURRENT_LOCATION + " - \nexploringRandomly: " + exploringRandomly + "\nExploration target: " + explorationTarget + "\nalready explored: " + !communicator.chunkInfo.chunkIsGoodForMinerExploration(explorationTarget));
//    Printer.print("explorationTarget: " + explorationTarget);
    if (goToExplorationTarget()) {
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
