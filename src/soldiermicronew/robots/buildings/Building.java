package soldiermicronew.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import soldiermicronew.robots.Robot;

public abstract class Building extends Robot {
  public Building(RobotController rc) throws GameActionException {
    super(rc);
  }
}
