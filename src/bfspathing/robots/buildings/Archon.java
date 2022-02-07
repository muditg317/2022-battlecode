package bfspathing.robots.buildings;

import battlecode.common.*;
import bfspathing.communications.messages.*;
import bfspathing.utils.Cache;
import bfspathing.utils.Utils;

public class Archon extends Building {
  public static final int SUICIDE_ROUND = -500;

  public static final int MAX_RUBBLE_TO_STOP = 5;

  public static final int CRITICAL_HEALTH_TO_HEAL_LOWEST = 20;

  private int whichArchonAmI = 1;
  //  private List<MapLocation> archonLocs;
  private boolean hasMoved;
  public boolean moving;

  private final int localLead;
//  private MapSymmetry predictedSymmetry;

  private int minersSpawned;
  private int buildersSpawned;
  private int soldiersSpawned;
  private int sagesSpawned;

  private static final int INCOME_HISTORY_LENGTH = 10;
  private int lastTurnStartingLead;
  private int[] incomeHistory;
  private int leadIncome;
  private int movingTotalIncome;
  private int leadSpent;
  private int movingAvgIncome;

  private int lastTurnHealth;
  private int healthLostThisTurn;
  private SaveMeMessage saveMeRequest;

  int leadAtArchonLocation = 0;
  int initialMinersToSpawn = 2;

  private boolean canMoveToBetterLocation = false;

  private int minimumDistanceToEdge = 60;
  private int bestArchonToSpawnBuilderForLab = -1;
  private boolean labBuilderSpawned;
  private boolean saveUpForBuilderAndLab;

  public Archon(RobotController rc) throws GameActionException {
    super(rc);
//    whichArchonAmI = rc.getID() >> 1; // floor(id / 2)
//    archonLocs = new ArrayList<>();
//    //System.out.println("Hello from Archon constructor #"+whichArchonAmI + " at " + Cache.PerTurn.CURRENT_LOCATION);
    localLead = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED).length;

//    lastTurnStartingLead = 0;
    incomeHistory = new int[INCOME_HISTORY_LENGTH];
//    leadIncome = 0;
//    leadSpent = 0;
//    movingTotalIncome = 0;
//    movingAvgIncome = 0;
//
//    lastTurnHealth = 0;
//    healthLostThisTurn = 0;
//    saveMeRequest = null;
  }

  @Override
  protected void runTurn() throws GameActionException {
    updateHistories();
    if (rc.getRoundNum() == 1 && !doFirstTurn()) { // executes turn 1 and continues if needed
      return;
    }

    if (Cache.PerTurn.ROUND_NUM == 2) {
      communicator.archonInfo.readOurArchonLocs();
      int bestDistance = 20;
      switch (rc.getArchonCount()) {
        case 4:
          int candidateDistance = Utils.maxDistanceToCorner(communicator.archonInfo.ourArchon4);
          if (candidateDistance <= bestDistance) {
            bestArchonToSpawnBuilderForLab = 4;
            bestDistance = candidateDistance;
          }
        case 3:
          candidateDistance = Utils.maxDistanceToCorner(communicator.archonInfo.ourArchon3);
          if (candidateDistance <= bestDistance) {
            bestArchonToSpawnBuilderForLab = 3;
            bestDistance = candidateDistance;
          }
        case 2:
          candidateDistance = Utils.maxDistanceToCorner(communicator.archonInfo.ourArchon2);
          if (candidateDistance <= bestDistance) {
            bestArchonToSpawnBuilderForLab = 2;
            bestDistance = candidateDistance;
          }
        case 1:
          candidateDistance = Utils.maxDistanceToCorner(communicator.archonInfo.ourArchon1);
          if (candidateDistance <= bestDistance) {
            bestArchonToSpawnBuilderForLab = 1;
            bestDistance = candidateDistance;
          }
      }
    }

//    if (communicator.metaInfo.knownSymmetry != null && !communicator.archonInfo.mirrored) {
//      communicator.archonInfo.mirrorSelfToEnemies();
//    }

//    drawChunkBounds();
    if (!moving) {
      runArchonStationaryMode();
    } else {
      runArchonMoving();
    }

    if (rc.getRoundNum() == SUICIDE_ROUND) {
      rc.resign();
    }
  }

  /**
   * run function for an archon when it is stationary (turret mode)
   * @throws GameActionException if any action fails
   */
  private void runArchonStationaryMode() throws GameActionException {
    MapLocation nearbyEnemies = offensiveEnemyCentroid();
    if (saveMeRequest != null || nearbyEnemies != null) {
      if (healthLostThisTurn < Cache.PerTurn.HEALTH) broadcastSaveMe();
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        //rc.setIndicatorString("Spawn soldier!");
        soldiersSpawned++;
        leadSpent += RobotType.SOLDIER.buildCostLead;
      }
    }

    // Spawn new droid
    int archons = rc.getArchonCount();
    if (rc.isActionReady()) {
      if (saveUpForBuilderAndLab) {
        if (bestArchonToSpawnBuilderForLab == whichArchonAmI && !labBuilderSpawned) {
          labBuilderSpawned = spawnBuilderForLab();
        }
      } else {
        RobotType typeToSpawn = determineSpawnDroidType();
        if (typeToSpawn != null && (rc.getTeamLeadAmount(Cache.Permanent.OUR_TEAM) >= typeToSpawn.buildCostLead * 2 || rc.getRoundNum() % archons == whichArchonAmI % archons || whichArchonAmI == archons)) {
          if (spawnDroid(typeToSpawn)) {
            if (initialMinersToSpawn == minersSpawned && typeToSpawn == RobotType.MINER) saveUpForBuilderAndLab = true;
          }
        }
      }
    }

    // Repair damaged droid
    if (rc.isActionReady()) {
      healNearbyDroids();
    }

    // only allow movement if we know symmetry
