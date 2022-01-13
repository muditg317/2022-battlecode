package spreadtowers.robots;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import spreadtowers.utils.Cache;
import spreadtowers.utils.Global;
import spreadtowers.utils.Utils;
import spreadtowers.communications.Communicator;
import spreadtowers.communications.messages.Message;
import spreadtowers.robots.buildings.Archon;
import spreadtowers.robots.buildings.Laboratory;
import spreadtowers.robots.buildings.Watchtower;
import spreadtowers.robots.droids.Builder;
import spreadtowers.robots.droids.Miner;
import spreadtowers.robots.droids.Sage;
import spreadtowers.robots.droids.Soldier;

public abstract class Robot {
  private static final boolean RESIGN_ON_GAME_EXCEPTION = false;
  private static final boolean RESIGN_ON_RUNTIME_EXCEPTION = false;

  protected final RobotController rc;
  protected final Communicator communicator;
  protected int pendingMessages;

//  protected final StolenBFS2 stolenbfs;

  /**
   * Create a Robot with the given controller
   * Perform various setup tasks generic to ny robot (building/droid)
   * @param rc the controller
   */
  public Robot(RobotController rc) throws GameActionException {
    Global.setupGlobals(rc);
    Utils.setUpStatics(rc);
    Cache.setup(rc);
    this.rc = rc;
    this.communicator = Global.communicator;

//    this.stolenbfs = new StolenBFS2(rc);
    // Print spawn message
//    //System.out.println(this.creationStats);
    // Set indicator message
    rc.setIndicatorString("Just spawned!");
  }

  /**
   * Create a Robot-subclass instance from the provided controller
   * @param rc the controller object
   * @return a custom Robot instance
   */
  public static Robot fromRC(RobotController rc) throws GameActionException {
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
        //System.out.println(rc.getType() + " GameActionException");
        e.printStackTrace();
        //rc.setIndicatorDot(rc.getLocation(), 255,255,255);
        if (RESIGN_ON_GAME_EXCEPTION) rc.resign();
      } catch (Exception e) {
        // something bad
        //System.out.println(rc.getType() + " Exception");
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
//      //System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());
//    stolenbfs.initTurn();
    Cache.updateOnTurn();
//    communicator.cleanStaleMessages();
    Utils.startByteCodeCounting("reading");
    pendingMessages = communicator.readMessages();
    while (pendingMessages > 0) {
      Message message = communicator.getNthLastReceivedMessage(pendingMessages);
      ackMessage(message);
      pendingMessages--;
    }
    Utils.finishByteCodeCounting("reading");
//    if (pendingMessages > 0) //System.out.println("Got " + pendingMessages + " messages!");
    runTurn();

    Utils.startByteCodeCounting("sending");
    communicator.sendQueuedMessages();
    communicator.updateMetaIntsIfNeeded();
    Utils.finishByteCodeCounting("sending");
  }

  /**
   * acknowledge the provided message (happens at turn start)
   * @param message the message received
   */
  protected void ackMessage(Message message) throws GameActionException {}

  /**
   * Run a single turn for the robot
   * unique to each robot type
   */
  protected abstract void runTurn() throws GameActionException;

  /**
   * Wrapper for move() of RobotController that ensures enough bytecodes
   * @param dir where to move
   * @return if the robot moved
   * @throws GameActionException if movement failed
   */
  protected boolean move(Direction dir) throws GameActionException {
    if (Clock.getBytecodesLeft() > 11 && rc.canMove(dir)) {
      rc.move(dir);
      return true;
    }
    return false;
  }

