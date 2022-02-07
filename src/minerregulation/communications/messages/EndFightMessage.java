package minerregulation.communications.messages;

import battlecode.common.MapLocation;
import minerregulation.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class EndFightMessage extends Message {
  public static final MessageType TYPE = MessageType.END_FIGHT;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public EndFightMessage(MapLocation location) {
    super(TYPE);
    this.location = location;
  }

  public EndFightMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
