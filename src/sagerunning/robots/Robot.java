package sagerunning.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import sagerunning.communications.Communicator;
import sagerunning.communications.messages.EnemyFoundMessage;
import sagerunning.communications.messages.Message;
import sagerunning.communications.messages.RubbleAtLocationMessage;
import sagerunning.pathfinding.BFS;
import sagerunning.pathfinding.StolenBFS2;
import sagerunning.robots.buildings.Archon;
import sagerunning.robots.buildings.Laboratory;
import sagerunning.robots.buildings.Watchtower;
import sagerunning.robots.droids.Builder;
import sagerunning.robots.droids.Miner;
import sagerunning.robots.droids.Sage;
import sagerunning.robots.droids.Soldier;
import sagerunning.utils.Cache;
import sagerunning.utils.Global;
import sagerunning.utils.Printer;
import sagerunning.utils.Utils;

public abstract class Robot {
  private static final boolean RESIGN_ON_GAME_EXCEPTION = false;
  private static final boolean RESIGN_ON_RUNTIME_EXCEPTION = false;

  private static final int MAX_TURNS_FIGURE_SYMMETRY = 200;

  protected final RobotController rc;
  protected final Communicator communicator;
  protected int pendingMessages;

  protected int turnCount;
  protected boolean dontYield;

  protected MapLocation closestCommedEnemy;
  protected int distToClosestCommedEnemy;

  public static final boolean USE_STOLEN_BFS = true;
  protected final BFS bfs;

