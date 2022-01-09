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

import java.util.HashSet;
import java.util.Set;

public class Soldier extends Droid {

  MapLocation parentArchonLoc;
  MapLocation oppositeLoc;
  final MapLocation center;

  int visionSize;

  MapLocation raidTarget;
  boolean raidValidated;

  boolean canStartRaid;

  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
    center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
    visionSize = rc.getAllLocationsWithinRadiusSquared(center, 100).length;
    for (RobotInfo info : rc.senseNearbyRobots(2, creationStats.myTeam)) {
      if (info.type == RobotType.ARCHON) {
        parentArchonLoc = info.location;
        oppositeLoc = new MapLocation(rc.getMapWidth()-1-parentArchonLoc.x, rc.getMapHeight()-1-parentArchonLoc.y);
        break;
      }
    }
    canStartRaid = true;
  }

  @Override
  protected void runTurn() throws GameActionException {

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

    if (raidTarget == null && canStartRaid) {
      RobotInfo[] nearby = rc.senseNearbyRobots(creationStats.type.visionRadiusSquared, creationStats.myTeam);
      if (nearby.length > visionSize / 4) { // if many bois nearby (1/4 of vision)
        rc.setIndicatorString("Ready to raid!");
//      rc.setIndicatorLine(rc.getLocation(), oppositeLoc, 0,0,255);
        callForRaid(oppositeLoc);
        raidTarget = oppositeLoc;
      }
    }

    if (raidTarget != null) {
      if (moveForRaid()) { // reached target
//        raidTarget = null;
        if (!raidValidated) {
          for (RobotInfo enemy : rc.senseNearbyRobots(creationStats.visionRad, creationStats.opponent)) {
            if (enemy.type == RobotType.ARCHON) {
              raidValidated = true;
              break;
            }
          }
          if (!raidValidated) {
            broadcastEndRaid();
          }
        }
      }
    } else {
      {
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
//    System.out.println("Got start raid" + message.location);
    raidTarget = message.location;
    if (raidTarget.equals(oppositeLoc)) {
      canStartRaid = false;
    }
  }

  private void ackEndRaidMessage(EndRaidMessage message) throws GameActionException {
    // TODO: if not ready for raid (maybe not in center yet or something), ignore
    if (raidTarget != null && raidTarget.equals(message.location)) {
      raidTarget = null;
      raidValidated = false;
//      System.out.println("Got end raid on " + message.location + " - from rnd: " + message.header.cyclicRoundNum + "/" + Message.Header.toCyclicRound(rc.getRoundNum()));
    }
    if (message.location.equals(oppositeLoc)) {
      canStartRaid = false;
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
        && rc.getLocation().distanceSquaredTo(raidTarget) <= creationStats.visionRad;
  }
}
