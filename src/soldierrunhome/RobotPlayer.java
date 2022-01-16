package soldierrunhome;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import soldierrunhome.robots.Robot;

/**
 * This RobotPlayer will create a custom object for each type of robot which internally handles what it should do
 */
public strictfp class RobotPlayer {

    /**
     * Create the correct type of Robot object based on rc.getType()
     * Call the run method of that Robot
     *
     * @param rc  The RobotController object
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // While loop is completely unnecessary but used to ensure that the method is never exit
        while (true) {
            try {
                Robot robot = Robot.fromRC(rc);
                robot.runLoop();
            } catch (Exception e) {
                //System.out.println("FATAL ERROR - " + rc.getLocation());
                e.printStackTrace();
                rc.resign();
            }
        }
    }
}
