package soldiermicronew.communications.messages;

import battlecode.common.MapLocation;
import soldiermicronew.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class ArchonSavedMessage extends Message {
  public static final int PRIORITY = 2;
  public static final MessageType TYPE = MessageType.ARCHON_SAVED;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public ArchonSavedMessage(int priority, MapLocation location, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.location = location;
  }

  public ArchonSavedMessage(MapLocation location, int roundNum) {
    this(PRIORITY, location, roundNum);
  }

  public ArchonSavedMessage(Header header, int[] information) {
    super(header);
    this.location = Utils.decodeLocation(information[0]);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
