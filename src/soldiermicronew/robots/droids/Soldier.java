package soldiermicronew.robots.droids;

import battlecode.common.*;
import soldiermicronew.communications.messages.*;
import soldiermicronew.utils.Cache;
import soldiermicronew.utils.Utils;

public class Soldier extends Droid {
  /* fraction of distance to the target where bots should meet up */
  public static final double MEETUP_FACTOR = 0.25;
  public static int VISION_FRACTION_TO_RAID = 4;

  MapLocation myPotentialTarget;
  final MapLocation meetupPoint;

  int visionSize;

  boolean canStartRaid;
  MapLocation raidTarget;
  boolean raidValidated;

  RobotInfo robotToChase;
  MapLocation target;
  boolean reachedTarget;


  MapLocation archonToSave;

  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
    target = Utils.randomMapLocation();
    if (rc.senseNearbyLocationsWithLead().length > 15) VISION_FRACTION_TO_RAID = 6;
    int mapW = Cache.Permanent.MAP_WIDTH;
    int mapH = Cache.Permanent.MAP_HEIGHT;
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
    meetupPoint = Utils.lerpLocations(Cache.PerTurn.CURRENT_LOCATION, myPotentialTarget, MEETUP_FACTOR);
    visionSize = rc.getAllLocationsWithinRadiusSquared(meetupPoint, 100).length;
    canStartRaid = true;
  }

  @Override
  protected void runTurn() throws GameActionException {
//    //System.out.println();

    // miner-like random exploration (random target and go to it)

    // get all robots in vision
    boolean enemySoldierExists = false;
    boolean enemyMinerExists = false;
    boolean enemyBuilderExists = false;
    boolean enemyArchonExists = false;
    boolean enemySageExists = false;
    
    for (RobotInfo robot : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      switch(robot.type) {
        case SOLDIER:
          enemySoldierExists = true;
          break;
        case MINER:
          enemyMinerExists = true;
          break;
        case BUILDER:
          enemyBuilderExists = true;
          break;
        case ARCHON:
          enemyArchonExists = true;
          break;
        case SAGE:
          enemySageExists = true;
          break;
      }
    }


    // store global var robotInfo of the thing we are chasing
    // if we are chasing, don't search for new targets -- bytecode optimization stuff
//    //System.out.println("Soldier " + Cache.PerTurn.CURRENT_LOCATION + " --\nenemySoldierExists: " + enemySoldierExists + "\nenemyMinerExists: " + enemyMinerExists + "\nenemyBuilderExists: " + enemyBuilderExists + "\nenemyArchonExists: " + enemyArchonExists + "\nenemySageExists: " + enemySageExists);
//    //System.out.println("\nrobotToChase: " + robotToChase + "\ntarget: " + target + "\nreachedTarget: " + reachedTarget);

    if (enemySoldierExists) attackEnemySoldier();
    else if (enemyMinerExists) attackEnemyMiner();
    else if (enemyBuilderExists) attackEnemyBuilder();
    else if (enemyArchonExists) attackEnemyArchon();
    else if (enemySageExists) attackEnemySage();
    else {
      // if no one is in vision, we 1) go to the cached location if exists or 2) the random target location
      // cached location is set to null if we go there and no enemy is found in vision radius
//      setIndicatorString("no enemy", null);
      if (robotToChase != null) {
        if (robotToChase.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION) > Utils.DSQ_1by1) {
//          //System.out.println("MOVE TO CACHED LOCATION");
          moveTowardsAvoidRubble(robotToChase.location);
        } else {
//          //System.out.println("RESET ROBOT TO CHASE");
          robotToChase = null;
        }
      }
      if (robotToChase == null) {
        //rc.setIndicatorString("Soldier goToTarget - " + target);
        reachedTarget = goToTarget();
      }
    }

    if (robotToChase != null) {
      //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, robotToChase.location, 0, 0, 255);
      //rc.setIndicatorDot(robotToChase.location, 0, 255, 0);
    }

    // if we reached target, reset target
    if (reachedTarget) {
      target = Utils.randomMapLocation(); // new random target
    }

    //TODO: should technically check cases again if I just moved and have action cooldown, but this is fine for now!!

    // else if we see miner or builder, keep miner as close as possible to me

    // soldier caches last seen enemy? --> miner takes priority?

    // archon --> some type of formula to see if we can maybe kill it? or just prioritize closer unit if no soldier

    //

