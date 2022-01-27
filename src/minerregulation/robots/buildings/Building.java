package minerregulation.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import minerregulation.robots.Robot;

public abstract class Building extends Robot {
  public Building(RobotController rc) throws GameActionException {
    super(rc);
    updateSymmetryComms();
//    updateVisibleChunks();
  }
}
