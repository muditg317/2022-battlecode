package firstbot.robots;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import firstbot.Utils;
import firstbot.communications.Communicator;
import firstbot.robots.buildings.Archon;
import firstbot.robots.buildings.Laboratory;
import firstbot.robots.buildings.Watchtower;
import firstbot.robots.droids.Builder;
import firstbot.robots.droids.Miner;
import firstbot.robots.droids.Sage;
import firstbot.robots.droids.Soldier;

import java.util.Arrays;

public abstract class Robot {
  private static final boolean RESIGN_ON_GAME_EXCEPTION = true;
  private static final boolean RESIGN_ON_RUNTIME_EXCEPTION = true;

  protected static class CreationStats {
    public final int roundNum;
    public final MapLocation spawnLocation;
    private final int health;
    private final RobotType type;

    public CreationStats(RobotController rc) {
      this.roundNum = rc.getRoundNum();
      this.spawnLocation = rc.getLocation();
      this.health = rc.getHealth();
      this.type = rc.getType();
    }

    @Override
    public String toString() {
      return String.format("%s at %s - HP: %4d", type, spawnLocation, health);
    }
  }

  protected final RobotController rc;
  protected final Communicator communicator;
  protected int pendingMessages;

  protected final CreationStats creationStats;


  /**
   * Create a Robot with the given controller
   * Perform various setup tasks generic to ny robot (building/droid)
   * @param rc the controller
   */
  public Robot(RobotController rc) {
    this.rc = rc;
    this.communicator = new Communicator(rc);

    this.creationStats = new CreationStats(rc);

    // Print spawn message
    System.out.println(this.creationStats);
    // Set indicator message
    rc.setIndicatorString("Just spawned!");
  }

  /**
   * Create a Robot-subclass instance from the provided controller
   * @param rc the controller object
   * @return a custom Robot instance
   */
  public static Robot fromRC(RobotController rc) {
    switch (rc.getType()) {
      case ARCHON:     return new Archon(rc);
      case LABORATORY: return new Laboratory(rc);
      case WATCHTOWER: return new Watchtower(rc);
      case MINER:      return new Miner(rc);
      case BUILDER:    return new Builder(rc);
      case SOLDIER:    return new Soldier(rc);
      case SAGE:       return new Sage(rc);
      default:         throw  new RuntimeException("Cannot create Robot-subclass for invalid RobotType: " + rc.getType());
    }
  }

  /**
   * Run the wrapper for the main loop function of the robot
   * This is generic to all robots
   */
  public void runLoop() {
    /*
      should never exit - Robot will die otherwise (Clock.yield() to end turn)
     */
    while (true) {
      // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
      try {
        this.runTurnWrapper();
      } catch (GameActionException e) {
        // something illegal in the Battlecode world
        System.out.println(rc.getType() + " GameActionException");
        e.printStackTrace();
        if (RESIGN_ON_GAME_EXCEPTION) rc.resign();
      } catch (Exception e) {
        // something bad
        System.out.println(rc.getType() + " Exception");
        e.printStackTrace();
        if (RESIGN_ON_GAME_EXCEPTION || RESIGN_ON_RUNTIME_EXCEPTION) rc.resign();
      } finally {
        // end turn - make code wait until next turn
        Clock.yield();
      }
    }
  }

  /**
   * wrap intern run turn method with generic actions for all robots
   */
  private void runTurnWrapper() throws GameActionException {
//      System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

    pendingMessages = communicator.readMessages();
    if (pendingMessages > 0) System.out.println("Got " + pendingMessages + " messages!");
    runTurn();
    communicator.sendQueuedMessages();
    communicator.updateMetaIntsIfNeeded();
  }

  /**
   * Run a single turn for the robot
   * unique to each robot type
   */
  protected abstract void runTurn() throws GameActionException;

