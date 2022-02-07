package spawnorder.utils;

import battlecode.common.RobotController;
import spawnorder.communications.Communicator;

public class Global {
  public static RobotController rc;
  public static Communicator communicator;

  public static void setupGlobals(RobotController rc) {
    Global.rc = rc;
    Global.communicator = new Communicator();
  }
}
