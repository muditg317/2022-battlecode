package soldierrunhome.robots.buildings;

import battlecode.common.*;
import soldierrunhome.communications.messages.ArchonHelloMessage;
import soldierrunhome.communications.messages.ArchonSavedMessage;
import soldierrunhome.communications.messages.Message;
import soldierrunhome.communications.messages.SaveMeMessage;
import soldierrunhome.utils.Cache;
import soldierrunhome.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Archon extends Building {
  public static final int SUICIDE_ROUND = -50;

  private int whichArchonAmI;
  private List<MapLocation> archonLocs;

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

  public Archon(RobotController rc) throws GameActionException {
    super(rc);
    whichArchonAmI = rc.getID() >> 1; // floor(id / 2)
    archonLocs = new ArrayList<>();
//    //System.out.println("Hello from Archon constructor #"+whichArchonAmI + " at " + Cache.PerTurn.CURRENT_LOCATION);
    localLead = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED).length;

    lastTurnStartingLead = 0;
    incomeHistory = new int[INCOME_HISTORY_LENGTH];
    leadIncome = 0;
    leadSpent = 0;
    movingTotalIncome = 0;
    movingAvgIncome = 0;

    lastTurnHealth = 0;
    healthLostThisTurn = 0;
    saveMeRequest = null;
  }

  @Override
  protected void runTurn() throws GameActionException {
    updateHistories();
    if (rc.getRoundNum() == 1 && !doFirstTurn()) { // executes turn 1 and continues if needed
      return;
    }

//    if (Cache.PerTurn.ROUND_NUM % 10 == 1) {
//      int chunksPerArchon = Utils.NUM_MAP_CHUNKS / rc.getArchonCount();
//      for (int chunk = chunksPerArchon * (whichArchonAmI-2), max = chunksPerArchon * (whichArchonAmI-1) + 10; chunk < max; chunk++) {
//        MapLocation loc = Utils.chunkIndexToLocation(chunk);
////        //System.out.println("loc: " + loc + " chunk: " + chunk2 + " decode: " + Utils.chunkIndexToLocation(chunk));
//        int width = Cache.Permanent.MAP_WIDTH;
//        int height = Cache.Permanent.MAP_HEIGHT;
//        int x = loc.x;
//        int y = loc.y;
//        int dimsMin255 = 255 * (width - x) * (height - y);
//        int xy255 = 255 * x * y;
//        int r = 2 * dimsMin255 / Cache.Permanent.MAP_AREA;
//
//        int g = (dimsMin255 + xy255) / Cache.Permanent.MAP_AREA;
//
//        int b = 2 * xy255 / Cache.Permanent.MAP_AREA;
//
//        //rc.setIndicatorDot(loc, r, g, b);
//
//        MapLocation tl = loc.translate(-Cache.Permanent.CHUNK_WIDTH/2, -1 + (int) Math.ceil(Cache.Permanent.CHUNK_HEIGHT/2.));
//        MapLocation tr = loc.translate(-1 + (int) Math.ceil(Cache.Permanent.CHUNK_WIDTH/2.), -1 + (int) Math.ceil(Cache.Permanent.CHUNK_HEIGHT/2.));
//        MapLocation bl = loc.translate(-Cache.Permanent.CHUNK_WIDTH/2, -Cache.Permanent.CHUNK_HEIGHT/2);
//        MapLocation br = loc.translate(-1 + (int) Math.ceil(Cache.Permanent.CHUNK_WIDTH/2.), -Cache.Permanent.CHUNK_HEIGHT/2);
//
//        //rc.setIndicatorLine(tl, tr, r, g, b);
//        //rc.setIndicatorLine(tl, bl, r, g, b);
//        //rc.setIndicatorLine(br, tr, r, g, b);
//        //rc.setIndicatorLine(br, bl, r, g, b);
////        if (chunk < 33) {
////          //rc.setIndicatorDot(loc, 255 / 33 * chunk, 0, 0);
////        } else if (chunk < 66) {
////          //rc.setIndicatorDot(loc, 255 / 33 * (chunk - 33), 255 / 33 * (chunk - 33), 0);
////        } else {
////          //rc.setIndicatorDot(loc, 0, 255 / 34 * (chunk - 66), 255 / 34 * (chunk - 66));
////        }
//
//      }
//    }

    MapLocation nearbyEnemies = offensiveEnemyCentroid();
    if (saveMeRequest != null || nearbyEnemies != null) {
      if (healthLostThisTurn < Cache.PerTurn.HEALTH) broadcastSaveMe();
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        //rc.setIndicatorString("Spawn soldier!");
        soldiersSpawned++;
        leadSpent += RobotType.SOLDIER.buildCostLead;
      }
    }

    // Spawn new droid if none to repair
    int archons = rc.getArchonCount();
    if (rc.isActionReady() && rc.getRoundNum() % archons == whichArchonAmI % archons) {
      spawnDroid();
    }

    // Repair damaged droid
    if (rc.isActionReady()) {
      for (RobotInfo info : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM)) {
        if (Cache.Permanent.ROBOT_TYPE.canRepair(info.type) && info.health < info.type.getMaxHealth(info.level)) { // we see a damaged friendly
          rc.repair(info.location);
          break;
        }
      }
    }

    if (rc.getRoundNum() == SUICIDE_ROUND) {
      rc.resign();
    }
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

    updateSymmetryComms();
