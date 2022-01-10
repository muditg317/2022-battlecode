package firstbot.utils;

import battlecode.common.RobotController;
import firstbot.communications.Communicator;

public class Global {
  public static RobotController rc;
  public static Communicator communicator;

  public static void setupGlobals(RobotController rc) {
    Global.rc = rc;
    Global.communicator = new Communicator();
  }
}
