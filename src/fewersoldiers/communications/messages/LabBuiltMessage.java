package fewersoldiers.communications.messages;

import battlecode.common.MapLocation;
import fewersoldiers.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class LabBuiltMessage extends Message {
  public static final MessageType TYPE = MessageType.LAB_BUILT;
  public static final int MESSAGE_LENGTH = 0;
  public MapLocation location;

  public LabBuiltMessage(MapLocation location) {
    super(TYPE);
    this.location = location;
  }

  public LabBuiltMessage(Header header, int headerInt) {
    super(header);
    this.location = Utils.decodeLocationLower(headerInt);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt() | Utils.encodeLocationLower(location)};
  }
}
