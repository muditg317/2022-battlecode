package firstbot.robots.buildings;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import firstbot.Constants;

public class Archon extends Building {
  public Archon(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Pick a direction to build in.
    Direction dir = Constants.directions[Constants.rng.nextInt(Constants.directions.length)];
    if (Constants.rng.nextBoolean()) {
      // Let's try to build a miner.
      rc.setIndicatorString("Trying to build a miner");
      if (rc.canBuildRobot(RobotType.MINER, dir)) {
        rc.buildRobot(RobotType.MINER, dir);
      }
    } else {
      // Let's try to build a soldier.
      rc.setIndicatorString("Trying to build a soldier");
      if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
        rc.buildRobot(RobotType.SOLDIER, dir);
      }
    }
  }


}
