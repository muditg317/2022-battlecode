package firstbot.robots.droids;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import firstbot.communications.messages.LeadFoundMessage;
import firstbot.communications.messages.LeadRequestMessage;
import firstbot.communications.messages.Message;
import firstbot.utils.Cache;
import firstbot.utils.Utils;

import java.util.Arrays;

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

//  // did we just move?
//  int currentIndex;
//  long visited0;
//  long visited1;
//  MapLocation bestMapLocation = null;
//  int bestRubble = 101;
//  int bestDistance = 9999;


  private LeadRequestMessage leadRequest; // TODO: make a message that the other boi will overwrite (will rely on getting ack within 1 turn or else sad)

  public Miner(RobotController rc) throws GameActionException {
    super(rc);
    target = Utils.randomMapLocation();
    bestSquaredDistanceToTarget = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(target);
    leadRequest = null;
    //13x13 because the vision circle is circumscribed by a 9x9 square
    // and then 2 more on each side because we do not want a miner within 2x2 square (this is just a buffer and will always be 0)
//    System.out.println("Miner init cost: " + Clock.getBytecodeNum());
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
//    System.out.println("Miner run(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    mineSurroundingResourcesIfPossible(); // performs action of mining gold and then lead until cooldown is reached
//    System.out.println("Miner execMining(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

//    checkLeadRequestResponseIfPending();

    checkNeedToRunAway();
    //    System.out.println("Miner runAway(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    boolean resourcesLeft = checkIfResourcesLeft(); // check if any gold or lead (>1) is within robot range, return true if so
//    System.out.println("Miner checkRss(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    // lets remove target if we havent gotten closer to it in 5 moves?
    if (runAwayTarget != null) { // if enemy attacking unit is within range
      target = Utils.randomMapLocation(); // new random target
      if (runAway()) runAwayTarget = null; // runAway() is true iff we move away
    } else if (Cache.PerTurn.ROUND_NUM >= 15 && resourcesLeft && followLead()) {

      // performs action of moving to lead
    } else {
      reachedTarget = goToTarget(); // performs action of moving to target location
    }
//    System.out.println("Miner movement done(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    mineSurroundingResourcesIfPossible();
//    System.out.println("Miner execMining(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    if (reachedTarget) {
      target = Utils.randomMapLocation(); // new random target
      // must be 50 away from me and
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
    MapLocation responseLocation = target != null ? target : Cache.PerTurn.CURRENT_LOCATION;
    if (message.from.distanceSquaredTo(responseLocation) > MAX_SQDIST_FOR_TARGET) return; // don't answer if too far

    rc.setIndicatorString("Answer lead request: " + responseLocation);

    message.respond(responseLocation);
    rc.setIndicatorString("Respond to lead request! " + responseLocation);
    rc.setIndicatorDot(responseLocation, 0,255,0);
    rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, responseLocation, 0,255,0);
  }


  /**
   * mine all the adjacent resources while action is ready
   *    gold first
   * @throws GameActionException if mining fails
   */
  private void mineSurroundingResourcesIfPossible() throws GameActionException {
    if (rc.isActionReady()) {
      mineSurroundingGold();
    }
    if (rc.isActionReady()) {
      mineSurroundingLead();
    }
  }

  /**
   * subroutine to mine gold from all adjacent tiles
   * @throws GameActionException if mining fails
   */
  private void mineSurroundingGold() throws GameActionException {
    if (!rc.isActionReady()) return;

    // Try to mine on squares around us.
    for (MapLocation toMine : rc.senseNearbyLocationsWithGold(Cache.Permanent.ACTION_RADIUS_SQUARED, 2)) {
      int goldThere = rc.senseGold(toMine);
      while (rc.isActionReady() && goldThere-- > 0) rc.mineGold(toMine);
      if (!rc.isActionReady()) return;
    }
  }

  /**
   * subroutine to mine lead from all adjacent tiles
   *    leaves 1pb in every tile
   *    attemps to mine all tiles evenly
   * @throws GameActionException if mining fails
   */
  private void mineSurroundingLead() throws GameActionException {
    if (!rc.isActionReady()) return;
    // Try to mine on squares around us.
    MapLocation[] locs = rc.senseNearbyLocationsWithLead(Cache.Permanent.ACTION_RADIUS_SQUARED, 2);
    if (locs.length == 0) return;
    boolean mined = true;
    while (rc.isActionReady() && mined) {
      mined = false;
      for (MapLocation toMine : locs) {
//      int leadThere = rc.senseLead(toMine);
        if (Clock.getBytecodesLeft() > 15 && rc.isActionReady() && rc.senseLead(toMine) > 1) {
          rc.mineLead(toMine);
          mined = true;
        }
        if (!rc.isActionReady()) return;
      }
    }
  }

  /**
   * if the miner has no target but has a pending request, check the response
   *    if response received, register new target
   */
  private void checkLeadRequestResponseIfPending() {
    if (target == null && leadRequest != null) {
      rc.setIndicatorString("Checking request response!");
      if (leadRequest.readSharedResponse()) {
        //System.out.println("Got request response!!" + leadRequest.location);
        registerTarget(leadRequest.location);
      }
      leadRequest = null;
    }
  }

  /**
   * look for enemies nearby and determine if they need to be ran from
   *    sets runAwayTarget
   */
  private void checkNeedToRunAway() {
    MapLocation enemies = offensiveEnemyCentroid();
    if (enemies != null) {
      MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
      runAwayTarget = new MapLocation((myLoc.x << 1) - enemies.x, (myLoc.y << 1) - enemies.y);
      Direction backToSelf = runAwayTarget.directionTo(myLoc);
      while (!rc.canSenseLocation(runAwayTarget)) runAwayTarget = runAwayTarget.add(backToSelf);
      rc.setIndicatorDot(enemies, 255,255,0);
      rc.setIndicatorLine(enemies, runAwayTarget, 255, 255, 0);
      rc.setIndicatorString("Enemies at " + enemies);
    }
  }

  /**
   * run away from enemies based on runaway target
   * @return true if reached target
   */
  private boolean runAway() throws GameActionException {
    if (moveOptimalTowards(runAwayTarget)) {
      rc.setIndicatorString("run away! " + runAwayTarget);
      rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, runAwayTarget, 0,255,0);
      return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(runAwayTarget, Cache.Permanent.ACTION_RADIUS_SQUARED);
    }
    return false;
  }

  /*
   * @return true iff there are resources that can be mined (gold > 0 || lead > 1)
   * @throws GameActionException
   */
  private boolean checkIfResourcesLeft() throws GameActionException {
    return
        rc.senseNearbyLocationsWithGold(Cache.Permanent.VISION_RADIUS_SQUARED).length > 0
        || rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, 2).length > 0;
