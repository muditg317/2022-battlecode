package firstbot.robots.droids;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import firstbot.communications.messages.EndRaidMessage;
import firstbot.communications.messages.Message;
import firstbot.communications.messages.StartRaidMessage;

public class Soldier extends Droid {

  boolean justBorn;
  MapLocation parentArchonLoc;
  MapLocation oppositeLoc;
  final MapLocation center;

  int visionSize;

  MapLocation raidTarget;

  public Soldier(RobotController rc) {
    super(rc);
    justBorn = true;
    center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
  }

  @Override
  protected void runTurn() throws GameActionException {
    if (justBorn) {
      visionSize = rc.getAllLocationsWithinRadiusSquared(center, 100).length;
      for (RobotInfo info : rc.senseNearbyRobots(2, creationStats.myTeam)) {
        if (info.type == RobotType.ARCHON) {
          parentArchonLoc = info.location;
          oppositeLoc = new MapLocation(rc.getMapWidth()-1-parentArchonLoc.x, rc.getMapHeight()-1-parentArchonLoc.y);
          break;
        }
      }
    }

    // Try to attack someone
    if (rc.isActionReady()) {
      for (RobotInfo enemy : rc.senseNearbyRobots(creationStats.actionRad, creationStats.opponent)) {
        MapLocation toAttack = enemy.location;
        if (rc.canAttack(toAttack)) {
          rc.attack(toAttack);
          if (raidTarget != null && enemy.health < creationStats.type.getDamage(0)) { // we killed it
            if (enemy.type == RobotType.ARCHON && enemy.location.distanceSquaredTo(raidTarget) <= creationStats.actionRad) {
              broadcastEndRaid();
            }
          }
        }
      }
    }

    if (raidTarget != null) {
      if (moveForRaid()) { // reached target
//        raidTarget = null;
      }
    } else {
      RobotInfo[] nearby = rc.senseNearbyRobots(creationStats.type.visionRadiusSquared, creationStats.myTeam);
      if (nearby.length > visionSize / 3) { // if many bois nearby (1/3 of vision)
        rc.setIndicatorString("Ready to raid!");
//      rc.setIndicatorLine(rc.getLocation(), oppositeLoc, 0,0,255);
        callForRaid(oppositeLoc);
        raidTarget = oppositeLoc;
      } else {
        moveInDirLoose(rc.getLocation().directionTo(center));
      }
    }
  }

  @Override
  protected void ackMessage(Message message) throws GameActionException {
    if (message instanceof StartRaidMessage) {
      ackStartRaidMessage((StartRaidMessage) message);
    } else if (message instanceof EndRaidMessage) {
      ackEndRaidMessage((EndRaidMessage) message);
    }
  }

  /**
   * receive a message to start a raid
   * @param message the raid message
   * @throws GameActionException if some part of ack fails
   */
  private void ackStartRaidMessage(StartRaidMessage message) throws GameActionException {
    // TODO: if not ready for raid (maybe not in center yet or something), ignore
    raidTarget = message.location;
  }

  private void ackEndRaidMessage(EndRaidMessage message) throws GameActionException {
    // TODO: if not ready for raid (maybe not in center yet or something), ignore
    if (raidTarget == message.location) {
      raidTarget = null;
    }
  }

  public void callForRaid(MapLocation location) {
    StartRaidMessage message = new StartRaidMessage(location, rc.getRoundNum());
    communicator.enqueueMessage(message);
  }

  public void broadcastEndRaid() {
    EndRaidMessage message = new EndRaidMessage(raidTarget, rc.getRoundNum());
    communicator.enqueueMessage(message);
  }

  /**
   * move towards raid target (TODO: add pathing)
   * @return if reached target
   * @throws GameActionException if moving fails
   */
  private boolean moveForRaid() throws GameActionException {
    rc.setIndicatorLine(rc.getLocation(), raidTarget, 0,0,255);
    return moveInDirLoose(rc.getLocation().directionTo(raidTarget))
        && rc.getLocation().distanceSquaredTo(raidTarget) <= creationStats.actionRad;
  }
}
