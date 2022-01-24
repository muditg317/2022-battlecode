package betterlabs.communications.messages;

import battlecode.common.MapLocation;
import betterlabs.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class SaveMeMessage extends Message {
  public static final MessageType TYPE = MessageType.SAVE_ME;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public SaveMeMessage(MapLocation location) {
    super(TYPE);
    this.location = location;
  }

  public SaveMeMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
