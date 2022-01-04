package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import firstbot.Constants;

public class Soldier extends Droid {
  public Soldier(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Try to attack someone
    int radius = rc.getType().actionRadiusSquared;
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
    if (enemies.length > 0) {
      MapLocation toAttack = enemies[0].location;
      if (rc.canAttack(toAttack)) {
        rc.attack(toAttack);
      }
    }

    // Also try to move randomly.
    Direction dir = Constants.directions[Constants.rng.nextInt(Constants.directions.length)];
    if (rc.canMove(dir)) {
      rc.move(dir);
      System.out.println("I moved!");
    }
  }
}
