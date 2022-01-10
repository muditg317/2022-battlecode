package noarchonsaving.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Watchtower extends Building {
  public Watchtower(RobotController rc) throws GameActionException {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Try to attack someone
    if (rc.isActionReady()) {
      for (RobotInfo enemy : rc.senseNearbyRobots(creationStats.actionRad, creationStats.opponent)) {
        MapLocation toAttack = enemy.location;
        if (rc.canAttack(toAttack)) {
          rc.attack(toAttack);
        }
      }
    }
  }
}
