package bfspathing.robots.droids;

import battlecode.common.*;
import bfspathing.communications.messages.*;
import bfspathing.utils.Cache;
import bfspathing.utils.Utils;

public class Soldier extends Droid {
  private final int HALF_RANGE_TO_CHASE_FROM;

  MapLocation archonToSave;

  MapLocation fightToJoin;

  private boolean chasingCommedEnemy;

  public Soldier(RobotController rc) throws GameActionException {
    super(rc);
    HALF_RANGE_TO_CHASE_FROM = (Cache.Permanent.ACTION_RADIUS_SQUARED+1) / 2;
  }

  @Override
  protected void runTurn() throws GameActionException {
    if (closestCommedEnemy != null) {
      explorationTarget = closestCommedEnemy;
      exploringRandomly = false;
      chasingCommedEnemy = true;

      if (checkNeedToStayOnSafeSide()) {
        MapLocation friendly = communicator.archonInfo.getNearestFriendlyArchon(explorationTarget);
        MapLocation enemy = communicator.archonInfo.getNearestEnemyArchon(explorationTarget);
        Direction backHome = enemy.directionTo(friendly);
        int tries = 20;
        while (tries-- > 0 && friendly.distanceSquaredTo(explorationTarget) > enemy.distanceSquaredTo(explorationTarget)) {
          explorationTarget = explorationTarget.add(backHome);
        }
      }
    }

    if (archonToSave != null
      && !isMovementDisabled
      && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(archonToSave, Cache.Permanent.VISION_RADIUS_SQUARED)
      && (!offensiveEnemiesNearby() || !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), RobotType.ARCHON.actionRadiusSquared))) {
//      //Printer.cleanPrint();
//      //Printer.print("archonToSave: " + archonToSave);
//      //Printer.submitPrint();
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


  RobotInfo lastAttackedEnemy;
  void runNew() throws GameActionException {
    if (anyOffensiveEnemies()) {
      attackEnemies();
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
    } else if (!isMovementDisabled) {
      if (lastAttackedEnemy != null && lastAttackedEnemy.type.damage <= 0) {
//        //Printer.print("robotToChase: " + robotToChase);
        attackAtAndMoveTo(lastAttackedEnemy.location, lastAttackedEnemy.location, true);
        if (lastAttackedEnemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, HALF_RANGE_TO_CHASE_FROM)) {
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

  protected boolean attackEnemies() throws GameActionException {
    MicroInfo best = null;
//    Cache.PerTurn.cacheEnemyInfos();
//    //Printer.cleanPrint();
//    //Printer.print("isMovementDisabled: " + isMovementDisabled);
//    //Printer.print("needToRunHomeForSaving: " + needToRunHomeForSaving,"needToRunHomeForSuicide: " + needToRunHomeForSuicide);
//    //Printer.print("movementCooldown: " + rc.getMovementCooldownTurns(), "actionCooldown: " + rc.getActionCooldownTurns());
//    //Printer.submitPrint();
    for (Direction dir : Utils.directionsNine) {
      if (dir != Direction.CENTER && (isMovementDisabled || !rc.canMove(dir))) continue;
//      MapLocation newLoc = Cache.PerTurn.CURRENT_LOCATION.add(dir);
//      if (dir != Direction.CENTER && Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS.length < 6 && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(Cache.PerTurn.CURRENT_LOCATION), Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(Cache.PerTurn.CURRENT_LOCATION)))) {
//        if (!newLoc.isWithinDistanceSquared(communicator.archonInfo.getNearestFriendlyArchon(newLoc), newLoc.distanceSquaredTo(communicator.archonInfo.getNearestEnemyArchon(newLoc)))) {
//          continue;
//        }
//      }
//      //Printer.cleanPrint();
      MicroInfo curr = new MicroInfo.MicroInfoGeneric(this, dir);
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
//            //Printer.print("Bytecode for 1 update: " + (Clock.getBytecodeNum() - s));
          }
      }
      curr.finalizeInfo();
//      ((MicroInfo.MicroInfoGeneric)curr).utilPrint();
      if (best == null || curr.isBetterThan(best)) {
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

  private boolean cachedCheckNeedToStay;
  private int cacheStateOnLastNeedToStayCalc = -1;
  public boolean checkNeedToStayOnSafeSide() {
    if (Cache.PerTurn.cacheState == cacheStateOnLastNeedToStayCalc) return cachedCheckNeedToStay;
    cacheStateOnLastNeedToStayCalc = Cache.PerTurn.cacheState;
    int numFriendlyOffense = 0;
    int numEnemyOffense = 0;
    for (RobotInfo robot : Cache.PerTurn.ALL_NEARBY_ROBOTS) {
      if (robot.type.damage > 0) {
        if (robot.team == Cache.Permanent.OUR_TEAM) {
          numFriendlyOffense++;
        } else {
          numEnemyOffense++;
        }
      }
    }
    return cachedCheckNeedToStay = (numFriendlyOffense < 4 || numEnemyOffense * 1.5 >= numFriendlyOffense);
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
    Direction dirToMove = (isMovementDisabled || !rc.isMovementReady() || whereToMove.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, HALF_RANGE_TO_CHASE_FROM))
        ? Direction.CENTER
        : (usePathing) ? getOptimalDirectionTowards(whereToMove) : Cache.PerTurn.CURRENT_LOCATION.directionTo(whereToMove);
    if (dirToMove == null) {
//      //System.out.printf("Can't move\n%s -> %s!\n", Cache.PerTurn.CURRENT_LOCATION, whereToMove);
      dirToMove = Direction.CENTER;
    }
    int rubbleHere = rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION);
    if (rubbleHere > 20) {
      Direction leastRubble = getLeastRubbleMoveableDirAroundDir(dirToMove);
      if (leastRubble != null && rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION.add(leastRubble)) < rubbleHere) {
        dirToMove = leastRubble;
      }
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
    if (whereToAttack != null
        && !whereToAttack.isWithinDistanceSquared(newLoc, Cache.Permanent.ACTION_RADIUS_SQUARED)
        && rubbleHere <= rc.senseRubble(newLoc)) {
      attacked = attackTarget(whereToAttack);
    }
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
//      if (rc.isMovementReady() && !whereToMove.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, HALF_RANGE_TO_CHASE_FROM)) {
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
   * send message to fellow soldiers to join my fight
   */
  void broadcastJoinOrEndMyFight(MicroInfo<?,?> executedMicro) {
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

  @Override
  public void ackMessage(Message message) throws GameActionException {
    super.ackMessage(message);
    if (message instanceof SaveMeMessage) {
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

  /**
   * attack a nearby enemy (kinda random based on order from sense function)
   * @return true if attacked
   * @throws GameActionException if attacking fails
   */
  private boolean attackNearby() throws GameActionException {
    if (!rc.isActionReady()) return false;

    for (RobotInfo enemy : rc.senseNearbyRobots(Cache.Permanent.ACTION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
      MapLocation toAttack = enemy.location;
      if (attackTarget(toAttack)) return true;
    }
    return false;
  }

}
