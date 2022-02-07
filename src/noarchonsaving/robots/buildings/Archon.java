package noarchonsaving.robots.buildings;

import battlecode.common.AnomalyType;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import noarchonsaving.Utils;
import noarchonsaving.communications.messages.ArchonHelloMessage;
import noarchonsaving.communications.messages.Message;

import java.util.ArrayList;
import java.util.List;

public class Archon extends Building {
  public static final int SUICIDE_ROUND = -500;

  private int whichArchonAmI;
  private List<MapLocation> archonLocs;

  private final int localLead;
//  private MapSymmetry predictedSymmetry;

  private int minersSpawned;
  private int buildersSpawned;
  private int soldiersSpawned;
  private int sagesSpawned;

  private int lastTurnStartingLead;
  private int leadIncome;
  private int leadSpent;

  public Archon(RobotController rc) throws GameActionException {
    super(rc);
    whichArchonAmI = rc.getID() >> 1; // floor(id / 2)
    archonLocs = new ArrayList<>();
//    //System.out.println("Hello from Archon constructor #"+whichArchonAmI + " at " + rc.getLocation());
    localLead = rc.senseNearbyLocationsWithLead(creationStats.visionRad).length;

    lastTurnStartingLead = 0;
    leadIncome = 0;
    leadSpent = 0;
  }

  @Override
  protected void runTurn() throws GameActionException {
    leadIncome = rc.getTeamLeadAmount(creationStats.myTeam) - lastTurnStartingLead + leadSpent;
    lastTurnStartingLead = rc.getTeamLeadAmount(creationStats.myTeam);
    leadSpent = 0;
//    if (whichArchonAmI == rc.getArchonCount()) {
//      //System.out.println("Lead income: " + leadIncome);
//    }
    if (rc.getRoundNum() == 1 && !doFirstTurn()) { // executes turn 1 and continues if needed
      return;
    }

    // Repair damaged droid
    if (rc.isActionReady()) {
      for (RobotInfo info : rc.senseNearbyRobots(creationStats.type.actionRadiusSquared, creationStats.myTeam)) {
        if (creationStats.type.canRepair(info.type) && info.health < info.type.getMaxHealth(info.level)) { // we see a damaged friendly
          rc.repair(info.location);
          break;
        }
      }
    }

//    //System.out.println("rng bound: " + (rc.getArchonCount()-whichArchonAmI+3));

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
    lastTurnStartingLead = rc.getTeamLeadAmount(creationStats.myTeam);
  }

  /**
   * Run the first turn for this archon
   * @return if running should continue
   */
  private boolean doFirstTurn() {
//    //System.out.println("Hello from Archon #"+whichArchonAmI + " at " + rc.getLocation());
    ArchonHelloMessage helloMessage = generateArchonHello();
    communicator.enqueueMessage(helloMessage);
    archonLocs.add(rc.getLocation());

    if (whichArchonAmI == rc.getArchonCount()) {
      //System.out.println("I am the last archon! locs: " + archonLocs);

    }

    return true;
  }

  private ArchonHelloMessage generateArchonHello() {
//    boolean notHoriz = false;
//    MapLocation myLoc = rc.getLocation();
//    int width = rc.getMapWidth();
//    int height = rc.getMapHeight();
//    int dToPastCenter = Math.abs(myLoc.x - width) + 1;
//    if (dToPastCenter*dToPastCenter <= rc.getType().visionRadiusSquared) { // can see both sides of the width midpoint
//      //System.out.println("archon at " + myLoc + " - can see width midpoint");
////      rc.senseRubble()
//    }
    return new ArchonHelloMessage(rc.getLocation(), false, false, false);
  }

  @Override
  protected void ackMessage(Message message) throws GameActionException {
    if (message instanceof ArchonHelloMessage) {
      ackArchonHello((ArchonHelloMessage) message);
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
//    //System.out.println("Got archon hello!");
  }

  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private void spawnDroid() throws GameActionException {
    if (needMiner()) {
      if (buildRobot(RobotType.MINER, getBestLeadDirProbabilistic())) {
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
    return rc.getTeamLeadAmount(rc.getTeam()) < 2000 && ( // if we have > 2000Pb, just skip miners
        rc.getRoundNum() < 100
        || leadIncome < localLead
//        || (localLead > 10 && localLead < rc.senseNearbyRobots(creationStats.visionRad, creationStats.myTeam).length) // lots of local lead available
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
