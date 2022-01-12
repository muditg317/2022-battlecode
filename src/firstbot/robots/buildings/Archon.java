package firstbot.robots.buildings;

import battlecode.common.AnomalyType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import firstbot.utils.Cache;
import firstbot.utils.Utils;
import firstbot.communications.messages.ArchonHelloMessage;
import firstbot.communications.messages.ArchonSavedMessage;
import firstbot.communications.messages.Message;
import firstbot.communications.messages.SaveMeMessage;

import java.util.ArrayList;
import java.util.List;

public class Archon extends Building {
  public static final int SUICIDE_ROUND = 5;

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

  public Archon(RobotController rc) throws GameActionException {
    super(rc);
    whichArchonAmI = rc.getID() >> 1; // floor(id / 2)
    archonLocs = new ArrayList<>();
//    System.out.println("Hello from Archon constructor #"+whichArchonAmI + " at " + Cache.PerTurn.CURRENT_LOCATION);
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

    if (healthLostThisTurn < Cache.PerTurn.HEALTH && (saveMeRequest != null || offensiveEnemiesNearby())) {
      broadcastSaveMe();
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        rc.setIndicatorString("Spawn soldier!");
        soldiersSpawned++;
        leadSpent += RobotType.SOLDIER.buildCostLead;
      }
    }

//    if (saveMeRequest != null) {
//      broadcastSaveMe();
//    }

    // Repair damaged droid
    if (rc.isActionReady()) {
      for (RobotInfo info : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM)) {
        if (Cache.Permanent.ROBOT_TYPE.canRepair(info.type) && info.health < info.type.getMaxHealth(info.level)) { // we see a damaged friendly
          rc.repair(info.location);
          break;
        }
      }
    }

//    System.out.println("rng bound: " + (rc.getArchonCount()-whichArchonAmI+3));

    // Spawn new droid if none to repair
    int archons = rc.getArchonCount();
    if (rc.isActionReady() && rc.getRoundNum() % archons == whichArchonAmI % archons) {
//    if (rc.isActionReady() && (Utils.rng.nextInt(rc.getArchonCount()-whichArchonAmI+2) <= 1)) {
//    if (rc.isActionReady() && (Utils.rng.nextInt((rc.getArchonCount()>>1)|1) <= whichArchonAmI>>1)) {
        //Utils.rng.nextInt(Math.max(1, rc.getArchonCount()-whichArchonAmI)) <= 1
      spawnDroid();
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
//    rc.setIndicatorString("income - " + leadIncome + " avg: " + movingAvgIncome + " tot: " + movingTotalIncome);
//    if (whichArchonAmI == rc.getArchonCount()) {
//      System.out.println("Lead income: " + leadIncome);
//    }

    healthLostThisTurn = lastTurnHealth - Cache.PerTurn.HEALTH;
    rc.setIndicatorString("health: " + Cache.PerTurn.HEALTH + " - lastHP: " + lastTurnHealth + " - lost: " + healthLostThisTurn);
    lastTurnHealth = Cache.PerTurn.HEALTH;

    
  }

  /**
   * Run the first turn for this archon
   * @return if running should continue
   */
  private boolean doFirstTurn() {
//    System.out.println("Hello from Archon #"+whichArchonAmI + " at " + Cache.PerTurn.CURRENT_LOCATION);
    ArchonHelloMessage helloMessage = generateArchonHello();
    communicator.enqueueMessage(helloMessage);
    archonLocs.add(Cache.PerTurn.CURRENT_LOCATION);

    if (whichArchonAmI == rc.getArchonCount()) {
      System.out.println("I am the last archon! locs: " + archonLocs);

    }

    return true;
  }

  private ArchonHelloMessage generateArchonHello() {
//    boolean notHoriz = false;
//    MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
//    int width = Cache.Permanent.MAP_WIDTH;
//    int height = Cache.Permanent.MAP_HEIGHT;
//    int dToPastCenter = Math.abs(myLoc.x - width) + 1;
//    if (dToPastCenter*dToPastCenter <= rc.getType().visionRadiusSquared) { // can see both sides of the width midpoint
//      System.out.println("archon at " + myLoc + " - can see width midpoint");
////      rc.senseRubble()
//    }
    return new ArchonHelloMessage(Cache.PerTurn.CURRENT_LOCATION, false, false, false);
  }

  @Override
  protected void ackMessage(Message message) throws GameActionException {
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
//    if (rc.getRoundNum() == 1)
//      whichArchonAmI++;
    archonLocs.add(message.location);
//    System.out.println("Got archon hello!");
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
//      System.out.println("Ignore archon saved message: " + (saveMeRequest != null ? saveMeRequest.location : "null") + " vs " + message.location);
    }
  }

  /**
   * archon is bouta die, request saving
   * store request internally
   * @throws GameActionException if messaging fails
   */
  private void broadcastSaveMe() throws GameActionException {
    saveMeRequest = new SaveMeMessage(Cache.PerTurn.CURRENT_LOCATION, rc.getRoundNum());
    communicator.enqueueMessage(saveMeRequest);
  }

  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private void spawnDroid() throws GameActionException {
    if (needMiner()) {
      MapLocation bestLead = getBestLeadLocProbabilistic();
      Direction dir = bestLead == null ? Utils.randomDirection() : Cache.PerTurn.CURRENT_LOCATION.directionTo(bestLead);
      if (buildRobot(RobotType.MINER, dir)) {
        rc.setIndicatorString("Spawn miner!");
        minersSpawned++;
        leadSpent += RobotType.MINER.buildCostLead;
      }
    } else if (needBuilder()) {
      if (buildRobot(RobotType.BUILDER, Utils.randomDirection())) {
        rc.setIndicatorString("Spawn builder!");
        buildersSpawned++;
        leadSpent += RobotType.BUILDER.buildCostLead;
      }
    } else {
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        rc.setIndicatorString("Spawn soldier!");
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
    return rc.getTeamLeadAmount(rc.getTeam()) < 500 && ( // if we have > 2000Pb, just skip miners
//        movingAvgIncome < 10
        (rc.getRoundNum() < 100 && localLead > 15 && movingAvgIncome < rc.getRoundNum()*1.5)
        || (rc.getRoundNum() < 100 && localLead < 15)
//        || movingAvgIncome < localLead
        || (localLead > 10 && localLead < rc.senseNearbyRobots(Cache.Permanent.VISION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM).length) // lots of local lead available
        || estimateAvgLeadIncome() / (minersSpawned+1) > 3 // spawn miners until we reach less than 5pb/miner income
    );
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
//        || getRoundsSinceLastAnomaly(AnomalyType.CHARGE) / 50 < buildersSpawned
    ); // need at least 1 builder per X rounds since charge anomaly
  }

  /**
   * estimates the amount of lead that has been spent by the whole team + the currently useful lead
   *    has no idea about builder expenditure
   * @return the estimated lead total
   */
  private int estimateTotalLeadInGame() {
    return rc.getArchonCount() * (
          minersSpawned * RobotType.MINER.buildCostLead
        + buildersSpawned * RobotType.BUILDER.buildCostLead
        + soldiersSpawned * RobotType.SOLDIER.buildCostLead
        + sagesSpawned * RobotType.SAGE.buildCostLead
        );
  }

  /**
   * estimates the average lead income per round of the game
   *    based on the estimateTotalLeadInGame
   *    resets round counter when charges occur (because most miners should be wiped by charge)
   * @return the estimated avg lead/round income
   */
  private int estimateAvgLeadIncome() {
    return estimateTotalLeadInGame() / (1 + getRoundsSinceLastAnomaly(AnomalyType.CHARGE));
  }


}