//    //rc.setIndicatorString("Soldier run - " + Cache.PerTurn.ROUND_NUM);
//    // Try to attack someone
//    if (raidTarget != null) attackTarget(raidTarget);
//    //rc.setIndicatorString("Soldier attackTarget - " + Cache.PerTurn.ROUND_NUM);
//
//    checkForAndCallRaid();
//    //rc.setIndicatorString("Soldier checkRaid - " + Cache.PerTurn.ROUND_NUM);
//
//    if (archonToSave != null) {
//      if (raidTarget != null) {
//        //rc.setIndicatorString("need to save arhcon! - end raid");
//        broadcastEndRaid();
//        raidTarget = null;
//        raidValidated = false;
//      }
//      if (moveTowardsAvoidRubble(archonToSave) && checkDoneSaving()) {
//        finishSaving();
//      }
//      attackNearby();
//    } else if (raidTarget != null) {
//      if (moveForRaid()) { // reached target
////        raidTarget = null;
//        if (!raidValidated) {
//          for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
//            if (enemy.type == RobotType.ARCHON) {
//              raidValidated = true;
//              break;
//            }
//          }
//          if (!raidValidated) {
//            //rc.setIndicatorString("raid (" + raidTarget + ") not valid -- no archon");
//            broadcastEndRaid();
//          }
////        } else if (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length > visionSize / VISION_FRACTION_TO_RAID) {
////          broadcastEndRaid();
//        }
//      }
//      //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, raidTarget, 0,0,255);
//    } else {
////      moveInDirLoose(Cache.PerTurn.CURRENT_LOCATION.directionTo(center));
//      moveTowardsAvoidRubble(meetupPoint);
//    }
//    //rc.setIndicatorString("Soldier movement done - " + Cache.PerTurn.ROUND_NUM);
//
////    if (!raidValidated) {
////      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
////        if (enemy.type == RobotType.ARCHON && (raidTarget == null || !raidTarget.equals(enemy.location))) {
////          //rc.setIndicatorString("Saw archon at " + enemy.location + " -- call raid!");
////          callForRaid(enemy.location);
////        }
////      }
////    }
//
//    attackNearby();
//    //rc.setIndicatorString("Soldier attackNearby - " + Cache.PerTurn.ROUND_NUM);

  }

  /**
   *
   * assuming there is a target for the miner, approach it
   *    currently very naive -- should use path finding!
   * @return if the miner is within the action radius of the target
   * @throws GameActionException if movement or line indication fails
   */
  private boolean goToTarget() throws GameActionException {
//    Direction goal = Cache.PerTurn.CURRENT_LOCATION.directionTo(target);
    if (moveTowardsAvoidRubble(target)) {
      //rc.setIndicatorString("Approaching target" + target);
//    moveInDirLoose(goal);
      //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, target, 255, 10, 10);
      //rc.setIndicatorDot(target, 0, 255, 0);
    }
    return Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(target, Cache.Permanent.ACTION_RADIUS_SQUARED); // set target to null if found!
  }

  private void attackEnemySage() {
    // if they attacked us, then let's attack them back for X (18) rounds.
    // Otherwise or after 18 rounds, add location to "banned" list and avoid getting close to it again or something
    //TODO: get health last round (based on health loss determine if sage attacked)
    MapLocation enemyLocation = null;
    int minHealth = Integer.MAX_VALUE;

    setIndicatorString("attackEnemySage", enemyLocation);
  }

  private boolean attackEnemyArchon() throws GameActionException {
    // move towards and attack
    MapLocation enemyLocation = null;
    int minHealth = Integer.MAX_VALUE;

    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      if (enemy.type == RobotType.ARCHON && enemy.health < minHealth) {
        minHealth = enemy.health;
        enemyLocation = enemy.location;
        robotToChase = enemy;
      }
    }
    setIndicatorString("attackEnemyArchon", enemyLocation);
    if (enemyLocation != null) {
      if (rc.isMovementReady() && (!enemyLocation.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_1by1))) moveTowardsAvoidRubble(enemyLocation);
      if (rc.canAttack(enemyLocation)) return attackTarget(enemyLocation);
    }

    return false;
  }

  private boolean attackEnemyBuilder() throws GameActionException {

    // same as miner

    MapLocation enemyLocation = null;
    int minHealth = Integer.MAX_VALUE;
    int minDistance = Integer.MAX_VALUE;

    if (rc.isMovementReady()) {
      // find least health enemy miner in vision radius
      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
        int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(enemy.location);
        if (enemy.type == RobotType.BUILDER && (enemy.health < minHealth || (enemy.health == minHealth && candidateDistance < minDistance))) {
          minHealth = enemy.health;
          enemyLocation = enemy.location;
          minDistance = candidateDistance;
          robotToChase = enemy;
        }
      }
    } else {
      // find least health enemy miner in action radius
      // the robotToChase is set to the miner with the least health irregardless of distance
      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
        int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(enemy.location);
        if (enemy.type == RobotType.BUILDER && (enemy.health < minHealth || (enemy.health == minHealth && candidateDistance < minDistance))) {
          robotToChase = enemy;
          if (enemy.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION) <= Cache.Permanent.ACTION_RADIUS_SQUARED) {
            minHealth = enemy.health;
            enemyLocation = enemy.location;
            minDistance = candidateDistance;
          }
        }
      }
    }
