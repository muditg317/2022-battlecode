package firstbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import firstbot.Constants;
import firstbot.communications.Communicator;
import firstbot.robots.buildings.Archon;
import firstbot.robots.buildings.Laboratory;
import firstbot.robots.buildings.Watchtower;
import firstbot.robots.droids.Builder;
import firstbot.robots.droids.Miner;
import firstbot.robots.droids.Sage;
import firstbot.robots.droids.Soldier;

public abstract class Robot {

  protected final RobotController rc;
  protected final Communicator communicator;

  protected final int roundCreated;

  /**
   * Create a Robot with the given controller
   * Perform various setup tasks generic to ny robot (building/droid)
   * @param rc the controller
   */
  public Robot(RobotController rc) {
    this.rc = rc;
    this.communicator = new Communicator(rc);

    this.roundCreated = rc.getRoundNum();

    // Print spawn message
    System.out.printf("%s at %s - HP: %4d\n", rc.getType(), rc.getLocation(), rc.getHealth());
    // Set indicator message
    rc.setIndicatorString("Just spawned!");
  }

  /**
   * Create a Robot-subclass instance from the provided controller
   * @param rc the controller object
   * @return a custom Robot instance
   */
  public static Robot fromRC(RobotController rc) {
    switch (rc.getType()) {
      case ARCHON:     return new Archon(rc);
      case LABORATORY: return new Laboratory(rc);
      case WATCHTOWER: return new Watchtower(rc);
      case MINER:      return new Miner(rc);
      case BUILDER:    return new Builder(rc);
      case SOLDIER:    return new Soldier(rc);
      case SAGE:       return new Sage(rc);
      default:         throw  new RuntimeException("Cannot create Robot-subclass for invalid RobotType: " + rc.getType());
    }
  }

  /**
   * Run the wrapper for the main loop function of the robot
   * This is generic to all robots
   */
  public void runLoop() {
    /*
      should never exit - Robot will die otherwise (Clock.yeild() to end turn)
     */
    while (true) {
      // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
      try {
        this.runTurnWrapper();
      } catch (GameActionException e) {
        // something illegal in the Battlecode world
        System.out.println(rc.getType() + " GameActionException");
        e.printStackTrace();
      } catch (Exception e) {
        // something bad
        System.out.println(rc.getType() + " Exception");
        e.printStackTrace();
      } finally {
        // end turn - make code wait until next turn
        Clock.yield();
      }
    }
  }

  /**
   * wrap intern run turn method with generic actions for all robots
   */
  private void runTurnWrapper() throws GameActionException {
//      System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

    runTurn();
    communicator.sendQueuedMessages();

  }

  /**
   * Run a single turn for the robot
   * unique to each robot type
   */
  protected abstract void runTurn() throws GameActionException;

  protected void moveRandomly() throws GameActionException {
    // Also try to move randomly.
    Direction dir = Constants.directions[Constants.rng.nextInt(Constants.directions.length)];
    if (rc.canMove(dir)) {
      rc.move(dir);
//      System.out.println("I moved!");
    }
  }
}
