package firstbot.communications.messages;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import firstbot.Utils;
import firstbot.communications.Communicator;

import java.util.Arrays;

/**
 * A message sent by wandering miners looking for information about where to go
 */
public class LeadRequestMessage extends Message {
  public static final int PRIORITY = 1;
  public static final MessageType TYPE = MessageType.LEAD_REQUEST;
  public static final int MESSAGE_LENGTH = 1;
  public MapLocation location;
  public boolean answered;

  public LeadRequestMessage(int priority, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    location = null;
    answered = false;
  }

  public LeadRequestMessage(int roundNum) {
    this(PRIORITY, roundNum);
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
    answered = checkAnsweredFlag(information);
    location = answered ? Utils.decodeLocation(information[0]) : null;
  }

  private static boolean checkAnsweredFlag(int[] information) {
    return (information[0] & 1) == 1;
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), encodeLocation(location)};
  }

  private static int encodeLocation(MapLocation location) {
    return location == null ? 0 : (Utils.encodeLocation(location) | 1);
  }

  /**
   * use the given communicator to check if this message has been responded to
   * @param communicator the communicator that has access to the shared array
   * @return whether the message has a response or not
   */
  public boolean readSharedResponse(Communicator communicator) {
    if (!communicator.headerMatches(writeInfo.startIndex, header)) return false; // return false if the message is no longer valid
//    System.out.println("Read response at " + (writeInfo.startIndex+1));
    int[] information = communicator.readInts(writeInfo.startIndex+1, header.numInformationInts);
//    System.out.println("Process request response: " + Arrays.toString(information));
    processInformation(information);
    return answered;
  }

  /**
   * use the given communicator to write a response into the shared buffer with the provided lead location
   * @param communicator the communicator to use for shared memory writing
   * @param leadTarget the found lead of the response
   * @throws GameActionException if writing fails
   */
  public void respond(Communicator communicator, MapLocation leadTarget) throws GameActionException {
    System.out.println("Respond to request! " + writeInfo.startIndex);
    communicator.writeInts(writeInfo.startIndex + 1, new int[]{encodeLocation(leadTarget)});
  }
}