//    updateVisibleChunks();

    ArchonHelloMessage helloMessage = generateArchonHello();
    communicator.enqueueMessage(helloMessage);
    archonLocs.add(Cache.PerTurn.CURRENT_LOCATION);

//    if (whichArchonAmI == rc.getArchonCount()) {
//      //System.out.println("I am the last archon! locs: " + archonLocs);
//
//    }

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
//    //System.out.println("I am archon #" + whichArchonAmI + " at " + Cache.PerTurn.CURRENT_LOCATION + " with " + leadAtArchonLocation + " lead and " + avgRubble + " avg rubble" + " and " + totalMoves + " moves to reach enemy archon" + " and " + initialMinersToSpawn + " miners to spawn");

    // find direction s.t. the miner can go far in the direction


    return true;
  }

  /**
   * generate a message to be sent to the other arhcons on the first turn of the game
   *    this message contains various bits of info that should be shared
   *    - location
   *    - symmetry info (TODO: implement)
   * @return the message to send to the other archons
   */
  private ArchonHelloMessage generateArchonHello() {
//    boolean notHoriz = false;
//    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
//    int width = Cache.Permanent.MAP_WIDTH;
//    int height = Cache.Permanent.MAP_HEIGHT;
//    int dToPastCenter = Math.abs(myLoc.x - width) + 1;
//    if (dToPastCenter*dToPastCenter <= rc.getType().visionRadiusSquared) { // can see both sides of the width midpoint
//      //System.out.println("archon at " + myLoc + " - can see width midpoint");
////      rc.senseRubble()
//    }
    return new ArchonHelloMessage(Cache.PerTurn.CURRENT_LOCATION, false, false, false);
  }

  @Override
  public void ackMessage(Message message) throws GameActionException {
    super.ackMessage(message);
    if (message instanceof ArchonHelloMessage) {
      ackArchonHello((ArchonHelloMessage) message);
    } else if (message instanceof ArchonSavedMessage) {
      ackArchonSaved((ArchonSavedMessage) message);
    }
  }

  /**
   * acknowledge a hello from another archon
   * @param message the hello
   */
  public void ackArchonHello(ArchonHelloMessage message) {
    archonLocs.add(message.location);
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
   * archon is bouta die, request saving
   * store request internally
   * @throws GameActionException if messaging fails
   */
  private void broadcastSaveMe() throws GameActionException {
    MapLocation toSave = offensiveEnemyCentroid();
    saveMeRequest = new SaveMeMessage(toSave != null ? toSave : Cache.PerTurn.CURRENT_LOCATION, rc.getRoundNum());
    communicator.enqueueMessage(saveMeRequest);
  }

  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private void spawnDroid() throws GameActionException {
    if (needMiner()) {
      MapLocation bestLead = getWeightedAvgLeadLoc();
      // TODO: if bestLead is null, spawn on low rubble instead of random

      Direction dir = bestLead == null
          ? Utils.randomDirection()
          : Cache.PerTurn.CURRENT_LOCATION.directionTo(bestLead);
      // using getOptimalDirectionTowards(bestLead) causes slightly worse performance lol
      if (dir == Direction.CENTER) dir = Utils.randomDirection();

//      //System.out.println("I need a miner! bestLead: " + bestLead + " dir: " + dir);
      if (buildRobot(RobotType.MINER, dir)) {
        //rc.setIndicatorString("Spawn miner!");
        minersSpawned++;
        leadSpent += RobotType.MINER.buildCostLead;
      }
    } else if (needBuilder()) {
      if (buildRobot(RobotType.BUILDER, Utils.randomDirection())) {
        //rc.setIndicatorString("Spawn builder!");
        buildersSpawned++;
        leadSpent += RobotType.BUILDER.buildCostLead;
      }
    } else {
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        //rc.setIndicatorString("Spawn soldier!");
        soldiersSpawned++;
        leadSpent += RobotType.SOLDIER.buildCostLead;
      }
    }
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
        || (minersSpawned < soldiersSpawned / 2.0 && minersSpawned * rc.getArchonCount() <= 15 + Cache.PerTurn.ROUND_NUM / 100);
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
        rc.getRoundNum() % 10 == 0
        || buildersSpawned < 5
    );
  }

  private void sendLeadRefreshMessages() {
    if (rc.getRoundNum() % 20 != 0) return; //do this on every 20th round

    // go through chunk messages in communication

    //

  }

}