//    //System.out.println("ATTACK BUILDER -- enemyLocation: " + enemyLocation + " minHealth: " + minHealth + " isMovementReady: " + rc.isMovementReady());
//    //System.out.println("robotToChase: " + robotToChase);
    setIndicatorString("attackEnemyBuilder", enemyLocation);

    if (enemyLocation != null) {
      if (rc.isMovementReady() && minDistance > Utils.DSQ_1by1) moveTowardsAvoidRubble(enemyLocation);
      if (rc.canAttack(enemyLocation)) return attackTarget(enemyLocation);
    }

    return false;
  }

  private boolean attackEnemySoldier() throws GameActionException {
    // if closest enemy soldier is within radius, try to get it further away

    // we basically see if everything else stayed the exact same, is moving to a different location improve the overall position?

    //keep in action radius

    // if we see soldier, keep soldier on the edge of action radius (consider low rubble squares with < rubble)
    // maybe if we a function -->
    //  E = # of enemy soldiers that can attack you... F = # of friendly soldiers that can attack closest enemy to you
    //  R = rubble at some location  (consider later maybe: EE is number of enemies that can attack you at that location radius?)
    // calculating EE is not too expensive --> senseRobotsAtLocation(candidate, actionRadius + constant <-- if their soldiers move in) => (take soldiers sum)

    // currentBest: (# of attacks I can do till I die) = (# of turns till I die) / (# of turns between action cooldown)
    // consider candidate ': (# of attacking I can do till I die)' = (# of turns till I die)' / (# of turns between action cooldown)'
    // candidate > currentBest, currentBest = candidate
    // (# of turns till I die)' / (# of turns between action cooldown)' > (# of turns till I die) / (# of turns between action cooldown)

    double bestScore = Double.NEGATIVE_INFINITY;
    MapLocation bestLocation = null;
    MapLocation bestEnemySoldier = null;
    int bestDistance = -1;

    boolean currentLocationInActionRange = false;

    // my location and all adjacent locations
    for (Direction dir : Utils.directionsNine) {
      MapLocation candidate = Cache.PerTurn.CURRENT_LOCATION.add(dir);
      if (!rc.canSenseLocation(candidate)) continue;

      int numEnemySoldiers = 0;
      double averageEnemyDamagePerRound = 0;
      int minDistance = Integer.MAX_VALUE;
      MapLocation closestEnemySoldier = null; // can only be null iff averageEnemyDamagePerRound == 0 and averageEnemyDamagePerRound == 0, score=0

      for (RobotInfo enemy : rc.senseNearbyRobots(candidate, Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
        if (enemy.type == RobotType.SOLDIER) {
          numEnemySoldiers++;
          // determine how quickly enemy can attack
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, rc.senseRubble(enemy.location));
          averageEnemyDamagePerRound += (3 / turnsTillNextCooldown);

          // store closest enemy soldier to candidate location
          int candidateDistance = candidate.distanceSquaredTo(enemy.location);
          if (candidateDistance < minDistance) {
            minDistance = candidateDistance;
            closestEnemySoldier = enemy.location;
          }
        }
      }

      int numFriendlySoldiers = 0;
      double averageFriendlyDamagePerRound = 0;
      if (closestEnemySoldier != null) {
        if (closestEnemySoldier.distanceSquaredTo(candidate) <= Cache.Permanent.ACTION_RADIUS_SQUARED && rc.isActionReady()) {
          numFriendlySoldiers++;
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, rc.senseRubble(candidate));
          averageFriendlyDamagePerRound += (3 / turnsTillNextCooldown);
        }
        for (RobotInfo friendly : rc.senseNearbyRobots(closestEnemySoldier, Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OUR_TEAM)) {
          if (friendly.type == RobotType.SOLDIER && !friendly.location.equals(Cache.PerTurn.CURRENT_LOCATION)) {
            numFriendlySoldiers++;
            double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, rc.senseRubble(friendly.location));
            averageFriendlyDamagePerRound += (3 / turnsTillNextCooldown);
          }
        }
      }

      if (Direction.CENTER.equals(dir) && closestEnemySoldier != null) currentLocationInActionRange = true;

      // if the score is non-negative:
        // if my current location is not in action range of any soldier enemy AND I can attack if I were in range of an enemy, and the candidate score is equal to the best score so far
        // pick the movement between candidate and bestLocationFoundSoFar where we have largest distance s.t. distance <= action radius

        // WEIRD CASE (GO AGAINST SCORE):
        // if my current location is not in action range of any soldier enemy AND I cannot attack due to cooldown even if I were in range of an enemy, and the candidate score is equal to the best score so far
        // pick the movement between candidate and bestLocationFoundSoFar where we satisfy distance > action radius and rubble is the lowest (then maximum distance for rubble ties)

        // if my current location is in action range of an enemy AND I can attack, and the candidate score is equal to the best score so far
        // move to least rubble square (technically score should account for this already)? or attack and then pick the movement between candidate and bestLocationFoundSoFar with highest distance

        // WEIRD CASE (GO AGAINST SCORE):
        // if my current location is in action range of an enemy AND I cannot attack this turn due to cooldown, and the candidate score is equal to the best score so far
        // pick the movement between candidate and bestLocationFoundSoFar where we satisfy distance > action radius and rubble is the lowest (then maximum distance for rubble ties)


      // if my current location is in range of an enemy AND I can attack if I were in range of an enemy, and the candidate score is equal to the best score so far
      // pick the movement between candidate and bestLocationFoundSoFar

      // if I move to the candidate location, what is the distance to the closest enemy soldier?
      // if I move to the current best location, what is the distance to the closest enemy soldier?
      // pick the one with largest distance s.t distance <= action radius

