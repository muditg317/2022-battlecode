package firstbot.robots.droids;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import firstbot.communications.messages.SingleIntMessage;

public class Miner extends Droid {
  public Miner(RobotController rc) {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    mineSurroundingGold();
    mineSurroundingLead();

    moveToHighLeadProbabilistic();
//    moveRandomly();

    SingleIntMessage read = (SingleIntMessage) communicator.readMessageAt(0);
    if (read == null) return;
    rc.setIndicatorString("Reading message: " + read.data);
  }

  /**
   * subroutine to mine gold from all adjacent tiles
   */
  private void mineSurroundingGold() throws GameActionException {
    // Try to mine on squares around us.
    MapLocation me = rc.getLocation();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
        while (rc.canMineGold(mineLocation)) {
          rc.mineGold(mineLocation);
        }
      }
    }
  }

  /**
   * subroutine to mine lead from all adjacent tiles
   * leaves 1pb in every tile
   */
  private void mineSurroundingLead() throws GameActionException {
    // Try to mine on squares around us.
    MapLocation me = rc.getLocation();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
        while (rc.canSenseLocation(mineLocation) && rc.senseLead(mineLocation) > 1 && rc.canMineLead(mineLocation)) {
          rc.mineLead(mineLocation);
        }
      }
    }
  }
}
