package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import firstbot.Utils;
import firstbot.communications.messages.LeadFoundMessage;
import firstbot.communications.messages.Message;
import firstbot.communications.messages.SingleIntMessage;

public class Miner extends Droid {

  int turnsWandering;
  private static final int WANDERING_TURNS_TO_BROADCAST_LEAD = 10; // if the miner wanders for >= 10 turns, it will broadcast when lead is found
  MapLocation target;
  private static final int WANDERING_TURNS_TO_FOLLOW_LEAD = 3;
  private static final int MAX_SQDIST_FOR_TARGET = 100;

  public Miner(RobotController rc) {
    super(rc);
    target = null;
  }

  @Override
  protected void runTurn() throws GameActionException {
    while (pendingMessages > 0) {
      Message message = communicator.getNthLastReceivedMessage(pendingMessages);
      if (message instanceof LeadFoundMessage) { // if lead was found somewhere far
        acknowledgeLeadFoundMessage((LeadFoundMessage) message);
      }
      pendingMessages--;
    }

    mineSurroundingGold();
    mineSurroundingLead();

    if (rc.isMovementReady()) {
      boolean foundLead = followLead();
      if (foundLead) target = null; // nullify target if we found lead now
    }
    if (rc.isMovementReady()) {
      if (target != null) {
        goToTarget();
      }
    }
    if (rc.isMovementReady()) {
      moveRandomly();
      turnsWandering++;
    }
  }

  /**
   * if lead is found somewhere, potentially start targetting it!
   * @param message the received broadcast about lead
   */
  private void acknowledgeLeadFoundMessage(LeadFoundMessage message) {
    if (turnsWandering > WANDERING_TURNS_TO_FOLLOW_LEAD) {
      int distToNewTarget = rc.getLocation().distanceSquaredTo(message.location);
      if (distToNewTarget < MAX_SQDIST_FOR_TARGET) {
        if (target == null || distToNewTarget < rc.getLocation().distanceSquaredTo(target)) {
          target = message.location;
          rc.setIndicatorString("Follow lead broadcast!: " + target);
        }
      }
    }
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

  /**
   * assuming there is a target for the miner, approach it
   *    currently very naive -- should use path finding!
   * @throws GameActionException if movement or line indication fails
   */
  private void goToTarget() throws GameActionException {
    Direction goal = rc.getLocation().directionTo(target);
    rc.setIndicatorString("Approaching target" + target + " -- " + goal);
    if (rc.canMove(goal)) {
      rc.move(goal);
    } else { // if goal is occupied, try instead to move in a semi-close direction
      Direction try1 = goal.rotateLeft();
      Direction try2 = goal.rotateRight();
      if (Utils.rng.nextBoolean()) {
        goal = try2;
      } else {
        goal = try1;
        try1 = try2;
      }
      if (rc.canMove(goal)) rc.move(goal);
      else if (rc.canMove(try1)) rc.move(try1);
    }
    rc.setIndicatorLine(rc.getLocation(), target, 255,10,10);
    if (rc.getLocation().distanceSquaredTo(target) < creationStats.type.visionRadiusSquared / 2) { // set target to null if found!
      target = null;
    }
  }

  private boolean followLead() throws GameActionException {
    boolean followedLead = moveToHighLeadProbabilistic(false);
    if (followedLead) {
      if (turnsWandering > WANDERING_TURNS_TO_BROADCAST_LEAD) {
        broadcastLead(rc.getLocation());
      }
      turnsWandering = 0;
    }
    return followedLead;
  }

  /**
   * send a LeadFoundMessage with the specified location
   * @param location the location where to find lead!
   */
  private void broadcastLead(MapLocation location) {
    communicator.enqueueMessage(new LeadFoundMessage(location, rc.getRoundNum()));
    rc.setIndicatorDot(location, 0, 255, 0);
    rc.setIndicatorString("Broadcast lead! " + location);
//    System.out.println("Broadcast lead! " + location);
  }
}
