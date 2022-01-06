package firstbot.robots.buildings;

import battlecode.common.AnomalyType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import firstbot.Utils;
import firstbot.communications.messages.SingleIntMessage;

public class Archon extends Building {
  public static final int SUICIDE_ROUND = -1;

  private int minersSpawned;
  private int buildersSpawned;
  private int soldiersSpawned;
  private int sagesSpawned;

  public Archon(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Repair damaged droid
    if (rc.isActionReady()) {
      for (RobotInfo info : rc.senseNearbyRobots()) {
        if (info.getTeam().isPlayer() && info.getHealth() < info.getType().getMaxHealth(info.getLevel())) { // we see a damaged friendly
          if (rc.canRepair(info.getLocation())) rc.repair(info.getLocation());
        }
      }
    }

    // Spawn new droid if none to repair
    if (rc.isActionReady() && rc.getRoundNum() == 1) {
      spawnDroid();
    }

    // send test messages
//    if (rc.getRoundNum() == 1) {
//      communicator.enqueueMessage(new SingleIntMessage(69, 10), 10);
//      communicator.enqueueMessage(new SingleIntMessage(420, 20), 20);
//    }

    if (rc.getRoundNum() == SUICIDE_ROUND) {
      rc.resign();
    }
  }

  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private void spawnDroid() throws GameActionException {
    if (needMiner()) {
      if (buildRobot(RobotType.MINER, getBestLeadDirProbabilistic())) {
        rc.setIndicatorString("Spawn miner!");
        minersSpawned++;
      }
    } else if (needBuilder()) {
      if (buildRobot(RobotType.BUILDER, Utils.randomDirection())) {
        rc.setIndicatorString("Spawn builder!");
        soldiersSpawned++;
      }
    } else {
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        rc.setIndicatorString("Spawn soldier!");
        soldiersSpawned++;
      }
    }
  }

  /**
   * decides if more miners are needed currently
   * @return boolean of necessity of building a miner
   */
  private boolean needMiner() {
    return rc.getTeamLeadAmount(rc.getTeam()) < 2000 && ( // if we have > 2000Pb, just skip miners
        rc.getRoundNum() < 100
        || estimateAvgLeadIncome() / minersSpawned > 3 // spawn miners until we reach less than 5pb/miner income
    );
  }

  /**
   * decides if a builder needs to be built
   *    ASSUMES - needMiner == false
   * @return boolean of need for builder
   */
  private boolean needBuilder() {
    return rc.getTeamLeadAmount(rc.getTeam()) > 2000 // if lots of lead, make builder to spend that lead
        || getRoundsSinceLastAnomaly(AnomalyType.CHARGE) / 50 < buildersSpawned; // need at least 1 builder per X rounds since charge anomaly
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
