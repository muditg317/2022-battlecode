package chunking.communications.messages;

import battlecode.common.MapLocation;
import chunking.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class EndRaidMessage extends Message {
  public static final int PRIORITY = 2;
  public static final MessageType TYPE = MessageType.END_RAID;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public EndRaidMessage(int priority, MapLocation location, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.location = location;
  }

  public EndRaidMessage(MapLocation location, int roundNum) {
    this(PRIORITY, location, roundNum);
  }

  public EndRaidMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
