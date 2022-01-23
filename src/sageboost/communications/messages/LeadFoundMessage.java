package sageboost.communications.messages;

import battlecode.common.MapLocation;
import sageboost.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class LeadFoundMessage extends Message {
  public static final MessageType TYPE = MessageType.LEAD_FOUND;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public LeadFoundMessage(MapLocation location) {
    super(TYPE);
    this.location = location;
  }

  public LeadFoundMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
