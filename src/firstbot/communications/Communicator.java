package firstbot.communications;

import battlecode.common.RobotController;

/**
 * This class will have a variety of methods related to communications between robots
 * There should be a generic interface for sending messages and reading them
 * Every robot should have a communicator instantiated with its own RobotController
 *    (used to read/write to the shared array)
 */
public class Communicator {
  private final RobotController rc;

  public Communicator(RobotController rc) {
    this.rc = rc;
  }
}