  /**
   * if the robot can move, choose a random direction and move
   * will try 16 times in case some directions are blocked
   * @throws GameActionException if movement fails
   */
  protected void moveRandomly() throws GameActionException {
    if (rc.isMovementReady()) {
      int failedTries = 0;
      Direction dir;
      do {
        dir = Utils.randomDirection();
      } while (!rc.canMove(dir) && ++failedTries < 16);
      if (failedTries < 16) { // only move if we didnt fail 16 times and never find a valid direction to move
        rc.move(dir);
      }
    }
  }

  /**
   * sense all around the robot for lead, choose a direction probabilistically
   *    weighted by how much lead is reached by moving in that direction
   *    IGNORES 0-1 Pb
   * @return the most lead-full direction
   * @throws GameActionException if some game op fails
   */
  protected Direction getBestLeadDirProbabilistic() throws GameActionException {
    int[] leadInDirection = new int[Utils.directions.length];
    int totalSeen = 0;
    MapLocation myLoc = rc.getLocation();
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(myLoc, rc.getType().visionRadiusSquared)) {
      boolean isNorth = loc.y >= myLoc.y;
      boolean isEast = loc.x >= myLoc.x;
      int leadSeen = rc.senseLead(loc); // don't check canSense because we know it is valid and in range
      if (leadSeen <= 1) { // ignore 0-1 Pb
        continue;
      }
      totalSeen += leadSeen;
      if (isNorth) {
        // check if location is open before adding it to weighting
        // if (!rc.isLocationOccupied(rc.adjacentLocation(Direction.NORTH))) {
        leadInDirection[Direction.NORTH.ordinal()] += leadSeen;
        if (isEast) {
          leadInDirection[Direction.EAST.ordinal()] += leadSeen;
          leadInDirection[Direction.NORTHEAST.ordinal()] += leadSeen;
        } else {
          leadInDirection[Direction.WEST.ordinal()] += leadSeen;
          leadInDirection[Direction.NORTHWEST.ordinal()] += leadSeen;
        }
      } else {
        leadInDirection[Direction.SOUTH.ordinal()] += leadSeen;
        if (isEast) {
          leadInDirection[Direction.EAST.ordinal()] += leadSeen;
          leadInDirection[Direction.SOUTHEAST.ordinal()] += leadSeen;
        } else {
          leadInDirection[Direction.WEST.ordinal()] += leadSeen;
          leadInDirection[Direction.SOUTHWEST.ordinal()] += leadSeen;
        }
      }
    }
    totalSeen *= 3; // each location affects 3 direction entries
    if (totalSeen == 0) {
      return Utils.randomDirection();
    }
    int randomInt = Utils.rng.nextInt(totalSeen);
    for (int i = 0; i < leadInDirection.length; i++) {
      if (randomInt <= leadInDirection[i]) return Utils.directions[i];
      randomInt -= leadInDirection[i];
    }
    System.out.println("WEIGHTED PICK FAILED: " + Arrays.toString(leadInDirection));
    throw new RuntimeException("Weighted sum should be able to choose one a direction");
  }

  protected void moveToHighLeadProbabilistic() throws GameActionException {
    Direction dir = getBestLeadDirProbabilistic();
    if (rc.canMove(dir)) rc.move(dir);
  }

  /**
   * build the specified robot type in the specified direction
   * @param type the robot type to build
   * @param dir where to build it
   * @return method success
   * @throws GameActionException when building fails
   */
  protected boolean buildRobot(RobotType type, Direction dir) throws GameActionException {
    if (rc.canBuildRobot(type, dir)) {
      rc.buildRobot(type, dir);
      return true;
    }
    return false;
  }

  /**
   * returns the number of rounds since the last anomaly of a certain type
   * @param type the anomaly to look for
   * @return the turns since occurence (or roundNum if never occurred)
   */
  protected int getRoundsSinceLastAnomaly(AnomalyType type) {
    int turnsSince = rc.getRoundNum();
    for (AnomalyScheduleEntry anomaly : rc.getAnomalySchedule()) {
      if (anomaly.anomalyType == type) turnsSince = rc.getRoundNum() - anomaly.roundNumber;
      if (anomaly.roundNumber >= rc.getRoundNum()) return turnsSince;
    }
    return turnsSince;
  }
}
