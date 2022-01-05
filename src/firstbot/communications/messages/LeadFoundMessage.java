package firstbot.communications.messages;

import battlecode.common.MapLocation;

/**
 * A simple message with a single integer of information
 * primarily used for testing
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

  public LeadFoundMessage(Header header, int[] information) {
    super(header);
    this.location = decodeLocation(information[0]);
  }

  public LeadFoundMessage(MapLocation location, int roundNum) {
    this(PRIORITY, location, roundNum);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), encodeLocation(location)};
  }

  private static int encodeLocation(MapLocation location) {
    return location.x << 8 | location.y;
  }

  private static MapLocation decodeLocation(int encoded) {
    return new MapLocation((encoded >>> 8) & 0xff, encoded & 0xff);
  }
}
