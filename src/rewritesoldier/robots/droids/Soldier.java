package rewritesoldier.robots.droids;

import battlecode.common.*;
import rewritesoldier.communications.messages.*;
import rewritesoldier.utils.Cache;
import rewritesoldier.utils.Utils;

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

  MapLocation archonToSave;

  MapLocation fightToJoin;

  private boolean chasingCommedEnemy;

  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
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
    visionSize = rc.getAllLocationsWithinRadiusSquared(meetupPoint, Cache.Permanent.VISION_RADIUS_SQUARED).length;
    canStartRaid = true;
  }

  boolean enemySoldierExistsInVision;
  boolean enemyMinerExistsInVision;
  boolean enemyBuilderExistsInVision;
  boolean enemyArchonExistsInVision;
  boolean enemySageExistsInVision;

  boolean enemySoldierExistsInAction;
  boolean enemyMinerExistsInAction;
  boolean enemyBuilderExistsInAction;
  boolean enemyArchonExistsInAction;
  boolean enemySageExistsInAction;


  @Override
  protected void runTurn() throws GameActionException {
//    if (fightToJoin != null && !fightToJoin.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED*2)) {
//      explorationTarget = fightToJoin;
//    }
    if (closestCommedEnemy != null) {
      explorationTarget = closestCommedEnemy;
      exploringRandomly = false;
      MapLocation friendly = communicator.archonInfo.getNearestFriendlyArchon(explorationTarget);
      MapLocation enemy = communicator.archonInfo.getNearestEnemyArchon(explorationTarget);
      Direction backHome = enemy.directionTo(friendly);
      int tries = 20;
      while (tries-- > 0 && friendly.distanceSquaredTo(explorationTarget) > enemy.distanceSquaredTo(explorationTarget)) {
        explorationTarget = explorationTarget.add(backHome);
      }
      chasingCommedEnemy = true;
    }

    if (archonToSave != null && !needToRunHomeForSaving && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(archonToSave, Cache.Permanent.VISION_RADIUS_SQUARED)) {
//      //Utils.print("archonToSave: " + archonToSave);
      if (moveOptimalTowards(archonToSave) && checkDoneSaving()) {
        finishSaving();
      }
    }


    runNew();


    if (rc.isActionReady()) {
      attackNearby();
    }

    fightToJoin = null;
  }



  void runOld() throws GameActionException {

    if (needToRunHomeForSaving) {
      robotToChase = null;
    }

    // if we cannot move, attack is ready, and there is an enemy in action --> ATTACK!
    if (!rc.isMovementReady() && rc.isActionReady() && processEnemiesInAction()) {
      attackPriority(enemySoldierExistsInAction, enemyMinerExistsInAction, enemyBuilderExistsInAction, enemyArchonExistsInAction, enemySageExistsInAction);
    }
    // always do this as it sets up internal variables for us to use later?
    if (processEnemiesInVision() || !needToRunHomeForSaving) {
      //todo: robot remembers soldiers?
      attackPriority(enemySoldierExistsInVision, enemyMinerExistsInVision, enemyBuilderExistsInVision, enemyArchonExistsInVision, enemySageExistsInVision);
    }

    // if we can still attack and there are enemies, ATTACK! This happens when we do not move on purpose yet there are higher priority enemies in vision...
    if (rc.isActionReady() && processEnemiesInAction()) {
      boolean tmpNeedToRunHomeForSaving = needToRunHomeForSaving;
      needToRunHomeForSaving = true; // this will disable any movement options for us (I dont think it's needed but just in case)
      attackPriority(enemySoldierExistsInAction, enemyMinerExistsInAction, enemyBuilderExistsInAction, enemyArchonExistsInAction, enemySageExistsInAction);
      needToRunHomeForSaving = tmpNeedToRunHomeForSaving;
    }


    // store global var robotInfo of the thing we are chasing
    // if we are chasing, don't search for new targets -- bytecode optimization stuff

    if (robotToChase != null) {
      if (!rc.canSenseRobot(robotToChase.ID)) {
        robotToChase = null;
      } else {
        robotToChase = rc.senseRobot(robotToChase.ID);
      }
    }
  }

  RobotInfo lastAttackedEnemy;
  void runNew() throws GameActionException {
    if (anyOffensiveEnemies() && attackEnemies()) {
//      attackEnemies();
    } else if (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length > 0) {
      RobotInfo best = null;
      int distToBest = 9999;
      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
        if (best == null) {
          best = enemy;
          distToBest = enemy.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION);
        } else if (MicroInfo.getPriority(enemy.type) > MicroInfo.getPriority(best.type)) {
          best = enemy;
          distToBest = enemy.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION);
        } else if (enemy.health < best.health) {
          best = enemy;
          distToBest = enemy.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION);
        } else if (enemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, distToBest -1)) {
          best = enemy;
          distToBest = enemy.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION);
        }
      }
      attackAtAndMoveTo(best.location, best.location, true);
      lastAttackedEnemy = best;
    } else if (!needToRunHomeForSaving) {
      if (lastAttackedEnemy != null && lastAttackedEnemy.type.damage <= 0) {
//        //Utils.print("robotToChase: " + robotToChase);
        attackAtAndMoveTo(lastAttackedEnemy.location, lastAttackedEnemy.location, true);
        if (lastAttackedEnemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_2by2)) {
          lastAttackedEnemy = null;
        }
      }


      if (lastAttackedEnemy == null) {
        if (closestCommedEnemy == null && chasingCommedEnemy) {
          randomizeExplorationTarget(true);
          chasingCommedEnemy = false;
        }
        doExploration();
      }

      if (lastAttackedEnemy != null) {
        if (!rc.canSenseRobot(lastAttackedEnemy.ID)) {
          lastAttackedEnemy = null;
        } else {
          lastAttackedEnemy = rc.senseRobot(lastAttackedEnemy.ID);
        }
      }

    }
  }

  private boolean attackEnemySoldier() throws GameActionException {
    MicroInfo best = null;
    for (Direction dir : Utils.directionsNine) {
      if (dir != Direction.CENTER && (needToRunHomeForSaving || !rc.canMove(dir))) continue;
//      MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//      if (dir != Direction.CENTER && Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS.length < 6 && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION)))) {
//        if (!newLoc.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(newLoc), newLoc.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(newLoc)))) {
//          continue;
//        }
//      }
      MicroInfo curr = new MicroInfo.MicroInfoSoldierOnly(this, dir);
      for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
        curr.update(enemy);
      }
      curr.finalizeInfo();
      if (best == null || curr.isBetterThan(best)) {
//        //Utils.print("best: " + best,  "curr: " + curr);
        best = curr;
      }
    }

    return best != null && best.execute();
  }

  private boolean attackEnemies() throws GameActionException {
    MicroInfo best = null;
//    Cache.PerTurn.cacheEnemyInfos();
    for (Direction dir : Utils.directionsNine) {
      if (dir != Direction.CENTER && (needToRunHomeForSaving || !rc.canMove(dir))) continue;
//      MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//      if (dir != Direction.CENTER && Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS.length < 6 && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION)))) {
//        if (!newLoc.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(newLoc), newLoc.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(newLoc)))) {
//          continue;
//        }
//      }
      Utils.cleanPrint();
//      int bc = Clock.getBytecodeNum();
//      //Utils.print("Bytecode before micro: " + bc);
      MicroInfo curr = new MicroInfo.MicroInfoGeneric(this, dir);
//      //Utils.print("Bytecode to create microinfo: " + (Clock.getBytecodeNum() - bc));
      switch (Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS.length) {
        case 10:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[9]);
        case 9:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[8]);
        case 8:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[7]);
        case 7:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[6]);
        case 6:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[5]);
        case 5:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[4]);
        case 4:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[3]);
        case 3:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[2]);
        case 2:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[1]);
        case 1:
          curr.update(Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS[0]);
          break;
        default:
          for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
