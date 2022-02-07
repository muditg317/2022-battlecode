package donothing;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * This RobotPlayer will simply do nothing regardless of robot type
 */
public strictfp class RobotPlayer {

    /**
     * constantly yield the turn
     * @param rc  The RobotController object
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            Clock.yield();
        }
    }

}
