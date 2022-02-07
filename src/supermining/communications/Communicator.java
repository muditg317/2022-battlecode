package supermining.communications;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import supermining.utils.Global;
import supermining.utils.Utils;
import supermining.communications.messages.Message;

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

    public MetaInfo() {
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

  public Communicator() {
    this.rc = Global.rc;
    sharedBuffer = new int[GameConstants.SHARED_ARRAY_LENGTH];
    metaInfo = new MetaInfo();

    messageQueue = new PriorityQueue<>();
    sentMessages = new ArrayList<>();
    received = new ArrayList<>();

    intsWritten = 0;
  }

  /**
   * reset the sharedBuffer with the contents of the entire shared array
   */
  private void reloadBuffer() throws GameActionException {
    Utils.startByteCodeCounting("readShared");
    // TODO: better logic for reading to the internal buffer because that boi is getting messed up
//    for (int i = 0; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
//      sharedBuffer[i] = rc.readSharedArray(i);
//    }
    {
      sharedBuffer[0] = rc.readSharedArray(0);
      sharedBuffer[1] = rc.readSharedArray(1);
      sharedBuffer[2] = rc.readSharedArray(2);
      sharedBuffer[3] = rc.readSharedArray(3);
      sharedBuffer[4] = rc.readSharedArray(4);
      sharedBuffer[5] = rc.readSharedArray(5);
      sharedBuffer[6] = rc.readSharedArray(6);
      sharedBuffer[7] = rc.readSharedArray(7);
      sharedBuffer[8] = rc.readSharedArray(8);
      sharedBuffer[9] = rc.readSharedArray(9);
      sharedBuffer[10] = rc.readSharedArray(10);
      sharedBuffer[11] = rc.readSharedArray(11);
      sharedBuffer[12] = rc.readSharedArray(12);
      sharedBuffer[13] = rc.readSharedArray(13);
      sharedBuffer[14] = rc.readSharedArray(14);
      sharedBuffer[15] = rc.readSharedArray(15);
      sharedBuffer[16] = rc.readSharedArray(16);
      sharedBuffer[17] = rc.readSharedArray(17);
      sharedBuffer[18] = rc.readSharedArray(18);
      sharedBuffer[19] = rc.readSharedArray(19);
      sharedBuffer[20] = rc.readSharedArray(20);
      sharedBuffer[21] = rc.readSharedArray(21);
      sharedBuffer[22] = rc.readSharedArray(22);
      sharedBuffer[23] = rc.readSharedArray(23);
      sharedBuffer[24] = rc.readSharedArray(24);
      sharedBuffer[25] = rc.readSharedArray(25);
      sharedBuffer[26] = rc.readSharedArray(26);
      sharedBuffer[27] = rc.readSharedArray(27);
      sharedBuffer[28] = rc.readSharedArray(28);
      sharedBuffer[29] = rc.readSharedArray(29);
      sharedBuffer[30] = rc.readSharedArray(30);
      sharedBuffer[31] = rc.readSharedArray(31);
      sharedBuffer[32] = rc.readSharedArray(32);
      sharedBuffer[33] = rc.readSharedArray(33);
      sharedBuffer[34] = rc.readSharedArray(34);
      sharedBuffer[35] = rc.readSharedArray(35);
      sharedBuffer[36] = rc.readSharedArray(36);
      sharedBuffer[37] = rc.readSharedArray(37);
      sharedBuffer[38] = rc.readSharedArray(38);
      sharedBuffer[39] = rc.readSharedArray(39);
      sharedBuffer[40] = rc.readSharedArray(40);
      sharedBuffer[41] = rc.readSharedArray(41);
      sharedBuffer[42] = rc.readSharedArray(42);
      sharedBuffer[43] = rc.readSharedArray(43);
      sharedBuffer[44] = rc.readSharedArray(44);
      sharedBuffer[45] = rc.readSharedArray(45);
      sharedBuffer[46] = rc.readSharedArray(46);
      sharedBuffer[47] = rc.readSharedArray(47);
      sharedBuffer[48] = rc.readSharedArray(48);
      sharedBuffer[49] = rc.readSharedArray(49);
      sharedBuffer[50] = rc.readSharedArray(50);
      sharedBuffer[51] = rc.readSharedArray(51);
      sharedBuffer[52] = rc.readSharedArray(52);
      sharedBuffer[53] = rc.readSharedArray(53);
      sharedBuffer[54] = rc.readSharedArray(54);
      sharedBuffer[55] = rc.readSharedArray(55);
      sharedBuffer[56] = rc.readSharedArray(56);
      sharedBuffer[57] = rc.readSharedArray(57);
      sharedBuffer[58] = rc.readSharedArray(58);
      sharedBuffer[59] = rc.readSharedArray(59);
      sharedBuffer[60] = rc.readSharedArray(60);
      sharedBuffer[61] = rc.readSharedArray(61);
      sharedBuffer[62] = rc.readSharedArray(62);
      sharedBuffer[63] = rc.readSharedArray(63);
    }
    Utils.finishByteCodeCounting("readShared");
    metaInfo.updateFromBuffer(sharedBuffer);
    intsWritten = 0;
  }

  /**
   * clean out own stale messages if they are at the start of the valid region
   * @return if cleaned
   */
  public boolean cleanStaleMessages() {
    if (!sentMessages.isEmpty()) {
//      if (rc.getType() == RobotType.ARCHON || rc.getRoundNum() == 632) {
//        //System.out.println("bounds before cleaning: " + metaInfo);
//      }
      for (Message message : sentMessages) {
        if (message.writeInfo.startIndex == metaInfo.validRegionStart) {
          Message last = sentMessages.get(sentMessages.size() - 1);
          metaInfo.validRegionStart = (last.writeInfo.startIndex + last.header.numInformationInts + 1) % NUM_MESSAGING_INTS;
          if ((metaInfo.validRegionEnd + 1) % NUM_MESSAGING_INTS == metaInfo.validRegionStart) {
            metaInfo.validRegionEnd = metaInfo.validRegionStart;
          }
          intsWritten++;
//          if (rc.getType() == RobotType.ARCHON || rc.getRoundNum() == 632) {
//            //System.out.println("Cleaning " + (sentMessages.size() - sentMessages.indexOf(message)) + " messages!");
//            //System.out.println("Clearing messages! - starting from " + message.header.type + " on " + message.header.cyclicRoundNum + " at " + message.writeInfo.startIndex);
//            //System.out.println("Last message cleaned: " + last.header.type + " on " + message.header.cyclicRoundNum + " at " + last.writeInfo.startIndex);
//            //System.out.println("new bounds from cleaning: " + metaInfo);
//          }
          return true;
        }
      }
//      sentMessages.clear();
    }
    return false;
  }

  /**
   * read all the messages in the sharedArray
   * @return the number of messages that were read
   * @throws GameActionException thrown if readMessageAt fails
   */
  public int readMessages() throws GameActionException {
    Utils.startByteCodeCounting("reloadBuffer");
    reloadBuffer();
//    if (rc.getRoundNum() == 582) {
//      //System.out.println("Reading on round 582 -- " + metaInfo);
//      //System.out.println(Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd- metaInfo.validRegionStart+1)));
//    }
    Utils.finishByteCodeCounting("reloadBuffer");
    cleanStaleMessages(); // clean out stale bois
    sentMessages.clear();
    int origin = metaInfo.validRegionStart;
    int ending = metaInfo.validRegionEnd;
    if (ending < origin) {
      ending += GameConstants.SHARED_ARRAY_LENGTH;
    }
    if (ending == origin) { // no messages to read
      return 0;
    }
//    //System.out.println("Reading messages: " + metaInfo);
    int messages = 0;
//    int lastAckdRound = received.isEmpty() ? 0 : getNthLastReceivedMessage(1).header.cyclicRoundNum;
//    if (!received.isEmpty()) {
//      Message last = getNthLastReceivedMessage(1);
//      //System.out.println("last message: " + last.header.type + "\t -- ");
//    }
//    int maxRoundNum = Message.Header.toCyclicRound(rc.getRoundNum());
//    if (maxRoundNum < lastAckdRound) maxRoundNum += Message.Header.ROUND_NUM_CYCLE_SIZE;
//    //System.out.println("ack messages within: (" + lastAckdRound + ", " + maxRoundNum + "]");
//    int thisRound = rc.getRoundNum();
    while (origin < ending) {
      Message message = readMessageAt(origin % NUM_MESSAGING_INTS);
//      if (message.header.withinCyclic(lastAckdRound, maxRoundNum)) { // skip stale messages
//      if (message.header.withinRounds(thisRound-2,thisRound)) { // skip stale messages
        received.add(message);
        messages++;
//      }
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
    Message.Header header = null;
    try {
      header = Message.Header.fromReadInt(headerInt);
      header.validate();
    } catch (Exception e) {
      //System.out.println("Failed to parse header! " + header);
      //System.out.println("Reading bounds: " + metaInfo);
      //System.out.println("ints: " + Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd-metaInfo.validRegionStart + 1)));
      //System.out.printf("Read at %d\n", messageOrigin);
      //System.out.println("Header int: " + headerInt);
      throw e;
    }
    int[] information = new int[header.numInformationInts];
    for (int i = 0; i < header.numInformationInts; i++) {
      information[i] = sharedBuffer[(messageOrigin + i + 1) % NUM_MESSAGING_INTS];
    }
//    try {
      return Message.fromHeaderAndInfo(header, information).setWriteInfo(new Message.WriteInfo(messageOrigin));
//    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
//      //System.out.println("Message instantiation failed!");
//      //System.out.println("Reading bounds: " + metaInfo);
//      //System.out.println("ints: " + Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd-metaInfo.validRegionStart + 1)));
//      //System.out.printf("Read at %d\n", messageOrigin);
//      //System.out.println("Header int: " + headerInt);
//      throw new RuntimeException("Failed to initialize message", e);
//    }
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
//    if (start within where i need to write) { check priority
    boolean updateStart = metaInfo.validRegionStart == metaInfo.validRegionEnd; // no valid messages currently
    int[] messageBits = message.toEncodedInts();
    //System.out.printf("SEND %s MESSAGE:\n%d - %s\n", message.header.type, metaInfo.validRegionEnd+1, Arrays.toString(messageBits));
//    //System.out.println(message.header);
    int origin = metaInfo.validRegionEnd;
    int messageOrigin = (origin + 1) % NUM_MESSAGING_INTS;
    for (int messageChunk : messageBits) {
      origin = (origin + 1) % NUM_MESSAGING_INTS;
      if (origin == metaInfo.validRegionStart) { // about to overwrite the start!
        // reread that message and move validStart past that message!
//        //System.out.printf("Shift valid region start for %s at %d\n", message.header.type, metaInfo.validRegionEnd+1);
//        //System.out.println("From: " + metaInfo);
//        //System.out.println("header: " + Message.Header.fromReadInt(sharedBuffer[origin]));
        metaInfo.validRegionStart += readMessageAt(origin).size();
        metaInfo.validRegionStart %= NUM_MESSAGING_INTS;
//        //System.out.println("To  : " + metaInfo);
        // TODO: some criteria on deciding not to "evict" that info
      }
//      //System.out.println("Write to shared " + origin + ": " + messageChunk);
      rc.writeSharedArray(origin, messageChunk);
    }
    sentMessages.add(message);
    //rc.setIndicatorDot(rc.getLocation(), 0,255,0);
    metaInfo.validRegionEnd = origin;
    if (updateStart) { // first message!
      metaInfo.validRegionStart = origin - message.header.numInformationInts;
      if (metaInfo.validRegionStart < 0) metaInfo.validRegionStart += NUM_MESSAGING_INTS;
//      //System.out.println("Move start: " + metaInfo);
    }
    message.setWriteInfo(new Message.WriteInfo(messageOrigin));
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
//    if (rc.getRoundNum() == 632) {
//      //System.out.println("Update meta: " + metaInfo + " -- " + Arrays.toString(metaInts));
//      //System.out.println("pre ints: " + Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd - metaInfo.validRegionStart + 1)));
//      for (int i = metaInfo.validRegionStart; i <= metaInfo.validRegionEnd; i++) {
//        //System.out.println("post: " + rc.readSharedArray(i));
//      }
//    }
    for (int i = 0; i < NUM_META_INTS; i++) {
      if (metaInts[i] < 0 || metaInts[i] > GameConstants.MAX_SHARED_ARRAY_VALUE) {
        //System.out.println("FAILED META UPDATE -- " + metaInfo + Arrays.toString(metaInts));
      }
      rc.writeSharedArray(META_INT_START + i, metaInts[i]);
    }
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
//    //System.out.println("Checking header at " + headerIndex + ": " + sharedBuffer[headerIndex] + " -- " + header.toInt());
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
//    //System.out.println("Read ints at " + startIndex + ": " + numInts);
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
//    //System.out.println("Write ints at " + startIndex + ": " + Arrays.toString(information));
    for (int i = 0; i < information.length; i++) {
      rc.writeSharedArray((startIndex + i) % NUM_MESSAGING_INTS, information[i]);
    }
  }

}