//            int s = Clock.getBytecodeNum();
            curr.update(enemy);
//            //Utils.print("Bytecode for 1 update: " + (Clock.getBytecodeNum() - s));
          }
      }
//      //Utils.print("Bytecode to update 1 microinfo: " + (Clock.getBytecodeNum() - bc));
//      bc = Clock.getBytecodeNum();
      curr.finalizeInfo();
//      //Utils.print("Bytecode to finalize 1 microinfo: " + (Clock.getBytecodeNum() - bc));
//      Utils.submitPrint();
//      if (Cache.Permanent.ID == 10676 && Cache.PerTurn.ROUND_NUM == 70) {
//        ((MicroInfo.MicroInfoGeneric)curr).utilPrint();
//      }
      if (best == null || curr.isBetterThan(best)) {
//        //Utils.print("best: " + best,  "curr: " + curr);
        best = curr;
      }
    }

    return best != null && best.execute();
  }

  private boolean anyOffensiveEnemies() {
    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      if (enemy.type.damage > 0) return true;
    }
    return false;
  }

  /**
   * find and attack/deal with an enemy miner/builder
   *    SHOULD GUARANTEE >=1 miner/builder is in vision radius
   * @return true if attacked
   * @throws GameActionException if sensing or attacking fails
   */
  private boolean attackAndChaseEnemyMinerOrBuilder(boolean enemyMinerExists, boolean enemyBuilderExists) throws GameActionException {
    MapLocation enemyLocation = setNonOffensiveDroidToChaseAndChooseTarget(enemyMinerExists, enemyBuilderExists);
//    //Utils.print("RUNNING attackEnemyMinerOrBuilder()", "enemyLocation: " + enemyLocation);
    setIndicatorString("attackEnemyMiner/Builder", enemyLocation);

    return attackAtAndMoveTo(enemyLocation, enemyLocation, true);
  }

  /**
   * find and attack/deal with an enemy archon
   *    SHOULD GUARANTEE >=1 archon is in vision radius
   * @return true if attacked
   * @throws GameActionException if sensing or attacking fails
   */
  private boolean attackAndChaseEnemyArchon() throws GameActionException {
//    //Utils.print("RUNNING attackEnemyArchon()");
    // move towards and attack
    RobotInfo archon = findLowestHealthEnemyOfType(RobotType.ARCHON);
    MapLocation enemyLocation = archon.location;
    robotToChase = archon;

    setIndicatorString("attackEnemyArchon", enemyLocation);

    return attackAtAndMoveTo(enemyLocation, enemyLocation, true);
//
//    if (enemyLocation != null) {
//      if (rc.isMovementReady() && (!enemyLocation.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_1by1))) moveOptimalTowards(enemyLocation);
//      if (rc.canAttack(enemyLocation)) return attackTarget(enemyLocation);
//    }
//
//    return false;
  }

  /**
   * find and attack/deal with an enemy sage
   *    SHOULD GUARANTEE >=1 sage is in vision radius
   *    TODO: implement
   * @return true if attacked
   */
  private boolean attackEnemySage() {
//    //Utils.print("RUNNING attackEnemySage()");
    // if they attacked us, then let's attack them back for X (18) rounds.
    // Otherwise or after 18 rounds, add location to "banned" list and avoid getting close to it again or something
    // TODO: get health last round (based on health loss determine if sage attacked)
    MapLocation enemyLocation = null;
//    int minHealth = Integer.MAX_VALUE;

    setIndicatorString("attackEnemySage", enemyLocation);
    return false;
  }

  /**
   * Iterates over enemy robots and chooses the lowest health droid to attack
   * also sets the robot to chase
   *    can ONLY chase miners/builder
   *    will CHASE lowest health unit
   *    will ATTACK lowest health unit in action range
   * @return the coords of the enemy to attack
   */
  private MapLocation setNonOffensiveDroidToChaseAndChooseTarget(boolean enemyMinerExists, boolean enemyBuilderExists) {
    // possibly do score function of health and distance to enemy
    // TODO: ensure that the below priority system works
    // if not, revert to simple, choose miner > builder
    RobotType preferredType =
        enemyBuilderExists
            && (!enemyMinerExists
            || rc.getTeamLeadAmount(Cache.Permanent.OPPONENT_TEAM) > 1.3 * rc.getTeamLeadAmount(Cache.Permanent.OUR_TEAM))
            ? RobotType.BUILDER
            : RobotType.MINER;
    RobotInfo bestTarget = null;
    MapLocation enemyLocation = null;
    int minHealth = Integer.MAX_VALUE;
    int minDistance = Integer.MAX_VALUE;

    boolean canMove = rc.isMovementReady();

    // find least health enemy miner in action radius
    // the robotToChase is set to the miner with the least health irregardless of distance
    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      int candidateDistance = Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(enemy.location);
//        //System.out.println("enemy: " + enemy.location + " " + enemy.health + " " + candidateDistance + " minHealth: " + minHealth + " minDistance: " + minDistance);
      if (enemy.type == preferredType && (enemy.health < minHealth || (enemy.health == minHealth && candidateDistance < minDistance))) {
        bestTarget = enemy;
//        //System.out.println(bestToTarget);
        if (canMove || candidateDistance <= Cache.Permanent.ACTION_RADIUS_SQUARED) {
          minHealth = enemy.health;
          enemyLocation = enemy.location;
          minDistance = candidateDistance;
        }
      }
    }

    // shouldn't ever happen
    if (bestTarget == null) return null;

    robotToChase = bestTarget;
    return enemyLocation;
  }

  /**
   * attack a given location and also move
   *    depending on usePathing, either use moveOptimal or a direct move call
   * @param whereToAttack the location to attack
   * @param whereToMove the location to move to
   * @param usePathing whether to use optimal planning or move in direction
   * @return true if attacked
   * @throws GameActionException if attacking or moving fails
   */
  private boolean attackAtAndMoveTo(MapLocation whereToAttack, MapLocation whereToMove, boolean usePathing) throws GameActionException {

    boolean attacked = false;
    Direction dirToMove = (needToRunHomeForSaving || !rc.isMovementReady() || whereToMove.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_1by1))
        ? Direction.CENTER
        : (usePathing) ? getOptimalDirectionTowards(whereToMove) : Cache.PerTurn.CURRENT_LOCATION.directionTo(whereToMove);
    if (dirToMove == null) {
//      //System.out.printf("Can't move\n%s -> %s!\n", Cache.PerTurn.CURRENT_LOCATION, whereToMove);
      dirToMove = Direction.CENTER;
    }
    MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(dirToMove);
