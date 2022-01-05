package firstbot.robots.buildings;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import firstbot.Constants;
import firstbot.communications.messages.SingleIntMessage;

public class Archon extends Building {
  public Archon(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Pick a direction to build in.
    Direction dir = Constants.directions[Constants.rng.nextInt(Constants.directions.length)];
    // Let's try to build a miner.
    rc.setIndicatorString("Trying to build a miner");
    if (rc.canBuildRobot(RobotType.MINER, dir)) {
      rc.buildRobot(RobotType.MINER, dir);
    }

    if (rc.getRoundNum() == 1) {
      communicator.enqueueMessage(new SingleIntMessage(69, 10), 10);
      communicator.enqueueMessage(new SingleIntMessage(420, 20), 20);
    }
  }


}
