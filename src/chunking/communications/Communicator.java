package chunking.communications;

import battlecode.common.*;
import chunking.communications.messages.Message;
import chunking.containers.FastQueue;
import chunking.utils.Cache;
import chunking.utils.Global;
import chunking.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class will have a variety of methods related to communications between robots
 * There should be a generic interface for sending messages and reading them
 * Every robot should have a communicator instantiated with its own RobotController
 *    (used to read/write to the shared array)
 */
public class Communicator {

  /**
   * information about a subchunk of the map
   *    lead presence, danger, etc
   */
  public static class ChunkInfo {
    public static final int NUM_CHUNK_INTS = Utils.NUM_MAP_CHUNKS / Utils.CHUNK_INFOS_PER_INT;
    public static final int CHUNK_INTS_START = GameConstants.SHARED_ARRAY_LENGTH - NUM_CHUNK_INTS;

    /**
     * next three encoded into 2 bits
     * 00 - unexplored
     * 01 - explored, no rss
     * 10 - explored, rss exist
     * 11 - explored, rss depleted
     */
//    boolean explored;
//    boolean hasResources;
//    boolean resourcesDepleted;
    public static final int EXPLORATION_AND_LEAD_MASK = 0b0011;
    public static final int EXPLORED_NO_RSS_MASK = 0b0001;
    public static final int EXPLORED_W_RSS_MASK = 0b0010;
    public static final int EXPLORED_RSS_DEPLETED_MASK = 0b0011;

    /**
     * indicates presence of dangerous enemies
     *    soldier, sage,
     *    watch tower,
     *    archon
     */
//    boolean hasDangerousUnits;
    public static final int DANGEROUS_UNIT_MASK = 0b1000;

    /**
     * indicates presence of non-dangerous enemies
     *    miner, builder
     */
//    boolean hasNonOffensiveUnits;
    public static final int NON_OFFENSIVE_UNIT_MASK = 0b0100;

    /**
     * checks if the chunk at a given index has dangerous units
     * @param chunkIndex the chunk to check
     * @return whether the chunk has dangerous units
     * @throws GameActionException if reading fails
     */
    public boolean chunkHasDangerousUnits(int chunkIndex) throws GameActionException {
      int sharedArrIndex = chunkIndex / Utils.CHUNK_INFOS_PER_INT + CHUNK_INTS_START;
      int chunkInfoInt = (Global.rc.readSharedArray(sharedArrIndex) >> ((chunkIndex % Utils.CHUNK_INFOS_PER_INT) >> 2)) & 0b1111;
      return (chunkInfoInt & ChunkInfo.DANGEROUS_UNIT_MASK) > 0;
    }
    public boolean chunkHasBeenExplored(int chunkIndex) throws GameActionException {
//      //System.out.println("checking chunk " + chunkIndex);
      int sharedArrIndex = chunkIndex / Utils.CHUNK_INFOS_PER_INT + CHUNK_INTS_START;
//      //System.out.println("sharedArrIndex: " + sharedArrIndex);
      int chunkInfoInt = (Global.rc.readSharedArray(sharedArrIndex) >> ((chunkIndex % Utils.CHUNK_INFOS_PER_INT) >> 2)) & 0b1111;
//      //System.out.println("chunkInfoInt: " + chunkInfoInt);
      return (chunkInfoInt & ChunkInfo.EXPLORATION_AND_LEAD_MASK) > 0;
    }
    public boolean chunkHasBeenExplored(MapLocation mapLoc) throws GameActionException {
//      //System.out.println("checking chunk " + chunkIndex);
      int chunkIndex = Utils.encodeLocationToChunkIndex(mapLoc);
      int sharedArrIndex = chunkIndex / Utils.CHUNK_INFOS_PER_INT + CHUNK_INTS_START;
//      //System.out.println("sharedArrIndex: " + sharedArrIndex);
      int chunkInfoInt = (Global.rc.readSharedArray(sharedArrIndex) >> ((chunkIndex % Utils.CHUNK_INFOS_PER_INT) >> 2)) & 0b1111;
//      //System.out.println("chunkInfoInt: " + chunkInfoInt);
      return (chunkInfoInt & ChunkInfo.EXPLORATION_AND_LEAD_MASK) > 0;
    }

