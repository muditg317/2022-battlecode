package firstbot.communications.messages;

import battlecode.common.GameConstants;

/**
 * All message types should subclass this class
 * A Communicator can send and read Message instances from the shared array
 */
public abstract class Message {

  /**
   * enum for the different message types that will be sent
   */
  public enum MessageType {
    SINGLE_INT, LEAD_FOUND
  }

  /**
   * header information present for any message
   */
  public static class Header {
    private static final int TOTAL_BITS_PER_INT = 16;

    private static final int PRIORITY_SIZE = 2;
    private static final int PRIORITY_START = TOTAL_BITS_PER_INT - PRIORITY_SIZE;
    private static final int PRIORITY_MAX = (1 << PRIORITY_SIZE) - 1;
    public final int priority; // 0-3             -- 2 bits [15,14]

    private static final int TYPE_SIZE = 3;
    private static final int TYPE_START = PRIORITY_START - TYPE_SIZE;
    private static final int TYPE_MAX = (1 << TYPE_SIZE) - 1;
    public final MessageType type; // 0-7         -- 3 bits [13,11]

    private static final int NUM_INTS_SIZE = 6;
    private static final int NUM_INTS_START = TYPE_START - NUM_INTS_SIZE;
    private static final int NUM_INTS_MAX = (1 << NUM_INTS_SIZE) - 1;
    public final int numInformationInts; // 0-63  -- 6 bits [10,5]

    private static final int ROUND_NUM_SIZE = 5;
    private static final int ROUND_NUM_START = NUM_INTS_START - ROUND_NUM_SIZE;
    private static final int ROUND_NUM_MAX = (1 << ROUND_NUM_SIZE) - 1;
    private static final int ROUND_NUM_CYCLE_SIZE = ROUND_NUM_MAX + 1;
    public int cyclicRoundNum; // 0-31            -- 5 bits [4,0]

    public Header(int priority, MessageType type, int numInformationInts, int roundNum) {
      this.priority = priority;
      this.type = type;
      this.numInformationInts = numInformationInts;
      this.cyclicRoundNum = roundNum % ROUND_NUM_CYCLE_SIZE;
    }

    public static Header fromReadInt(int readInt) {
      return new Header(
          (readInt >>> PRIORITY_START) & PRIORITY_MAX,
          MessageType.values()[(readInt >>> TYPE_START) & TYPE_MAX],
          (readInt >>> NUM_INTS_START) & NUM_INTS_MAX,
          (readInt >>> ROUND_NUM_START) & ROUND_NUM_MAX);
    }

    public int toInt() {
      return
            priority << PRIORITY_START
          | type.ordinal() << TYPE_START
          | numInformationInts << NUM_INTS_START
          | cyclicRoundNum << ROUND_NUM_START;
    }

    public void rescheduleBy(int roundsToDelay) {
      cyclicRoundNum = (cyclicRoundNum + roundsToDelay) % ROUND_NUM_CYCLE_SIZE;
    }

    public boolean fromRound(int roundNum) {
      return cyclicRoundNum == roundNum % (ROUND_NUM_MAX+1);
    }

    @Override
    public String toString() {
      return String.format("MessHdr{pr=%d,tp=%s,len=%d,rnd=%d", priority, type, numInformationInts, cyclicRoundNum);
    }
  }

  /**
   * the header for this message
   */
  public Header header;

  /**
   * create a message with the given header
   * @param header the message meta-information
   */
  public Message(Header header) {
    this.header = header;
  }

  /**
   * create a message with the given meta-information
   * @param priority message priority
   * @param type message type (from the MessageType enum)
   * @param numInformationInts how many ints of information are needed for this message
   * @param roundNum the round on which this message is/was/will be sent
   */
  public Message(int priority, MessageType type, int numInformationInts, int roundNum) {
    this(new Header(priority, type, numInformationInts, roundNum));
  }

  public static Message fromHeaderAndInfo(Header header, int[] information) {
    switch (header.type) {
      case SINGLE_INT: return new SingleIntMessage(header, information);
      case LEAD_FOUND: return new LeadFoundMessage(header, information);
      default: throw new RuntimeException("Cannot read message with invalid type! " + header.type);
    }
  }

  /**
   * convert the header of this message to an encoded integer
   * @return the encoded header
   */
  protected int getHeaderInt() {
    return header.toInt();
  }

  /**
   * convert the header+information of this message into ints
   * @return the complete message encoding - length == header.numInformationBits+1
   */
  public abstract int[] toEncodedInts();

  public void reschedule(int roundsToDelay) {
    header.rescheduleBy(roundsToDelay);
  }

  public int size() {
    return header.numInformationInts + 1;
  }
}
