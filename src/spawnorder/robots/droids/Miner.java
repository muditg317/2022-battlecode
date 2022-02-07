package spawnorder.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import spawnorder.utils.Cache;
import spawnorder.utils.Utils;
import spawnorder.communications.messages.LeadFoundMessage;
import spawnorder.communications.messages.LeadRequestMessage;
import spawnorder.communications.messages.Message;

public class Miner extends Droid {

  int turnsWandering;
  private static final int WANDERING_TURNS_TO_BROADCAST_LEAD = 10; // if the miner wanders for >= 10 turns, it will broadcast when lead is found
  private static final int MAX_WANDERING_REQUEST_LEAD = 8; // if wandering for 5+ turns, request lead broadcast
  MapLocation target;
  boolean reachedTarget;
  int bestSquaredDistanceToTarget;
  private static final int WANDERING_TURNS_TO_FOLLOW_LEAD = 3;
  private static final int MAX_SQDIST_FOR_TARGET = 200;

  MapLocation runAwayTarget;

  private LeadRequestMessage leadRequest; // TODO: make a message that the other boi will overwrite (will rely on getting ack within 1 turn or else sad)

  public Miner(RobotController rc) throws GameActionException {
    super(rc);
    target = Utils.randomMapLocation();
    bestSquaredDistanceToTarget = rc.getLocation().distanceSquaredTo(target);
    leadRequest = null;
  }


  /*

  1) call executeMining() => mine gold and then lead if possible
  2) if there is an enemy attacking unit, move away (and set new random location?)
  3) if there is >1 lead in sense, move to mine location
  4) if I have exhausted the mine (executeMining() returns false), continue moving to random location
  5) call executeMining() again
  6) if at location, new location

   */
  @Override
  protected void runTurn() throws GameActionException {
    executeMining(); // performs action of mining gold and then lead until cooldown is reached
    // executeLeadTarget(); // if miner doesnt already have a target and has a pending request that it sent last turn
    executeRunFromEnemy(); // set runAwayTarget if enemy attacking unit is within robot range (possibly bugged rn)
    boolean resourcesLeft = checkIfResourcesLeft(); // check if any gold or lead (>1) is within robot range, return true if so

    // lets remove target if we havent gotten closer to it in 5 moves?
//    if (rc.getID() == 10001) {
//      //System.out.println("target: " + target + " reached: " + reachedTarget + " resourcesLeft: " + resourcesLeft);
//    }
    if (runAwayTarget != null) { // if enemy attacking unit is within range
      target = Utils.randomMapLocation(); // new random target
      if (runAway()) runAwayTarget = null; // runAway() is true iff we move away
    } else if (resourcesLeft && followLead()) {
      // performs action of moving to lead
    } else {
      reachedTarget = goToTarget(); // performs action of moving to target location
    }

    executeMining();

    if (reachedTarget) {
      target = Utils.randomMapLocation(); // new random target
    }

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
    if (message.from.distanceSquaredTo(responseLocation) > MAX_SQDIST_FOR_TARGET) return; // don't answer if too far

    rc.setIndicatorString("Answer lead request: " + responseLocation);

    message.respond(responseLocation);
    rc.setIndicatorString("Respond to lead request! " + responseLocation);
    //rc.setIndicatorDot(responseLocation, 0,255,0);
    //rc.setIndicatorLine(rc.getLocation(), responseLocation, 0,255,0);
  }


