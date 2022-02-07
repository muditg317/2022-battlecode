package soldierrunhome.communications.messages;

import battlecode.common.MapLocation;
import soldierrunhome.utils.Utils;


/**
 * A message sent by wandering miners looking for information about where to go
 */
public class RubbleAtLocationMessage extends Message {
  public static final int PRIORITY = 1;
  public static final MessageType TYPE = MessageType.RUBBLE_AT_LOCATION;
  public static final int MESSAGE_LENGTH = 2;

  public MapLocation location;
  public int rubble;

  public RubbleAtLocationMessage(int priority, MapLocation location, int rubble, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.location = location;
    this.rubble = rubble;
  }

  public RubbleAtLocationMessage(MapLocation location, int rubble, int roundNum) {
    this(PRIORITY, location, rubble, roundNum);
  }

  public RubbleAtLocationMessage(Header header, int information, int information2) {
    super(header);
    processInformation(information, information2);
  }

  /**
   * read the information ints into the data of the message
   * @param information the ints
   */
  private void processInformation(int information, int information2) {
    location = Utils.decodeLocation(information);
    rubble = information2;
  }


  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location), rubble};
  }
}
