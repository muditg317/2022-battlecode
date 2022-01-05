package firstbot.robots.buildings;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import firstbot.Utils;
import firstbot.communications.messages.SingleIntMessage;

public class Archon extends Building {

  private int minersSpawned;
  private int buildersSpawned;
  private int soldiersSpawned;
  private int sagesSpawned;

  public Archon(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Pick a direction to build in.
    Direction dir = getBestLeadDirProbabilistic();
    // Let's try to build a droid.
    if (rc.isActionReady()) {
      spawnDroid();
    }

    if (rc.getRoundNum() == 1) {
      communicator.enqueueMessage(new SingleIntMessage(69, 10), 10);
      communicator.enqueueMessage(new SingleIntMessage(420, 20), 20);
    }
  }

  /**
   * Spawn some droid around the archon based on some heuristics
   */
  private void spawnDroid() throws GameActionException {
    if (needMiner()) {
      if (buildRobot(RobotType.MINER, getBestLeadDirProbabilistic())) {
        rc.setIndicatorString("Build miner!");
        minersSpawned++;
      }
    } else {
      if (buildRobot(RobotType.SOLDIER, Utils.randomDirection())) {
        rc.setIndicatorString("Build soldier!");
        soldiersSpawned++;
      }
    }
  }

  /**
   * decides if more miners are needed currently
   * @return boolean of necessity of building a miner
   */
  private boolean needMiner() {
    return rc.getRoundNum() < 100
        || estimateAvgLeadIncome() / minersSpawned > 5; // spawn miners until we reach less than 5pb/miner income
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
   * @return the estimated avg lead/round income
   */
  private int estimateAvgLeadIncome() {
    return estimateTotalLeadInGame() / rc.getRoundNum();
  }


}
