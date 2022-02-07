package firstbot.robots.droids;

import battlecode.common.*;
import firstbot.utils.Cache;
import firstbot.utils.Global;
import firstbot.utils.Printer;
import firstbot.utils.Utils;

public abstract class MicroInfo<T extends MicroInfo<T, U>, U extends Soldier> {

  public final U robotDoingMicro;
  public final MapLocation location;
  public final Direction direction;
  public final boolean isMovement;

  public static int getPriority(RobotType type) {
    switch (type) {
      case SAGE:
        return 9;
      case SOLDIER:
        return 8;
      case WATCHTOWER:
        return 7;
      case MINER:
        return 6;
      case ARCHON:
        return 5;
      case BUILDER:
        return 4;
      case LABORATORY:
        return 3;
    }
    return 0;
  }
  
  protected MicroInfo(U robotDoingMicro, Direction direction) {
    this.robotDoingMicro = robotDoingMicro;
    this.direction = direction;
    this.location = Cache.PerTurn.CURRENT_LOCATION.add(direction);
    isMovement = direction != Direction.CENTER;
  }

  /**
   * update the microinfo option with the provided enemy within current vision radius
   *    info should decide if this enemy is relevant to it or not
   * @param nextEnemy the enemy to register with the microinfo
   * @throws GameActionException if updating fails
   */
  public abstract void update(RobotInfo nextEnemy) throws GameActionException;

  /**
   * finalize any information that was collected from the updates
   */
  public abstract void finalizeInfo() throws GameActionException;

  /**
   * compare this microinfo to another
   * @param other the other microinfo option
   * @return true if this is a better option
   */
  public abstract boolean isBetterThan(T other) throws GameActionException;

  /**
   * execute this microinfo
   * @return true if successful (attacked)
   */
  public abstract boolean execute() throws GameActionException;

  public static class MicroInfoSoldiers extends MicroInfo<MicroInfoSoldiers, Soldier> {
    final int rubble;
    final int distanceToFriendlyArchon;
    final int distanceToEnemyArchon;
    final boolean tooCloseToEnemy;

    int totalOffensiveEnemies;
    int numOffendingEnemies;
    int numOffendingSages;
    double enemyDPS;
    double friendlyDPS;

    RobotInfo closestOffensive;
    int distToClosestOffensive = 9999;
    boolean shouldMoveInAnyways;

    RobotInfo bestTarget;
    int distToTarget = 9999;
    boolean hasTarget;
    int targetPriority;
    int rubbleOfTarget;


    boolean isAttackAndExit;
    boolean shouldAttackAndExit;

    boolean isMovingInToAttack;
    boolean shouldMoveInToAttack;

    boolean isAttackWithoutFlee;
    boolean shouldAttackWithoutFlee;

    boolean isBadIdea;

    public MicroInfoSoldiers(Soldier soldier, Direction direction) throws GameActionException {
      super(soldier, direction);
      this.rubble = Global.rc.senseRubble(location);
      this.distanceToEnemyArchon = this.distanceToFriendlyArchon = 0;
//      this.distanceToFriendlyArchon = Global.communicator.archonInfo.getNearestFriendlyArchon(location).distanceSquaredTo(location);
//      this.distanceToEnemyArchon = Global.communicator.archonInfo.getNearestEnemyArchon(location).distanceSquaredTo(location);
      this.tooCloseToEnemy = this.distanceToEnemyArchon <= this.distanceToFriendlyArchon;
//      Printer.print("Moving to " + location, "distToFriendArchon " + distanceToFriendlyArchon, "distToEnemyArchon " + distanceToEnemyArchon, "tooClose: " + tooCloseToEnemy);
    }