//      //System.out.println("candLoc: " + candidate + " --\nnumEnemySoldiers: " + numEnemySoldiers + "\nenemyDmgPerRound: " + averageEnemyDamagePerRound + "\nclosestEnemySoldier: " + closestEnemySoldier + " --\nnumFriendlySoldiers: " + numFriendlySoldiers + "\nFriendlyDmgPerRound: " + averageFriendlyDamagePerRound);
      double score = averageFriendlyDamagePerRound - averageEnemyDamagePerRound + (averageFriendlyDamagePerRound * 0.01);
      if (rc.isMovementReady() && closestEnemySoldier != null) {
        int dist = closestEnemySoldier.distanceSquaredTo(candidate);
        if (rc.isActionReady()) {
          // stay inside action radius
          if (dist <= Cache.Permanent.ACTION_RADIUS_SQUARED) {
            score += dist * 0.005;
          }
        } else {
          if (dist > Cache.Permanent.ACTION_RADIUS_SQUARED) {
            score += dist * 0.005;
          }
        }
      }
      boolean isBetter = score > bestScore;

      // if the score is negative:
        // break ties by picking the movement between candidate and bestLocationFoundSoFar where distance is the furthest from enemy (GTFO -- miner code?)
      if (bestScore == score && score < 0) { // bestScore and score can only be negative if a closestEnemySoldier exists for each I think
        assert closestEnemySoldier != null;
        assert bestDistance != -1;
        int candidateDistance = candidate.distanceSquaredTo(closestEnemySoldier); // get distance between candidate and closest enemy to candidate
        // bestDistance is the distance between the bestLocation found so far and closest enemy to bestLocation
        if (candidateDistance > bestDistance) {
          bestLocation = candidate;
          bestDistance = candidateDistance;
        }
      }


      if (bestScore == score && score >= 0) {
        if (!currentLocationInActionRange && closestEnemySoldier != null) { // if I am not in range of any enemy soldier currently and moving would put me in range
          if (rc.isActionReady()) { // is ready to attack (case 1)
            if (bestDistance != -1) {

            }
          } else { // case 2

          }
        }

      }


