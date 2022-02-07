package furyandminerrunning.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import furyandminerrunning.utils.Cache;

public class Watchtower extends Building {
  public Watchtower(RobotController rc) throws GameActionException {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Try to attack someone
    if (rc.isActionReady()) {
      for (RobotInfo enemy : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
        MapLocation toAttack = enemy.location;
        if (rc.canAttack(toAttack)) {
          rc.attack(toAttack);
        }
      }
    }
  }
}