    @Override
    public void update(RobotInfo nextEnemy) throws GameActionException {
      if (nextEnemy.type.damage > 0) {
        totalOffensiveEnemies++;
        if (nextEnemy.location.isWithinDistanceSquared(location, distToClosestOffensive - 1)) {
          closestOffensive = nextEnemy;
          distToClosestOffensive = nextEnemy.location.distanceSquaredTo(location);
        }
        if (location.isWithinDistanceSquared(nextEnemy.location, nextEnemy.type.actionRadiusSquared)) {
          numOffendingEnemies++;
          if (nextEnemy.type == RobotType.SAGE) {
            numOffendingSages++;
          }
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(nextEnemy.type.actionCooldown, Global.rc.senseRubble(nextEnemy.location));
          enemyDPS += (nextEnemy.type.damage / turnsTillNextCooldown);
        }
      } else { // TODO remove and make it work for any enemy
        return;
      }
      if (!nextEnemy.location.isWithinDistanceSquared(location, Cache.Permanent.ACTION_RADIUS_SQUARED)
              && !nextEnemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        return;
      }
      if (bestTarget == null) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      }
      if (getPriority(nextEnemy.type) > getPriority(bestTarget.type)) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      } else if (getPriority(nextEnemy.type) < getPriority(bestTarget.type)) {
        return;
      }
      if (nextEnemy.health < bestTarget.health) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      } else if (nextEnemy.health > bestTarget.health) {
        return;
      }
      if (nextEnemy.location.isWithinDistanceSquared(location, distToTarget - 1)) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      }
    }

    @Override
    public void finalizeInfo() throws GameActionException {
      hasTarget = Global.rc.isActionReady() && this.bestTarget != null;
      if (hasTarget) {
        targetPriority = getPriority(bestTarget.type);
        rubbleOfTarget = Global.rc.senseRubble(bestTarget.location);

        // one enemy at edge of action and no others, we are attacking then moving away
        isAttackAndExit = !location.isWithinDistanceSquared(bestTarget.location, bestTarget.type.actionRadiusSquared);
        // only do so if there was just that one enemy
        shouldAttackAndExit = (isAttackAndExit
                && rubble <= Global.rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) && ( // TODO: numOffending=0 doesn't account for going from 2 -> 1 enemies
                bestTarget.type.damage <= 0
                        || (numOffendingEnemies == 0)))
                || (numOffendingEnemies <= 1 && bestTarget.health <= Cache.Permanent.ROBOT_TYPE.damage)
        ;
        if (shouldAttackAndExit) {
          isBadIdea = false;
          return;
        } else if (isAttackAndExit) isBadIdea = true;


        // no enemies in current action, but one enemy in vision which we are moving towards
        isMovingInToAttack = !bestTarget.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED);
        // only move in if it's a 1v1 and rubble is less than the enemy's
        // TODO: potentially replace numOffensive=1 with something based on enemyDPS (which is based on rubble of enemies)
        shouldMoveInToAttack = isMovingInToAttack
