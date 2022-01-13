package supermining.robots.droids;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import supermining.utils.Cache;
import supermining.utils.Utils;
import supermining.communications.messages.ArchonSavedMessage;
import supermining.communications.messages.EndRaidMessage;
import supermining.communications.messages.Message;
import supermining.communications.messages.SaveMeMessage;
import supermining.communications.messages.StartRaidMessage;

public class Soldier extends Droid {
  /* fraction of distance to the target where bots should meet up */
  public static final double MEETUP_FACTOR = 0.25;

  MapLocation myPotentialTarget;
  final MapLocation meetupPoint;

  int visionSize;

  boolean canStartRaid;
  MapLocation raidTarget;
  boolean raidValidated;


  MapLocation archonToSave;

  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
    int mapW = rc.getMapWidth();
    int mapH = rc.getMapHeight();
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
    meetupPoint = Utils.lerpLocations(rc.getLocation(), myPotentialTarget, MEETUP_FACTOR);
    visionSize = rc.getAllLocationsWithinRadiusSquared(meetupPoint, 100).length;
    canStartRaid = true;
  }

  @Override
  protected void runTurn() throws GameActionException {

    // Try to attack someone
    if (raidTarget != null) attackTarget(raidTarget);

    checkForAndCallRaid();

    if (archonToSave != null) {
      if (raidTarget != null) {
        broadcastEndRaid();
        raidTarget = null;
      }
      if (moveTowardsAvoidRubble(archonToSave) && checkDoneSaving()) {
        finishSaving();
      }
      attackTarget(archonToSave);
    } else if (raidTarget != null) {
      if (moveForRaid()) { // reached target
//        raidTarget = null;
        if (!raidValidated) {
          for (RobotInfo enemy : rc.senseNearbyRobots(Cache.Permanent.VISION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
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
      moveTowardsAvoidRubble(meetupPoint);
    }

    attackNearby();
  }

  @Override
  protected void ackMessage(Message message) throws GameActionException {
    if (message instanceof StartRaidMessage) {
      ackStartRaidMessage((StartRaidMessage) message);
    } else if (message instanceof EndRaidMessage) {
      ackEndRaidMessage((EndRaidMessage) message);
    } else if (message instanceof SaveMeMessage) {
      ackSaveMeMessage((SaveMeMessage) message);
    } else if (message instanceof ArchonSavedMessage) {
      ackArchonSavedMessage((ArchonSavedMessage) message);
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
   * acknowledge an archon that needs saving
   * @param message the request for saving from an archon
   */
  private void ackSaveMeMessage(SaveMeMessage message) {
    if (archonToSave == null || Utils.rng.nextInt(5) < 2) { // not already saving or 2/5 chance to switch
      archonToSave = message.location;
    }
  }

  /**
   * acknowledge an archon is done being saved
   * @param message the message to stop saving an archon
   */
  private void ackArchonSavedMessage(ArchonSavedMessage message) {
    if (archonToSave != null && archonToSave.equals(message.location)) { // not already saving or 2/5 chance to switch
      archonToSave = null;
    }
  }

  /**
   * checks if the archon has been saved
   * @return true if saving is done
   * @throws GameActionException if checking fails
   */
  private boolean checkDoneSaving() throws GameActionException {
    if (!archonToSave.isWithinDistanceSquared(rc.getLocation(), Cache.Permanent.ACTION_RADIUS_SQUARED)) return false;
    RobotInfo archon = rc.senseRobotAtLocation(archonToSave);
    if (archon == null) return true;
    return !offensiveEnemiesNearby();
  }

  /**
   * stop saving the archon
   * update internals and broadcast message
   * @throws GameActionException if updating or messaging fails
   */
  private void finishSaving() throws GameActionException {
    if (archonToSave == null) return;
    communicator.enqueueMessage(new ArchonSavedMessage(archonToSave, rc.getRoundNum()));
    archonToSave = null;
  }

  /**
   * check if this soldier is allowed to trigger a raid based on its internals
   * @return true if allowed to trigger raid
   */
  private boolean canCallRaid() {
    if (archonToSave != null || raidTarget != null || !canStartRaid) return false;
    RobotInfo[] nearby = rc.senseNearbyRobots(Cache.Permanent.VISION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM);
    int nearbySoldiers = 0;
    for (RobotInfo friend : nearby) {
      if (friend.type == RobotType.SOLDIER) nearbySoldiers++;
    }
    // if many bois nearby (1/4 of vision)
    return nearbySoldiers > visionSize / 4;
  }

  /**
   * send a message to start a group raid at the given location
   * @param location where to raid
   */
  private void callForRaid(MapLocation location) {
    StartRaidMessage message = new StartRaidMessage(location, rc.getRoundNum());
    communicator.enqueueMessage(message);
  }

  /**
   * check if the soldier can trigger a raid, then do so
   * @return true if a raid was called
   */
  private boolean checkForAndCallRaid() {
    if (!canCallRaid()) return false;
    rc.setIndicatorString("Ready to raid!");
//      //rc.setIndicatorLine(rc.getLocation(), oppositeLoc, 0,0,255);
    callForRaid(myPotentialTarget);
    raidTarget = myPotentialTarget;
    return true;
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
        && rc.getLocation().distanceSquaredTo(raidTarget) <= Cache.Permanent.VISION_RADIUS_SQUARED;
  }

  /**
   * attack the specified target
   * @param target the location to try attacking
   * @return true if attacked the given target
   */
  private boolean attackTarget(MapLocation target) throws GameActionException {
    if (target == null || !rc.canAttack(target)) return false;
    rc.attack(target);
    return true;
  }

  /**
   * attack a nearby enemy (kinda random based on order from sense function)
   * @return true if attacked
   * @throws GameActionException if attacking fails
   */
  private boolean attackNearby() throws GameActionException {
    if (!rc.isActionReady()) return false;

    for (RobotInfo enemy : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
      MapLocation toAttack = enemy.location;
      if (rc.canAttack(toAttack)) {
        rc.attack(toAttack);
        if (raidTarget != null && enemy.health < Cache.Permanent.ROBOT_TYPE.damage) { // we killed it
          if (enemy.type == RobotType.ARCHON && enemy.location.distanceSquaredTo(raidTarget) <= Cache.Permanent.ACTION_RADIUS_SQUARED) {
            broadcastEndRaid();
          }
        }
        return true;
      }
    }
    return false;
  }
}