//      if (score == bestScore && closestEnemySoldier != null) {
//        // choose the location that is furthest from the enemy but still in action radius
//        int candidateEnemyDistance = candidate.distanceSquaredTo(closestEnemySoldier); //candidate to closestEnemySoldier and current to closestEnemySoldier
//        int currentEnemyDistance = bestLocation.distanceSquaredTo(closestEnemySoldier);
//        if (currentEnemyDistance < candidateEnemyDistance && currentEnemyDistance <= Cache.Permanent.ACTION_RADIUS_SQUARED) isBetter = true;
//      }

      if (isBetter) {
        bestScore = score;
        bestLocation = candidate;
        bestEnemySoldier = closestEnemySoldier;
        if (closestEnemySoldier != null) bestDistance = candidate.distanceSquaredTo(closestEnemySoldier);
      }
    }


    setIndicatorString("attackEnemySoldier score(" + bestScore + ")", bestLocation);

    boolean attacked = false;
    if (bestEnemySoldier != null) {
      if (rc.canAttack(bestEnemySoldier)) attacked |= attackTarget(bestEnemySoldier);
    }
    if (bestLocation != null) {
      // might want to move out if bestLocation score is low (after attacking tho)
      if (rc.isMovementReady()) moveTowardsAvoidRubble(bestLocation);
    }
    if (bestEnemySoldier != null) {
      if (rc.canAttack(bestEnemySoldier)) attacked |= attackTarget(bestEnemySoldier);
    }

    return attacked;


    // let this bot determine for the local patch -> rate of our damage vs rate of enemy damage
    // pick the 3x3 location where the value (rate of our damage - rate of enemy damage) is highest

    // if the value is negative at location, we should leave the area entirely!
    // if the value is positive at location, we should stay in the area!

//    if (rc.isMovementReady()) {
//      // my location and all adjacent locations
//      for (Direction dir : Utils.directions) {
//        MapLocation candidate = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//        if (!rc.canSenseLocation(candidate)) continue;