    /**
     * gets the current chunk and returns the center of the closest unexplored chunk
     * @return the center of an unexplored chunk
     * @throws GameActionException if reading fails
     */
    public MapLocation centerOfClosestUnexploredChunk(MapLocation source) throws GameActionException {
      int myChunk = Utils.encodeLocationToChunkIndex(source);
      if (!chunkHasBeenExplored(myChunk)) return Utils.decodeChunkIndexToLocation(myChunk);
      for (Direction dir : Utils.directions) {
        int chunkToTest = myChunk + dir.dx + dir.dy * Cache.Permanent.NUM_HORIZONTAL_CHUNKS;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS == 0 && dir.dx < 0) continue;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS == Cache.Permanent.NUM_HORIZONTAL_CHUNKS - 1 && dir.dx > 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS == 0 && dir.dy < 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS == Cache.Permanent.NUM_VERTICAL_CHUNKS - 1 && dir.dy > 0) continue;
        if (!chunkHasBeenExplored(chunkToTest)) return Utils.decodeChunkIndexToLocation(chunkToTest);
      }
      for (Direction dir : Utils.directions) {
        int chunkToTest = myChunk + dir.dx*2 + dir.dy * Cache.Permanent.NUM_HORIZONTAL_CHUNKS;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS <= 1 && dir.dx < 0) continue;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS >= Cache.Permanent.NUM_HORIZONTAL_CHUNKS - 2 && dir.dx > 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS == 0 && dir.dy < 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS == Cache.Permanent.NUM_VERTICAL_CHUNKS - 1 && dir.dy > 0) continue;
        if (!chunkHasBeenExplored(chunkToTest)) return Utils.decodeChunkIndexToLocation(chunkToTest);
      }
      for (Direction dir : Utils.directions) {
        int chunkToTest = myChunk + dir.dx + dir.dy*2 * Cache.Permanent.NUM_HORIZONTAL_CHUNKS;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS == 0 && dir.dx < 0) continue;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS == Cache.Permanent.NUM_HORIZONTAL_CHUNKS - 1 && dir.dx > 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS <= 1 && dir.dy < 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS >= Cache.Permanent.NUM_VERTICAL_CHUNKS - 2 && dir.dy > 0) continue;
        if (!chunkHasBeenExplored(chunkToTest)) return Utils.decodeChunkIndexToLocation(chunkToTest);
      }
      for (Direction dir : Utils.directions) {
        int chunkToTest = myChunk + dir.dx*2 + dir.dy*2 * Cache.Permanent.NUM_HORIZONTAL_CHUNKS;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS <= 1 && dir.dx < 0) continue;
        if (myChunk % Cache.Permanent.NUM_HORIZONTAL_CHUNKS >= Cache.Permanent.NUM_HORIZONTAL_CHUNKS - 2 && dir.dx > 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS <= 1 && dir.dy < 0) continue;
        if (myChunk / Cache.Permanent.NUM_HORIZONTAL_CHUNKS >= Cache.Permanent.NUM_VERTICAL_CHUNKS - 2 && dir.dy > 0) continue;
        if (!chunkHasBeenExplored(chunkToTest)) return Utils.decodeChunkIndexToLocation(chunkToTest);
      }
      return null;
    }

    public void markExplored(MapLocation explorationTarget) throws GameActionException {
      int chunkToMark = Utils.encodeLocationToChunkIndex(explorationTarget);
      int sharedArrIndex = chunkToMark / Utils.CHUNK_INFOS_PER_INT + CHUNK_INTS_START;
      int existingChunkSetInfo = Global.rc.readSharedArray(sharedArrIndex);
      int shiftAmt = (chunkToMark % Utils.CHUNK_INFOS_PER_INT) >> 2;
//      int currentChunkInfo = (existingChunkSetInfo >> shiftAmt) & 0b1111;
      Global.rc.writeSharedArray(sharedArrIndex, existingChunkSetInfo | (EXPLORED_NO_RSS_MASK << shiftAmt));
    }
  }

  public static class MetaInfo {
    public static final int NUM_META_INTS = 1;
    public static final int META_INT_START = ChunkInfo.CHUNK_INTS_START - NUM_META_INTS;

    public static final int VALID_REGION_IND = META_INT_START;
    private int validRegionStart; // 0-62    -- 6 bits [15,10]
    private int validRegionEnd;   // 0-62    -- 6 bits [9,4]

    public static final int SYMMETRY_INFO_IND = VALID_REGION_IND;
    public Utils.MapSymmetry knownSymmetry; // determined by next three bools
    public Utils.MapSymmetry guessedSymmetry; // determined by next three bools
    private static final int NOT_HORIZ_MASK = 0b1000;
    public boolean notHorizontal;     // 0-1               -- 1 bit  [3]
    private static final int NOT_VERT_MASK = 0b100;
    public boolean notVertical;       // 0-1               -- 1 bit  [2]
    private static final int NOT_ROT_MASK = 0b10;
    public boolean notRotational;     // 0-1               -- 1 bit  [1]
    private static final int ALL_SYM_INFO_MASK = NOT_HORIZ_MASK|NOT_VERT_MASK|NOT_ROT_MASK;

    public boolean dirty;

    public MetaInfo() {
      knownSymmetry = null;
      guessedSymmetry = null;
      notHorizontal = false;
      notVertical = false;
      notRotational = false;

//      chunks = new ChunkInfo[Utils.NUM_MAP_CHUNKS];
//      chunks[0] = new ChunkInfo();
//      chunks[1] = new ChunkInfo();
//      chunks[2] = new ChunkInfo();
//      chunks[3] = new ChunkInfo();
//      chunks[4] = new ChunkInfo();
//      chunks[5] = new ChunkInfo();
//      chunks[6] = new ChunkInfo();
//      chunks[7] = new ChunkInfo();
//      chunks[8] = new ChunkInfo();
//      chunks[9] = new ChunkInfo();
//      chunks[10] = new ChunkInfo();
//      chunks[11] = new ChunkInfo();
//      chunks[12] = new ChunkInfo();
//      chunks[13] = new ChunkInfo();
//      chunks[14] = new ChunkInfo();
//      chunks[15] = new ChunkInfo();
//      chunks[16] = new ChunkInfo();
//      chunks[17] = new ChunkInfo();
//      chunks[18] = new ChunkInfo();
//      chunks[19] = new ChunkInfo();
//      chunks[20] = new ChunkInfo();
//      chunks[21] = new ChunkInfo();
//      chunks[22] = new ChunkInfo();
//      chunks[23] = new ChunkInfo();
//      chunks[24] = new ChunkInfo();
//      chunks[25] = new ChunkInfo();
//      chunks[26] = new ChunkInfo();
//      chunks[27] = new ChunkInfo();
//      chunks[28] = new ChunkInfo();
//      chunks[29] = new ChunkInfo();
//      chunks[30] = new ChunkInfo();
//      chunks[31] = new ChunkInfo();
//      chunks[32] = new ChunkInfo();
//      chunks[33] = new ChunkInfo();
//      chunks[34] = new ChunkInfo();
//      chunks[35] = new ChunkInfo();
//      chunks[36] = new ChunkInfo();
//      chunks[37] = new ChunkInfo();
//      chunks[38] = new ChunkInfo();
//      chunks[39] = new ChunkInfo();
//      chunks[40] = new ChunkInfo();
//      chunks[41] = new ChunkInfo();
//      chunks[42] = new ChunkInfo();
//      chunks[43] = new ChunkInfo();
//      chunks[44] = new ChunkInfo();
//      chunks[45] = new ChunkInfo();
//      chunks[46] = new ChunkInfo();
//      chunks[47] = new ChunkInfo();
//      chunks[48] = new ChunkInfo();
//      chunks[49] = new ChunkInfo();
//      chunks[50] = new ChunkInfo();
//      chunks[51] = new ChunkInfo();
//      chunks[52] = new ChunkInfo();
//      chunks[53] = new ChunkInfo();
//      chunks[54] = new ChunkInfo();
//      chunks[55] = new ChunkInfo();
//      chunks[56] = new ChunkInfo();
//      chunks[57] = new ChunkInfo();
//      chunks[58] = new ChunkInfo();
//      chunks[59] = new ChunkInfo();
//      chunks[60] = new ChunkInfo();
//      chunks[61] = new ChunkInfo();
//      chunks[62] = new ChunkInfo();
//      chunks[63] = new ChunkInfo();
//      chunks[64] = new ChunkInfo();
//      chunks[65] = new ChunkInfo();
//      chunks[66] = new ChunkInfo();
//      chunks[67] = new ChunkInfo();
//      chunks[68] = new ChunkInfo();
//      chunks[69] = new ChunkInfo();
//      chunks[70] = new ChunkInfo();
//      chunks[71] = new ChunkInfo();
//      chunks[72] = new ChunkInfo();
//      chunks[73] = new ChunkInfo();
//      chunks[74] = new ChunkInfo();
//      chunks[75] = new ChunkInfo();
//      chunks[76] = new ChunkInfo();
//      chunks[77] = new ChunkInfo();
//      chunks[78] = new ChunkInfo();
//      chunks[79] = new ChunkInfo();
//      chunks[80] = new ChunkInfo();
//      chunks[81] = new ChunkInfo();
//      chunks[82] = new ChunkInfo();
//      chunks[83] = new ChunkInfo();
//      chunks[84] = new ChunkInfo();
//      chunks[86] = new ChunkInfo();
//      chunks[87] = new ChunkInfo();
//      chunks[88] = new ChunkInfo();
//      chunks[89] = new ChunkInfo();
//      chunks[90] = new ChunkInfo();
//      chunks[91] = new ChunkInfo();
//      chunks[92] = new ChunkInfo();
//      chunks[93] = new ChunkInfo();
//      chunks[94] = new ChunkInfo();
//      chunks[95] = new ChunkInfo();
//      chunks[96] = new ChunkInfo();
//      chunks[97] = new ChunkInfo();
//      chunks[98] = new ChunkInfo();
//      chunks[99] = new ChunkInfo();

      dirty = false;
    }

    /**
     * update the communicators meta information based on the meta ints from the shared array
     * @throws GameActionException if reading fails
     */
    public void updateFromShared() throws GameActionException {
      int validRegion = Global.rc.readSharedArray(VALID_REGION_IND);
      validRegionStart = (validRegion >>> 10) & 63;
      validRegionEnd = (validRegion >>> 4) & 63;

      int symmetryInfo = validRegion; //Global.rc.readSharedArray(SYMMETRY_INFO_IND);
      knownSymmetry = Utils.commsSymmetryMap[(symmetryInfo & ALL_SYM_INFO_MASK) >> 1];
      notHorizontal = (symmetryInfo & NOT_HORIZ_MASK) > 0;
      notVertical = (symmetryInfo & NOT_VERT_MASK) > 0;
      notRotational = (symmetryInfo & NOT_ROT_MASK) > 0;
      guessedSymmetry = knownSymmetry != null ? knownSymmetry : Utils.commsSymmetryGuessMap[(symmetryInfo & ALL_SYM_INFO_MASK) >> 1];

      dirty = false;
    }

    /**
     * update validstart/validend from shared array
     * @throws GameActionException if reading fails
     */
    public void reloadValidRegion() throws GameActionException {
      int validRegion = Global.rc.readSharedArray(VALID_REGION_IND);
      validRegionStart = (validRegion >>> 10) & 63;
      validRegionEnd = (validRegion >>> 4) & 63;
    }

    /**
     * convert the meta information about the communication buffer into a set of ints
     *    also write the ints to the shared array
     * @return true if updated
     * @throws GameActionException if writing fails
     */
    public boolean encodeAndWrite() throws GameActionException {
      if (!dirty) return false;
      Global.rc.writeSharedArray(VALID_REGION_IND,
            validRegionStart << 10
          | validRegionEnd << 4
          | ((notHorizontal ? NOT_HORIZ_MASK : 0) | (notVertical ? NOT_VERT_MASK : 0) | (notRotational ? NOT_ROT_MASK : 0)));
      dirty = false;
      return true;
    }

    @Override
    public String toString() {
      return String.format("ValidComms[%d:%d]", validRegionStart, validRegionEnd);
    }

    /**
     * update the meta info on map symmetry to disallow the gievn type of symmetry
     *    also updates knownSymmetry if possible
     *    sets dirty flag as well
     * @param blockedSymmetry the symmetry that is no longer allowed
     * @throws GameActionException if writing fails
     */
    public void setSymmetryCantBe(Utils.MapSymmetry blockedSymmetry) throws GameActionException {
      switch (blockedSymmetry) {
        case HORIZONTAL:
          notHorizontal = true;
          break;
        case VERTICAL:
          notVertical = true;
          break;
        case ROTATIONAL:
          notRotational = true;
          break;
      }
      //System.out.println("Bot at " + Cache.PerTurn.CURRENT_LOCATION + " realized sym can't be " + blockedSymmetry);
      int index = ((notHorizontal ? NOT_HORIZ_MASK : 0) | (notVertical ? NOT_VERT_MASK : 0) | (notRotational ? NOT_ROT_MASK : 0)) >> 1;
      knownSymmetry = Utils.commsSymmetryMap[index];
      guessedSymmetry = Utils.commsSymmetryGuessMap[index];
      //System.out.println("symIndex: " + index + " known: " + knownSymmetry + " -- guess: " + guessedSymmetry);
      dirty = true;
      encodeAndWrite();
    }
  }

  private static final int MIN_BYTECODES_TO_SEND_MESSAGE = 1000;

  private final RobotController rc;