//    int goldLocationsLength = rc.senseNearbyLocationsWithGold(Cache.Permanent.VISION_RADIUS_SQUARED).length;
//    if (goldLocationsLength > 0) return true;
//    int leadLocationsLength = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, 2).length;
//    if (leadLocationsLength > 0) return true;
//    return false;
  }

  /**
   * move towards lead with probability
   * @return if moved towards lead
   * @throws GameActionException if movement failed
   */
//  private boolean followLead() throws GameActionException {
//    boolean followedLead = moveToHighLeadProbabilistic();
//    if (followedLead) {
//      if (turnsWandering > WANDERING_TURNS_TO_BROADCAST_LEAD) {
//        broadcastLead(Cache.PerTurn.CURRENT_LOCATION);
//      }
//      turnsWandering = 0;
//    }
//    return followedLead;
//  }

  /**
   * move towards lead
   *  1. find all locations that can mine some resource (>1 lead or gold)
   *  2. for each location, determine the one with least rubble. Break ties by finding ones that are closer to current location
   * @return if moved towards lead
   * @throws GameActionException if movement failed
   */
  private boolean followLead() throws GameActionException {
//    System.out.println("Miner start followLeadPnay(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    boolean followedLead = moveTowardsOptimalLeadMiningPos();
    if (followedLead) {
      if (turnsWandering > WANDERING_TURNS_TO_BROADCAST_LEAD) {
        broadcastLead(rc.getLocation());
      }
      turnsWandering = 0;
    }
    return followedLead;
  }

  /**
   * move to a location that can mine resources the quickest
   * @return if the movement was successfully based on lead presence
   * @throws GameActionException if movement fails
   */
  protected boolean moveTowardsOptimalLeadMiningPos() throws GameActionException {
    MapLocation highLead = getOptimalLeadMiningPosition();
//    System.out.println("Miner finish getBestLead(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    if (Cache.PerTurn.ROUND_NUM <= 50 && highLead != null) {
      int numMovesToLead = Utils.maxSingleAxisDist(Cache.Permanent.START_LOCATION, highLead);
      int numMovesToCurrentLocation = Utils.maxSingleAxisDist(Cache.Permanent.START_LOCATION, Cache.PerTurn.CURRENT_LOCATION);
      if (numMovesToCurrentLocation - numMovesToLead >= 2) { // 8 10, false.. 9 10 true 11 10 true
        return false;
      }
    }

    if (highLead != null && (highLead.equals(Cache.PerTurn.CURRENT_LOCATION) || moveOptimalTowards(highLead))) {
      rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, highLead, 0, 0, 255);
      rc.setIndicatorString("lead: " + highLead);
      return true;
    }
