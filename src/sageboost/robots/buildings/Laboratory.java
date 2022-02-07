package sageboost.robots.buildings;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotMode;

public class Laboratory extends Building {

  int rate;

  public Laboratory(RobotController rc) throws GameActionException {
    super(rc);
  }

  @Override
  protected void runTurn() throws GameActionException {
    rate = rc.getTransmutationRate();

    // I am now spawned,
    // if I am in portiable
    if (rc.getMode() == RobotMode.PROTOTYPE) {
      // maybe do some calculations while I can't do anything
      runPrototype();
      return;
    }

//    if (rate > 2) {
//      //System.out.println("Not optimal rate: " + rate);
//    } else {
//      //System.out.println("Transmuting optimally");
//    }

    if (rc.canTransmute()) {
      rc.transmute();
    }

  }

  // If there are lots of enemies near me, RUN BOI

  private void runPrototype() {
  }
}
