package rewritesoldier.communications.messages;

import battlecode.common.MapLocation;
import rewritesoldier.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class StartRaidMessage extends Message {
  public static final MessageType TYPE = MessageType.START_RAID;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public StartRaidMessage(MapLocation locationNum) {
    super(TYPE);
    this.location = location;
  }

  public StartRaidMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