//    System.out.println("high lead not found or blocked ");
    return false;
  }

  int[] leadByLocationMap;
  private static final int MAX_LEAD_LOCS_LEN = 10;
  /**
   * iterates over all visible lead and friendly miners to smartly determine what position would be optimal for lead mining
   * @return the best location to mine lead from
   * @throws GameActionException if any sensing fails during processing
   */
  private MapLocation getOptimalLeadMiningPosition() throws GameActionException {
//    System.out.println("Miner start getBestLeadWithHash(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    int minBound = 2;
    MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(-1, minBound);
    while (leadLocs.length > MAX_LEAD_LOCS_LEN) {
      int minLead = 99999;
      int maxLead = -1;
      for (MapLocation loc : leadLocs) {
        int lead = rc.senseLead(loc);
        if (lead < minLead) minLead = lead;
        if (lead > maxLead) maxLead = lead;
      }
      if (minLead < maxLead) {
        minBound = (maxLead + minLead) / 2;
        leadLocs = rc.senseNearbyLocationsWithLead(-1, minBound);
      } else {
        leadLocs = Arrays.copyOf(leadLocs, MAX_LEAD_LOCS_LEN);
      }
    }
//    System.out.println("Miner start create map(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    MapLocation bestLocation = null;
    int leastRubble = 101;
    int bestDist = 9999;
    int bestLead = 0;
    // clear out array
    leadByLocationMap = new int[121];
//    System.out.println("Miner start populate leadLocs(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    for (MapLocation lead : leadLocs) {
//      if (rejectedLocations.contains(lead)) continue; // ignore lead if we got rejected earlier

//      System.out.println("Miner start loc" + lead + "(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
      int leadThere = rc.senseLead(lead);

      int startX = 4 + lead.x - Cache.PerTurn.CURRENT_LOCATION.x;
      int startY = 4 + lead.y - Cache.PerTurn.CURRENT_LOCATION.y;
      if (lead.x == 0) startX++;
      if (lead.y == 0) startY++;
      int xCount = 3;
      int yCount = 3;
      if (lead.x == 0 || lead.x == Cache.Permanent.MAP_WIDTH) xCount = 2;
      if (lead.y == 0 || lead.y == Cache.Permanent.MAP_HEIGHT) yCount = 2;
      int indIncr = 11 - yCount;
      int start = startX * 11 + startY;
//      System.out.printf("start:[%d,%d] - count:[%d,%d] -- start:%d - incr:%d\n", startX, startY, xCount, yCount, start, indIncr);
      switch (xCount) {
        case 3:
          switch (yCount) {
            case 3:
              leadByLocationMap[start++] += leadThere;
            case 2:
              leadByLocationMap[start++] += leadThere;
            case 1:
              leadByLocationMap[start++] += leadThere;
          }
          start += indIncr;
        case 2:
          switch (yCount) {
            case 3:
              leadByLocationMap[start++] += leadThere;
            case 2:
              leadByLocationMap[start++] += leadThere;
            case 1:
              leadByLocationMap[start++] += leadThere;
          }
          start += indIncr;
        case 1:
          switch (yCount) {
            case 3:
              leadByLocationMap[start++] += leadThere;
            case 2:
              leadByLocationMap[start++] += leadThere;
            case 1:
              leadByLocationMap[start] += leadThere;
          }
      }
    }