//  private final int[] sharedBuffer;

  public final MetaInfo metaInfo;
  public final ChunkInfo chunkInfo;

  private static final int NUM_MESSAGING_INTS = MetaInfo.META_INT_START;
  private final FastQueue<Message> messageQueue;
  private final List<Message> sentMessages;
  private final List<Message> received;

  public Communicator() {
    this.rc = Global.rc;
//    sharedBuffer = new int[NUM_MESSAGING_INTS];
    metaInfo = new MetaInfo();
    chunkInfo = new ChunkInfo();

    messageQueue = new FastQueue<>(10);
    sentMessages = new ArrayList<>(5);
    received = new ArrayList<>();
  }

  /**
   * reset the sharedBuffer with the contents of the entire shared array
   */
  private void reloadBuffer() throws GameActionException {
    Utils.startByteCodeCounting("readShared");
    metaInfo.updateFromShared();
//    int toUpdate = (metaInfo.validRegionEnd - metaInfo.validRegionStart + 1 + NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS;
//    int ind;
//    for (int i = 0; i < toUpdate; i++) {
//      ind = (metaInfo.validRegionStart+i) % NUM_MESSAGING_INTS;
//      sharedBuffer[ind] = rc.readSharedArray(ind);
//    }
    Utils.finishByteCodeCounting("readShared");
  }

  /**
   * clean out own stale messages if they are at the start of the valid region
   * @return if cleaned
   * @throws GameActionException if writing fails
   */
  public boolean cleanStaleMessages() throws GameActionException {
    if (!sentMessages.isEmpty()) {
//      if (rc.getRoundNum() == 1471) {
//        //System.out.println("bounds before cleaning: " + metaInfo);
//      }
      for (Message message : sentMessages) {
        if (message.writeInfo.startIndex == metaInfo.validRegionStart) {
          //System.out.println("CLEAN " + message.header.type + ": " + metaInfo.validRegionStart);
          Message last = sentMessages.get(sentMessages.size() - 1);
          metaInfo.validRegionStart = (last.writeInfo.startIndex + last.size()) % NUM_MESSAGING_INTS;
          if ((metaInfo.validRegionEnd + 1) % NUM_MESSAGING_INTS == metaInfo.validRegionStart) {
            metaInfo.validRegionEnd = metaInfo.validRegionStart = 0;
          }
          metaInfo.dirty = true;
          metaInfo.encodeAndWrite();
//          if (rc.getRoundNum() == 1471) {
//            //System.out.println("Cleaning " + (sentMessages.size() - sentMessages.indexOf(message)) + " messages!");
//            //System.out.println("Clearing messages! - starting from " + message.header.type + " on " + message.header.cyclicRoundNum + " at " + message.writeInfo.startIndex);
//            //System.out.println("Last message cleaned: " + last.header.type + " on " + message.header.cyclicRoundNum + " at " + last.writeInfo.startIndex);
//            //System.out.println("new bounds from cleaning: " + metaInfo);
//          }
//          //System.out.println("\ncleaned  - " + metaInfo);
          return true;
        }
      }
//      sentMessages.clear();
    }
    return false;
  }

  /**
   * read all the messages in the sharedArray
   *    Also forward these to the robot!
   * @return the number of messages that were read
   * @throws GameActionException thrown if readMessageAt fails
   */
  public int readAndAckAllMessages() throws GameActionException {
    Utils.startByteCodeCounting("reloadBuffer");
    reloadBuffer();
//    //System.out.println("update meta - " + Clock.getBytecodeNum());
//    if (rc.getRoundNum() == 1471) {
//      //System.out.println("Reading on round 582 -- " + metaInfo);
//      //System.out.println(Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd- metaInfo.validRegionStart+1)));
//    }
    Utils.finishByteCodeCounting("reloadBuffer");
//    //System.out.println("\nstarting - " + metaInfo);

    cleanStaleMessages(); // clean out stale bois
    sentMessages.clear();
//    //System.out.println("clean stale - " + Clock.getBytecodeNum());
    int origin = metaInfo.validRegionStart;
    int ending = metaInfo.validRegionEnd;
    if (ending < origin) {
      ending += NUM_MESSAGING_INTS;
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
//      //System.out.println("\nBefore  read/ack message: " + Clock.getBytecodeNum());
      Message message = readMessageAt(origin % NUM_MESSAGING_INTS);
//      if (message != null) {
//      if (message.header.withinCyclic(lastAckdRound, maxRoundNum)) { // skip stale messages
//      if (message.header.withinRounds(thisRound-2,thisRound)) { // skip stale messages
      Global.robot.ackMessage(message);
//      received.add(message);
        messages++;
//      }
        origin += message.size();
//      //System.out.println("\nCost to read/ack message: " + Clock.getBytecodeNum());
//      } else {
//        origin++;
//      }
    }
    return messages;
  }

  /**
   * read a message from the shared array
   *    ASSUMES - messageOrigin is the start of a VALID message
   * @param messageOrigin where the message starts
   * @return the read message
   */
  private Message readMessageAt(final int messageOrigin) throws GameActionException {
//     assert messageOrigin < NUM_MESSAGING_INTS; // ensure that the message is within the messaging ints
    int headerInt = Global.rc.readSharedArray(messageOrigin);//sharedBuffer[messageOrigin];
    Message.Header header = null;
    try {
//      int beforeReadHeader = Clock.getBytecodeNum();
      header = Message.Header.fromReadInt(headerInt);
//      //System.out.println("Cost to read header: " + (Clock.getBytecodeNum() - beforeReadHeader));
      header.validate();
    } catch (Exception e) {
      //System.out.println("Failed to parse header! at: " + messageOrigin);
      //System.out.println("Reading bounds: " + metaInfo);
      //System.out.println("ints: " + Arrays.toString(readInts(metaInfo.validRegionStart, (metaInfo.validRegionEnd-metaInfo.validRegionStart + 1 + NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS)));
      //System.out.println("Header int: " + headerInt);
      //System.out.println("Header: " + header);
//      return null;
      throw e;
    }

    switch (header.numInformationInts) {
      case 0:
        return Message.fromHeaderAndInfo0(header
          ).setWriteInfo(new Message.WriteInfo(messageOrigin));
      case 1:
        return Message.fromHeaderAndInfo1(header,
            Global.rc.readSharedArray((messageOrigin + 1) % NUM_MESSAGING_INTS)
          ).setWriteInfo(new Message.WriteInfo(messageOrigin));
      case 2:
        return Message.fromHeaderAndInfo2(header,
            Global.rc.readSharedArray((messageOrigin + 1) % NUM_MESSAGING_INTS),
            Global.rc.readSharedArray((messageOrigin + 2) % NUM_MESSAGING_INTS)
          ).setWriteInfo(new Message.WriteInfo(messageOrigin));
      default:
        throw new RuntimeException("Not enough cases for big message! - " + header);
    }

//    int beforeMakeInfo = Clock.getBytecodeNum();
//    int[] information = new int[header.numInformationInts];
//    for (int i = 0; i < header.numInformationInts; i++) {
//      information[i] = Global.rc.readSharedArray((messageOrigin + i + 1) % NUM_MESSAGING_INTS);// sharedBuffer[(messageOrigin + i + 1) % NUM_MESSAGING_INTS];
//    }
//    //System.out.println("Cost to make info arr: " + (Clock.getBytecodeNum() - beforeMakeInfo));
//    try {
//      return Message.fromHeaderAndInfo(header, information).setWriteInfo(new Message.WriteInfo(messageOrigin));
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
   * add a message to the internal communicator queue
   * @param message the message to send
   */
  public void enqueueMessage(Message message) {
    messageQueue.push(message);
  }

  /**
   * reschedule a message to be sent in some number of turns
   *    should NOT happen often
   * @param message the message to reschedule
   */
  public void rescheduleMessage(Message message) {
    enqueueMessage(message);
  }

  /**
   * send all messages that should be sent by now
   * @throws GameActionException thrown if sendMessage fails
   */
  public void sendQueuedMessages() throws GameActionException {
    while (!messageQueue.isEmpty() && sendMessage(messageQueue.popFront()));
  }

  /**
   * write a certain message to the shared array
   * starts message after validRegionEnd and bumps validRegionStart as needed if ints are overwritten
   * @param message the message to write
   * @returns true if sent, false if rescheduled
   * @throws GameActionException thrown if writing to array fails
   */
  private boolean sendMessage(Message message) throws GameActionException {
    metaInfo.reloadValidRegion();
    if (Clock.getBytecodesLeft() < MIN_BYTECODES_TO_SEND_MESSAGE
        || ((metaInfo.validRegionEnd-metaInfo.validRegionStart+NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS) + message.size() > NUM_MESSAGING_INTS) { // will try to write more ints than available
      rescheduleMessage(message);
      return false;
    }
    boolean updateStart = metaInfo.validRegionStart == metaInfo.validRegionEnd; // no valid messages currently
    int[] messageBits = message.toEncodedInts();
    int origin = metaInfo.validRegionEnd;
    int messageOrigin = (origin + 1) % NUM_MESSAGING_INTS;
    //System.out.printf("SEND  %s:\n%d - %s\n", message.header.type, messageOrigin, Arrays.toString(messageBits));
//    //System.out.println(message.header);
    for (int messageChunk : messageBits) {
      origin = (origin + 1) % NUM_MESSAGING_INTS;
      if (origin == metaInfo.validRegionStart) { // about to overwrite the start!
        Message messageAt = readMessageAt(origin);
        metaInfo.validRegionStart += messageAt != null ? messageAt.size() : 1;
        metaInfo.validRegionStart %= NUM_MESSAGING_INTS;
      }
//      //System.out.println("Write to shared " + origin + ": " + messageChunk);
      rc.writeSharedArray(origin, messageChunk);
    }
    sentMessages.add(message);
    //rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 0,255,0);
    metaInfo.validRegionEnd = origin;
    if (updateStart) { // first message!
      metaInfo.validRegionStart = origin - message.header.numInformationInts;
      if (metaInfo.validRegionStart < 0) metaInfo.validRegionStart += NUM_MESSAGING_INTS;
//      //System.out.println("Move start: " + metaInfo);
    }
    message.setWriteInfo(new Message.WriteInfo(messageOrigin));
    metaInfo.dirty = true;
    metaInfo.encodeAndWrite();
    return true;
  }

  /**
   * updates the meta ints in the shared memory if needed
   *    comunicator is dirty
   *      any messages were written
   * @return true if updated
   * @throws GameActionException if updating fails
   */
  public boolean updateMetaIntsIfNeeded() throws GameActionException {
//    //System.out.println("\nend turn - " + metaInfo);
    return metaInfo.encodeAndWrite();
  }

  /**
   * returns true if the integer at the specified index matches the provided message header
   *    ASSUMES data has already been read into sharedBuffer
   * @param headerIndex the index to check
   * @param header the message metadata to verify
   * @return the sameness
   * @throws GameActionException if reading fails
   */
  public boolean headerMatches(int headerIndex, Message.Header header) throws GameActionException {
//    //System.out.println("Checking header at " + headerIndex + ": " + sharedBuffer[headerIndex] + " -- " + header.toInt());
//    return sharedBuffer[headerIndex] == header.toInt();
    return Global.rc.readSharedArray(headerIndex) == header.toInt();
  }

  /**
   * reads numInts integers starting at startIndex into an integer array and sends them back for use
   *    reads from ALREADY processed sharedBuffer
   *    loops around based on NUM_MESSAGING_BITS
   * @param startIndex where to start
   * @param numInts the number of ints to read
   * @return the array of read ints
   * @throws GameActionException if reading fails
   */
  public int[] readInts(int startIndex, int numInts) throws GameActionException {
//    //System.out.println("Read ints at " + startIndex + ": " + numInts);
    int[] ints = new int[numInts];
    for (int i = 0; i < numInts; i++) {
      ints[i] = Global.rc.readSharedArray((startIndex+i) % NUM_MESSAGING_INTS);//sharedBuffer[(startIndex+i) % NUM_MESSAGING_INTS];
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
