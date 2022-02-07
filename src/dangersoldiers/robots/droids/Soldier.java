package dangersoldiers.robots.droids;

import battlecode.common.*;
import dangersoldiers.communications.messages.*;
import dangersoldiers.utils.Cache;
import dangersoldiers.utils.Utils;

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
  MapLocation lastSoldierAttack;
  double lastSoldierTradeScore;

  MapLocation archonToSave;

  MapLocation fightToJoin;

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
//    //System.out.println();

    // miner-like random exploration (random target and go to it)

    if (fightToJoin != null && !fightToJoin.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.VISION_RADIUS_SQUARED*2)) {
      explorationTarget = fightToJoin;
    }

    if (archonToSave != null && !needToRunHomeForSaving && !Cache.PerTurn.CURRENT_LOCATION.isWithinDistanceSquared(archonToSave, Cache.Permanent.VISION_RADIUS_SQUARED)) {
      //Utils.print("archonToSave: " + archonToSave);
      if (moveOptimalTowards(archonToSave) && checkDoneSaving()) {
        finishSaving();
      }
    }

    if (needToRunHomeForSaving) {
      robotToChase = null;
    }

    {
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

    if (robotToChase != null) {
      //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, robotToChase.location, 0, 0, 255);
      //rc.setIndicatorDot(robotToChase.location, 0, 255, 0);
    }

    //TODO: should technically check cases again if I just moved and have action cooldown, but this is fine for now!!

    // else if we see miner or builder, keep miner as close as possible to me

    // soldier caches last seen enemy? --> miner takes priority?

    // archon --> some type of formula to see if we can maybe kill it? or just prioritize closer unit if no soldier

    if (rc.isActionReady()) {
      attackNearby();
    }

    fightToJoin = null;
  }

