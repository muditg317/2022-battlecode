package firstbot.communications.messages;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import firstbot.utils.Global;
import firstbot.utils.Utils;


/**
 * A message sent by wandering miners looking for information about where to go
 */
public class LeadRequestMessage extends Message {
  public static final int PRIORITY = 1;
  public static final MessageType TYPE = MessageType.LEAD_REQUEST;
  public static final int MESSAGE_LENGTH = 2;

  public MapLocation from;
  public MapLocation location;
  public boolean answered;

  public LeadRequestMessage(int priority, MapLocation from, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.from = from;
    location = null;
    answered = false;
  }

  public LeadRequestMessage(MapLocation from, int roundNum) {
    this(PRIORITY, from, roundNum);
  }

  public LeadRequestMessage(Header header, int[] information) {
    super(header);
    processInformation(information);
  }

  /**
   * read the information ints into the data of the message
   * @param information the ints
   */
  private void processInformation(int[] information) {
    from = Utils.decodeLocation(information[0]);
    answered = checkAnsweredFlag(information);
    location = answered ? Utils.decodeLocation(information[1]) : null;
  }

  private static boolean checkAnsweredFlag(int[] information) {
    return (information[1] & 1) == 1;
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(from), encodeResponse(location)};
  }

  private static int encodeResponse(MapLocation location) {
    return location == null ? 0 : (Utils.encodeLocation(location) | 1);
  }

  /**
   * use the given communicator to check if this message has been responded to
   * @return whether the message has a response or not
   */
  public boolean readSharedResponse() {
    if (!Global.communicator.headerMatches(writeInfo.startIndex, header)) return false; // return false if the message is no longer valid
    System.out.println("Read response at " + (writeInfo.startIndex+1));
    int[] information = Global.communicator.readInts(writeInfo.startIndex+1, header.numInformationInts);
    processInformation(information);
    System.out.println("Process request response: " + location);
    return answered;
  }

  /**
   * use the given communicator to write a response into the shared buffer with the provided lead location
   * @param leadTarget the found lead of the response
   * @throws GameActionException if writing fails
   */
  public void respond(MapLocation leadTarget) throws GameActionException {
    System.out.println("Respond to request! " + writeInfo.startIndex + ": " + leadTarget + " - " + encodeResponse(leadTarget));
    Global.communicator.writeInts(writeInfo.startIndex + 2, new int[]{encodeResponse(leadTarget)});
  }
}