  private void executeMining() throws GameActionException {
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

  private void executeLeadTarget() {
    if (target == null && leadRequest != null) {
      rc.setIndicatorString("Checking request response!");
      if (leadRequest.readSharedResponse()) {
        //System.out.println("Got request response!!" + leadRequest.location);
        registerTarget(leadRequest.location);
      }
      leadRequest = null;
    }
  }

  private void executeRunFromEnemy() {
    MapLocation enemies = findEnemies();
    if (enemies != null) {
      MapLocation myLoc = rc.getLocation();
      runAwayTarget = new MapLocation((myLoc.x << 1) - enemies.x, (myLoc.y << 1) - enemies.y);
      Direction backToSelf = runAwayTarget.directionTo(myLoc);
      while (!rc.canSenseLocation(runAwayTarget)) runAwayTarget = runAwayTarget.add(backToSelf);
      //rc.setIndicatorDot(myLoc, 255,255,0);
      //rc.setIndicatorLine(enemies, runAwayTarget, 255, 255, 0);
    }
  }


  /**
   * check if there are any enemy (soldiers) to run away from
   * @return the map location where there are offensive enemies (null if none)
   */
  private MapLocation findEnemies() {
    RobotInfo[] enemies = rc.senseNearbyRobots(Cache.Permanent.VISION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM);
    if (enemies.length == 0) return null;
    int avgX = 0;
    int avgY = 0;
    int count = 0;
    for (RobotInfo enemy : enemies) {
      if (enemy.type.damage > 0) { // enemy can hurt me
        avgX += enemy.location.x * enemy.type.damage;
        avgY += enemy.location.y * enemy.type.damage;
        count += enemy.type.damage;
      }
    }
    if (count == 0) return null;
    return new MapLocation(avgX / count, avgY / count);
  }

  /**
   * run away from enemies based on runaway target
   * @return true if reached target
   */
  private boolean runAway() throws GameActionException {
    if (moveTowardsAvoidRubble(runAwayTarget)) {
      rc.setIndicatorString("run away! " + runAwayTarget);
      return rc.getLocation().isWithinDistanceSquared(runAwayTarget, Cache.Permanent.ACTION_RADIUS_SQUARED);
    }
    return false;
  }

   /*
   * @return true iff there are resources that can be mined (gold > 0 || lead > 1)
   * @throws GameActionException
   */
  private boolean checkIfResourcesLeft() throws GameActionException {
    MapLocation me = rc.getLocation();
    int goldLocationsLength = rc.senseNearbyLocationsWithGold(Cache.Permanent.VISION_RADIUS_SQUARED).length;
    if (goldLocationsLength > 0) return true;
    int leadLocationsLength = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, 2).length;
    if (leadLocationsLength > 0) return true;
    return false;
  }

  /**
   * move towards lead with probability
   * @return if moved towards lead
   * @throws GameActionException if movement failed
   */
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
   *
   * assuming there is a target for the miner, approach it
   *    currently very naive -- should use path finding!
   * @return if the miner is within the action radius of the target
   * @throws GameActionException if movement or line indication fails
   */
  private boolean goToTarget() throws GameActionException {
    turnsWandering = 0;
//    Direction goal = rc.getLocation().directionTo(target);
    if (moveTowardsAvoidRubble(target)) {
      rc.setIndicatorString("Approaching target" + target);
//    moveInDirLoose(goal);
      //rc.setIndicatorLine(rc.getLocation(), target, 255, 10, 10);
      //rc.setIndicatorDot(target, 0, 255, 0);
    }
    return rc.getLocation().isWithinDistanceSquared(target, Cache.Permanent.ACTION_RADIUS_SQUARED); // set target to null if found!
  }

  /**
   * send a LeadFoundMessage with the specified location
   * @param location the location where to find lead!
   */
  private void broadcastLead(MapLocation location) {
//    communicator.enqueueMessage(new LeadFoundMessage(location, rc.getRoundNum()));
//    //rc.setIndicatorDot(location, 0, 255, 0);
//    rc.setIndicatorString("Broadcast lead! " + location);
//    //System.out.println("Broadcast lead! " + location);
  }

  /**
   * returns true if the miner is ready to request lead
   *    currently: been wandering + no lead pilesnearby (including 1pb tiles)
   * @return needs to request
   * @throws GameActionException if sensing lead fails
   */
  private boolean needToRequestLead() throws GameActionException {
    return turnsWandering > MAX_WANDERING_REQUEST_LEAD
        && rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED).length == 0;
  }

  /**
   * send a RequestLeadMessage
   */
  private void requestLead() {
    leadRequest = new LeadRequestMessage(rc.getLocation(), rc.getRoundNum());
    communicator.enqueueMessage(leadRequest);
    //rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
    rc.setIndicatorString("Requesting lead!");
    //System.out.println("Requesting lead!");
  }
}