//  private FastQueue<MicroInfo> movementMicroOptions = new FastQueue<>(9);
  private boolean attackEnemySoldier() throws GameActionException {
    Utils.cleanPrint();
//    movementMicroOptions.clear();
//    movementMicroOptions.push(new MicroInfo(Cache.PerTurn.CURRENT_LOCATION));
//    if (!needToRunHomeForSaving) {
//      for (Direction dir : Utils.directions) {
//        if (rc.canMove(dir)) movementMicroOptions.push(new MicroInfo(Cache.PerTurn.CURRENT_LOCATION.add(dir)));
//      }
//    }
//
//    for (RobotInfo enemy : Cache.PerTurn.ALL_NEARBY_ENEMY_ROBOTS) {
//      for (int i = movementMicroOptions.startIter(); --i >= 0;) {
//        movementMicroOptions.next().update(enemy);
//      }
//    }
//
//    MicroInfo best = movementMicroOptions.popFront().finalizeInfo();
//    MicroInfo curr;
//    while (!movementMicroOptions.isEmpty()) {
//      if ((curr = movementMicroOptions.popFront()).finalizeInfo().isBetterThan(best)) {
//        //Utils.print("best: " + best,  "curr: " + curr);
//        best = curr;
//      }
//    }

    MicroInfo best = null;
    for (Direction dir : Utils.directionsNine) {
      if (dir != Direction.CENTER && (needToRunHomeForSaving || !rc.canMove(dir))) continue;
      MicroInfo curr = new MicroInfo(Cache.PerTurn.CURRENT_LOCATION.add(dir));
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

  private boolean attackEnemySoldierOld() throws GameActionException {

    //Utils.print("RUNNING attackEnemySoldier()");

    double bestScore = Double.NEGATIVE_INFINITY;
    MapLocation bestLocation = null;
    MapLocation bestEnemySoldier = null;
    int bestDistance = -1;

    boolean hasHugeAdvantage = false;
    boolean cannotAttackSoMoveAway = false;
    boolean canAttackSoMoveIn = false;


    // my location and all adjacent locations
    for (Direction dir : Utils.directionsNine) {
      if (dir != Direction.CENTER && (needToRunHomeForSaving || !rc.canMove(dir))) continue;
      MapLocation candidate = Cache.PerTurn.CURRENT_LOCATION.add(dir);

      int numEnemySoldiers = 0;
      double averageEnemyDamagePerRound = 0;
      int minDistance = Integer.MAX_VALUE;
      MapLocation closestEnemySoldier = null; // can only be null iff averageEnemyDamagePerRound == 0 and averageEnemyDamagePerRound == 0, score=0

      // use vision radius to count enemies (assume they can all attack and will move into action radius if needed), and calculate DPS
      for (RobotInfo enemy : rc.senseNearbyRobots(candidate, Cache.Permanent.VISION_RADIUS_SQUARED, Cache.Permanent.OPPONENT_TEAM)) {
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

      // use vision radius centered on closest enemy to count friends (assume friends can all attack and will move into action radius if needed), and calculate DPS
      int numFriendlySoldiers = 0;
      double averageFriendlyDamagePerRound = 0;
      if (closestEnemySoldier != null) {
        if (closestEnemySoldier.distanceSquaredTo(candidate) <= Cache.Permanent.ACTION_RADIUS_SQUARED) {// && rc.isActionReady()) {
          numFriendlySoldiers++;
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, rc.senseRubble(candidate));
          averageFriendlyDamagePerRound += (3 / turnsTillNextCooldown);
        }
        for (RobotInfo friendly : rc.senseNearbyRobots(
            closestEnemySoldier,
            Cache.Permanent.VISION_RADIUS_SQUARED,
            Cache.Permanent.OUR_TEAM)) {
          if (friendly.type == RobotType.SOLDIER) {
            numFriendlySoldiers++;
            double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, rc.senseRubble(friendly.location));
            averageFriendlyDamagePerRound += (3 / turnsTillNextCooldown);
          }
        }
      }
      if (closestEnemySoldier == null) continue;

      double scoreDiff = averageFriendlyDamagePerRound - averageEnemyDamagePerRound;
      double scoreRatio = averageFriendlyDamagePerRound / averageEnemyDamagePerRound;

      // closestEnemySoldier guaranteed not null bc otherwise friendly and enemy would both be 0
      if (scoreRatio >= 2 || scoreDiff >= 5) { // huge advantage
        // "go in"
        hasHugeAdvantage = true;
      }

      boolean needToGoInToAttack = rc.isActionReady() && !closestEnemySoldier.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED);

      double score = 1.01 * averageFriendlyDamagePerRound - averageEnemyDamagePerRound;
//      //System.out.println("candLoc: " + candidate + " --\nnumEnemySoldiers: " + numEnemySoldiers + "\nenemyDmgPerRound: " + averageEnemyDamagePerRound + "\nclosestEnemySoldier: " + closestEnemySoldier + " --\nnumFriendlySoldiers: " + numFriendlySoldiers + "\nFriendlyDmgPerRound: " + averageFriendlyDamagePerRound + "\nscore: " + score);
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



      if (isBetter) {
        bestScore = score;
        bestLocation = candidate;
        bestEnemySoldier = closestEnemySoldier;
        if (closestEnemySoldier != null) bestDistance = candidate.distanceSquaredTo(closestEnemySoldier);
      }
    }


    setIndicatorString(String.format("atkSldr(%4.2f)", bestScore), bestLocation);

    // if we are in a negative trade that we don't know how to escape
    if (bestScore <= 0 && rc.isMovementReady() && bestLocation.equals(Cache.PerTurn.CURRENT_LOCATION)) {
      Direction toEscape = getOptimalDirectionAway(offensiveEnemyCentroid());
      if (toEscape != null) {
        bestLocation = Cache.PerTurn.CURRENT_LOCATION.add(toEscape);
      }
    }


    if (bestEnemySoldier != null) {
      lastSoldierAttack = bestEnemySoldier;
      lastSoldierTradeScore = bestScore;
    }

    return attackAtAndMoveTo(bestEnemySoldier, bestLocation, false);

  }