//    if (!Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION)))) {
//      if (!newLoc.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(newLoc), newLoc.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(newLoc)))) {
//        dirToMove = Direction.CENTER;
//      }
//    }
    // only try to attack early if rubble is better
//    if (rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) < rc.senseRubble(newLoc)) attacked |= attackTarget(whereToAttack);
    // TODO: why is above making things worse?????

    // only attack early if moving first screws us over
    if (whereToAttack != null && !whereToAttack.isWithinDistanceSquared(newLoc, Cache.Permanent.ACTION_RADIUS_SQUARED)) attacked |= attackTarget(whereToAttack);
    // only move if not CENTER and target isn't already within 1by1 from self
//    if (dirToMove != Direction.CENTER && rc.isMovementReady() && (!whereToMove.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_1by1))) {
      move(dirToMove);
//    }
    // retry attack if incomplete
    attacked |= attackTarget(whereToAttack);
    return attacked;

//    boolean attacked = false;
//    if (whereToAttack != null) {
//      attacked |= rc.canAttack(whereToAttack) && attackTarget(whereToAttack);
//    }
//    if (whereToMove != null) {
//      // might want to move out if bestLocation score is low (after attacking tho)
//      if (rc.isMovementReady() && !whereToMove.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_2by2)) {
//        if (usePathing) moveOptimalTowards(whereToMove);
//        else move(Cache.PerTurn.CURRENT_LOCATION.directionTo(whereToMove));
//      }
//    }
//    if (whereToAttack != null) {
//      attacked |= rc.canAttack(whereToAttack) && attackTarget(whereToAttack);
//    }
//
//    return attacked;
  }

  /**
   * checks if the archon has been saved
   * @return true if saving is done
   * @throws GameActionException if checking fails
   */
  private boolean checkDoneSaving() throws GameActionException {
    if (!archonToSave.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED)) return false;