//    //Printer.cleanPrint();;
    if (rc.canTransform() && communicator.metaInfo.knownSymmetry != null && (bestArchonToSpawnBuilderForLab != whichArchonAmI || labBuilderSpawned)) {
      // only move once / if we have more than 1 archon to keep spawning while we move
//      //Printer.print("has moved: " + hasMoved, "# of archons: " + rc.getArchonCount());
      updateShouldStop();
      if (!shouldStop && rc.getArchonCount() > 1) {
        boolean canMove = true;
        switch (rc.getArchonCount()) {
          case 1:
            canMove = false;
//            //Printer.print("whichArchonAmI: " + whichArchonAmI, "canMove: " + canMove);
            break;
          case 2:
            canMove = !communicator.archonInfo.ourArchonIsMoving(3 - whichArchonAmI);
//            //Printer.print("whichArchonAmI: " + whichArchonAmI, "canMove: " + canMove);
            break;
          case 3:
            canMove = !communicator.archonInfo.ourArchonIsMoving(((whichArchonAmI)%3)+1)
                    || !communicator.archonInfo.ourArchonIsMoving(((whichArchonAmI+1)%3)+1);
//            //Printer.print("whichArchonAmI: " + whichArchonAmI, "canMove: " + canMove);
            break;
          case 4:
            canMove = !communicator.archonInfo.ourArchonIsMoving(((whichArchonAmI)%4)+1)
                    || !communicator.archonInfo.ourArchonIsMoving(((whichArchonAmI+1)%4)+1)
                    || !communicator.archonInfo.ourArchonIsMoving(((whichArchonAmI+2)%4)+1);
//            //Printer.print("whichArchonAmI: " + whichArchonAmI, "canMove: " + canMove);
        }
//        //Printer.submitPrint();
        if (canMove) {
          startMoving();
        }
      }
    }
  }

