package firstbot.communications;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import firstbot.communications.messages.Message;
import firstbot.communications.messages.SingleIntMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * This class will have a variety of methods related to communications between robots
 * There should be a generic interface for sending messages and reading them
 * Every robot should have a communicator instantiated with its own RobotController
 *    (used to read/write to the shared array)
 */
public class Communicator {

  private static class MetaInfo {
    public int validRegionStart; // 0-62    -- 6 bits [15,10]
    public int validRegionEnd; // 0-62      -- 6 bits [9,4]

    public MetaInfo(RobotController rc) {
    }

    /**
     * update the communicators meta information based on the meta ints from the shared buffer
     * @param sharedBuffer the most up-to-date version of the shared memory buffer
     */
    public void updateFromBuffer(int[] sharedBuffer) {
      int metaInt = sharedBuffer[META_INT_START];
      validRegionStart = (metaInt >>> 10) & 63;
      validRegionEnd = (metaInt >>> 4) & 63;
    }

    /**
     * convert the meta information about the communication buffer into a set of ints
     * @return the encoded meta information
     */
    public int[] encode() {
      int[] encoded = new int[NUM_META_INTS];
      encoded[0] =
            validRegionStart << 10
          | validRegionEnd << 4;
      return encoded;
    }

    @Override
    public String toString() {
      return String.format("CommMeta{frm=%2d,to=%2d}", validRegionStart, validRegionEnd);
    }
  }

  private final RobotController rc;
  private final int[] sharedBuffer;

  private static final int NUM_META_INTS = 1;
  private static final int META_INT_START = GameConstants.SHARED_ARRAY_LENGTH - NUM_META_INTS;
  private final MetaInfo metaInfo;

  private static final int NUM_MESSAGING_INTS = META_INT_START;
  private final PriorityQueue<QueuedMessage> messageQueue;
  private final List<Message> sentMessages;
  private final List<Message> received;

  private int intsWritten;

  public Communicator(RobotController rc) {
    this.rc = rc;
    sharedBuffer = new int[GameConstants.SHARED_ARRAY_LENGTH];
    metaInfo = new MetaInfo(rc);

    messageQueue = new PriorityQueue<>();
    sentMessages = new ArrayList<>();
    received = new ArrayList<>();

    intsWritten = 0;
  }

  /**
   * reset the sharedBuffer with the contents of the entire shared array
   */
  private void reloadBuffer() throws GameActionException {
    for (int i = 0; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
      sharedBuffer[i] = rc.readSharedArray(i);
    }
    metaInfo.updateFromBuffer(sharedBuffer);
    intsWritten = 0;
  }

  /**
   * read all the messages in the sharedArray
   * @return the number of messages that were read
   * @throws GameActionException thrown if readMessageAt fails
   */
  public int readMessages() throws GameActionException {
    reloadBuffer();
    int origin = metaInfo.validRegionStart;
    int ending = metaInfo.validRegionEnd;
    if (ending < origin) {
      ending += GameConstants.SHARED_ARRAY_LENGTH;
    }
    if (ending == origin) { // no messages to read
      metaInfo.validRegionEnd = -1;
      metaInfo.validRegionStart = -1;
      return 0;
    }
//    System.out.println("Reading messages: " + metaInfo);
    int messages = 0;
    int lastAckdRound = received.isEmpty() ? 0 : getNthLastReceivedMessage(1).header.cyclicRoundNum;
    int maxRoundNum = Message.Header.toCyclicRound(rc.getRoundNum());
    if (maxRoundNum < lastAckdRound) maxRoundNum += Message.Header.ROUND_NUM_CYCLE_SIZE;
    while (origin < ending) {
      Message message = readMessageAt(origin % NUM_MESSAGING_INTS);
      if (message.header.within(lastAckdRound, maxRoundNum)) { // skip stale messages
        received.add(message);
        messages++;
      }
      origin += message.size();
    }
    return messages;
  }

  /**
   * read a message from the shared array
   *    ASSUMES - messageOrigin is the start of a VALID message
   * @param messageOrigin where the message starts
   * @return the read message
   */
  private Message readMessageAt(final int messageOrigin) {
//     assert messageOrigin < NUM_MESSAGING_INTS; // ensure that the message is within the messaging ints
    int headerInt = sharedBuffer[messageOrigin];
    Message.Header header;
    try {
      header = Message.Header.fromReadInt(headerInt);
    } catch (Exception e) {
      System.out.println("Failed to parse header!");
      System.out.println("Reading bounds: " + metaInfo);
      System.out.printf("Read at %d\n", messageOrigin);
      System.out.println("Header int: " + headerInt);
      throw e;
    }
    int[] information = new int[header.numInformationInts];
    for (int i = 0; i < header.numInformationInts; i++) {
      information[i] = sharedBuffer[(messageOrigin + i + 1) % NUM_MESSAGING_INTS];
    }
    return Message.fromHeaderAndInfo(header, information).setWriteInfo(new Message.WriteInfo(messageOrigin));
  }

  /**
   * add a message to the internal communicator queue to be sent on this turn
   * @param message the message to send at the end of this turn (end of robot turn, not the round)
   */
  public void enqueueMessage(Message message) {
    messageQueue.add(new QueuedMessage(message, rc.getRoundNum()));
  }

  /**
   * add a message to the internal communicator queue
   * @param message the message to send
   * @param roundToSend the round on which to send the message
   */
  public void enqueueMessage(Message message, int roundToSend) {
    assert message.header.fromRound(roundToSend); // ensure that future messages line up with themselves -- ensures proper retrieval
    messageQueue.add(new QueuedMessage(message, roundToSend));
  }

