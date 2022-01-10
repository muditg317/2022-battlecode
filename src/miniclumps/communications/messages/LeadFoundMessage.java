package miniclumps.communications.messages;

import battlecode.common.MapLocation;
import miniclumps.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class LeadFoundMessage extends Message {
  public static final int PRIORITY = 1;
  public static final MessageType TYPE = MessageType.LEAD_FOUND;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public LeadFoundMessage(int priority, MapLocation location, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.location = location;
  }

  public LeadFoundMessage(MapLocation location, int roundNum) {
    this(PRIORITY, location, roundNum);
  }

  public LeadFoundMessage(Header header, int[] information) {
    super(header);
    this.location = Utils.decodeLocation(information[0]);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
