package soldiermicro.communications.messages;

import battlecode.common.MapLocation;
import soldiermicro.utils.Utils;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class SaveMeMessage extends Message {
  public static final int PRIORITY = 2;
  public static final MessageType TYPE = MessageType.SAVE_ME;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;

  public SaveMeMessage(int priority, MapLocation location, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.location = location;
  }

  public SaveMeMessage(MapLocation location, int roundNum) {
    this(PRIORITY, location, roundNum);
  }

  public SaveMeMessage(Header header, int[] information) {
    super(header);
    this.location = Utils.decodeLocation(information[0]);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location)};
  }
}
