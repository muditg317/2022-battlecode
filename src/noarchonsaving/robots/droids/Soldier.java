package noarchonsaving.robots.droids;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import noarchonsaving.Utils;
import noarchonsaving.communications.messages.EndRaidMessage;
import noarchonsaving.communications.messages.Message;
import noarchonsaving.communications.messages.StartRaidMessage;

public class Soldier extends Droid {

  MapLocation myPotentialTarget;
  final MapLocation center;

  int visionSize;

  MapLocation raidTarget;
  boolean raidValidated;

  boolean canStartRaid;

  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
    int mapW = rc.getMapWidth();
    int mapH = rc.getMapHeight();
    center = new MapLocation(mapW/2, mapH/2);
    visionSize = rc.getAllLocationsWithinRadiusSquared(center, 100).length;
    if (Math.abs(mapW - mapH) > 3) { // not a square default to flip sym for targetting
      switch (Utils.rng.nextInt(2)) {
        case 0:
          myPotentialTarget = new MapLocation(mapW-1-parentArchonLoc.x, parentArchonLoc.y);
          break;
        default:
          myPotentialTarget = new MapLocation(parentArchonLoc.x, mapH - 1 - parentArchonLoc.y);
      }
    } else {
      myPotentialTarget = new MapLocation(mapW-1-parentArchonLoc.x, mapH-1-parentArchonLoc.y);
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
      int nearbySoldiers = 0;
      for (RobotInfo friend : nearby) {
        if (friend.type == RobotType.SOLDIER) nearbySoldiers++;
      }
      if (nearbySoldiers > visionSize / 4) { // if many bois nearby (1/4 of vision)
        rc.setIndicatorString("Ready to raid!");
//      //rc.setIndicatorLine(rc.getLocation(), oppositeLoc, 0,0,255);
        callForRaid(myPotentialTarget);
        raidTarget = myPotentialTarget;
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
      //rc.setIndicatorLine(rc.getLocation(), raidTarget, 0,0,255);
    } else {
//      moveInDirLoose(rc.getLocation().directionTo(center));
      moveTowardsAvoidRubble(center);
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
//    //System.out.println("Got start raid" + message.location);
    raidTarget = message.location;
    if (raidTarget.equals(myPotentialTarget)) {
      canStartRaid = false;
    }
  }

  /**
   * receive a message that a raid is over
   * @param message raid ending message with a specific raid target
   * @throws GameActionException if ack fails
   */
  private void ackEndRaidMessage(EndRaidMessage message) throws GameActionException {
    // TODO: if not ready for raid (maybe not in center yet or something), ignore
    if (raidTarget != null && raidTarget.equals(message.location)) {
      raidTarget = null;
      raidValidated = false;
//      //System.out.println("Got end raid on " + message.location + " - from rnd: " + message.header.cyclicRoundNum + "/" + Message.Header.toCyclicRound(rc.getRoundNum()));
    }
    if (message.location.equals(myPotentialTarget)) {
      canStartRaid = false;
    }
  }

  /**
   * send a message to start a group raid at the given location
   * @param location where to raid
   */
  public void callForRaid(MapLocation location) {
    StartRaidMessage message = new StartRaidMessage(location, rc.getRoundNum());
    communicator.enqueueMessage(message);
  }

  /**
   * send a message to the team that the current raid is either over or invalid
   */
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
    //rc.setIndicatorLine(rc.getLocation(), raidTarget, 0,0,255);
//    return moveInDirLoose(rc.getLocation().directionTo(raidTarget))
    return moveTowardsAvoidRubble(raidTarget)
        && rc.getLocation().distanceSquaredTo(raidTarget) <= creationStats.visionRad;
  }
}