//          && rubble <= rubbleOfTarget
                && rubble <= (bestTarget.type == RobotType.SAGE ? Math.max(15, rubbleOfTarget * 2) : rubbleOfTarget)
                && (
                bestTarget.type.damage <= 0
                        || (numOffendingEnemies <= 1)
                        || (numOffendingEnemies <= 2 && bestTarget.health <= Cache.Permanent.ROBOT_TYPE.damage)
                        || (bestTarget.type == RobotType.SAGE && numOffendingSages >= numOffendingEnemies - 1)
        );
        if (shouldMoveInToAttack) {
          isBadIdea = false;
          return;
        } else if (isMovingInToAttack) isBadIdea = true;


        // have a target but unable to fully escape
        isAttackWithoutFlee = numOffendingEnemies > 0;
        // only do so if we are not moving closer to the enemy
        shouldAttackWithoutFlee = isAttackWithoutFlee && (
                bestTarget.type.damage <= 0
                        || (rubble <= rubbleOfTarget)
        );
        if (shouldAttackWithoutFlee) {
          isBadIdea = false;
          return;
        } else if (isAttackWithoutFlee) isBadIdea = true;

      } else if (closestOffensive != null) { // no target but we have an offensive enemy in sight
        // approach the enemy (vision -> vision)
        shouldMoveInAnyways = totalOffensiveEnemies <= 1
                && rubble <= Global.rc.senseRubble(closestOffensive.location)
                && Global.rc.getActionCooldownTurns() < 20
                && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(
                closestOffensive.location,
                distToClosestOffensive);
      } else {
        isBadIdea = true;
      }
    }

    @Override
    public boolean isBetterThan(MicroInfoSoldiers other) throws GameActionException {
      if (this.isBadIdea != other.isBadIdea) return other.isBadIdea;
      if (this.isBadIdea) { // both are bad ideas
        // TODO: technically this accounts for 1v1 action->action but it may prefer going in if rubble is better closer
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.closestOffensive != null && other.closestOffensive != null) {
          if (this.distToClosestOffensive != other.distToClosestOffensive)
            return this.distToClosestOffensive > other.distToClosestOffensive;
        }
        // TODO: account for non-offensive units
        return this.direction != Direction.CENTER;
      }

      // check for attacking and moving out
      if (this.shouldAttackAndExit != other.shouldAttackAndExit) return this.shouldAttackAndExit;
      if (this.shouldAttackAndExit) { // both can hit an enemy then escape from all of them
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we can hit and then exit from other enemies attacks

      // check for moving in to attack a boi
      if (this.shouldMoveInToAttack != other.shouldMoveInToAttack) return this.shouldMoveInToAttack;
      if (this.shouldMoveInToAttack) { // both should move in to attack, choose better option
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, neither can move in for an attack

      // check for attacking but not fleeing
      if (this.shouldAttackWithoutFlee != other.shouldAttackWithoutFlee) return this.shouldAttackWithoutFlee;
      if (this.shouldAttackWithoutFlee) { // both are attacking and staying within action, prefer fewer enemies
        if (this.numOffendingEnemies != other.numOffendingEnemies)
          return this.numOffendingEnemies < other.numOffendingEnemies;
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we are attacking and failing to flee from

      // check for approaching an enemy but not attacking
      if (this.shouldMoveInAnyways != other.shouldMoveInAnyways) return this.shouldMoveInAnyways;
      if (this.shouldMoveInAnyways) { // both are approaching a single enemy, choose the better option
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToClosestOffensive != other.distToClosestOffensive)
          return this.distToClosestOffensive < other.distToClosestOffensive;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we are approaching or moving in to attack

      if (this.rubble != other.rubble) return this.rubble < other.rubble;
      if (this.closestOffensive != null && other.closestOffensive != null) {
        if (this.distToClosestOffensive != other.distToClosestOffensive)
          return this.distToClosestOffensive > other.distToClosestOffensive;
      }
      return this.direction != Direction.CENTER;
    }

    public boolean execute() throws GameActionException {
      boolean attacked = false;
      this.robotDoingMicro.lastAttackedEnemy = this.bestTarget;
      if (this.hasTarget && this.bestTarget.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED) && this.rubble <= Global.rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION)) {
        attacked = this.robotDoingMicro.attackTarget(bestTarget.location);
      }
      this.robotDoingMicro.move(Cache.PerTurn.CURRENT_LOCATION.directionTo(location));
      if (this.hasTarget && !attacked) {
        attacked = this.robotDoingMicro.attackTarget(bestTarget.location);
      }
      return attacked;
    }

    public void utilPrint() {
      Printer.cleanPrint();
      Printer.print("EVALUATE MOVING IN DIR: " + direction, "Moving: " + Cache.PerTurn.CURRENT_LOCATION + " --> " + location, "isMovement: " + isMovement);
      Printer.print("rubble: " + rubble, "distanceToFriendlyArchon: " + distanceToFriendlyArchon, "distanceToEnemyArchon: " + distanceToEnemyArchon, "tooCloseToEnemy: " + tooCloseToEnemy);
      Printer.print("totalOffensiveEnemies: " + totalOffensiveEnemies, "numOffendingEnemies: " + numOffendingEnemies);
      Printer.print("closestOffensive: " + closestOffensive, "shouldMoveInAnyways: " + shouldMoveInAnyways);
      Printer.print("bestTarget: " + bestTarget, "hasTarget: " + hasTarget, "rubbleOfTarget: " + rubbleOfTarget);
      Printer.print("canAttackAndExit: " + isAttackAndExit, "shouldAttackAndExit: " + shouldAttackAndExit);
      Printer.print("isMovingInToAttack: " + isMovingInToAttack, "shouldMoveInToAttack: " + shouldMoveInToAttack);
      Printer.print("isBadIdea: " + isBadIdea);
      Printer.submitPrint();
    }

  }

  public static class MicroInfoSages extends MicroInfo<MicroInfoSages, Sage> {
    private static final int MAX_RUBBLE_TO_GO_IN = 5;

    final int rubble;
    final int distanceToFriendlyArchon;
    final int distanceToEnemyArchon;
    final boolean tooCloseToEnemy;

    int totalOffensiveEnemies;
    int numOffendingEnemies;
    double enemyDPS;
    double friendlyDPS;

    RobotInfo closestOffensive;
    int distToClosestOffensive = 9999;
    boolean shouldMoveInAnyways;

    RobotInfo bestTarget;
    int distToTarget = 9999;
    boolean hasTarget;
    int targetPriority;
    int rubbleOfTarget;


    boolean isAttackAndExit;
    boolean shouldAttackAndExit;

    boolean isMovingInToAttack;
    boolean shouldMoveInToAttack;

    boolean isAttackWithoutFlee;
    boolean shouldAttackWithoutFlee;

    boolean isBadIdea;

    public MicroInfoSages(Sage sage, Direction direction) throws GameActionException {
      super(sage, direction);
      this.rubble = Global.rc.senseRubble(location);
      this.distanceToEnemyArchon = this.distanceToFriendlyArchon = 0;
//      this.distanceToFriendlyArchon = Global.communicator.archonInfo.getNearestFriendlyArchon(location).distanceSquaredTo(location);
//      this.distanceToEnemyArchon = Global.communicator.archonInfo.getNearestEnemyArchon(location).distanceSquaredTo(location);
      this.tooCloseToEnemy = this.distanceToEnemyArchon <= this.distanceToFriendlyArchon;
//      Printer.print("Moving to " + location, "distToFriendArchon " + distanceToFriendlyArchon, "distToEnemyArchon " + distanceToEnemyArchon, "tooClose: " + tooCloseToEnemy);
    }

    @Override
    public void update(RobotInfo nextEnemy) throws GameActionException {
      if (nextEnemy.type.damage > 0) {
        totalOffensiveEnemies++;
        if (nextEnemy.location.isWithinDistanceSquared(location, distToClosestOffensive-1)) {
          closestOffensive = nextEnemy;
          distToClosestOffensive = nextEnemy.location.distanceSquaredTo(location);
        }
        if (location.isWithinDistanceSquared(nextEnemy.location, nextEnemy.type.actionRadiusSquared)) {
          numOffendingEnemies++;
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(nextEnemy.type.actionCooldown, Global.rc.senseRubble(nextEnemy.location));
          enemyDPS += (nextEnemy.type.damage / turnsTillNextCooldown);
        }
      } else { // TODO remove and make it work for any enemy
        return;
      }
      if (!nextEnemy.location.isWithinDistanceSquared(location, Cache.Permanent.ACTION_RADIUS_SQUARED)
              && !nextEnemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        return;
      }
      if (bestTarget == null) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      }
      if (getPriority(nextEnemy.type) > getPriority(bestTarget.type)) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      } else if (getPriority(nextEnemy.type) < getPriority(bestTarget.type)) {
        return;
      }
      if (nextEnemy.health != bestTarget.health) {
        if (bestTarget.health <= Cache.Permanent.ROBOT_TYPE.damage
                ? (nextEnemy.health <= Cache.Permanent.ROBOT_TYPE.damage
                    && nextEnemy.health > bestTarget.health)
                : nextEnemy.health < bestTarget.health) {
          bestTarget = nextEnemy;
          distToTarget = nextEnemy.location.distanceSquaredTo(location);
        }
        return;
      }
      if (nextEnemy.location.isWithinDistanceSquared(location, distToTarget -1)) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      }
    }

    @Override
    public void finalizeInfo() throws GameActionException {
      hasTarget = Global.rc.isActionReady() && this.bestTarget != null;
      if (hasTarget) {
        targetPriority = getPriority(bestTarget.type);
        rubbleOfTarget = Global.rc.senseRubble(bestTarget.location);

        // one enemy at edge of action and no others, we are attacking then moving away
        isAttackAndExit = !location.isWithinDistanceSquared(bestTarget.location, Cache.Permanent.ACTION_RADIUS_SQUARED);
        // only do so if there was just that one enemy
        shouldAttackAndExit = (isAttackAndExit
                && rubble <= Global.rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) && ( // TODO: numOffending=0 doesn't account for going from 2 -> 1 enemies
                bestTarget.type.damage <= 0
                        || (numOffendingEnemies == 0)))
                || (numOffendingEnemies <= 1 && bestTarget.health <= Cache.Permanent.ROBOT_TYPE.damage)
        ;
        if (shouldAttackAndExit) {
          isBadIdea = false;
          return;
        }
        else if (isAttackAndExit) isBadIdea = true;


        // no enemies in current action, but one enemy in vision which we are moving towards
        isMovingInToAttack = !bestTarget.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED);
        // only move in if it's a 1v1 and rubble is less than the enemy's
        // TODO: potentially replace numOffensive=1 with something based on enemyDPS (which is based on rubble of enemies)
        shouldMoveInToAttack = isMovingInToAttack && rubble <= Math.min(MAX_RUBBLE_TO_GO_IN, rubbleOfTarget) && (
            bestTarget.type.damage <= 0
            || (numOffendingEnemies <= 1)
            || (numOffendingEnemies <= 2 && bestTarget.health <= Cache.Permanent.ROBOT_TYPE.damage)
        );
        if (shouldMoveInToAttack) {
          isBadIdea = false;
          return;
        }
        else if (isMovingInToAttack) isBadIdea = true;


        // have a target but unable to fully escape
        isAttackWithoutFlee = numOffendingEnemies > 0;
        // only do so if we are not moving closer to the enemy
        shouldAttackWithoutFlee = isAttackWithoutFlee && (
                bestTarget.type.damage <= 0
                        || (rubble <= rubbleOfTarget)
        );
        if (shouldAttackWithoutFlee) {
          isBadIdea = false;
          return;
        }
        else if (isAttackWithoutFlee) isBadIdea = true;

      } else if (closestOffensive != null) { // no target but we have an offensive enemy in sight
        // approach the enemy (vision -> vision)
        shouldMoveInAnyways = totalOffensiveEnemies <= 1
                && rubble <= Math.min(MAX_RUBBLE_TO_GO_IN, Global.rc.senseRubble(closestOffensive.location))
                && Global.rc.getActionCooldownTurns() < 20
                && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(
                        closestOffensive.location,
                        Cache.Permanent.ACTION_RADIUS_SQUARED)
                && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(
                        closestOffensive.location,
                        distToClosestOffensive);
      } else {
        isBadIdea = true;
      }
    }

    @Override
    public boolean isBetterThan(MicroInfoSages other) throws GameActionException {
      if (this.isBadIdea != other.isBadIdea) return other.isBadIdea;
      if (this.isBadIdea) { // both are bad ideas
        // TODO: technically this accounts for 1v1 action->action but it may prefer going in if rubble is better closer
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.closestOffensive != null && other.closestOffensive != null) {
          if (this.distToClosestOffensive != other.distToClosestOffensive) return this.distToClosestOffensive > other.distToClosestOffensive;
        }
        // TODO: account for non-offensive units
        return this.direction != Direction.CENTER;
      }

      // check for attacking and moving out
      if (this.shouldAttackAndExit != other.shouldAttackAndExit) return this.shouldAttackAndExit;
      if (this.shouldAttackAndExit) { // both can hit an enemy then escape from all of them
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we can hit and then exit from other enemies attacks

      // check for moving in to attack a boi
      if (this.shouldMoveInToAttack != other.shouldMoveInToAttack) return this.shouldMoveInToAttack;
      if (this.shouldMoveInToAttack) { // both should move in to attack, choose better option
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, neither can move in for an attack

      // check for attacking but not fleeing
      if (this.shouldAttackWithoutFlee != other.shouldAttackWithoutFlee) return this.shouldAttackWithoutFlee;
      if (this.shouldAttackWithoutFlee) { // both are attacking and staying within action, prefer fewer enemies
        if (this.numOffendingEnemies != other.numOffendingEnemies) return this.numOffendingEnemies < other.numOffendingEnemies;
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we are attacking and failing to flee from

      // check for approaching an enemy but not attacking
      if (this.shouldMoveInAnyways != other.shouldMoveInAnyways) return this.shouldMoveInAnyways;
      if (this.shouldMoveInAnyways) { // both are approaching a single enemy, choose the better option
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToClosestOffensive != other.distToClosestOffensive) return this.distToClosestOffensive < other.distToClosestOffensive;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we are approaching or moving in to attack

      if (this.rubble != other.rubble) return this.rubble < other.rubble;
      if (this.closestOffensive != null && other.closestOffensive != null) {
        if (this.distToClosestOffensive != other.distToClosestOffensive) return this.distToClosestOffensive > other.distToClosestOffensive;
      }
      return this.direction != Direction.CENTER;
    }

    public boolean execute() throws GameActionException {
      boolean attacked = false;
      boolean shouldCharge = this.hasTarget && !this.bestTarget.type.isBuilding() && this.bestTarget.health <= this.bestTarget.type.health * 0.22;
      boolean shouldFury = this.hasTarget && this.bestTarget.type.isBuilding() && this.bestTarget.type.health * 0.10 >= Cache.Permanent.ROBOT_TYPE.damage;
      if (this.hasTarget && this.bestTarget.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED) && Global.rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) < this.rubble) {
        if (shouldCharge) attacked = this.robotDoingMicro.envision(AnomalyType.CHARGE);
        else if (shouldFury && this.robotDoingMicro.noFriendlyTurretModeBuildingsNearby()) attacked = this.robotDoingMicro.envision(AnomalyType.FURY);
        else attacked = this.robotDoingMicro.attackTarget(bestTarget.location);
      }

      this.robotDoingMicro.move(Cache.PerTurn.CURRENT_LOCATION.directionTo(location));

      if (this.hasTarget && !attacked) {
        if (shouldCharge) attacked = this.robotDoingMicro.envision(AnomalyType.CHARGE);
        else if (shouldFury && this.robotDoingMicro.noFriendlyTurretModeBuildingsNearby()) attacked = this.robotDoingMicro.envision(AnomalyType.FURY);
        else attacked = this.robotDoingMicro.attackTarget(bestTarget.location);
      }
      if (attacked) {
        this.robotDoingMicro.lastAttackedEnemy = this.bestTarget;
        this.robotDoingMicro.lastAttackedEnemyRubble = this.rubbleOfTarget;

      }
      return attacked;
    }

    public void utilPrint() {
      Printer.cleanPrint();
      Printer.print("EVALUATE MOVING IN DIR: " + direction, "Moving: " + Cache.PerTurn.CURRENT_LOCATION + " --> " + location, "isMovement: " + isMovement);
      Printer.print("rubble: " + rubble, "distanceToFriendlyArchon: " + distanceToFriendlyArchon, "distanceToEnemyArchon: " + distanceToEnemyArchon, "tooCloseToEnemy: " + tooCloseToEnemy);
      Printer.print("totalOffensiveEnemies: " + totalOffensiveEnemies, "numOffendingEnemies: " + numOffendingEnemies);
      Printer.print("closestOffensive: " + closestOffensive);
      Printer.print("bestTarget: " + bestTarget, "hasTarget: " + hasTarget, "rubbleOfTarget: " + rubbleOfTarget);
      Printer.print("canAttackAndExit: " + isAttackAndExit, "shouldAttackAndExit: " + shouldAttackAndExit);
      Printer.print("isMovingInToAttack: " + isMovingInToAttack, "shouldMoveInToAttack: " + shouldMoveInToAttack);
      Printer.print("shouldMoveInAnyways: " + shouldMoveInAnyways);
      Printer.print("isBadIdea: " + isBadIdea);
      Printer.submitPrint();
    }

  }
}