  /**
   * if the robot can move, choose a random direction and move
   * will try 16 times in case some directions are blocked
   * @return if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveRandomly() throws GameActionException {
    return move(Utils.randomDirection()) || move(Utils.randomDirection()); // try twice in case many blocked locs
//    if (rc.isMovementReady()) {
//      int failedTries = 0;
//      Direction dir;
//      do {
//        dir = Utils.randomDirection();
//      } while (!rc.canMove(dir) && ++failedTries < 16);
//      if (failedTries < 16) { // only move if we didnt fail 16 times and never find a valid direction to move
//        rc.move(dir);
//      }
//    }
  }

  /**
   * move in this direction or an adjacent direction if can't move
   * @param dir the direction to move in
   * @return if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveInDirLoose(Direction dir) throws GameActionException {
    return move(dir) || move(dir.rotateLeft()) || move(dir.rotateRight());
  }

  /**
   * move randomly in this general direction
   * @param dir the direction to move in
   * @return if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveInDirRandom(Direction dir) throws GameActionException {
    return move(Utils.randomSimilarDirectionPrefer(dir)) || move(Utils.randomSimilarDirection(dir));
  }

  /**
   * move towards the given target and avoid rubble naively
   * @param target the location to move towards
   * @return if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveTowardsAvoidRubble(MapLocation target) throws GameActionException {
    if (!rc.isMovementReady()) return false;
//    if (USE_STOLEN_BFS) {
//      stolenbfs.move(target, false);
//      if (!rc.isMovementReady()) return true;
//    }

    int bestPosDirInd = -1;
    int bestPosRubble = 101;
    int bestPosDist = -1;
    MapLocation myLoc = rc.getLocation();
    int dToLoc = myLoc.distanceSquaredTo(target);

    MapLocation newLoc; // temp
    int newLocDist; // temp
    for (int i = 0; i < Utils.directions.length; i++) {
      newLoc = myLoc.add(Utils.directions[i]);
      newLocDist = newLoc.distanceSquaredTo(target);
      if (rc.canMove(Utils.directions[i]) && newLocDist <= dToLoc) {
        if (rc.canSenseLocation(newLoc)) {
          int rubble = rc.senseRubble(newLoc);
          if (rubble < bestPosRubble || (rubble == bestPosRubble && newLocDist < bestPosDist)) {
            bestPosDirInd = i;
            bestPosRubble = rubble;
            bestPosDist = newLocDist;
          }
        }
      }
    }
    if (bestPosDirInd != -1) {
      return move(Utils.directions[bestPosDirInd]);
    }

    // inspired from pnay old code
    // Potential choices
    Direction dirToTarget = myLoc.directionTo(target);
    MapLocation a = myLoc.add(dirToTarget);
    MapLocation b = myLoc.add(dirToTarget.rotateRight());
    MapLocation c = myLoc.add(dirToTarget.rotateLeft());
    int costA = rc.canSenseLocation(a) ? rc.senseRubble(a) : 101;
    int costB = rc.canSenseLocation(b) ? rc.senseRubble(b) : 101;
    int costC = rc.canSenseLocation(c) ? rc.senseRubble(c) : 101;

//    return moveInDirLoose(dirToTarget);
    return (costA <= costB && costA <= costC && move(dirToTarget))
        || (costB <= costC && move(dirToTarget.rotateRight()))
        || (move(dirToTarget.rotateLeft()));
  }

//  protected boolean moveSafely(MapLocation loc, int rad){
//    if (loc == null) return false;
//    int d = rc.getLocation().distanceSquaredTo(loc);
//    d = Math.min(d, rad);
//    boolean[] imp = new boolean[Utils.directions.length];
//    boolean greedy = false;
//    for (int i = Utils.directions.length; i-- > 0; ){
//      MapLocation newLoc = rc.getLocation().add(Utils.directions[i]);
//      if (newLoc.distanceSquaredTo(loc) <= d){
//        imp[i] = true;
//        greedy = true;
//      }
//    }
//    stolenbfs.path.setImpassable(imp);
//    stolenbfs.move(loc, greedy);
//    return true;
//  }


  /**
   * sense all around the robot for lead, choose a location probabilistically
   *    weighted by how much lead is reached and how much rubble in the way
   *    IGNORES 0Pb
   * @return the most tile in the center of most lead
   * @throws GameActionException if some game op fails
   */
  protected MapLocation getBestLeadLocProbabilistic() throws GameActionException {
    final int MIN_LEAD = 1;
    final int MAX_LOCS = Clock.getBytecodesLeft() >> (Cache.Permanent.ROBOT_TYPE.isBuilding() ? 8 : 9); // div by 256 = *0.78/100 -- allowed to use 78% of bytecode on this
//    int[] leadInDirection = new int[Utils.directions.length];
    int avgX = 0;
    int avgY = 0;
    int totalSeen = 0;
    MapLocation myLoc = rc.getLocation();

    // for all loations I can sense =>
    // sum up lead and number of current miners, and see if miners > lead / 50: continue if so
    MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, MIN_LEAD);
    int incr = 1;
    if (leadLocs.length > MAX_LOCS) incr = leadLocs.length / MAX_LOCS;
    for (int i = 0, leadLocsLength = Math.min(leadLocs.length, MAX_LOCS * incr); i < leadLocsLength; i+=incr) {
      MapLocation loc = leadLocs[i];

      // if there is a miner within 2x2 blocks of the location, then ignore it
      int leadSeen = rc.senseLead(loc);

      int minersThere = 0;
      for (RobotInfo friend : rc.senseNearbyRobots(loc, 8, Cache.Permanent.OUR_TEAM)) {
        if (friend.type == RobotType.MINER) minersThere++;
      }
      if (minersThere > leadSeen / 75) continue;


      int rubbleThere = rc.senseRubble(loc);
      int rubbleOnPath = rc.senseRubble(myLoc.add(myLoc.directionTo(loc)));

      leadSeen *= 100 - rubbleThere;
      leadSeen *= 100 - rubbleOnPath;
//      leadSeen /= myLoc.distanceSquaredTo(loc)+1;
      avgX += loc.x * leadSeen;
      avgY += loc.y * leadSeen;
      totalSeen += leadSeen;
    }
//    totalSeen *= 3; // each location affects 3 direction entries
    if (totalSeen == 0) { // return null if no good lead direction
      return null; // Utils.randomDirection();
    }
    return new MapLocation(avgX / totalSeen, avgY / totalSeen);
//    int randomInt = Utils.rng.nextInt(totalSeen);
//    for (int i = 0; i < leadInDirection.length; i++) {
//      if (randomInt <= leadInDirection[i]) return Utils.directions[i];
//      randomInt -= leadInDirection[i];
//    }
//    //System.out.println("WEIGHTED PICK FAILED: " + Arrays.toString(leadInDirection));
//    throw new RuntimeException("Weighted sum should be able to choose one a direction");
  }

  /**
   * move to a location with high lead density
   * @return if the movement was successfully based on lead presence
   * @throws GameActionException if movement fails
   */
  protected boolean moveToHighLeadProbabilistic() throws GameActionException {
    MapLocation highLead = getBestLeadLocProbabilistic();
//    if (rc.getID() == 10001) {
//      //System.out.println("high lead: " + highLead);
//    }
    return highLead != null && moveTowardsAvoidRubble(highLead);
//
//    if (dir != null) {
//      if (rc.canMove(dir)) {
//        rc.move(dir);
//        return true;
//      }
//    }
//    if (defaultToRandomMovement) moveRandomly();
//    return false;
  }

  /**
   * build the specified robot type in the specified direction
   * @param type the robot type to build
   * @param dir where to build it (if null, choose random direction)
   * @return method success
   * @throws GameActionException when building fails
   */
  protected boolean buildRobot(RobotType type, Direction dir) throws GameActionException {
    if (dir == null) dir = Utils.randomDirection();
    if (rc.canBuildRobot(type, dir)) {
      rc.buildRobot(type, dir);
      return true;
    }
    return false;
  }

  /**
   * check if there are any enemy (soldiers) to run away from
   * @return the map location where there are offensive enemies (null if none)
   */
  protected MapLocation offensiveEnemyCentroid() {
    if (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length == 0) return null;
    int avgX = 0;
    int avgY = 0;
    int count = 0;
    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      if (enemy.type.damage > 0) { // enemy can hurt me
        avgX += enemy.location.x * enemy.type.damage;
        avgY += enemy.location.y * enemy.type.damage;
        count += enemy.type.damage;
      }
    }
    if (count == 0) return null;
    return new MapLocation(avgX / count, avgY / count);
  }

  /**
   * checks if there are offensive enemies nearby
   * @return true if there are enemies in vision
   */
  protected boolean offensiveEnemiesNearby() {
    return offensiveEnemyCentroid() != null;
  }

  /**
   * returns the number of rounds since the last anomaly of a certain type
   * @param type the anomaly to look for
   * @return the turns since occurence (or roundNum if never occurred)
   */
  protected int getRoundsSinceLastAnomaly(AnomalyType type) {
    int turnsSince = rc.getRoundNum();
    for (AnomalyScheduleEntry anomaly : rc.getAnomalySchedule()) {
      if (anomaly.roundNumber >= rc.getRoundNum()) return turnsSince;
      if (anomaly.anomalyType == type) turnsSince = rc.getRoundNum() - anomaly.roundNumber;
    }
    return turnsSince;
  }
}
