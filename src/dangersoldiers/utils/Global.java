package dangersoldiers.utils;

import battlecode.common.RobotController;
import dangersoldiers.communications.Communicator;
import dangersoldiers.robots.Robot;

public class Global {
  public static RobotController rc;
  public static Robot robot;
  public static Communicator communicator;

  public static void setupGlobals(RobotController rc, Robot robot) {
    Global.rc = rc;
    Global.robot = robot;
    Global.communicator = new Communicator();
  }
}