//    System.out.println("Miner finish populate leadLocs(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

//    System.out.println("Miner start check friends(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      leadByLocationMap[(5 + friend.location.x - Cache.PerTurn.CURRENT_LOCATION.x) * 11 + (5 + friend.location.y - Cache.PerTurn.CURRENT_LOCATION.y)] = 0;
      if (friend.type != RobotType.MINER) continue;
//      System.out.println("Friendly miner at " + friend.location + " -- " + friend);
      MapLocation[] takenLead = rc.senseNearbyLocationsWithLead(friend.location, Utils.DSQ_2by2, 2);
      if (takenLead.length == 0) continue;
      int leadToTake = 100 / takenLead.length;
      for (MapLocation takenLeadLoc : takenLead) { // 210 per run of this
        // if we are closer, ignore the miner
        if (friend.location.isWithinDistanceSquared(takenLeadLoc, Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(takenLeadLoc))) {
          continue;
        }
//        System.out.println("Miner already claims lead at " + takenLeadLoc + " -- bytecode: " + Clock.getBytecodeNum());
//        System.out.println("My live loc: " + rc.getLocation() + " -- can see taken lead: " + rc.getLocation().isWithinDistanceSquared(takenLeadLoc, Cache.Permanent.VISION_RADIUS_SQUARED));

        int startX = 4 + takenLeadLoc.x - Cache.PerTurn.CURRENT_LOCATION.x;
        int startY = 4 + takenLeadLoc.y - Cache.PerTurn.CURRENT_LOCATION.y;
        if (takenLeadLoc.x == 0) startX++;
        if (takenLeadLoc.y == 0) startY++;
        int xCount = 3;
        int yCount = 3;
        if (takenLeadLoc.x == 0 || takenLeadLoc.x == Cache.Permanent.MAP_WIDTH) xCount = 2;
        if (takenLeadLoc.y == 0 || takenLeadLoc.y == Cache.Permanent.MAP_HEIGHT) yCount = 2;
        int indIncr = 11 - yCount;
        int start = startX * 11 + startY;
//          System.out.printf("start:[%d,%d] - count:[%d,%d] -- start:%d - incr:%d\n", startX, startY, xCount, yCount, start, indIncr);
        switch (xCount) {
          case 3:
            switch (yCount) {
              case 3:
                leadByLocationMap[start++] -= leadToTake;
              case 2:
                leadByLocationMap[start++] -= leadToTake;
              case 1:
                leadByLocationMap[start++] -= leadToTake;
            }
            start += indIncr;
          case 2:
            switch (yCount) {
              case 3:
                leadByLocationMap[start++] -= leadToTake;
              case 2:
                leadByLocationMap[start++] -= leadToTake;
              case 1:
                leadByLocationMap[start++] -= leadToTake;
            }
            start += indIncr;
          case 1:
            switch (yCount) {
              case 3:
                leadByLocationMap[start++] -= leadToTake;
              case 2:
                leadByLocationMap[start++] -= leadToTake;
              case 1:
                leadByLocationMap[start] -= leadToTake;
            }
        }
      }
    }