//        int rateOfOurDamage = 0;
//        int rateOfEnemyDamage = 0;
//        // for all enemies, see how many can attack us
//        for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
//          if (friend.type != RobotType.SOLDIER || friend.location.equals(Cache.PerTurn.CURRENT_LOCATION)) continue;
//          for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
//            if (enemy.type != RobotType.SOLDIER) continue;
//
//
//            }
//          }
//        }
//      }
//  }
  }

  private boolean attackEnemyMiner() throws GameActionException {
    // possibly do score function of health and distance to enemy miner

    MapLocation enemyLocation = null;
    int minHealth = Integer.MAX_VALUE;
    int minDistance = Integer.MAX_VALUE;

    if (rc.isMovementReady()) {
      // find least health enemy miner in vision radius
      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
        int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(enemy.location);
        if (enemy.type == RobotType.MINER && (enemy.health < minHealth || (enemy.health == minHealth && candidateDistance < minDistance))) {
          minHealth = enemy.health;
          enemyLocation = enemy.location;
          minDistance = candidateDistance;
          robotToChase = enemy;
        }
      }
    } else {
      // find least health enemy miner in action radius
      // the robotToChase is set to the miner with the least health irregardless of distance
      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
        int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(enemy.location);
//        //System.out.println("enemy: " + enemy.location + " " + enemy.health + " " + candidateDistance + " minHealth: " + minHealth + " minDistance: " + minDistance);
        if (enemy.type == RobotType.MINER && (enemy.health < minHealth || (enemy.health == minHealth && candidateDistance < minDistance))) {
          robotToChase = enemy;
//          //System.out.println(robotToChase);
          if (enemy.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION) <= Cache.Permanent.ACTION_RADIUS_SQUARED) {
            minHealth = enemy.health;
            enemyLocation = enemy.location;
            minDistance = candidateDistance;
          }
        }
      }
    }
//    //System.out.println("ATTACK MINER -- enemyLocation: " + enemyLocation + " minHealth: " + minHealth + " isMovementReady: " + rc.isMovementReady());
//    //System.out.println("robotToChase: " + robotToChase);
    setIndicatorString("attackEnemyMiner", enemyLocation);

    if (enemyLocation != null) {
      if (rc.isMovementReady() && minDistance > Utils.DSQ_1by1) moveTowardsAvoidRubble(enemyLocation);
      if (rc.canAttack(enemyLocation)) return attackTarget(enemyLocation);
    }

    return false;
    // if outside action radius but in vision radius, do least health in vision radius -> move towards and attack
    // if its inside action radius, do least health in vision radius -> move towards and attack

    // cannot move -> action, move -> vision

    // if we cannot move: if outside action radius but in vision radius and cooldown, do one with least health in action radius -> attack

    //follow it!
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
//    if (raidValidated) {
//      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
//        if (enemy.type == RobotType.ARCHON && raidTarget.equals(enemy.location)) { // already raiding a different archon
//          return;
//        }
//      }
//    }
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
    if (!archonToSave.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) return false;
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
    int blocked = 0;
    for (RobotInfo friend : nearby) {
      if (friend.type == RobotType.SOLDIER) nearbySoldiers++;
      else blocked++;
    }
    // if many bois nearby (1/4 of vision)
    int minToRaid = (visionSize-blocked) / VISION_FRACTION_TO_RAID;
    //rc.setIndicatorString("soldiers: " + nearbySoldiers + " -- need: " + minToRaid);
    return nearbySoldiers > minToRaid;
  }

  /**
   * send a message to start a group raid at the given location
   * @param location where to raid
   */
  private void callForRaid(MapLocation location) {
    raidTarget = location;
    StartRaidMessage message = new StartRaidMessage(raidTarget, rc.getRoundNum());
    communicator.enqueueMessage(message);
  }

  /**
   * check if the soldier can trigger a raid, then do so
   * @return true if a raid was called
   */
  private boolean checkForAndCallRaid() {
    if (!canCallRaid()) return false;
    //rc.setIndicatorString("Ready to raid!");
//      //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, oppositeLoc, 0,0,255);
    callForRaid(myPotentialTarget);
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
    //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, raidTarget, 0,0,255);
//    return moveInDirLoose(Cache.PerTurn.CURRENT_LOCATION.directionTo(raidTarget))
    return moveTowardsAvoidRubble(raidTarget)
        && Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(raidTarget) <= Cache.Permanent.VISION_RADIUS_SQUARED;
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
            //rc.setIndicatorString("Archon target killed! -- end raid");
            broadcastEndRaid();
          }
        }
        return true;
      }
    }
    return false;
  }

  private void setIndicatorString(String custom, MapLocation enemyLocation) {
    //rc.setIndicatorString("Soldier " + custom + " -- enemyLocation: " + enemyLocation + " robotToChase: " + robotToChase);
    if (robotToChase != null) {
      //rc.setIndicatorString("Soldier " + custom + " - " + enemyLocation + " robotToChase: " + robotToChase.location);
    }
  }
}