  /**
   * Create a Robot with the given controller
   * Perform various setup tasks generic to ny robot (building/droid)
   * @param rc the controller
   */
  public Robot(RobotController rc) throws GameActionException {
    Global.setupGlobals(rc, this);
    Utils.setUpStatics();
    Cache.setup();
    Printer.cleanPrint();
    this.rc = rc;
    this.communicator = Global.communicator;

    bfs = new StolenBFS2(rc);

//    this.stolenbfs = new StolenBFS2(rc);
    // Print spawn message
//    //System.out.println(this.creationStats);
    // Set indicator message
    //rc.setIndicatorString("Just spawned!");
    turnCount = -1;
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
//        Printer.cleanPrint();
        Printer.submitPrint();
      } catch (GameActionException e) {
        // something illegal in the Battlecode world
        //System.out.println(rc.getType() + " GameActionException");
        Printer.submitPrint();
        e.printStackTrace();
        //rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 255,255,255);
        if (RESIGN_ON_GAME_EXCEPTION) rc.resign();
      } catch (Exception e) {
        // something bad
        //System.out.println(rc.getType() + " Exception");
        Printer.submitPrint();
        e.printStackTrace();
        //rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 255,255,255);
        if (RESIGN_ON_GAME_EXCEPTION || RESIGN_ON_RUNTIME_EXCEPTION) rc.resign();
      } finally {
        // end turn - make code wait until next turn
        if (!dontYield) Clock.yield();
        else {
          if (Clock.getBytecodesLeft() < 0.9 * Cache.Permanent.ROBOT_TYPE.bytecodeLimit) { // if don't have 90% of limit, still yield
            dontYield = false;
            Clock.yield();
//          } else {
//            //System.out.println("Skipping turn yeild!!");
          }
        }
      }
      Printer.cleanPrint();
    }
  }

  /**
   * wrap intern run turn method with generic actions for all robots
   */
  private void runTurnWrapper() throws GameActionException {
//    //System.out.println("\nvery start - " + rc.readSharedArray(Communicator.MetaInfo.META_INT_START));
//      //System.out.println("Age: " + turnCount + "; Location: " + Cache.PerTurn.CURRENT_LOCATION);
//    stolenbfs.initTurn();

    Cache.updateOnTurn();
    if (!dontYield) {
      //rc.setIndicatorString("ac: " + rc.getActionCooldownTurns() + " mc: " + rc.getMovementCooldownTurns());
    }
    dontYield = false;

    // PRE MESSAGE READING -----
    closestCommedEnemy = null;
    distToClosestCommedEnemy = 999999;

    bfs.initTurn();

//    //System.out.println("Update cache -- " + Clock.getBytecodeNum());
//    communicator.cleanStaleMessages();
    Utils.startByteCodeCounting("reading");
    pendingMessages = communicator.readAndAckAllMessages();
//    //System.out.println("# messages: " + pendingMessages + " -- " + Clock.getBytecodeNum());
//    while (pendingMessages > 0) {
//      Message message = communicator.getNthLastReceivedMessage(pendingMessages);
//      ackMessage(message);
//      pendingMessages--;
//    }
    Utils.finishByteCodeCounting("reading");
//    if (pendingMessages > 0) //System.out.println("Got " + pendingMessages + " messages!");

//    //System.out.println("After acking: " + Clock.getBytecodeNum());
    MapLocation initial = Cache.PerTurn.CURRENT_LOCATION;
    runTurnTypeWrapper();

    // if the bot moved on its turn
    if (!initial.equals(Cache.PerTurn.CURRENT_LOCATION)) {
      afterTurnWhenMoved();
    }
    //      int b = Clock.getBytecodeNum();
    //      int updatedChunks =
//    updateVisibleChunks();
    //      //System.out.println("updateVisibleChunks(" + updatedChunks + ") cost: " + (Clock.getBytecodeNum() - b));
    commNearbyEnemies();


    if (++turnCount != rc.getRoundNum() - Cache.Permanent.ROUND_SPAWNED) { // took too much bytecode
      //rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 255,0,255); // MAGENTA IF RAN OUT OF BYTECODE
      turnCount = rc.getRoundNum() - Cache.Permanent.ROUND_SPAWNED;
      dontYield = true;
    } else { // still on our turn logic
//    if (Clock.getBytecodesLeft() >= MIN_BYTECODES_TO_SEND) {
      Utils.startByteCodeCounting("sending");
//      //System.out.println("Bytecodes before send all messages: " + (Clock.getBytecodeNum()));
      communicator.sendQueuedMessages();
//      communicator.updateMetaIntsIfNeeded();
//      //System.out.println("Bytecodes after send all messages: " + (Clock.getBytecodeNum()));
      Utils.finishByteCodeCounting("sending");
//    }
    }
//    //System.out.println("\nvery end - " + rc.readSharedArray(Communicator.MetaInfo.META_INT_START));
  }

  /**
   * acknowledge the provided message (happens at turn start)
   * @param message the message received
   */
  public void ackMessage(Message message) throws GameActionException {
    switch (message.header.type) {
      case RUBBLE_AT_LOCATION:
        ackRubbleAtLocationMessage((RubbleAtLocationMessage) message);
        break;
      case ENEMY_FOUND:
        ackEnemyFound((EnemyFoundMessage) message);
    }
  }

  /**
   * Run a single turn for the robot
   * unique to buildings/droids
   */
  protected void runTurnTypeWrapper() throws GameActionException {
    runTurn();
  }

  /**
   * Run a single turn for the robot
   * unique to each robot type
   */
  protected abstract void runTurn() throws GameActionException;

  /**
   * run this code after the robot completes its turn
   *    should ONLY be run if the robot moved on this turn
   *  updates symmetry (broadcast rubble to check for symmetry failure)
   *  update chunks in the chunk info buffer of the shared array
   * @throws GameActionException if updating symmetry or visible chunks fails
   */
  protected void afterTurnWhenMoved() throws GameActionException {
    updateSymmetryComms();

  }

  /**
   * perform any universal code that robots should run to figure out map symmetry
   *    Currently - broadcast my location + the rubble there
   * @throws GameActionException if sensing fails
   */
  protected void updateSymmetryComms() throws GameActionException {
    // TODO: do it based on how many robots we have spawned (or total friends alive) or something
    if (Cache.PerTurn.HEALTH > 20 && communicator.metaInfo.knownSymmetry == null && Cache.PerTurn.ROUND_NUM < MAX_TURNS_FIGURE_SYMMETRY) {
      RubbleAtLocationMessage rubbleAtLocationMessage = new RubbleAtLocationMessage(Cache.PerTurn.CURRENT_LOCATION, rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION));
      ackRubbleAtLocationMessage(rubbleAtLocationMessage);
      if (communicator.metaInfo.knownSymmetry == null) communicator.enqueueMessage(rubbleAtLocationMessage);
//      if (communicator.metaInfo.knownSymmetry == null) communicator.enqueueMessage(rubbleAtLocationMessage);
    }
  }

  /**
   * check at the end of the turn if new enemies should be commed
   * @throws GameActionException if sending the message fails
   */
  protected void commNearbyEnemies() throws GameActionException {
    if (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length > 0) {
      RobotInfo enemy = Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[0];
      for (RobotInfo enemyInfo : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) { // look for archons
        if (enemyInfo.type == RobotType.ARCHON) {
          enemy = enemyInfo;
          int nearestEnemyIndex = communicator.archonInfo.getNearestEnemyArchonIndex(enemyInfo.location);
          if (nearestEnemyIndex == -1) {
            communicator.archonInfo.setEnemyArchonLoc(1, enemyInfo.location);
          } else if (!communicator.archonInfo.getEnemyArchon(nearestEnemyIndex).equals(enemyInfo.location)) {
            communicator.archonInfo.setEnemyArchonLoc(nearestEnemyIndex, enemyInfo.location);
          }
//          //System.out.println("Update enemy archon locs!");
//          communicator.archonInfo.readEnemyArchonLocs();
//          Printer.cleanPrint();
//          //Printer.print("Set enemy mirror");
//          //Printer.print("our 1: " + communicator.archonInfo.ourArchon1);
//          //Printer.print("our 2: " + communicator.archonInfo.ourArchon2);
//          //Printer.print("our 3: " + communicator.archonInfo.ourArchon3);
//          //Printer.print("our 4: " + communicator.archonInfo.ourArchon4);
//          //Printer.print("enemy 1: " + communicator.archonInfo.enemyArchon1);
//          //Printer.print("enemy 2: " + communicator.archonInfo.enemyArchon2);
//          //Printer.print("enemy 3: " + communicator.archonInfo.enemyArchon3);
//          //Printer.print("enemy 4: " + communicator.archonInfo.enemyArchon4);
//          Printer.submitPrint();
          break;
        }
      }
      // no already seen enemy or closest seen is very far
//      Printer.cleanPrint();
//      //Printer.print("closestCommedEnemy: " + closestCommedEnemy, "enemy: " + enemy);
//      if (closestCommedEnemy != null) {
//        //Printer.print("dist: " + closestCommedEnemy.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION));
//      }
//      Printer.submitPrint();
      if (closestCommedEnemy == null
          || !closestCommedEnemy.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED)) {
        communicator.enqueueMessage(new EnemyFoundMessage(enemy));
      }
    }
  }

  /**
   * receive the rubble location of anoter robot
   *    check if that helps us determine symmetry
   * @param message the rubble/location message
   * @throws GameActionException if sensing fails
   */
  private void ackRubbleAtLocationMessage(RubbleAtLocationMessage message) throws GameActionException {
    if (!communicator.metaInfo.notHorizontal) { // might be horizontal, check
      MapLocation xFlip = Utils.flipLocationX(message.location);
      if (rc.canSenseLocation(xFlip)) {
        int rubble = rc.senseRubble(xFlip);
        if (message.rubble != rubble) communicator.metaInfo.setSymmetryCantBe(Utils.MapSymmetry.HORIZONTAL);
      }
    }
    if (!communicator.metaInfo.notVertical) { // might be horizontal, check
      MapLocation yFlip = Utils.flipLocationY(message.location);
      if (rc.canSenseLocation(yFlip)) {
        int rubble = rc.senseRubble(yFlip);
        if (message.rubble != rubble) communicator.metaInfo.setSymmetryCantBe(Utils.MapSymmetry.VERTICAL);
      }
    }
    if (!communicator.metaInfo.notRotational) { // might be horizontal, check
      MapLocation rot = Utils.rotateLocation180(message.location);
      if (rc.canSenseLocation(rot)) {
        int rubble = rc.senseRubble(rot);
        if (message.rubble != rubble) communicator.metaInfo.setSymmetryCantBe(Utils.MapSymmetry.ROTATIONAL);
      }
    }
  }

  /**
   * acknowledge a message about a seen enemy
   * @param message the message about the seen enemy
   * @throws GameActionException if acking fails
   */
  private void ackEnemyFound(EnemyFoundMessage message) throws GameActionException {
//    Printer.cleanPrint();
//    //Printer.print("ackEnemyFound: " + message.enemyLocation, "dist: " + message.enemyLocation.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION));
//    Printer.submitPrint();
    if (message.enemyLocation.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, distToClosestCommedEnemy-1)) {
      closestCommedEnemy = message.enemyLocation;
      distToClosestCommedEnemy = message.enemyLocation.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION);
    }
  }

  /**
   * Wrapper for move() of RobotController that ensures enough bytecodes
   * @param dir where to move
   * @return if the robot moved
   * @throws GameActionException if movement failed
   */
  public boolean move(Direction dir) throws GameActionException {
    if (Clock.getBytecodesLeft() < 25) Clock.yield(); // todo: this should be larger? whenMoved takes a bit longer...
    if (rc.canMove(dir)) {
      rc.move(dir);
      Cache.PerTurn.whenMoved();
//      updateSymmetryComms();
      return true;
    }
    return false;
  }

  /**
   *
   * @param loc the location to test
   * @return if the robot is closer to the enemy archon than ours
   */
  protected boolean onEnemySide(MapLocation loc) {
    return true;
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
   * mimics moveInDirAvoidRubble but just gives the direction to move
   * @param goalDir desired direction
   * @return the closest direction with least rubble
   * @throws GameActionException if sensing fails
   */
  protected Direction getLeastRubbleMoveableDirAroundDir(Direction goalDir) throws GameActionException {
    MapLocation a = Cache.PerTurn.CURRENT_LOCATION.add(goalDir);
    MapLocation b = Cache.PerTurn.CURRENT_LOCATION.add(goalDir.rotateRight());
    MapLocation c = Cache.PerTurn.CURRENT_LOCATION.add(goalDir.rotateLeft());
    int costA = rc.canSenseLocation(a) ? rc.senseRubble(a) : 101;
    int costB = rc.canSenseLocation(b) ? rc.senseRubble(b) : 101;
    int costC = rc.canSenseLocation(c) ? rc.senseRubble(c) : 101;

    if (costA <= costB && costA <= costC && rc.canMove(goalDir)) return goalDir;
    if (costB <= costC && rc.canMove(goalDir.rotateRight())) return goalDir.rotateRight();
    if (rc.canMove(goalDir.rotateLeft())) return goalDir.rotateLeft();
//    if (rc.getRoundNum() == 28 && rc.getID() == 13009) {
//      //System.out.printf("Least rubble dir failed from %s\n\t%s:%d\n\t%s:%d\n\t%s:%d\n",Cache.PerTurn.CURRENT_LOCATION,goalDir,costA,goalDir.rotateRight(),costB,goalDir.rotateLeft(),costC);
//    }
    return null;
  }

  /**
   * move in the desired direction but accounting for rubble
   * @param goalDir the goal direction to move in
   * @return true if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveInDirAvoidRubble(Direction goalDir) throws GameActionException {
    MapLocation a = Cache.PerTurn.CURRENT_LOCATION.add(goalDir);
    MapLocation b = Cache.PerTurn.CURRENT_LOCATION.add(goalDir.rotateRight());
    MapLocation c = Cache.PerTurn.CURRENT_LOCATION.add(goalDir.rotateLeft());
    int costA = rc.canSenseLocation(a) ? rc.senseRubble(a) : 101;
    int costB = rc.canSenseLocation(b) ? rc.senseRubble(b) : 101;
    int costC = rc.canSenseLocation(c) ? rc.senseRubble(c) : 101;

    return (costA <= costB && costA <= costC && move(goalDir))
        || (costB <= costC && move(goalDir.rotateRight()))
        || (move(goalDir.rotateLeft()));
  }

  /**
   * get the best direction to move to reach the provided target
   *    accounts for rubble but greedily
   * @param target the location to approach
   * @return the best direction or null
   * @throws GameActionException if sensing fails
   */
  private MapLocation lastTarget = null;
  private MapLocation lastPosition = null;
  protected Direction getOptimalDirectionTowards(MapLocation target) throws GameActionException {
//    if (USE_STOLEN_BFS) {
//      return bfs.bestDir(target, false);
//    }
    if (Cache.PerTurn.CURRENT_LOCATION.equals(target)) return Direction.CENTER;
    Direction bestDirection = null;
    int bestPosRubble = 101;
    int bestPosDist = -1;
    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
    int dToLoc = myLoc.distanceSquaredTo(target);

    MapLocation offLimits = (lastTarget != null && lastTarget.equals(target)) ? lastPosition : null;
    lastPosition = Cache.PerTurn.CURRENT_LOCATION;
    lastTarget = target;

    MapLocation newLoc; // temp
    int newLocDist; // temp
    for (Direction candidateDir : Utils.directions) {
      newLoc = myLoc.add(candidateDir);
      if (newLoc.equals(offLimits)) continue; // not allowed to cycle location
      newLocDist = newLoc.distanceSquaredTo(target);
      if (rc.canMove(candidateDir) && newLocDist <= dToLoc) {
        if (rc.canSenseLocation(newLoc)) {
          int rubble = rc.senseRubble(newLoc);
          if (rubble < bestPosRubble || (rubble == bestPosRubble && newLocDist < bestPosDist)) {
            bestDirection = candidateDir;
            bestPosRubble = rubble;
            bestPosDist = newLocDist;
          }
        }
      }
    }
    return bestDirection;
  }

  /**
   * move towards the given target and avoid rubble naively
   * @param target the location to move towards
   * @return if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveOptimalTowards(MapLocation target) throws GameActionException {
//    //Printer.print("RUNNING moveOptimalTowards(): ", "target: " + target);
//    if (USE_STOLEN_BFS) {
//      Direction optimal = getOptimalDirectionTowards(target);
//      return optimal != null && move(optimal);
//    }
    if (!rc.isMovementReady()) return false;

    Direction bestDirection = getOptimalDirectionTowards(target);
    return bestDirection != null
        ? move(bestDirection)
        : moveInDirAvoidRubble(Cache.PerTurn.CURRENT_LOCATION.directionTo(target));
  }

  protected boolean moveBFS(MapLocation target) throws GameActionException {
    MapLocation curr = Cache.PerTurn.CURRENT_LOCATION;
    bfs.move(target, false);
    return !Cache.PerTurn.CURRENT_LOCATION.equals(curr);
  }

  /**
   * get the best direction to move to avoid the provided target
   *    accounts for rubble but greedily
   * @param source the location to avoid
   * @return the best direction or null
   * @throws GameActionException if sensing fails
   */
  protected Direction getOptimalDirectionAway(MapLocation source) throws GameActionException {
    Direction bestDirection = null;
    int bestPosRubble = 101;
    int bestPosDist = Integer.MAX_VALUE;
    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
    int dToLoc = myLoc.distanceSquaredTo(source);

    MapLocation newLoc; // temp
    int newLocDist; // temp
    for (Direction candidateDir : Utils.directions) {
      newLoc = myLoc.add(candidateDir);
      newLocDist = newLoc.distanceSquaredTo(source);
      if ((rc.canMove(candidateDir) && newLocDist >= dToLoc)) {
        if (rc.canSenseLocation(newLoc)) {
          int rubble = rc.senseRubble(newLoc);
          if (rubble < bestPosRubble || (rubble == bestPosRubble && newLocDist > bestPosDist)) {
            bestDirection = candidateDir;
            bestPosRubble = rubble;
            bestPosDist = newLocDist;
          }
        }
      }
    }
    return bestDirection;
  }

  /**
   * move away from the given target and avoid rubble
   * @param source the location to move away from
   * @return if moved
   * @throws GameActionException if movement fails
   */
  protected boolean moveOptimalAway(MapLocation source) throws GameActionException {
    if (!rc.isMovementReady()) return false;

    Direction bestDirection = getOptimalDirectionAway(source);
    return bestDirection != null
        ? move(bestDirection)
        : moveInDirAvoidRubble(source.directionTo(Cache.PerTurn.CURRENT_LOCATION));
  }

  /**
   * move away from the provided location and set some indicators
   * TODO: this method doesn't work well for some reason
   * @param toEscape the location to run away from
   * @return true if escaped from the given location (not in 2xvision)
   * @throws GameActionException if movement fails
   */
  protected boolean runAwayFrom(MapLocation toEscape) throws GameActionException {
    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
    boolean moved = false;

    MapLocation target = new MapLocation((myLoc.x << 1) - toEscape.x, (myLoc.y << 1) - toEscape.y);
    Direction backToSelf = target.directionTo(myLoc);
    Direction awayFromSelf = backToSelf.opposite();
//    while (rc.canSenseLocation(target)) target = target.add(awayFromSelf); // go as far out as possible
    while (!rc.canSenseLocation(target)) target = target.add(backToSelf); // come back in until sensible

    moved = moveOptimalTowards(target) || moveOptimalAway(toEscape) || moveInDirLoose(awayFromSelf);
//    if (moveOptimalAway(toEscape)) {
//      moved = true;
//    } else {
//      MapLocation target = new MapLocation((myLoc.x << 1) - toEscape.x, (myLoc.y << 1) - toEscape.y);
//      Direction backToSelf = target.directionTo(myLoc);
//      Direction awayFromSelf = backToSelf.opposite();
//      while (rc.canSenseLocation(target)) target = target.add(awayFromSelf); // go as far out as possible
//      while (!rc.canSenseLocation(target)) target = target.add(backToSelf); // come back in until sensible
//
//      if (moveOptimalTowards(target)) {
//        moved = true;
//      } else if (moveInDirLoose(awayFromSelf)) {
//        moved = true;
//      }
//    }
    if (moved) {
      //rc.setIndicatorDot(toEscape, 255,0,0);
      //rc.setIndicatorString("running away from " + toEscape);
      //rc.setIndicatorLine(myLoc, toEscape, 0, 255, 0);
    }
//    return !myLoc.isWithinDistanceSquared(toEscape, Cache.Permanent.VISION_RADIUS_SQUARED<<4) || !rc.onTheMap(myLoc.add(toEscape.directionTo(Cache.PerTurn.CURRENT_LOCATION)));
    return myLoc.isWithinDistanceSquared(target, Utils.DSQ_1by1);
  }

  public Direction getLeastRubbleDir() throws GameActionException {
    Direction best = null;
    int rubbleOfBest = 9999;
    for (Direction dir : Utils.directions) {
      if (!rc.canSenseLocation(Cache.PerTurn.CURRENT_LOCATION.add(dir))) continue;
      int rubble = rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION.add(dir));
      if (rubble < rubbleOfBest) {
        best = dir;
        rubbleOfBest = rubble;
      }
    }
    return best;
  }

  public Direction getLeastRubbleUnoccupiedDir() throws GameActionException {
    Direction best = null;
    int rubbleOfBest = 9999;
    for (Direction dir : Utils.directions) {
      MapLocation loc = Cache.PerTurn.CURRENT_LOCATION.add(dir);
      if (!rc.canSenseLocation(loc) || rc.isLocationOccupied(loc)) continue;
      int rubble = rc.senseRubble(loc);
      if (rubble < rubbleOfBest) {
        best = dir;
        rubbleOfBest = rubble;
      }
    }
    return best;
  }

  /**
   * sense all around the robot for lead, return a weighted average location
   *    weighted by how much lead is reached and how much rubble in the way
   *    IGNORES 0Pb
   * @return the tile in the center of most lead (null if no lead)
   * @throws GameActionException if some sensing fails
   */
  protected MapLocation getWeightedAvgLeadLoc() throws GameActionException {
    final int MIN_LEAD = 1; // making this 2 causes us to do slightly worse lol weird
    final int MAX_LOCS = Clock.getBytecodesLeft() >> (Cache.Permanent.ROBOT_TYPE.isBuilding() ? 8 : 9); // div by 256 = *0.78/100 -- allowed to use 78% of bytecode on this
//    int[] leadInDirection = new int[Utils.directions.length];
    int avgX = 0;
    int avgY = 0;
    int totalSeen = 0;
    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
    // for all loations I can sense =>
    // sum up lead and number of current miners, and see if miners > lead / 50: continue if so
    MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, MIN_LEAD);
    if (leadLocs.length == 0) return null;
    if (MAX_LOCS <= 1) return leadLocs[Utils.rng.nextInt(leadLocs.length)];
    int incr = 1;
    if (leadLocs.length > MAX_LOCS) incr = leadLocs.length / MAX_LOCS;
    for (int i = 0, leadLocsLength = Math.min(leadLocs.length, MAX_LOCS * incr); i < leadLocsLength; i+=incr) {
      MapLocation loc = leadLocs[i];
      // if there is a miner within 2x2 blocks of the location, then ignore it
      int leadSeen = rc.senseLead(loc);
//      int minersThere = 0;
//      for (RobotInfo friend : rc.senseNearbyRobots(loc, Utils.DSQ_1by1, Cache.Permanent.OUR_TEAM)) {
//        if (friend.type == RobotType.MINER) minersThere++;
//      }
//      if (minersThere > leadSeen / Utils.LEAD_PER_MINER_CLAIM) continue;

      int rubbleThere = rc.senseRubble(loc);
      int rubbleOnPath = rc.senseRubble(myLoc.add(myLoc.directionTo(loc)));

      leadSeen *= 100 - rubbleThere;
      leadSeen *= 100 - rubbleOnPath;
//      leadSeen /= myLoc.distanceSquaredTo(loc)+1;

      avgX += loc.x * leadSeen;
      avgY += loc.y * leadSeen;
      totalSeen += leadSeen;
    }
    // return null if no good lead direction
    if (totalSeen == 0) return null;
    return new MapLocation(avgX / totalSeen, avgY / totalSeen);
  }

  /**
   * build the specified robot type in the specified direction
   * @param type the robot type to build
   * @param dir where to build it (if null, choose random direction)
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

  protected boolean buildRobotInDirLoose(RobotType type, Direction dir) throws GameActionException {
    if (dir == null) return false;
    return buildRobot(type, dir) || buildRobot(type, dir.rotateRight()) || buildRobot(type, dir.rotateLeft());
  }

  /**
   * check if there are any enemy (soldiers) to run away from
   * @return the map location where there are offensive enemies (null if none)
   */
  private MapLocation cachedEnemyCentroid;
  private int cacheStateOnCalcOffensiveEnemyCentroid = -1;
  protected MapLocation offensiveEnemyCentroid() {
    if (cacheStateOnCalcOffensiveEnemyCentroid == Cache.PerTurn.cacheState) return cachedEnemyCentroid;
    cacheStateOnCalcOffensiveEnemyCentroid = Cache.PerTurn.cacheState;
    if (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length == 0) return (cachedEnemyCentroid = null);
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
    return (cachedEnemyCentroid = (count == 0 ? null : new MapLocation(avgX / count, avgY / count)));
  }

  /**
   * calculate the average location of friendly soldiers
   * @return the map location where there are offensive enemies (null if none)
   */
  private MapLocation cachedFriendlyCentroid;
  private int cacheStateOnCalcFriendlySoldierCentroid = -1;
  public MapLocation friendlySoldierCentroid() {
    if (cacheStateOnCalcFriendlySoldierCentroid == Cache.PerTurn.cacheState) return cachedFriendlyCentroid;
    cacheStateOnCalcFriendlySoldierCentroid = Cache.PerTurn.cacheState;
    if (Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS.length == 0) return (cachedFriendlyCentroid = null);
    int avgX = 0;
    int avgY = 0;
    int count = 0;
    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      if (friend.type == RobotType.SOLDIER) { // friend can hurt me
        avgX += friend.location.x;
        avgY += friend.location.y;
        count++;
      }
    }
    return (cachedFriendlyCentroid = (count == 0 ? null : new MapLocation(avgX / count, avgY / count)));
  }

  /**
   * checks if there are offensive enemies nearby
   * @return true if there are enemies in vision
   */
  protected boolean offensiveEnemiesNearby() {
    return offensiveEnemyCentroid() != null;
  }

  /**
   * looks through enemies in vision and finds the one with lowest health that matches the type criteria
   * @param enemyType the robottype to look for
   * @return the robot of specified type with lowest health
   */
  protected RobotInfo findLowestHealthEnemyOfType(RobotType enemyType) {
    RobotInfo weakestEnemy = null;
    int minHealth = Integer.MAX_VALUE;

    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      if (enemy.type == enemyType && enemy.health < minHealth) {
        minHealth = enemy.health;
        weakestEnemy = enemy;
      }
    }

    return weakestEnemy;
  }
}