//    RobotInfo archon = rc.senseRobotAtLocation(archonToSave);
//    if (archon == null) return true;
    return !offensiveEnemiesNearby();
  }

  /**
   * stop saving the archon
   * update internals and broadcast message
   * @throws GameActionException if updating or messaging fails
   */
  private void finishSaving() throws GameActionException {
    if (archonToSave == null) return;
    communicator.enqueueMessage(new ArchonSavedMessage(archonToSave));
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
    StartRaidMessage message = new StartRaidMessage(raidTarget);
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
    EndFightMessage message = new EndFightMessage(raidTarget);
    communicator.enqueueMessage(message);
  }

  /**
   * move towards raid target (TODO: add pathing)
   * @return if reached target
   * @throws GameActionException if moving fails
   */
  private boolean moveForRaid() throws GameActionException {
//    //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, raidTarget, 0,0,255);
//    return moveInDirLoose(Cache.PerTurn.CURRENT_LOCATION.directionTo(raidTarget))
    return moveOptimalTowards(raidTarget)
        && Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(raidTarget) <= Cache.Permanent.VISION_RADIUS_SQUARED;
  }

  /**
   * send message to fellow soldiers to join my fight
   */
  void broadcastJoinOrEndMyFight(MicroInfo<?> executedMicro) {
//    if (executedMicro.chosenEnemyToAttack != null && executedMicro.chosenEnemyToAttack.health <= Cache.Permanent.ROBOT_TYPE.damage) {
//      communicator.enqueueMessage(new EndFightMessage(executedMicro.chosenEnemyToAttack.location));
//      return;
//    }
//    MapLocation enemy = offensiveEnemyCentroid();
//    if (enemy == null) return;
//    MapLocation friends = friendlySoldierCentroid();
//    if (friends == null) friends = Cache.PerTurn.CURRENT_LOCATION;
//    MapLocation whereToJoin = enemy;//new MapLocation((enemy.x+friends.x) / 2, (enemy.y+friends.y) / 2);
//    if (fightToJoin == null || !fightToJoin.isWithinDistanceSquared(whereToJoin, Cache.Permanent.VISION_RADIUS_SQUARED*2)) {
//      communicator.enqueueMessage(new JoinTheFightMessage(whereToJoin));
//    }
  }

  /**
   * attack the specified target
   * @param target the location to try attacking
   * @return true if attacked the given target
   */
  boolean attackTarget(MapLocation target) throws GameActionException {
    if (target == null || !rc.canAttack(target)) return false;
    rc.attack(target);
    return true;
  }

  /**
   * iterate over all of vision and set all booleans about soldier existence
   * @return true if there are any enemies
   * @throws GameActionException if any sensing fails
   */
  private boolean processEnemiesInVision() throws GameActionException {
    // get all robots in vision
    enemySoldierExistsInVision = false;
    enemyMinerExistsInVision = false;
    enemyBuilderExistsInVision = false;
    enemyArchonExistsInVision = false;
    enemySageExistsInVision = false;

    for (RobotInfo robot : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
      switch(robot.type) {
        case SOLDIER:
          enemySoldierExistsInVision = true;
          break;
        case MINER:
          enemyMinerExistsInVision = true;
          break;
        case BUILDER:
          enemyBuilderExistsInVision = true;
          break;
        case ARCHON:
          enemyArchonExistsInVision = true;
          break;
        case SAGE:
          enemySageExistsInVision = true;
          break;
      }
    }

    return enemySoldierExistsInVision || enemyMinerExistsInVision || enemyBuilderExistsInVision || enemyArchonExistsInVision || enemySageExistsInVision;
  }

  /**
   * iterate over all of action and set all booleans about soldier existence
   * @return true if there are any enemies
   * @throws GameActionException if any sensing fails
   */
  private boolean processEnemiesInAction() throws GameActionException {
    enemySoldierExistsInAction = false;
    enemyMinerExistsInAction = false;
    enemyBuilderExistsInAction = false;
    enemyArchonExistsInAction = false;
    enemySageExistsInAction = false;

    for (RobotInfo robot : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
      switch(robot.type) {
        case SOLDIER:
          enemySoldierExistsInAction = true;
          break;
        case MINER:
          enemyMinerExistsInAction = true;
          break;
        case BUILDER:
          enemyBuilderExistsInAction = true;
          break;
        case ARCHON:
          enemyArchonExistsInAction = true;
          break;
        case SAGE:
          enemySageExistsInAction = true;
          break;
      }
    }
    return enemySoldierExistsInAction || enemyMinerExistsInAction || enemyBuilderExistsInAction || enemyArchonExistsInAction || enemySageExistsInAction;
  }

  public void attackPriority(boolean enemySoldierExists, boolean enemyMinerExists,
                             boolean enemyBuilderExists, boolean enemyArchonExists,
                             boolean enemySageExists) throws GameActionException {
    if (enemySoldierExists) attackEnemySoldier();
    else if (enemyMinerExists || enemyBuilderExists) attackAndChaseEnemyMinerOrBuilder(enemyMinerExists, enemyBuilderExists);
    else if (enemyArchonExists) attackAndChaseEnemyArchon();
    else if (enemySageExists) attackEnemySage();
    else if (!needToRunHomeForSaving) {
//      if (lastSoldierAttack != null) {
//        if (lastSoldierTradeScore > 0) {
//          moveOptimalTowards(lastSoldierAttack);
//        } else {
//          moveOptimalAway(lastSoldierAttack);
//        }
////        if (!rc.canSenseLocation(lastSoldierAttack) || rc.senseRobotAtLocation(lastSoldierAttack) == null) {
//        lastSoldierAttack = null;
////        }
//      }
      if (robotToChase != null) {
//        //Utils.print("robotToChase: " + robotToChase);
        attackAtAndMoveTo(robotToChase.location, robotToChase.location, true);
        if (robotToChase.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_2by2)) {
          robotToChase = null;
        }
      }

      if (robotToChase == null) {
        if (closestCommedEnemy == null && chasingCommedEnemy) {
          randomizeExplorationTarget(true);
          chasingCommedEnemy = false;
        }
        doExploration();
      }
      // if no one is in vision, we 1) go to the cached location if exists or 2) the random target location
      // cached location is set to null if we go there and no enemy is found in vision radius
//      setIndicatorString("no enemy", null);
    }
  }

  @Override
  public void ackMessage(Message message) throws GameActionException {
    super.ackMessage(message);
    if (message instanceof StartRaidMessage) {
      ackStartRaidMessage((StartRaidMessage) message);
    } else if (message instanceof SaveMeMessage) {
      ackSaveMeMessage((SaveMeMessage) message);
    } else if (message instanceof ArchonSavedMessage) {
      ackArchonSavedMessage((ArchonSavedMessage) message);
    } else if (message instanceof JoinTheFightMessage) {
      ackJoinFightMessage((JoinTheFightMessage) message);
    } else if (message instanceof EndFightMessage) {
      ackEndFightMessage((EndFightMessage) message);
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

//  /**
//   * receive a message that a raid is over
//   * @param message raid ending message with a specific raid target
//   * @throws GameActionException if ack fails
//   */
//  private void ackEndRaidMessage(EndRaidMessage message) throws GameActionException {
//    // TODO: if not ready for raid (maybe not in center yet or something), ignore
//    if (raidTarget != null && raidTarget.equals(message.location)) {
//      raidTarget = null;
//      raidValidated = false;
////      //System.out.println("Got end raid on " + message.location + " - from rnd: " + message.header.cyclicRoundNum + "/" + Message.Header.toCyclicRound(rc.getRoundNum()));
//    }
//    if (message.location.equals(myPotentialTarget)) {
//      canStartRaid = false;
//    }
//  }

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
    if (archonToSave != null && archonToSave.isWithinDistanceSquared(message.location, RobotType.ARCHON.visionRadiusSquared)) { // not already saving or 2/5 chance to switch
      archonToSave = null;
    }
  }

  /**
   * acknowledge an ongoing fight and decide if we should join
   * @param message the message to join a fight
   */
  private void ackJoinFightMessage(JoinTheFightMessage message) {
    if (fightToJoin == null || message.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, fightToJoin.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION)-1)) {
      fightToJoin = message.location;
    }
  }

  /**
   * receive a message that a fight is over
   * @param message fight ending message with a specific fight location
   * @throws GameActionException if ack fails
   */
  private void ackEndFightMessage(EndFightMessage message) throws GameActionException {
//    if (fightToJoin != null) {
//      if (message.location.isWithinDistanceSquared(fightToJoin, Cache.Permanent.VISION_RADIUS_SQUARED*2)) {
        fightToJoin = null;
//        if (message.location.isWithinDistanceSquared(explorationTarget, Cache.Permanent.VISION_RADIUS_SQUARED*2)) {
          randomizeExplorationTarget(true);
//        }
//      }
//    }
  }


  private void setIndicatorString(String custom, MapLocation enemyLocation) {
    //rc.setIndicatorString(custom + "-" + enemyLocation + " aCD:" + rc.getActionCooldownTurns() + " mCD:" + rc.getMovementCooldownTurns());
//    if (robotToChase != null) //rc.setIndicatorString("Soldier " + custom + " - " + enemyLocation + " robotToChase: " + robotToChase.location);
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

}