  /**
   * reschedule a message to be sent in some number of turns
   *    should NOT happen often
   * @param message the message to reschedule
   * @param roundsToDelay the number of turns to wait before sending the message again
   */
  public void rescheduleMessageByRounds(Message message, int roundsToDelay) {
    assert message.header.fromRound(rc.getRoundNum()); // ensure that the message was just attempted to be sent
    message.reschedule(roundsToDelay);
    enqueueMessage(message, rc.getRoundNum() + roundsToDelay);
  }

  /**
   * send all messages that should be sent by now
   * @throws GameActionException thrown if sendMessage fails
   */
  public void sendQueuedMessages() throws GameActionException {
    while (!messageQueue.isEmpty() && messageQueue.peek().roundToSend <= rc.getRoundNum()) {
      sendMessage(messageQueue.poll().message);
    }
  }

  /**
   * write a certain message to the shared array
   * starts message after validRegionEnd and bumps validRegionStart as needed if ints are overwritten
   * @param message the message to write
   * @throws GameActionException thrown if writing to array fails
   */
  private void sendMessage(Message message) throws GameActionException {
    if (intsWritten + message.size() > NUM_MESSAGING_INTS) { // will try to write more ints than available
      rescheduleMessageByRounds(message, 1);
      return;
    }
    int[] messageBits = message.toEncodedInts();
//    System.out.printf("SEND %s MESSAGE: %s\n", message.header.type, Arrays.toString(messageBits));
//    System.out.println(message.header);
    int origin = metaInfo.validRegionEnd;
    for (int messageChunk : messageBits) {
      origin = (origin + 1) % NUM_MESSAGING_INTS;
      if (origin == metaInfo.validRegionStart) { // about to overwrite the start!
        // reread that message and move validStart past that message!
//        System.out.printf("Shift valid region start for %s at %d\n", message.header.type, metaInfo.validRegionEnd+1);
//        System.out.println("From: " + metaInfo);
//        System.out.println("header: " + Message.Header.fromReadInt(sharedBuffer[origin]));
        metaInfo.validRegionStart += readMessageAt(origin).size();
        metaInfo.validRegionStart %= NUM_MESSAGING_INTS;
//        System.out.println("To  : " + metaInfo);
        // TODO: some criteria on deciding not to "evict" that info
      }
      rc.writeSharedArray(origin, messageChunk);
    }
    sentMessages.add(message);
    if (metaInfo.validRegionStart == -1) { // first message!
      metaInfo.validRegionStart = origin - message.header.numInformationInts;
    }
    metaInfo.validRegionEnd = origin;
    message.setWriteInfo(new Message.WriteInfo(Math.floorMod(origin - message.header.numInformationInts, NUM_MESSAGING_INTS)));
    intsWritten += message.size();
  }

  /**
   * updates the meta ints in the shared memory if needed
   *    comunicator is dirty
   *      any messages were written
   * @throws GameActionException if updating fails
   */
  public void updateMetaIntsIfNeeded() throws GameActionException {
    if (intsWritten > 0) {
      updateMetaInts();
    }
  }

  /**
   * write metaInfo to the end of the shared array
   * @throws GameActionException if writing fails
   */
  public void updateMetaInts() throws GameActionException {
    int[] metaInts = metaInfo.encode();
    for (int i = 0; i < NUM_META_INTS; i++) {
      if (metaInts[i] > 65535) {
        System.out.println("FAILED META UPDATE -- " + metaInfo + Arrays.toString(metaInts));
      }
      rc.writeSharedArray(META_INT_START + i, metaInts[i]);
    }
//    System.out.println("Update meta: " + metaInfo);
  }

  /**
   * get a message from the received message list
   * @param n how far back in the inbo to look
   * @return the message
   */
  public Message getNthLastReceivedMessage(int n) {
    return received.get(received.size() - n);
  }

  /**
   * returns true if the integer at the specified index matches the provided message header
   *    ASSUMES data has already been read into sharedBuffer
   * @param headerIndex the index to check
   * @param header the message metadata to verify
   * @return the sameness
   */
  public boolean headerMatches(int headerIndex, Message.Header header) {
//    System.out.println("Checking header at " + headerIndex + ": " + sharedBuffer[headerIndex] + " -- " + header.toInt());
    return sharedBuffer[headerIndex] == header.toInt();
  }

  /**
   * reads numInts integers starting at startIndex into an integer array and sends them back for use
   *    reads from ALREADY processed sharedBuffer
   *    loops around based on NUM_MESSAGING_BITS
   * @param startIndex where to start
   * @param numInts the number of ints to read
   * @return the array of read ints
   */
  public int[] readInts(int startIndex, int numInts) {
//    System.out.println("Read ints at " + startIndex + ": " + numInts);
    int[] ints = new int[numInts];
    for (int i = 0; i < numInts; i++) {
      ints[i] = sharedBuffer[(startIndex+i) % NUM_MESSAGING_INTS];
    }
    return ints;
  }

  /**
   * write a set of ints into the message buffer starting at the given index
   *    cycles indices based on NUM_MESSAGING_INTS
   * @param startIndex where to start writing
   * @param information the ints to write
   * @throws GameActionException if writing fails
   */
  public void writeInts(int startIndex, int[] information) throws GameActionException {
//    System.out.println("Write ints at " + startIndex + ": " + Arrays.toString(information));
    for (int i = 0; i < information.length; i++) {
      rc.writeSharedArray((startIndex + i) % NUM_MESSAGING_INTS, information[i]);
    }
  }

}
