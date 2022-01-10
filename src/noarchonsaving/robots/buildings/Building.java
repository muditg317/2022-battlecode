package noarchonsaving.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import noarchonsaving.robots.Robot;

public abstract class Building extends Robot {
  public Building(RobotController rc) throws GameActionException {
    super(rc);
  }
}
