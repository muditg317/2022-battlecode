package builderbugfix.communications.messages;

import battlecode.common.MapLocation;
import builderbugfix.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class ArchonSavedMessage extends Message {
  public static final MessageType TYPE = MessageType.ARCHON_SAVED;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public ArchonSavedMessage(MapLocation location) {
    super(TYPE);
    this.location = location;
  }

  public ArchonSavedMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