//    System.out.println("Miner end check friends(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);


//    MapLocation candidateLocation = null;
//    for (int t = numLocsToCheck; --t >= 0; ) {
//      int i = locsToCheck[t];
    for (MapLocation leadLoc : leadLocs) {
      for (MapLocation candidateLocation : rc.getAllLocationsWithinRadiusSquared(leadLoc, Utils.DSQ_1by1)) {

//      System.out.println("Miner check one candidate(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

        int i = (5 + candidateLocation.x - Cache.PerTurn.CURRENT_LOCATION.x) * 11 + (5 + candidateLocation.y - Cache.PerTurn.CURRENT_LOCATION.y);
        int candidateLead = leadByLocationMap[i];
        leadByLocationMap[i] = 0;
//        int candidateLead = leadByLocationMap[i];
//        leadByLocationMap[i] = 0;
        if (candidateLead <= 0) {
//        rejectedLocations.add(candidateLocation);
          continue;
        }
//      MapLocation candidateLocation = new MapLocation((i/11)-5+Cache.PerTurn.CURRENT_LOCATION.x, (i%11)-5+Cache.PerTurn.CURRENT_LOCATION.y);

        if (!rc.canSenseLocation(candidateLocation)) continue;

        int candidateRubble = rc.senseRubble(candidateLocation);

        if (candidateRubble > leastRubble) continue;
        int candidateDist = candidateLocation.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION);
        if (candidateRubble == leastRubble) {
          if (candidateLead < bestLead) continue;
          if (candidateLead == bestLead && candidateDist >= bestDist) continue;
        }
        bestLocation = candidateLocation;
        leastRubble = candidateRubble;
        bestDist = candidateDist;
        bestLead = candidateLead;
      }
    }
//    System.out.println("Best lead at " + bestLocation + " -- bestRubble: " + leastRubble + " bestLead: " + bestLead + " bestDistance: " + bestDist);

    return bestLocation;
  }

  /**
   * register the location with the miner with some regulations
   * @param newTarget the target to set
   * @return if the target was set
   */
  private boolean registerTarget(MapLocation newTarget) {
    int distToNewTarget = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(newTarget);
    if (distToNewTarget > MAX_SQDIST_FOR_TARGET) { // target too far to follow
      return false;
    }
    // if we already have a target that's closer
    if (target != null && distToNewTarget >= Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(target)) {
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
//    Direction goal = Cache.PerTurn.CURRENT_LOCATION.directionTo(target);
    if (moveOptimalTowards(target)) {
      rc.setIndicatorString("Approaching target" + target);
//    moveInDirLoose(goal);
      rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, target, 255, 10, 10);
      rc.setIndicatorDot(target, 0, 255, 0);
    }
    return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(target, Cache.Permanent.ACTION_RADIUS_SQUARED); // set target to null if found!
  }

  /**
   * send a LeadFoundMessage with the specified location
   * @param location the location where to find lead!
   */
  private void broadcastLead(MapLocation location) {
//    communicator.enqueueMessage(new LeadFoundMessage(location, Cache.PerTurn.ROUND_NUM));
//    rc.setIndicatorDot(location, 0, 255, 0);
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
    leadRequest = new LeadRequestMessage(Cache.PerTurn.CURRENT_LOCATION, Cache.PerTurn.ROUND_NUM);
    communicator.enqueueMessage(leadRequest);
    rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 0, 0, 255);
    rc.setIndicatorString("Requesting lead!");
    //System.out.println("Requesting lead!");
  }
}
