package bfspathing.utils;

import battlecode.common.RobotController;
import bfspathing.communications.Communicator;
import bfspathing.robots.Robot;

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
