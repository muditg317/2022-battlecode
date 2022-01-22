package rewritesoldier.robots.droids;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import rewritesoldier.utils.Cache;
import rewritesoldier.utils.Global;
import rewritesoldier.utils.Utils;

public abstract class MicroInfo<T extends MicroInfo<T>> {

  public final Soldier soldier;
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
  
  protected MicroInfo(Soldier soldier, Direction direction) {
    this.soldier = soldier;
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
   * finalie any information that was collected from the updates
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
  
  public static class MicroInfoSoldierOnly extends MicroInfo<MicroInfoSoldierOnly> {
    
    private int rubble;
//    private FastQueue<RobotInfo> soldierFriendsFastQueue;
//    private int numFriendsAvailable;
    private int numEnemies;
    private double enemyDPS;
    private int numHelpingFriends;
    private double friendlyDPS;
    private RobotInfo closestEnemy;
    private int dToClosest = 9999;
    private RobotInfo bestEnemyInRange;
    private int healthOfBestInRange = 9999;
    private RobotInfo bestEnemyOnlyIfMoved;
    private int healthOfBestIfMoved = 9999;

    private RobotInfo chosenEnemyToAttack;
    private double scoreDiff;
    private double scoreRatio;
    private boolean hasHugeAdvantage;

    private boolean hasTarget;
    private boolean mustMoveFirst;
    private boolean mustAttackFirst;

    private boolean isMovingFurtherAway;
    private boolean isLeavingActionRadius;

    protected MicroInfoSoldierOnly(Soldier soldier, Direction direction) throws GameActionException {
      super(soldier, direction);
      this.rubble = Global.rc.senseRubble(location);
    }

    @Override
    public void update(RobotInfo nextEnemy) throws GameActionException {
      if (nextEnemy.type != RobotType.SOLDIER) return;
      if (nextEnemy.location.distanceSquaredTo(location) < dToClosest) {
        closestEnemy = nextEnemy;
        dToClosest = nextEnemy.location.distanceSquaredTo(location);
      }

      // if enemy is within action radius of current location, select the minimum health enemy.
      // If the enemy is not within action radius of current location, check if it will be after moving
      if (nextEnemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        if (nextEnemy.health < healthOfBestInRange) {
          bestEnemyInRange = nextEnemy;
          healthOfBestInRange = nextEnemy.health;
        }
      } else if (nextEnemy.location.isWithinDistanceSquared(location, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        // enemy was not in current action range but is in action range of candidate location
        if (nextEnemy.health < healthOfBestIfMoved) {
          bestEnemyOnlyIfMoved = nextEnemy;
          healthOfBestIfMoved = nextEnemy.health;
        }
      }

      if (nextEnemy.location.isWithinDistanceSquared(location, Cache.Permanent.VISION_RADIUS_SQUARED)) {
        // if enemy can see you, assume they can move in and hurt you
        numEnemies++;
        double turnsTillNextCooldown = Utils.turnsTillNextCooldown(nextEnemy.type.actionCooldown, Global.rc.senseRubble(nextEnemy.location));
        enemyDPS += (nextEnemy.type.damage / turnsTillNextCooldown);
      }
//      int friendsToCheck = soldierFriendsFastQueue.size();
//      for (int i = 0; i < friendsToCheck; i++) {
//        RobotInfo friend = soldierFriendsFastQueue.popFront();
//        if (friend.location.isWithinDistanceSquared(nextEnemy.location, Cache.Permanent.VISION_RADIUS_SQUARED)) {
//          // if friend can see enemy, assume they can help you
//          numHelpingFriends++;
//          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, Global.rc.senseRubble(friend.location));
//          friendlyDPS += (3 / turnsTillNextCooldown);
//          break;
//        }
//        soldierFriendsFastQueue.push(friend);
//      }
    }

    private void computeFriendlyDPSWithClosestEnemy() throws GameActionException {
      if (closestEnemy == null) return;
      if (closestEnemy.location.isWithinDistanceSquared(location, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        // if the closest enemy is in range, chose it as attack priority
        numHelpingFriends++;
        double turnsTillNextCooldown = Utils.turnsTillNextCooldown(Global.rc.getType().actionCooldown, Global.rc.senseRubble(location));
        friendlyDPS += (3 / turnsTillNextCooldown);
      }
      for (RobotInfo friendly : Global.rc.senseNearbyRobots(
          closestEnemy.location,
          Cache.Permanent.ACTION_RADIUS_SQUARED,
          Cache.Permanent.OUR_TEAM)) {
        if (friendly.type == RobotType.SOLDIER) {// && friendly.location.isWithinDistanceSquared(closestEnemy.location, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
          numHelpingFriends++;
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(friendly.type.actionCooldown, Global.rc.senseRubble(friendly.location));
          friendlyDPS += (3 / turnsTillNextCooldown);
        }
      }
    }

    /**
     * if there are enemies only reachable by moving,
     *    pick the better one
     *    mark ourselves as needing to move first
     *   NOTE - chosenEnemyToAttack CAN still be NULL
     */
    public void pickBestEnemyToAttack() {
      chosenEnemyToAttack = bestEnemyInRange;
      if (bestEnemyOnlyIfMoved != null) { // there is an enemy that can only be reached by moving, decide if it is better than current choice
        if (chosenEnemyToAttack == null || bestEnemyOnlyIfMoved.health < chosenEnemyToAttack.health) {
          chosenEnemyToAttack = bestEnemyOnlyIfMoved;
          mustMoveFirst = true;
        }
      }
      // technically both can be false but if we dont HAVE to move first, we might as well not move first
      //    b/c you won't reveal a better target
      //    exception: 25 -> 13 but whatever
      hasTarget = chosenEnemyToAttack != null;
      // enable regardless of actionReady -- based on same isBetter logic
      mustAttackFirst = hasTarget && !mustMoveFirst; // && Global.rc.isActionReady();
      // has a target and candidate is further awat
      isMovingFurtherAway = hasTarget && chosenEnemyToAttack.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION) < chosenEnemyToAttack.location.distanceSquaredTo(location);
      isLeavingActionRadius = isMovingFurtherAway && !chosenEnemyToAttack.location.isWithinDistanceSquared(location, Cache.Permanent.ACTION_RADIUS_SQUARED);
    }

    @Override
    public void finalizeInfo() throws GameActionException {
      computeFriendlyDPSWithClosestEnemy();
      pickBestEnemyToAttack();

      scoreDiff = friendlyDPS - enemyDPS;
      if (enemyDPS == 0) { // there were only enemies in vision which we have now run away from
        // almost guaranteed no enemies in current action radius since there would still be in vision
        //    exception is the 13->25 distance locations
        scoreRatio = friendlyDPS;
      } else {
        scoreRatio = scoreDiff > 0 ? friendlyDPS / enemyDPS : 0;
      }

//       closestEnemySoldier guaranteed not null bc otherwise friendly and enemy would both be 0
      if (scoreRatio >= 2 || scoreDiff >= 5) { // huge advantage -->  "go in"
        hasHugeAdvantage = true;
      }

      if (!hasHugeAdvantage) {
        //change score based on how close to enemy base vs ours
        Utils.MapSymmetry guess = Global.communicator.metaInfo.guessedSymmetry;

      }

//      //Utils.print("\n\nlocation: " + location, "enemyDPS: " + enemyDPS, "friendlyDPS: " + friendlyDPS);
//      //Utils.print("closestEnemy: " + closestEnemy, "bestEnemyInRange: " + bestEnemyInRange, "bestEnemyOnlyIfMoved: " + bestEnemyOnlyIfMoved, "chosenEnemyToAttack: " + chosenEnemyToAttack);
//      //Utils.print("scoreDiff: " + scoreDiff, "scoreRatio: " + scoreRatio);
//      //Utils.print("hasTarget: " + hasTarget, "mustMoveFirst: " + mustMoveFirst, "mustAttackFirst: " + mustAttackFirst);
//      //Utils.print("isMovingFurtherAway: " + isMovingFurtherAway, "isLeavingActionRadius: " + isLeavingActionRadius);

      // true when:
      //    I must go in
    }

    @Override
    public boolean isBetterThan(MicroInfoSoldierOnly other) {
      if (this.rubble != other.rubble) return this.rubble < other.rubble;
//      if (this.rubble > 25 && this.rubble >= other.rubble * 2) return false;
//      if (other.rubble > 25 && other.rubble >= this.rubble * 2) return true;

      if (this.hasHugeAdvantage != other.hasHugeAdvantage) return this.hasHugeAdvantage;
      if (this.hasHugeAdvantage) {
        if (this.hasTarget != other.hasTarget) return this.hasTarget;
        if (!this.hasTarget) return false;
        boolean thisCloser = this.location.isWithinDistanceSquared(this.closestEnemy.location, Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(this.closestEnemy.location));
        if (thisCloser
            != other.location.isWithinDistanceSquared(other.closestEnemy.location, Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(other.closestEnemy.location))
        ) return thisCloser;
        if (!thisCloser) return false;
        if (this.location.equals(Cache.PerTurn.CURRENT_LOCATION)) return false;
        if (other.location.equals(Cache.PerTurn.CURRENT_LOCATION)) return true;
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
        if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
        // i am NOT closer to our friends --> i'm staying further --> i'm better
        MapLocation myFriends = soldier.friendlySoldierCentroid();
        if (myFriends == null) return true;
        return !this.location.isWithinDistanceSquared(myFriends, other.location.distanceSquaredTo(myFriends));
      }

      if (this.mustAttackFirst != other.mustAttackFirst) {
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        return this.mustAttackFirst;
      }

      // neither me nor other has huge advantage, and both can attack
      if (this.mustAttackFirst) {
        // one set of logic
        // if this goes out of action radius and other does not, then this is better (careful about rubble?)
        // if both go out of action radius, then higher score is better. If tied score, pick further one out of action radius
        // if neither go out of action radius,

        if (this.isLeavingActionRadius != other.isLeavingActionRadius) {
          if (Global.rc.isActionReady()) {
            if (this.scoreDiff >= other.scoreDiff * 0.7 && other.rubble > this.rubble * 2) return true;
            if (other.scoreDiff >= this.scoreDiff * 0.7 && this.rubble > other.rubble * 2) return false;
          }
          return this.isLeavingActionRadius;
        }
        if (this.isLeavingActionRadius) { //both are leaving
          if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
          if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
          if (this.rubble < other.rubble) return true;
          if (this.rubble > other.rubble) return false;
          if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
          if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
          return this.location.distanceSquaredTo(soldier.parentArchonLoc) < other.location.distanceSquaredTo(soldier.parentArchonLoc);
        } else { // both are staying in action radius
          if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
          if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
          if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
          if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
          if (this.rubble < other.rubble) return true;
          if (this.rubble > other.rubble) return false;
          return this.location.distanceSquaredTo(soldier.parentArchonLoc) < other.location.distanceSquaredTo(soldier.parentArchonLoc);
        }

      } else if (Global.rc.isActionReady() && this.mustMoveFirst && other.mustMoveFirst) {
        // other set
        // if this can go in action radius and other cannot, this is better
        // choose positive score
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
        if (this.location.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.location.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
        if (this.rubble < other.rubble) return true;
        if (this.rubble > other.rubble) return false;
        return this.location.distanceSquaredTo(soldier.parentArchonLoc) > other.location.distanceSquaredTo(soldier.parentArchonLoc);
      }

      if (this.mustMoveFirst != other.mustMoveFirst) {
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        return this.mustMoveFirst;
      }


      // what if action ready, moving does not result in the guy in action radius, and other must move first


      return false;
    }

    @Override
    public boolean execute() throws GameActionException {
      soldier.broadcastJoinOrEndMyFight(this);
      if (mustMoveFirst) {
        soldier.move(Cache.PerTurn.CURRENT_LOCATION.directionTo(location));
        return hasTarget && soldier.attackTarget(chosenEnemyToAttack.location);
      }
      boolean attacked = hasTarget && soldier.attackTarget(chosenEnemyToAttack.location);
      soldier.move(Cache.PerTurn.CURRENT_LOCATION.directionTo(location));
      return attacked;
    }
  }

  public static class MicroInfoGeneric extends MicroInfo<MicroInfoGeneric> {
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

    public MicroInfoGeneric(Soldier soldier, Direction direction) throws GameActionException {
      super(soldier, direction);
      this.rubble = Global.rc.senseRubble(location);
      this.distanceToEnemyArchon = this.distanceToFriendlyArchon = 0;
//      this.distanceToFriendlyArchon = Global.communicator.archonInfo.getNearestFriendlyArchon(location).distanceSquaredTo(location);
//      this.distanceToEnemyArchon = Global.communicator.archonInfo.getNearestEnemyArchon(location).distanceSquaredTo(location);
      this.tooCloseToEnemy = this.distanceToEnemyArchon <= this.distanceToFriendlyArchon;
//      //Utils.print("Moving to " + location, "distToFriendArchon " + distanceToFriendlyArchon, "distToEnemyArchon " + distanceToEnemyArchon, "tooClose: " + tooCloseToEnemy);
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
      if (nextEnemy.health < bestTarget.health) {
        bestTarget = nextEnemy;
        distToTarget = nextEnemy.location.distanceSquaredTo(location);
        return;
      } else if (nextEnemy.health > bestTarget.health) {
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
        isAttackAndExit = !location.isWithinDistanceSquared(bestTarget.location, bestTarget.type.actionRadiusSquared);
        // only do so if there was just that one enemy
        shouldAttackAndExit = (isAttackAndExit
            && rubble <= Global.rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION) && ( // TODO: numOffending=0 doesn't account for going from 2 -> 1 enemies
            bestTarget.type.damage <= 0
            || (numOffendingEnemies == 0)))
            || (numOffendingEnemies == 1 && bestTarget.health <= Cache.Permanent.ROBOT_TYPE.damage)
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
        shouldMoveInToAttack = isMovingInToAttack && rubble <= rubbleOfTarget && (
            bestTarget.type.damage <= 0
            || (numOffendingEnemies == 1)
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
        shouldMoveInAnyways = totalOffensiveEnemies == 1
            && rubble <= Global.rc.senseRubble(closestOffensive.location)
            && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(
                closestOffensive.location,
                distToClosestOffensive);
      } else {
        isBadIdea = true;
      }
    }

    @Override
    public boolean isBetterThan(MicroInfoGeneric other) throws GameActionException {
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

      // check for approaching an enemy but not attacking
      if (this.shouldMoveInAnyways != other.shouldMoveInAnyways) return this.shouldMoveInAnyways;
      if (this.shouldMoveInAnyways) { // both are approaching a single enemy, choose the better option
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToClosestOffensive != other.distToClosestOffensive) return this.distToClosestOffensive < other.distToClosestOffensive;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we are approaching or moving in to attack

      // check for attacking but not fleeing
      if (this.shouldAttackWithoutFlee != other.shouldAttackWithoutFlee) return this.shouldAttackWithoutFlee;
      if (this.shouldAttackWithoutFlee) { // both are attacking and staying within action, prefer fewer enemies
        if (this.numOffendingEnemies != other.numOffendingEnemies) return this.numOffendingEnemies < other.numOffendingEnemies;
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distToTarget != other.distToTarget) return this.distToTarget > other.distToTarget;
        return this.direction != Direction.CENTER;
      }
      // from here, there is no enemy which we are attacking and failing to flee from

      if (this.rubble != other.rubble) return this.rubble < other.rubble;
      if (this.closestOffensive != null && other.closestOffensive != null) {
        if (this.distToClosestOffensive != other.distToClosestOffensive) return this.distToClosestOffensive > other.distToClosestOffensive;
      }
      return this.direction != Direction.CENTER;
    }

    private boolean isBetterThanOld(MicroInfoGeneric other) {

      if (this.rubble != other.rubble) return this.rubble < other.rubble;

      if (this.numOffendingEnemies != other.numOffendingEnemies) { // always prefer less offensive units
//        if (this.numOffensiveEnemies == 1 && other.numOffensiveEnemies == 0 // this moved towards an enemy
////            && this.hasTarget && this.bestTarget.type.damage <= 0
//        ) return Global.rc.isActionReady();
//        if (this.numOffensiveEnemies == 0 && other.numOffensiveEnemies == 1 // other moved towards an enemy
////            && other. mekjlnrwelkjgng
//        ) return !Global.rc.isActionReady();
        return this.numOffendingEnemies < other.numOffendingEnemies;
      }

      if (this.hasTarget != other.hasTarget) { // one or the other has a target
        // return the one with the target if it's a nonOffensive target
        if (this.tooCloseToEnemy == other.tooCloseToEnemy) {
          if (this.tooCloseToEnemy) {
            if (this.hasTarget) return this.bestTarget.type.damage <= 0;
            else return other.bestTarget.type.damage <= 0;
          } else {
            if (this.hasTarget) return this.bestTarget.type.damage > 0;
            else return other.bestTarget.type.damage > 0;
          }
        } else {
//          if (this.hasTarget) {
//            return this.tooCloseToEnemy;
//          } else {
//            return !this.tooCloseToEnemy;
//          }
          return this.tooCloseToEnemy;
        }
      }
      if (!this.hasTarget) { // both have no target
        if (this.rubble != other.rubble) return this.rubble < other.rubble;
        if (this.distanceToFriendlyArchon != other.distanceToFriendlyArchon) return this.distanceToFriendlyArchon < other.distanceToFriendlyArchon;
        if (this.distanceToEnemyArchon != other.distanceToEnemyArchon) return this.distanceToEnemyArchon > other.distanceToEnemyArchon;
        return false;
      }
      // assume both have target now
      if (this.targetPriority != other.targetPriority) return this.targetPriority > other.targetPriority;
      if (this.bestTarget.health != other.bestTarget.health) return this.bestTarget.health < other.bestTarget.health;
      if (this.rubble != other.rubble) return this.rubble < other.rubble;
      if (this.distanceToFriendlyArchon != other.distanceToFriendlyArchon) return this.distanceToFriendlyArchon < other.distanceToFriendlyArchon;
      if (this.distanceToEnemyArchon != other.distanceToEnemyArchon) return this.distanceToEnemyArchon > other.distanceToEnemyArchon;
      return false;
    }

    public boolean execute() throws GameActionException {
      boolean attacked = false;
      soldier.lastAttackedEnemy = this.bestTarget;
      if (this.hasTarget && this.bestTarget.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED) && this.rubble <= Global.rc.senseRubble(Cache.PerTurn.CURRENT_LOCATION)) {
        attacked = soldier.attackTarget(bestTarget.location);
      }
      soldier.move(Cache.PerTurn.CURRENT_LOCATION.directionTo(location));
      if (this.hasTarget && !attacked) {
        attacked = soldier.attackTarget(bestTarget.location);
      }
      return attacked;
    }

    public void utilPrint() {
      Utils.cleanPrint();
      //Utils.print("EVALUATE MOVING IN DIR: " + direction, "Moving: " + Cache.PerTurn.CURRENT_LOCATION + " --> " + location, "isMovement: " + isMovement);
      //Utils.print("rubble: " + rubble, "distanceToFriendlyArchon: " + distanceToFriendlyArchon, "distanceToEnemyArchon: " + distanceToEnemyArchon, "tooCloseToEnemy: " + tooCloseToEnemy);
      //Utils.print("totalOffensiveEnemies: " + totalOffensiveEnemies, "numOffendingEnemies: " + numOffendingEnemies);
      //Utils.print("closestOffensive: " + closestOffensive, "shouldMoveInAnyways: " + shouldMoveInAnyways);
      //Utils.print("bestTarget: " + bestTarget, "hasTarget: " + hasTarget, "rubbleOfTarget: " + rubbleOfTarget);
      //Utils.print("canAttackAndExit: " + isAttackAndExit, "shouldAttackAndExit: " + shouldAttackAndExit);
      //Utils.print("isMovingInToAttack: " + isMovingInToAttack, "shouldMoveInToAttack: " + shouldMoveInToAttack);
      //Utils.print("isBadIdea: " + isBadIdea);
      Utils.submitPrint();
    }

  }
}
