package furyandminerrunning.communications.messages;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import furyandminerrunning.utils.Utils;

public class EnemyFoundMessage extends Message {
    public static final MessageType TYPE = MessageType.ENEMY_FOUND;
    public static final int MESSAGE_LENGTH = 0;
    public MapLocation enemyLocation;

    public EnemyFoundMessage(RobotInfo enemy) {
        super(TYPE);
        this.enemyLocation = enemy.location;
    }

    public EnemyFoundMessage(Header header, int headerInt) {
        super(header);
        this.enemyLocation = Utils.decodeLocationLower(headerInt);
    }

    public int[] toEncodedInts() {
        return new int[]{getHeaderInt() | Utils.encodeLocationLower(enemyLocation)};
    }
}
