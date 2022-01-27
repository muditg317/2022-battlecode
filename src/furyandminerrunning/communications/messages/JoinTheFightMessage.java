package furyandminerrunning.communications.messages;

import battlecode.common.MapLocation;
import furyandminerrunning.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class JoinTheFightMessage extends Message {
    public static final MessageType TYPE = MessageType.JOIN_THE_FIGHT;
    public static final int MESSAGE_LENGTH = 1;
    public MapLocation location;

    public JoinTheFightMessage(MapLocation location) {
        super(TYPE);
        this.location = location;
    }

    public JoinTheFightMessage(Header header, int information) {
        super(header);
        this.location = Utils.decodeLocation(information);
    }

    public int[] toEncodedInts() {
        return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
    }
}