//  private int timesGoneIn = 0;
  private class MicroInfo {

    private MapLocation myLocation;
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

    public MicroInfo(MapLocation myLocation) throws GameActionException {
      this.myLocation = myLocation;
      this.rubble = rc.senseRubble(myLocation);
//      this.soldierFriendsFastQueue = new FastQueue<>(10);
//      for (RobotInfo friend : Cache.PerTurn.ALL_NEARBY_FRIENDLY_ROBOTS) {
//        if (friend.type == RobotType.SOLDIER) {
//          if (++numFriendsAvailable <= 10) soldierFriendsFastQueue.push(friend);
//        }
//      }
    }

    /**
     * update this micro info with the provided enemy within current vision radius
     *    info should decide if this enemy is relevant to it or not
     *    guaranteed closestEnemy is nonNull, but bestEnemyInRange and bestEnemyOnlyIfMoved may be null
     * @param nextEnemy
     * @throws GameActionException
     */
    public void update(RobotInfo nextEnemy) throws GameActionException {
      if (nextEnemy.type != RobotType.SOLDIER) return;
      if (nextEnemy.location.distanceSquaredTo(myLocation) < dToClosest) {
        closestEnemy = nextEnemy;
        dToClosest = nextEnemy.location.distanceSquaredTo(myLocation);
      }

      // if enemy is within action radius of current location, select the minimum health enemy.
      // If the enemy is not within action radius of current location, check if it will be after moving
      if (nextEnemy.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        if (nextEnemy.health < healthOfBestInRange) {
          bestEnemyInRange = nextEnemy;
          healthOfBestInRange = nextEnemy.health;
        }
      } else if (nextEnemy.location.isWithinDistanceSquared(myLocation, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        // enemy was not in current action range but is in action range of candidate location
        if (nextEnemy.health < healthOfBestIfMoved) {
          bestEnemyOnlyIfMoved = nextEnemy;
          healthOfBestIfMoved = nextEnemy.health;
        }
      }

      if (nextEnemy.location.isWithinDistanceSquared(myLocation, Cache.Permanent.VISION_RADIUS_SQUARED)) {
        // if enemy can see you, assume they can move in and hurt you
        numEnemies++;
        double turnsTillNextCooldown = Utils.turnsTillNextCooldown(nextEnemy.type.actionCooldown, rc.senseRubble(nextEnemy.location));
        enemyDPS += (nextEnemy.type.damage / turnsTillNextCooldown);
      }
//      int friendsToCheck = soldierFriendsFastQueue.size();
//      for (int i = 0; i < friendsToCheck; i++) {
//        RobotInfo friend = soldierFriendsFastQueue.popFront();
//        if (friend.location.isWithinDistanceSquared(nextEnemy.location, Cache.Permanent.VISION_RADIUS_SQUARED)) {
//          // if friend can see enemy, assume they can help you
//          numHelpingFriends++;
//          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(10, rc.senseRubble(friend.location));
//          friendlyDPS += (3 / turnsTillNextCooldown);
//          break;
//        }
//        soldierFriendsFastQueue.push(friend);
//      }
    }

    private void computeFriendlyDPSWithClosestEnemy() throws GameActionException {
      if (closestEnemy == null) return;
      if (closestEnemy.location.isWithinDistanceSquared(myLocation, Cache.Permanent.ACTION_RADIUS_SQUARED)) {
        // if the closest enemy is in range, chose it as attack priority
        numHelpingFriends++;
        double turnsTillNextCooldown = Utils.turnsTillNextCooldown(rc.getType().actionCooldown, rc.senseRubble(myLocation));
        friendlyDPS += (3 / turnsTillNextCooldown);
      }
      for (RobotInfo friendly : rc.senseNearbyRobots(
              closestEnemy.location,
              Cache.Permanent.VISION_RADIUS_SQUARED,
              Cache.Permanent.OUR_TEAM)) {
        if (friendly.type == RobotType.SOLDIER) {
          numHelpingFriends++;
          double turnsTillNextCooldown = Utils.turnsTillNextCooldown(friendly.type.actionCooldown, rc.senseRubble(friendly.location));
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
      mustAttackFirst = hasTarget && !mustMoveFirst; // && rc.isActionReady();
      // has a target and candidate is further awat
      isMovingFurtherAway = hasTarget && chosenEnemyToAttack.location.distanceSquaredTo(Cache.PerTurn.CURRENT_LOCATION) < chosenEnemyToAttack.location.distanceSquaredTo(myLocation);
      isLeavingActionRadius = isMovingFurtherAway && !chosenEnemyToAttack.location.isWithinDistanceSquared(myLocation, Cache.Permanent.ACTION_RADIUS_SQUARED);
    }

    public MicroInfo finalizeInfo() throws GameActionException {
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
        Utils.MapSymmetry guess = communicator.metaInfo.guessedSymmetry;

      }

//      //Utils.print("\n\nmyLocation: " + myLocation, "enemyDPS: " + enemyDPS, "friendlyDPS: " + friendlyDPS);
//      //Utils.print("closestEnemy: " + closestEnemy, "bestEnemyInRange: " + bestEnemyInRange, "bestEnemyOnlyIfMoved: " + bestEnemyOnlyIfMoved, "chosenEnemyToAttack: " + chosenEnemyToAttack);
//      //Utils.print("scoreDiff: " + scoreDiff, "scoreRatio: " + scoreRatio);
//      //Utils.print("hasTarget: " + hasTarget, "mustMoveFirst: " + mustMoveFirst, "mustAttackFirst: " + mustAttackFirst);
//      //Utils.print("isMovingFurtherAway: " + isMovingFurtherAway, "isLeavingActionRadius: " + isLeavingActionRadius);

      // true when:
      //    I must go in
      return this;
    }

    // within high advantage
    //    among tiles that get closer, choose highest score
    //    pick higher score, then lower rubble, then less distance? =>

    public boolean isBetterThan(MicroInfo other) throws GameActionException {
      if (this.rubble > 25 && this.rubble >= other.rubble * 2) return false;
      if (other.rubble > 25 && other.rubble >= this.rubble * 2) return true;

      if (this.hasHugeAdvantage != other.hasHugeAdvantage) return this.hasHugeAdvantage;
      if (this.hasHugeAdvantage) {
        if (this.hasTarget != other.hasTarget) return this.hasTarget;
        if (!this.hasTarget) return false;
        boolean thisCloser = this.myLocation.isWithinDistanceSquared(this.closestEnemy.location, Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(this.closestEnemy.location));
        if (thisCloser
            != other.myLocation.isWithinDistanceSquared(other.closestEnemy.location, Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(other.closestEnemy.location))
        ) return thisCloser;
        if (!thisCloser) return false;
        if (this.myLocation.equals(Cache.PerTurn.CURRENT_LOCATION)) return false;
        if (other.myLocation.equals(Cache.PerTurn.CURRENT_LOCATION)) return true;
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
        if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
        // i am NOT closer to our friends --> i'm staying further --> i'm better
        MapLocation myFriends = friendlySoldierCentroid();
        if (myFriends == null) return true;
        return !this.myLocation.isWithinDistanceSquared(myFriends, other.myLocation.distanceSquaredTo(myFriends));
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
          if (rc.isActionReady()) {
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
          if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
          if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
          return this.myLocation.distanceSquaredTo(parentArchonLoc) < other.myLocation.distanceSquaredTo(parentArchonLoc);
        } else { // both are staying in action radius
          if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
          if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
          if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
          if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
          if (this.rubble < other.rubble) return true;
          if (this.rubble > other.rubble) return false;
          return this.myLocation.distanceSquaredTo(parentArchonLoc) < other.myLocation.distanceSquaredTo(parentArchonLoc);
        }

      } else if (rc.isActionReady() && this.mustMoveFirst && other.mustMoveFirst) {
        // other set
        // if this can go in action radius and other cannot, this is better
        // choose positive score
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) < other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return true;
        if (this.myLocation.distanceSquaredTo(this.chosenEnemyToAttack.location) > other.myLocation.distanceSquaredTo(other.chosenEnemyToAttack.location)) return false;
        if (this.rubble < other.rubble) return true;
        if (this.rubble > other.rubble) return false;
        return this.myLocation.distanceSquaredTo(parentArchonLoc) > other.myLocation.distanceSquaredTo(parentArchonLoc);
      }

      if (this.mustMoveFirst != other.mustMoveFirst) {
        if (this.scoreDiff != other.scoreDiff) return this.scoreDiff > other.scoreDiff;
        if (this.scoreRatio != other.scoreRatio) return this.scoreRatio > other.scoreRatio;
        return this.mustMoveFirst;
      }


      // what if action ready, moving does not result in the guy in action radius, and other must move first


      return false;
    }

    public boolean execute() throws GameActionException {
      broadcastJoinOrEndMyFight(MicroInfo.this);
      if (mustMoveFirst) {
        move(Cache.PerTurn.CURRENT_LOCATION.directionTo(myLocation));
        return hasTarget && attackTarget(chosenEnemyToAttack.location);
      }
      boolean attacked = hasTarget && attackTarget(chosenEnemyToAttack.location);
      move(Cache.PerTurn.CURRENT_LOCATION.directionTo(myLocation));
      return attacked;
    }
  }

  /**
   * find and attack/deal with an enemy miner/builder
   *    SHOULD GUARANTEE >=1 miner/builder is in vision radius
   * @return true if attacked
   * @throws GameActionException if sensing or attacking fails
   */
  private boolean attackAndChaseEnemyMinerOrBuilder(boolean enemyMinerExists, boolean enemyBuilderExists) throws GameActionException {
    MapLocation enemyLocation = setNonOffensiveDroidToChaseAndChooseTarget(enemyMinerExists, enemyBuilderExists);
    //Utils.print("RUNNING attackEnemyMinerOrBuilder()", "enemyLocation: " + enemyLocation);
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
    //Utils.print("RUNNING attackEnemyArchon()");
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
    //Utils.print("RUNNING attackEnemySage()");
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
    //rc.setIndicatorLine(Cache.PerTurn.CURRENT_LOCATION, raidTarget, 0,0,255);
//    return moveInDirLoose(Cache.PerTurn.CURRENT_LOCATION.directionTo(raidTarget))
    return moveOptimalTowards(raidTarget)
        && Cache.PerTurn.CURRENT_LOCATION.distanceSquaredTo(raidTarget) <= Cache.Permanent.VISION_RADIUS_SQUARED;
  }

  /**
   * send message to fellow soldiers to join my fight
   */
  private void broadcastJoinOrEndMyFight(MicroInfo executedMicro) {
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
  private boolean attackTarget(MapLocation target) throws GameActionException {
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
        //Utils.print("robotToChase: " + robotToChase);
        attackAtAndMoveTo(robotToChase.location, robotToChase.location, true);
        if (robotToChase.location.isWithinDistanceSquared(Cache.PerTurn.CURRENT_LOCATION, Utils.DSQ_2by2)) {
          robotToChase = null;
        }
      }

      if (robotToChase == null) {
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
