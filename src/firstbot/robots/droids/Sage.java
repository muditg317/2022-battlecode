package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import firstbot.utils.Cache;
import firstbot.utils.Printer;
import firstbot.utils.Utils;

public class Sage extends Soldier {
  public Sage(RobotController rc) throws GameActionException {
    super(rc);
  }

  @Override
  protected boolean attackEnemies() throws GameActionException {
    MicroInfo best = null;
//    Cache.PerTurn.cacheEnemyInfos();
    for (Direction dir : Utils.directionsNine) {
      if (dir != Direction.CENTER && (isMovementDisabled || !rc.canMove(dir))) continue;
//      MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//      if (dir != Direction.CENTER && Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS.length < 6 && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION)))) {
//        if (!newLoc.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(newLoc), newLoc.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(newLoc)))) {
//          continue;
//        }
//      }
      Printer.cleanPrint();
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
//            Printer.print("Bytecode for 1 update: " + (Clock.getBytecodeNum() - s));
          }
      }
      curr.finalizeInfo();
      if (best == null || curr.isBetterThan(best)) {
        best = curr;
      }
    }

    return best != null && best.execute();
  }
}
