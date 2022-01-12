package firstbot.robots.droids;

import battlecode.common.*;
import firstbot.communications.messages.LeadFoundMessage;
import firstbot.communications.messages.LeadRequestMessage;
import firstbot.communications.messages.Message;
import firstbot.utils.Cache;
import firstbot.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class Miner extends Droid {

  int turnsWandering;
  private static final int WANDERING_TURNS_TO_BROADCAST_LEAD = 10; // if the miner wanders for >= 10 turns, it will broadcast when lead is found
  private static final int MAX_WANDERING_REQUEST_LEAD = 8; // if wandering for 5+ turns, request lead broadcast
  MapLocation target;
  boolean reachedTarget;
  int bestSquaredDistanceToTarget;
  int[][] friendMinerRobots; // stores the round number that a miner robot was there (init to 0s). DO NOT USE OLD VALUES ACROSS ROUNDS UNLESS ROBOT DOES NOT MOVE!
  int[][] frindMinerDP;
  private static final int WANDERING_TURNS_TO_FOLLOW_LEAD = 3;
  private static final int MAX_SQDIST_FOR_TARGET = 200;

  MapLocation runAwayTarget;

  // did we just move?
  boolean justMoved;
  long visited0;
  long visited1;
  MapLocation bestMapLocation = null;
  int bestRubble = 101;
  int bestDistance = 9999;


  private LeadRequestMessage leadRequest; // TODO: make a message that the other boi will overwrite (will rely on getting ack within 1 turn or else sad)

  public Miner(RobotController rc) throws GameActionException {
    super(rc);
    target = Utils.randomMapLocation();
    bestSquaredDistanceToTarget = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(target);
    leadRequest = null;
    justMoved = false;
    //13x13 because the vision circle is circumscribed by a 9x9 square
    // and then 2 more on each side because we do not want a miner within 2x2 square (this is just a buffer and will always be 0)
    friendMinerRobots = new int[13][13];
    frindMinerDP = new int[13][13];
    System.out.println("Miner init cost: " + Clock.getBytecodeNum());
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
    System.out.println("Miner run(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    int movementCooldown = rc.getMovementCooldownTurns();

    executeMining(); // performs action of mining gold and then lead until cooldown is reached
    System.out.println("Miner execMining(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
// executeLeadTarget(); // if miner doesnt already have a target and has a pending request that it sent last turn
    executeRunFromEnemy(); // set runAwayTarget if enemy attacking unit is within robot range (possibly bugged rn)
    System.out.println("Miner runAway(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    boolean resourcesLeft = checkIfResourcesLeft(); // check if any gold or lead (>1) is within robot range, return true if so
    System.out.println("Miner checkRss(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    // lets remove target if we havent gotten closer to it in 5 moves?
//    if (rc.getID() == 10001) {
//      //System.out.println("target: " + target + " reached: " + reachedTarget + " resourcesLeft: " + resourcesLeft);
//    }
    if (runAwayTarget != null) { // if enemy attacking unit is within range
      target = Utils.randomMapLocation(); // new random target
      if (runAway()) runAwayTarget = null; // runAway() is true iff we move away
    } else if (resourcesLeft && followLeadPranay()) {
      // performs action of moving to lead
    } else {
      reachedTarget = goToTarget(); // performs action of moving to target location
    }
    System.out.println("Miner movement done(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    executeMining();
    System.out.println("Miner execMining(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    if (reachedTarget) {
      target = Utils.randomMapLocation(); // new random target
    }

    if (justMoved) justMoved = false;
    if (rc.getMovementCooldownTurns() != movementCooldown) {
      justMoved = true;
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
    if (!rc.isActionReady()) return;

    // Try to mine on squares around us.
    for (MapLocation toMine : rc.senseNearbyLocationsWithGold(Cache.Permanent.ACTION_RADIUS_SQUARED, 2)) {
      rc.mineGold(toMine);
      if (!rc.isActionReady()) return;
    }
//    MapLocation me = Cache.PerTurn.CURRENT_LOCATION;
//    for (int dx = -1; dx <= 1; dx++) {
//      for (int dy = -1; dy <= 1; dy++) {
//        MapLocation mineLocation = me.translate(dx,dy);
//        while (rc.canMineGold(mineLocation)) {
//          rc.mineGold(mineLocation);
//        }
//      }
//    }
  }

  /**
   * subroutine to mine lead from all adjacent tiles
   * leaves 1pb in every tile
   */
  private void mineSurroundingLead() throws GameActionException {
    if (!rc.isActionReady()) return;
    // Try to mine on squares around us.
//    MapLocation me = Cache.PerTurn.CURRENT_LOCATION;
    for (MapLocation toMine : rc.senseNearbyLocationsWithLead(Cache.Permanent.ACTION_RADIUS_SQUARED, 2)) {
      rc.mineLead(toMine);
      if (!rc.isActionReady()) return;
    }
//    for (int dx = -1; dx <= 1; dx++) {
//      for (int dy = -1; dy <= 1; dy++) {
//        MapLocation mineLocation = me.translate(dx,dy);
//        while (rc.canSenseLocation(mineLocation) && rc.senseLead(mineLocation) > 1 && rc.canMineLead(mineLocation)) {
//          rc.mineLead(mineLocation);
//        }
//      }
//    }
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
      MapLocation myLoc = Cache.PerTurn.CURRENT_LOCATION;
      runAwayTarget = new MapLocation((myLoc.x << 1) - enemies.x, (myLoc.y << 1) - enemies.y);
      Direction backToSelf = runAwayTarget.directionTo(myLoc);
      while (!rc.canSenseLocation(runAwayTarget)) runAwayTarget = runAwayTarget.add(backToSelf);
      rc.setIndicatorDot(myLoc, 255,255,0);
      rc.setIndicatorLine(enemies, runAwayTarget, 255, 255, 0);
    }
  }


  /**
   * check if there are any enemy (soldiers) to run away from
   * @return the map location where there are offensive enemies (null if none)
   */
  private MapLocation findEnemies() {
    if (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length == 0) return null;
    int avgX = 0;
    int avgY = 0;
    int count = 0;
    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
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
  private boolean followLead() throws GameActionException {
    boolean followedLead = moveToHighLeadProbabilistic();
    if (followedLead) {
      if (turnsWandering > WANDERING_TURNS_TO_BROADCAST_LEAD) {
        broadcastLead(Cache.PerTurn.CURRENT_LOCATION);
      }
      turnsWandering = 0;
    }
    return followedLead;
  }

  /**
   * move towards lead
   *  1. find all locations that can mine some resource (>1 lead or gold)
   *  2. for each location, determine the one with least rubble. Break ties by finding ones that are closer to current location
   * @return if moved towards lead
   * @throws GameActionException if movement failed
   */
  private boolean followLeadPranay() throws GameActionException {
    System.out.println("Miner start followLeadPnay(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    boolean followedLead = moveToLeadResources();
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
  protected boolean moveToLeadResources() throws GameActionException {
    MapLocation highLead = getBestLeadWithHash();
    System.out.println("Miner finish getBestLeadLocPranay(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    if (highLead != null) {
      rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, highLead, 0, 0, 255);
      rc.setIndicatorString("lead: " + highLead);
    }
    System.out.println("high lead: " + highLead);
    return highLead != null && (highLead.equals(Cache.PerTurn.CURRENT_LOCATION) || moveTowardsAvoidRubble(highLead));
  }

  private MapLocation getBestLeadWithHash() throws GameActionException {
    MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, 2);
    Map<MapLocation, Integer> leads = new HashMap<>(leadLocs.length);
    for (MapLocation lead : leadLocs) {
      int leadThere = rc.senseLead(lead);
      for (MapLocation local : rc.getAllLocationsWithinRadiusSquared(lead, Utils.DSQ_1by1)) {
        leads.merge(local, leadThere, Integer::sum);
      }
    }

    for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
      if (friend.type == RobotType.MINER) {
        MapLocation[] takenLead = rc.senseNearbyLocationsWithLead(friend.location, Utils.DSQ_2by2, 2);
        if (takenLead.length == 0) continue;
        int leadToTake = -75 / takenLead.length;
        for (MapLocation takenLeadLoc : takenLead) {
          // if we are closer, ignore the miner
          if (Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(takenLeadLoc) <= friend.location.distanceSquaredTo(takenLeadLoc)) {
            continue;
          }
          for (MapLocation local : rc.getAllLocationsWithinRadiusSquared(takenLeadLoc, Utils.DSQ_1by1)) {
            leads.merge(local, leadToTake, Integer::sum);
          }
//          leads.merge(takenLeadLoc, leadToTake, Integer::sum);
        }
      }
    }

    MapLocation bestLocation = null;
    int leastRubble = 101;
    int bestDist = 9999;
    int bestLead = 0;

    for (MapLocation candidateLocation : leadLocs) {
      int candidateLead = leads.get(candidateLocation);
      if (candidateLead <= 0) continue;

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

    return bestLocation;

  }

  /**
   * potential bug: say we have a patch of lead that this robot should join to help existing robots.
   * Due to our 5x5 clump box, the robot may have to mine the clump from a different square that has higher rubble even though there may exist a closer one with less rubble
   *
   * potential bug: does not account gold
   * @return the most tile in the center of most lead
   * @throws GameActionException if some game op fails
   */
  protected MapLocation getBestLeadLocPranay() throws GameActionException {
    System.out.println("Miner start getBestLeadLocPnay(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
    MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(Cache.Permanent.VISION_RADIUS_SQUARED, 2);
//    HashSet<MapLocation> visitedLocations = new HashSet<>();

    if (justMoved) { // reset states
      visited0 = 0;
      visited1 = 0;
      bestMapLocation = null;
      bestRubble = 101;
      bestDistance = 9999;
    }

    //to update the array, we do the following (<1k bytecode estimated):
    // rc.senseNearbyRobots(all) and set the locations where there is a miner robot to the round number
//    RobotInfo[] all_nearby_friendly_robots = Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS;
//    for (int i = 0, all_nearby_friendly_robotsLength = all_nearby_friendly_robots.length; i < all_nearby_friendly_robotsLength; i++) {
//      RobotInfo friend = all_nearby_friendly_robots[i];
//      if (friend.type == RobotType.MINER) {
//        int xIndexMapped = 6 + (friend.location.x - Cache.PerTurn.CURRENT_LOCATION.x);
//        int yIndexMapped = 6 + (friend.location.y - Cache.PerTurn.CURRENT_LOCATION.y);
//        friendMinerRobots[xIndexMapped][yIndexMapped] = Cache.PerTurn.ROUND_NUM;
//      }
//    }
    System.out.println("Miner read friendlies(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

//    for(int i=2;i<13;i++) {
//      int prsum=0;
//      for(int j=2;j<13;j++) { //row-wise
//        if (friendMinerRobots[i][j] == Cache.PerTurn.ROUND_NUM) {
//          frindMinerDP[i][j] = ++prsum;
//        } else {
//          frindMinerDP[i][j] = prsum;
//        }
//      }
//      System.out.println("Miner fill dp row[" + i + "](" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
//    }
//    System.out.println("Miner fill dp rowwise(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

//    for(int j=2;j<13;j++) {
//      int prsum=0;
//      for(int i=2;i<13;i++) { //col-wise
//        frindMinerDP[i][j]+=prsum;
//        prsum=frindMinerDP[i][j];
//      }
//      System.out.println("Miner fill dp col[" + j + "](" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);
//    }
//    System.out.println("Miner read fill dp colwise(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    MapLocation topLeft = Cache.PerTurn.CURRENT_LOCATION.translate(-4,4);
    for (int i = 0, leadLocsLength = Math.min(leadLocs.length, 100) ; i < leadLocsLength; ++i) {
      int startByteCode = Clock.getBytecodeNum();
      for (int dx = -1; dx <= 1; ++dx) {
        for (int dy = -1; dy <= 1; ++dy) { // TODO: maybe a more optimized way to iterate and check if lead is on or adj to a location?
          MapLocation candidateLocation = leadLocs[i].translate(dx, dy);
          int dxdybyte = Clock.getBytecodeNum();
          System.out.println("Bytecode dx/dy: " + (dxdybyte - startByteCode));

          // location cannot be sensed from robot or we already tried this location (bytecode: 200)
          if (!rc.canSenseLocation(candidateLocation)) continue;
          MapLocation visitedIndex = candidateLocation.translate(-topLeft.x, -topLeft.y);
          int xyz = Clock.getBytecodeNum();
          int bitDex = visitedIndex.x * 9 + visitedIndex.y;
          int xyz2 = Clock.getBytecodeNum();
          System.out.println("bytecode to multiply: " + (xyz2-xyz));
          int moddedBitDex = bitDex & 0b111111;
          long mask = 1L << moddedBitDex;
          if (bitDex < 64) {
            if ((visited0 & mask) > 0)
              continue;
            else
              visited0 |= mask;
          } else {
            if ((visited1 & mask) > 0)
              continue;
            else
              visited1 |= mask;
          }
//          if (visitedLocations.contains(candidateLocation)) continue;
//          visitedLocations.add(candidateLocation);
          int visitbyte = Clock.getBytecodeNum();
          System.out.println("Bytecode visitedLocations: " + (visitbyte - dxdybyte));


          int candidateRubble = rc.senseRubble(candidateLocation);
          int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(candidateLocation); //7

          if (candidateRubble < bestRubble || (candidateRubble == bestRubble && candidateDistance < bestDistance)) {
            // candidate location.. check if too many miners there?
            // on a candidate location query we simply check the round number of the 5x5 matches (25 spots) and increment if there is a miner
            int leadSeen = rc.senseLead(candidateLocation); //TODO: maybe a 3x3 clump centered around candidateLocation instead?
            int before2x2gridbyte = Clock.getBytecodeNum();
            System.out.println("Bytecode to filter coord: " + (before2x2gridbyte - visitbyte));
//            int xIndexMapped = 6 + (candidateLocation.x - Cache.PerTurn.CURRENT_LOCATION.x);
//            int yIndexMapped = 6 + (candidateLocation.y - Cache.PerTurn.CURRENT_LOCATION.y);
//            System.out.printf("DP on 5x5: %d,%d\n", xIndexMapped, yIndexMapped);
            // use dp to calc miners there
            int minersThere = 0;
//            if(xIndexMapped>2 && yIndexMapped>2) {
//              minersThere = frindMinerDP[xIndexMapped+2][yIndexMapped+2]-frindMinerDP[xIndexMapped+2][yIndexMapped-3]-frindMinerDP[xIndexMapped-3][yIndexMapped+2]+frindMinerDP[xIndexMapped-3][yIndexMapped-3];
//            } else if(xIndexMapped>2) { //not col1
//              minersThere = frindMinerDP[xIndexMapped+2][yIndexMapped+2]-frindMinerDP[xIndexMapped-3][yIndexMapped+2];
//            } else if(yIndexMapped>2) { //not row1
//              minersThere = frindMinerDP[xIndexMapped+2][yIndexMapped+2]-frindMinerDP[xIndexMapped+2][yIndexMapped-3];
//            } else { //when row1==0 && row2==0
//              minersThere = frindMinerDP[xIndexMapped+2][yIndexMapped+2];
//            }
//            minersThere =   frindMinerDP[xIndexMapped+2][yIndexMapped+2]-frindMinerDP[xIndexMapped+2][yIndexMapped-3]-frindMinerDP[xIndexMapped-3][yIndexMapped+2]+frindMinerDP[xIndexMapped-3][yIndexMapped-3];
//            int minersThere = frindMinerDP[row2]          [col2]          -frindMinerDP[row2]          [col1-1]        -frindMinerDP[row1-1]        [col2]          +frindMinerDP[row1-1]        [col1-1];

              for (RobotInfo bot : rc.senseNearbyRobots(candidateLocation, 8, Cache.Permanent.OUR_TEAM)) {
                if (bot.type == RobotType.MINER) minersThere++;
              }

//            if (friendMinerRobots[xIndexMapped + -2][yIndexMapped + -2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -2][yIndexMapped + -1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -2][yIndexMapped + 0] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -2][yIndexMapped + 1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -2][yIndexMapped + 2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -1][yIndexMapped + -2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -1][yIndexMapped + -1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -1][yIndexMapped + 0] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -1][yIndexMapped + 1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + -1][yIndexMapped + 2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 0][yIndexMapped + -2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 0][yIndexMapped + -1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 0][yIndexMapped + 0] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 0][yIndexMapped + 1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 0][yIndexMapped + 2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 1][yIndexMapped + -2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 1][yIndexMapped + -1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 1][yIndexMapped + 0] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 1][yIndexMapped + 1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 1][yIndexMapped + 2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 2][yIndexMapped + -2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 2][yIndexMapped + -1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 2][yIndexMapped + 0] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 2][yIndexMapped + 1] == Cache.PerTurn.ROUND_NUM) ++minersThere;
//            if (friendMinerRobots[xIndexMapped + 2][yIndexMapped + 2] == Cache.PerTurn.ROUND_NUM) ++minersThere;
            //todo:
//                if (Math.abs(dx2) == 1 && Math.abs(dy2) == 1) {
//                  MapLocation tmp = new MapLocation(xIndexMapped + dx2, yIndexMapped + dy2);
//                  if (rc.canSenseLocation(tmp)) rc.senseLead(tmp);
//                }
            int endByteCode = Clock.getBytecodeNum();
            System.out.println("bytecode for 5x5 check: " + (endByteCode - before2x2gridbyte));
            System.out.println("Result for " + candidateLocation + " -- miners: " + minersThere + " lead: " + leadSeen + " rubble: " + candidateRubble + " dist: " + candidateDistance);
            if (minersThere > leadSeen / 75) continue;

            // we have found a better miner!
            bestMapLocation = candidateLocation;
            bestRubble = candidateRubble;
            bestDistance = candidateDistance;

          }
        }
      }
    }
    return bestMapLocation;
  }


  /**
   * potential bug: say we have a patch of lead that this robot should join to help existing robots.
   * Due to our 5x5 clump box, the robot may have to mine the clump from a different square that has higher rubble even though there may exist a closer one with less rubble
   *
   * potential bug: does not account gold
   * @return the most tile in the center of most lead
   * @throws GameActionException if some game op fails
   */
  protected MapLocation getBestLeadLocPranay2() throws GameActionException {
    System.out.println("Miner start getBestLeadLocPranay2(" + Clock.getBytecodeNum() + ") - " + Cache.PerTurn.ROUND_NUM);

    if (justMoved || true) { // reset states
      visited0 = 0;
      visited1 = 0;
      bestMapLocation = null;
      bestRubble = 101;
      bestDistance = 9999;
    }

    MapLocation[] allLocationsWithinRadiusSquared = rc.getAllLocationsWithinRadiusSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED);
    for (int i = 0, allLocationsWithinRadiusSquaredLength = allLocationsWithinRadiusSquared.length; i < allLocationsWithinRadiusSquaredLength; i++) {
      int startByteCode = Clock.getBytecodeNum();
      MapLocation candidateLocation = allLocationsWithinRadiusSquared[i];
      System.out.println(" candidateLocation: " + candidateLocation);
      if (rc.senseNearbyLocationsWithLead(candidateLocation, Utils.DSQ_1by1, 2).length > 0) {
        // a valid location
        int candidateRubble = rc.senseRubble(candidateLocation);
        int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(candidateLocation);
        System.out.println("VALID candidateLocation: " + candidateLocation + " candidateRubble: " + candidateRubble + " bestRubble: " + bestRubble + " candidateDistance: " + candidateDistance + " bestDistance: " + bestDistance);
        if (candidateRubble < bestRubble || (candidateRubble == bestRubble && candidateDistance < bestDistance)) {

          int leadSeen = rc.senseLead(candidateLocation);
          int minersThere = 0;
          for (RobotInfo bot : rc.senseNearbyRobots(candidateLocation, Utils.DSQ_2by2, Cache.Permanent.OUR_TEAM)) {
            if (bot.type == RobotType.MINER) minersThere++;
          }
          int endByteCode = Clock.getBytecodeNum();
          System.out.println("bytecode: " + (endByteCode - startByteCode));
          System.out.println("Result for " + candidateLocation + " -- miners: " + minersThere + " lead: " + leadSeen + " rubble: " + candidateRubble + " dist: " + candidateDistance);
          if (minersThere > leadSeen / 75) continue;

          // we have found a better miner!
          bestMapLocation = candidateLocation;
          bestRubble = candidateRubble;
          bestDistance = candidateDistance;
        }
      }
    }
    return bestMapLocation;
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
    if (moveTowardsAvoidRubble(target)) {
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
