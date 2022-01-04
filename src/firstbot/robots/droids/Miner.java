package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import firstbot.Constants;

public class Miner extends Droid {
  public Miner(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    // Try to mine on squares around us.
    MapLocation me = rc.getLocation();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
        // Notice that the Miner's action cooldown is very low.
        // You can mine multiple times per turn!
        while (rc.canMineGold(mineLocation)) {
          rc.mineGold(mineLocation);
        }
        while (rc.canMineLead(mineLocation)) {
          rc.mineLead(mineLocation);
        }
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