//  private MapLocation tooMuchStartingRubbleEscapeLocation;
  private MapLocation whereToGo;
  private boolean shouldStop;
  /**
   * run function for an archon when it is moving
   * @throws GameActionException if any action fails
   */
  private void runArchonMoving() throws GameActionException {
    if (whereToGo == null || closestCommedEnemy == null) { // determine closest enemy archon to move towards
//      whereToGo = communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION);
      whereToGo = Utils.applySymmetry(Cache.Permanent.START_LOCATION, communicator.metaInfo.guessedSymmetry);
//      if ()
    }
    if (closestCommedEnemy != null) whereToGo = closestCommedEnemy;

    if (rc.isTransformReady()) {
      updateShouldStop();

      if (!shouldStop) {
        if (moveOptimalTowards(whereToGo)) {
          communicator.archonInfo.setOurArchonLoc(whichArchonAmI, Cache.PerTurn.CURRENT_LOCATION);
        }
      } else {
        if (rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) < MAX_RUBBLE_TO_STOP) {
          stopMoving();
        } else {
          MapLocation lowestRubbleLoc = null;
          int lowestRubble = 9999;
          int distToClosestCurr = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(whereToGo);
          if (closestCommedEnemy != null) {
            distToClosestCurr = Math.min(distToClosestCurr, distToClosestCommedEnemy);
          }
          for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_2by2)) {
            if (!rc.isLocationOccupied(loc) && !loc.isWithinDistanceSquared(whereToGo, distToClosestCurr-1)) {
              int rubble = rc.senseRubble(loc);
              if (rubble < lowestRubble) {
                lowestRubbleLoc = loc;
                lowestRubble = rubble;
              } else if (rubble == lowestRubble && loc.distanceSquaredTo(whereToGo) < lowestRubbleLoc.distanceSquaredTo(whereToGo)) {
                lowestRubbleLoc = loc;
              }
            }
          }
          if (lowestRubbleLoc != null) {
            if (moveOptimalTowards(lowestRubbleLoc)) {
              communicator.archonInfo.setOurArchonLoc(whichArchonAmI, Cache.PerTurn.CURRENT_LOCATION);
            }
          } else {
            if (moveOptimalAway(whereToGo)) {
              communicator.archonInfo.setOurArchonLoc(whichArchonAmI, Cache.PerTurn.CURRENT_LOCATION);
            }
          }
        }
      }
    }
  }

//  private int lastTurnStartedMoving = -1;
//  private int turnFirstMoved = -1;
  private void startMoving() throws GameActionException {
    rc.transform();
    communicator.archonInfo.setOurArchonMoving(whichArchonAmI);
    moving = true;
//    lastTurnStartedMoving = Cache.PerTurn.ROUND_NUM;
//    if (turnFirstMoved == -1) turnFirstMoved = lastTurnStartedMoving;
    whereToGo = null;
    shouldStop = false;
  }

  public void updateShouldStop() throws GameActionException {
    // check if any enemy damaging units or any damaged friendly soldiers
    shouldStop = false;

//    if (tooMuchStartingRubbleEscapeLocation != null && lastTurnStartedMoving == turnFirstMoved) {
//      shouldStop = rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) < 10;
//      return;
//    }

    if (whereToGo != null && whereToGo.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
      shouldStop = true;
      return;
    }

    for (RobotInfo ri : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      if (ri.type.damage > 0 || ri.type == RobotType.ARCHON) {
        shouldStop = true;
        return;
      }
    }

//    if (!shouldStop) {
    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      if (friend.type.damage > 0 && friend.health < CRITICAL_HEALTH_TO_HEAL_LOWEST && Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(friend.location, Cache.Permanent.ACTION_RADIUS_SQUARED)) { //todo: maybe consider changing to a threshold
        shouldStop = true;
        return;
      }
    }
