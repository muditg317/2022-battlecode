package builderbugfix.robots.droids;

import battlecode.common.*;
import builderbugfix.utils.Cache;
import builderbugfix.utils.Utils;

public class Sage extends Soldier {
  public Sage(RobotController rc) throws GameActionException {
    super(rc);
  }

  @Override
  protected boolean attackEnemies() throws GameActionException {
    MicroInfo best = null;
//    Cache.PerTurn.cacheEnemyInfos();
//    if (Cache.Permanent.ID == 10532 && Cache.PerTurn.ROUND_NUM == 259) {
//      Printer.cleanPrint();
//      Printer.print("isMovementDisabled: " + isMovementDisabled);
//      Printer.print("needToRunHomeForSaving: " + needToRunHomeForSaving,"needToRunHomeForSuicide: " + needToRunHomeForSuicide);
//      Printer.print("movementCooldown: " + rc.getMovementCooldownTurns(), "actionCooldown: " + rc.getActionCooldownTurns());
//      Printer.submitPrint();
//    }
    for (Direction dir : Utils.directionsNine) {
//      if (Cache.Permanent.ID == 10532 && Cache.PerTurn.ROUND_NUM == 259) {
//        //Printer.cleanPrint();
//        //Printer.print("test move in dir: " + dir);
//        //Printer.print("rc.canMove: " + rc.canMove(dir));
//        //Printer.submitPrint();
//      }
      if (dir != Direction.CENTER && (isMovementDisabled || !rc.canMove(dir))) continue;
//      MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//      if (dir != Direction.CENTER && Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS.length < 6 && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION)))) {
//        if (!newLoc.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(newLoc), newLoc.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(newLoc)))) {
//          continue;
//        }
//      }
//      //Printer.cleanPrint();
      MicroInfo curr = new MicroInfo.MicroInfoSages(this, dir);
      switch (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length) {
        case 10:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[9]);
        case 9:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[8]);
        case 8:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[7]);
        case 7:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[6]);
        case 6:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[5]);
        case 5:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[4]);
        case 4:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[3]);
        case 3:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[2]);
        case 2:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[1]);
        case 1:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[0]);
          break;
        default:
          for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
//            int s = Clock.getBytecodeNum();
            curr.update(enemy);
//            //Printer.print("Bytecode for 1 update: " + (Clock.getBytecodeNum() - s));
          }
      }
      curr.finalizeInfo();
//      if (Cache.Permanent.ID == 10532 && Cache.PerTurn.ROUND_NUM == 259) {
//        ((MicroInfo.MicroInfoSages)curr).utilPrint();
//      }
      if (best == null || curr.isBetterThan(best)) {
        best = curr;
      }
    }

    return best != null && best.execute();
  }

  /**
   * envision the provided anomaly
   * @param anomalyType the anomaly to envision
   * @return true if envisioned successfully
   * @throws GameActionException if envisioning fails
   */
  public boolean envision(AnomalyType anomalyType) throws GameActionException {
    if (rc.canEnvision(anomalyType)) {
      rc.envision(anomalyType);
      return true;
    }
    return false;
  }
}
