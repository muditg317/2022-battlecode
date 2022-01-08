package firstbot.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import firstbot.communications.messages.LeadFoundMessage;
import firstbot.communications.messages.LeadRequestMessage;
import firstbot.communications.messages.Message;

public class Miner extends Droid {

  int turnsWandering;
  private static final int WANDERING_TURNS_TO_BROADCAST_LEAD = 10; // if the miner wanders for >= 10 turns, it will broadcast when lead is found
  private static final int MAX_WANDERING_REQUEST_LEAD = 5; // if wandering for 5+ turns, request lead broadcast
  MapLocation target;
  private static final int WANDERING_TURNS_TO_FOLLOW_LEAD = 3;
  private static final int MAX_SQDIST_FOR_TARGET = 200;

  private LeadRequestMessage leadRequest; // TODO: make a message that the other boi will overwrite (will rely on getting ack within 1 turn or else sad)

  public Miner(RobotController rc) {
    super(rc);
    target = null;
    leadRequest = null;
  }

  @Override
  protected void runTurn() throws GameActionException {
    doMining();

    if (target == null && leadRequest != null) {
      rc.setIndicatorString("Checking request response!");
      if (leadRequest.readSharedResponse(communicator)) {
        registerTarget(leadRequest.location);
      }
      leadRequest = null;
    }


    if (followLead()) target = null;
    else if (target != null) goToTarget();
    else if (moveRandomly()) {
      turnsWandering++;
      if (needToRequestLead()) requestLead();
    }
//    if (rc.isMovementReady()) {
//      boolean foundLead = followLead();
//      if (foundLead) target = null; // nullify target if we found lead now
//    }
//    if (rc.isMovementReady()) {
//      if (target != null) {
//        goToTarget();
//      }
//    }
//    if (rc.isMovementReady()) {
//      if (moveRandomly()) turnsWandering++;
//      if (needToRequestLead()) {
//        requestLead();
//      }
//    }

    doMining();
  }

  @Override
  protected void ackMessage(Message message) throws GameActionException {
    if (message instanceof LeadFoundMessage) { // if lead was found somewhere far
      acknowledgeLeadFoundMessage((LeadFoundMessage) message);
    } else if (message instanceof LeadRequestMessage) {
      acknowledgeLeadRequestMessage((LeadRequestMessage) message);
    }
  }

  /**
   * if lead is found somewhere, potentially start targetting it!
   * @param message the received broadcast about lead
   */
  private void acknowledgeLeadFoundMessage(LeadFoundMessage message) {
    if (turnsWandering <= WANDERING_TURNS_TO_FOLLOW_LEAD) { // we haven't wandered enough to care
      return;
    }
    registerTarget(message.location);
  }

  /**
   * if some miner is looking for lead, tell him where to go!
   * @param message the received request for lead
   */
  private void acknowledgeLeadRequestMessage(LeadRequestMessage message) throws GameActionException {
    rc.setIndicatorString("Got lead request: " + message.answered + "|" + message.location + "|" + turnsWandering);
    if (turnsWandering > 0) { // can't suggest lead if we wandering too
      if (message.answered) registerTarget(message.location); // if we wandering, just take someone elses answer lol
      return;
    }
    if (message.answered) return; // some other miner already satisfied this request

    // we have a target, forward it to the requester
    MapLocation responseLocation = target != null ? target : rc.getLocation();
    rc.setIndicatorString("Answer lead request: " + responseLocation);

    message.respond(communicator, responseLocation);
    rc.setIndicatorString("Respond to lead request! " + responseLocation);
    rc.setIndicatorDot(responseLocation, 0,255,0);
    rc.setIndicatorLine(rc.getLocation(), responseLocation, 0,255,0);
  }

  private void doMining() throws GameActionException {
    if (rc.isActionReady()) {
      mineSurroundingGold();
    }
    if (rc.isActionReady()) {
      mineSurroundingLead();
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
   * register the location with the miner with some regulations
   * @param newTarget the target to set
   * @return if the target was set
   */
  private boolean registerTarget(MapLocation newTarget) {
    int distToNewTarget = rc.getLocation().distanceSquaredTo(newTarget);
    if (distToNewTarget > MAX_SQDIST_FOR_TARGET) { // target too far to follow
      return false;
    }
    // if we already have a target that's closer
    if (target != null && distToNewTarget >= rc.getLocation().distanceSquaredTo(target)) {
      return false;
    }
    target = newTarget;
    turnsWandering = 0;
    leadRequest = null;
    rc.setIndicatorString("Got new target! " + target);
    return true;
  }

  /**
   * assuming there is a target for the miner, approach it
   *    currently very naive -- should use path finding!
   * @throws GameActionException if movement or line indication fails
   */
  private void goToTarget() throws GameActionException {
    turnsWandering = 0;
    Direction goal = rc.getLocation().directionTo(target);
    rc.setIndicatorString("Approaching target" + target + " -- " + goal);
    if (!move(goal)) { // if goal is occupied, try instead to move in a semi-close direction
      Direction try1 = goal.rotateLeft();
      Direction try2 = goal.rotateRight();
//      if (Utils.rng.nextBoolean()) {
//        goal = try2;
//      } else {
//        goal = try1;
//        try1 = try2;
//      }
      boolean ignored = move(try1) || move(try2);
    }
    rc.setIndicatorLine(rc.getLocation(), target, 255,10,10);
    rc.setIndicatorDot(target, 0, 255, 0);
    if (rc.getLocation().distanceSquaredTo(target) < creationStats.type.visionRadiusSquared / 2) { // set target to null if found!
      target = null;
    }
  }

  private boolean followLead() throws GameActionException {
    boolean followedLead = moveToHighLeadProbabilistic();
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
//    communicator.enqueueMessage(new LeadFoundMessage(location, rc.getRoundNum()));
//    rc.setIndicatorDot(location, 0, 255, 0);
//    rc.setIndicatorString("Broadcast lead! " + location);
//    System.out.println("Broadcast lead! " + location);
  }

  /**
   * returns true if the miner is ready to request lead
   *    currently: been wandering + no lead pilesnearby (including 1pb tiles)
   * @return needs to request
   * @throws GameActionException if sensing lead fails
   */
  private boolean needToRequestLead() throws GameActionException {
    return turnsWandering > MAX_WANDERING_REQUEST_LEAD
        && rc.senseNearbyLocationsWithLead(creationStats.type.visionRadiusSquared).length == 0;
  }

  /**
   * send a RequestLeadMessage
   */
  private void requestLead() {
    leadRequest = new LeadRequestMessage(rc.getRoundNum());
    communicator.enqueueMessage(leadRequest);
    rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
    rc.setIndicatorString("Requesting lead!");
    System.out.println("Requesting lead!");
  }
}