//    }

  }

  private void stopMoving() throws GameActionException {
    rc.transform();
    communicator.archonInfo.setOurArchonNotMoving(whichArchonAmI);
    moving = false;
    hasMoved = true;
    whereToGo = null;
  }

  /**
   * update whatever internal history values that this archon stores
   */
  private void updateHistories() {
    leadIncome = rc.getTeamLeadAmount(Cache.Permanent.OUR_TEAM) - lastTurnStartingLead + leadSpent;
    lastTurnStartingLead = rc.getTeamLeadAmount(Cache.Permanent.OUR_TEAM);
    leadSpent = 0;
    movingTotalIncome += leadIncome - incomeHistory[Cache.PerTurn.ROUND_NUM % INCOME_HISTORY_LENGTH];
    incomeHistory[Cache.PerTurn.ROUND_NUM % INCOME_HISTORY_LENGTH] = leadIncome;
    movingAvgIncome = movingTotalIncome / INCOME_HISTORY_LENGTH;
    //rc.setIndicatorString("income - " + leadIncome + " avg: " + movingAvgIncome + " tot: " + movingTotalIncome);
//    if (whichArchonAmI == rc.getArchonCount()) {
//      //System.out.println("Lead income: " + leadIncome);
//    }

    healthLostThisTurn = lastTurnHealth - Cache.PerTurn.HEALTH;
//    //rc.setIndicatorString("health: " + Cache.PerTurn.HEALTH + " - lastHP: " + lastTurnHealth + " - lost: " + healthLostThisTurn);
    lastTurnHealth = Cache.PerTurn.HEALTH;
  }

  /**
   * Run the first turn for this archon
   * @return if running should continue
   */
  private boolean doFirstTurn() throws GameActionException {
//    //System.out.println("Hello from Archon #"+whichArchonAmI + " at " + Cache.PerTurn.CURRENT_LOCATION);

    communicator.archonInfo.setOurArchonLoc(whichArchonAmI, Cache.PerTurn.CURRENT_LOCATION);

    if (whichArchonAmI == 1) {
      communicator.metaInfo.initializeValidRegion();
    }

    ArchonHelloMessage helloMessage = generateArchonHello();
    communicator.enqueueMessage(helloMessage);

//    if (whichArchonAmI == 1) {
      runTeamStartupLogic();
//    }

    return true;
  }

  private void runTeamStartupLogic() throws GameActionException {

    int rubbleHere = rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION);
    int leastRubble = rubbleHere;
    MapLocation leastRubbleLocation = Cache.PerTurn.CURRENT_LOCATION;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED)) {
      int candidateRubble = rc.senseRubble(loc);
      if (candidateRubble < leastRubble) {
        leastRubble = candidateRubble;
        leastRubbleLocation = loc;
      }
    }

    if (leastRubble < rubbleHere) {
      // worth to move?
    }


    minimumDistanceToEdge = Math.min(Cache.PerTurn.CURRENT_LOCATION.x, Cache.PerTurn.CURRENT_LOCATION.y);
    minimumDistanceToEdge = Math.min(minimumDistanceToEdge, Cache.Permanent.MAP_WIDTH - Cache.PerTurn.CURRENT_LOCATION.x);
    minimumDistanceToEdge = Math.min(minimumDistanceToEdge, Cache.Permanent.MAP_HEIGHT - Cache.PerTurn.CURRENT_LOCATION.y);


    // let's get some data...
    int mapHeight = Cache.Permanent.MAP_HEIGHT;
    int mapWidth = Cache.Permanent.MAP_WIDTH;
    int mapSize = mapHeight * mapWidth;
    for (MapLocation loc : rc.senseNearbyLocationsWithLead(-1,2)) {
      leadAtArchonLocation += rc.senseLead(loc);
    }
    // estimate on average, distance to enemy archon --> 3 rotations and see # units need to walk, and do some shit based on that (and median rubble?)
    int distHorizontal = Math.abs((mapWidth - 2 *Cache.Permanent.START_LOCATION.x));
    int distVertical = Math.abs((mapHeight - 2 *Cache.Permanent.START_LOCATION.y));
    int distRotational = Math.max(distHorizontal, distVertical);

    int minDist = Math.min(distHorizontal, distVertical);

    int totalRubble = 0;
    int numRubble = 0;
    for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED)) {
      totalRubble += rc.senseRubble(loc);
      numRubble++;
    }
    int avgRubble = totalRubble / numRubble;

    double turnsTillNextMove = Utils.turnsTillNextCooldown(RobotType.SOLDIER.movementCooldown, avgRubble);
    int totalMoves = (int) (minDist * turnsTillNextMove);

    initialMinersToSpawn += totalMoves / 75;
  }

  /**
   * generate a message to be sent to the other arhcons on the first turn of the game
   *    this message contains various bits of info that should be shared
   *    - location
   *    - symmetry info (TODO: implement)
   * @return the message to send to the other archons
   */
  private ArchonHelloMessage generateArchonHello() {
    return new ArchonHelloMessage(Cache.PerTurn.CURRENT_LOCATION, false, false, false);
  }

  @Override
  public void ackMessage(Message message) throws GameActionException {
    super.ackMessage(message);
    if (message instanceof ArchonHelloMessage) {
      ackArchonHello((ArchonHelloMessage) message);
    } else if (message instanceof ArchonSavedMessage) {
      ackArchonSaved((ArchonSavedMessage) message);
    } else if (message instanceof LabBuiltMessage) {
      ackLabBuilt((LabBuiltMessage) message);
    }
  }

  /**
   * acknowledge a hello from another archon
   * @param message the hello
   */
  public void ackArchonHello(ArchonHelloMessage message) {
    if (Cache.PerTurn.ROUND_NUM == 1) {
      whichArchonAmI++;
    }

//    archonLocs.add(message.location);
  }

  /**
   * acknowledge save message
   * if saved self, then stop requesting saving
   * @param message the location of the archon that was saved
   */
  private void ackArchonSaved(ArchonSavedMessage message) {
    if (saveMeRequest != null && message.location.equals(saveMeRequest.location)) {
      saveMeRequest = null;
//    } else {
//      //System.out.println("Ignore archon saved message: " + (saveMeRequest != null ? saveMeRequest.location : "null") + " vs " + message.location);
    }
  }

  /**
   * acknowledge that a lab has been built, stop saving lead
   * @param message the location of the built lab
   */
  private void ackLabBuilt(LabBuiltMessage message) {
    saveUpForBuilderAndLab = false;
//    //System.out.println("ack lab has been built!");
  }

  /**
   * archon is bouta die, request saving
   * store request internally
   * @throws GameActionException if messaging fails
   */
  private void broadcastSaveMe() throws GameActionException {
    MapLocation toSave = offensiveEnemyCentroid();
    int advantage = 0;
    for (RobotInfo ri : Cache.PerTurn.ALL_NEARBY_ROBOTS) {
      if (ri.type.damage > 0) {
        if (ri.team == Cache.Permanent.OUR_TEAM) advantage += ri.health;
        else advantage -= ri.health;
      }
    }
    if (advantage <= 1) {
      saveMeRequest = new SaveMeMessage(toSave != null ? toSave : Cache.PerTurn.CURRENT_LOCATION);
      communicator.enqueueMessage(saveMeRequest);
    }
  }

  /**
   * spawn a builder which should be sent to spawn a lab
   * @return true if the builder was spawned
   * @throws GameActionException if building fails
   */
  private boolean spawnBuilderForLab() throws GameActionException {
    MapLocation corner = new MapLocation(
            Cache.PerTurn.CURRENT_LOCATION.x <= Cache.Permanent.MAP_WIDTH/2 ? 0 : (Cache.Permanent.MAP_WIDTH-1),
            Cache.PerTurn.CURRENT_LOCATION.y <= Cache.Permanent.MAP_HEIGHT/2 ? 0 : (Cache.Permanent.MAP_HEIGHT-1));
    Direction dirToCorner = Cache.PerTurn.CURRENT_LOCATION.directionTo(corner);
    if (buildRobotInDirLoose(RobotType.BUILDER, dirToCorner)) {
      //rc.setIndicatorString("Spawn builder!");
      buildersSpawned++;
      leadSpent += RobotType.BUILDER.buildCostLead;
      return true;
    }
    return false;
  }

  private int minerDirection = Utils.rng.nextInt(10);
  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private boolean spawnDroid(RobotType typeToSpawn) throws GameActionException {
    switch(typeToSpawn) {
      case MINER:
        Direction dir = Utils.randomDirection();
        if (Cache.PerTurn.ROUND_NUM >= 20) {
          MapLocation bestLead = getWeightedAvgLeadLoc();
          // TODO: if bestLead is null, spawn on low rubble instead of random
          if (bestLead != null) {
            dir = Cache.PerTurn.CURRENT_LOCATION.directionTo(bestLead);
          }
        } else {
          dir = Utils.directions[minerDirection % Utils.directions.length];
          minerDirection += Utils.rng.nextInt(2)+1;
        }

        // using getOptimalDirectionTowards(bestLead) causes slightly worse performance lol
        if (dir == Direction.CENTER) dir = getLeastRubbleUnoccupiedDir();

//      //System.out.println("I need a miner! bestLead: " + bestLead + " dir: " + dir);
        if (buildRobot(RobotType.MINER, dir) || buildRobot(RobotType.MINER, dir.rotateRight()) || buildRobot(RobotType.MINER, dir.rotateLeft())) {
          //rc.setIndicatorString("Spawn miner!");
          minersSpawned++;
          leadSpent += RobotType.MINER.buildCostLead;
          return true;
        }
        break;
      case BUILDER:
        if (buildRobot(RobotType.BUILDER, Utils.randomDirection())) {
          //rc.setIndicatorString("Spawn builder!");
          buildersSpawned++;
          leadSpent += RobotType.BUILDER.buildCostLead;
          return true;
        }
        break;
      case SOLDIER:
        if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
          //rc.setIndicatorString("Spawn soldier!");
          soldiersSpawned++;
          leadSpent += RobotType.SOLDIER.buildCostLead;
          return true;
        }
        break;
      case SAGE:
        if (buildRobot(RobotType.SAGE, Utils.randomDirection())) {
          //rc.setIndicatorString("Spawn sage!");
//          soldiersSpawned++;
//          leadSpent += RobotType.SAGE.buildCostLead;
          return true;
        }
    }
    return false;
  }

  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private RobotType determineSpawnDroidType() throws GameActionException {
    if (needSage()) return RobotType.SAGE;
    if (needMiner()) return RobotType.MINER;
    else if (needBuilder()) return RobotType.BUILDER;
    else if (needSoldier()) return RobotType.SOLDIER;
    return null;
  }

  private boolean needSage() throws GameActionException {
    return rc.getTeamGoldAmount(Cache.Permanent.OUR_TEAM) >= RobotType.SAGE.buildCostGold;
  }

  /**
   * decides if more miners are needed currently
   * @return boolean of necessity of building a miner
   */
  private boolean needMiner() throws GameActionException {
//    //System.out.printf("Archon%s checking need Miner -- \n\tminersSpawned=%d\n\trcArchonCount=%d\n\tsoldiersSpawned=%d\n", Cache.PerTurn.CURRENT_LOCATION, minersSpawned, rc.getArchonCount(), soldiersSpawned);
//    //System.out.println(Cache.PerTurn.CURRENT_LOCATION + " --\nminersSpawned: " + minersSpawned + "\nrc.getArchonCount(): " + rc.getArchonCount() + "\nsoldiersSpawned: " + soldiersSpawned);
    // TODO: something based on lead income

    return minersSpawned < initialMinersToSpawn
        || (/*minersSpawned < soldiersSpawned / 1.5 &&*/ minersSpawned * rc.getArchonCount() <= 15 + Cache.PerTurn.ROUND_NUM / 50);
//        || (movingAvgIncome < 3);
//    return rc.getTeamLeadAmount(rc.getTeam()) < 500 && ( // if we have > 2000Pb, just skip miners
////        movingAvgIncome < 10
//        (rc.getRoundNum() < 100 && localLead > 15 && movingAvgIncome < rc.getRoundNum()*1.5)
//        || (rc.getRoundNum() < 100 && localLead < 15)
////        || movingAvgIncome < localLead
//        || (localLead > 10 && localLead < rc.senseNearbyRobots(Cache.Permanent.VISION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM).length) // lots of local lead available
//        || estimateAvgLeadIncome() / (minersSpawned+1) > 3 // spawn miners until we reach less than 5pb/miner income
//    );
  }

  /**
   * decides if a builder needs to be built
   *    ASSUMES - needMiner == false
   * @return boolean of need for builder
   */
  private boolean needBuilder() {
    return rc.getTeamLeadAmount(rc.getTeam()) > 200 && (  // if lots of lead, make builder to spend that lead
        rc.getRoundNum() % 11 <= rc.getArchonCount()
        || buildersSpawned < 5
    );
  }

  private boolean needSoldier() throws GameActionException {
    return soldiersSpawned < 3 || soldiersSpawned < sagesSpawned / 3;
  }

  private int lastHealedID = -1;
  private void healNearbyDroids() throws GameActionException {
    // find lowest health soldier <20 health, and heal it
    // if does not exist, find highest health soldier < 49 and heal it
    // if does not exist, heal something
    RobotInfo lowestHealthFriend = null;
    RobotInfo highestHealthFriend = null;
    RobotInfo lowestOtherHealthFriend = null;
    RobotInfo higherOtherHealthFriend = null;

    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      if (RobotType.ARCHON.canRepair(friend.type) && Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(friend.location, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        if (friend.type.damage > 0) {
          if ((lowestHealthFriend == null || friend.health < lowestHealthFriend.health) && friend.health < CRITICAL_HEALTH_TO_HEAL_LOWEST) {
            lowestHealthFriend = friend;
          }
          if ((highestHealthFriend == null || friend.health > highestHealthFriend.health) && friend.health < friend.type.health) {
            highestHealthFriend = friend;
          }
        } else {
          if ((lowestOtherHealthFriend == null || friend.health < lowestOtherHealthFriend.health) && friend.health < friend.type.health) {
            lowestOtherHealthFriend = friend;
          }
        }
        if ((higherOtherHealthFriend == null || higherOtherHealthFriend.health < friend.health) && friend.health < friend.type.health) {
          higherOtherHealthFriend = friend;
        }
      }
    }

    if (lowestHealthFriend != null) rc.repair(lowestHealthFriend.location);
    else if (highestHealthFriend != null) rc.repair(highestHealthFriend.location);
    else if (lowestOtherHealthFriend != null) rc.repair(lowestOtherHealthFriend.location);
    else if (higherOtherHealthFriend != null) rc.repair(higherOtherHealthFriend.location);
//    //System.out.println("movingAvgIncome: " + movingAvgIncome);
//    if (rc.getTeamLeadAmount(Cache.Permanent.OUR_TEAM) < 60 - movingAvgIncome) {
//    if (lastHealedID != -1) {
//      if (rc.canSenseRobot(lastHealedID)) {
//        RobotInfo friendToHeal = rc.senseRobot(lastHealedID);
//        if (friendToHeal.health < friendToHeal.type.health && friendToHeal.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
//          rc.repair(friendToHeal.location);
//          return;
//        }
//      }
//    }
//    lastHealedID = -1;
//    RobotInfo lowestHealthFriend = null;
//    int lowestHealth = 9999;
//    for (RobotInfo info : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM)) {
//      if (Cache.Permanent.ROBOT_TYPE.canRepair(info.type) && info.health < lowestHealth && info.health < info.type.health) { // we see a damaged friendly
//        lowestHealth = info.health;
//        lowestHealthFriend = info;
//      }
//    }
//    if (lowestHealthFriend != null) {
//      rc.repair(lowestHealthFriend.location);
//      lastHealedID = lowestHealthFriend.ID;
//    }
//    } else {
//      //rc.setIndicatorString("No repair, save lead: " + rc.getTeamLeadAmount(Cache.Permanent.OUR_TEAM) + " / " + (60 - movingAvgIncome));
//    }
  }

  private void sendLeadRefreshMessages() {
    if (rc.getRoundNum() % 20 != 0) return; //do this on every 20th round

    // go through chunk messages in communication

    //

  }

  private void drawChunkBounds() {
    if (Cache.PerTurn.ROUND_NUM % 10 == 1) {
      int chunksPerArchon = Utils.MAX_MAP_CHUNKS / rc.getArchonCount();
      for (int chunk = chunksPerArchon * (whichArchonAmI-2), max = chunksPerArchon * (whichArchonAmI-1) + 10; chunk < max; chunk++) {
        MapLocation loc = Utils.chunkIndexToLocation(chunk);
//        //System.out.println("loc: " + loc + " chunk: " + chunk2 + " decode: " + Utils.chunkIndexToLocation(chunk));
        int width = Cache.Permanent.MAP_WIDTH;
        int height = Cache.Permanent.MAP_HEIGHT;
        int x = loc.x;
        int y = loc.y;
        int dimsMin255 = 255 * (width - x) * (height - y);
        int xy255 = 255 * x * y;
        int r = 2 * dimsMin255 / Cache.Permanent.MAP_AREA;

        int g = (dimsMin255 + xy255) / Cache.Permanent.MAP_AREA;

        int b = 2 * xy255 / Cache.Permanent.MAP_AREA;

        //rc.setIndicatorDot(loc, r, g, b);

        MapLocation tl = loc.translate(-Cache.Permanent.CHUNK_WIDTH/2, -1 + (int) Math.ceil(Cache.Permanent.CHUNK_HEIGHT/2.));
        MapLocation tr = loc.translate(-1 + (int) Math.ceil(Cache.Permanent.CHUNK_WIDTH/2.), -1 + (int) Math.ceil(Cache.Permanent.CHUNK_HEIGHT/2.));
        MapLocation bl = loc.translate(-Cache.Permanent.CHUNK_WIDTH/2, -Cache.Permanent.CHUNK_HEIGHT/2);
        MapLocation br = loc.translate(-1 + (int) Math.ceil(Cache.Permanent.CHUNK_WIDTH/2.), -Cache.Permanent.CHUNK_HEIGHT/2);

        //rc.setIndicatorLine(tl, tr, r, g, b);
        //rc.setIndicatorLine(tl, bl, r, g, b);
        //rc.setIndicatorLine(br, tr, r, g, b);
        //rc.setIndicatorLine(br, bl, r, g, b);
        if (chunk < 33) {
          //rc.setIndicatorDot(loc, 255 / 33 * chunk, 0, 0);
        } else if (chunk < 66) {
          //rc.setIndicatorDot(loc, 255 / 33 * (chunk - 33), 255 / 33 * (chunk - 33), 0);
        } else {
          //rc.setIndicatorDot(loc, 0, 255 / 34 * (chunk - 66), 255 / 34 * (chunk - 66));
        }

      }
    }
  }

}
