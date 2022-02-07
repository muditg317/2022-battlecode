package soldiermacro.communications.messages;

//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;

/**
 * All message types should subclass this class
 * A Communicator can send and read Message instances from the shared array
 */
public abstract class Message {

  /**
   * enum for the different message types that will be sent
   * MAX OF 8 types
   */
  public enum MessageType {
    ARCHON_HELLO(ArchonHelloMessage.MESSAGE_LENGTH),
    LEAD_FOUND(LeadFoundMessage.MESSAGE_LENGTH),
    LEAD_REQUEST(LeadRequestMessage.MESSAGE_LENGTH),
    START_RAID(StartRaidMessage.MESSAGE_LENGTH),
    END_FIGHT(EndFightMessage.MESSAGE_LENGTH),
    SAVE_ME(SaveMeMessage.MESSAGE_LENGTH),
    ARCHON_SAVED(ArchonSavedMessage.MESSAGE_LENGTH),
    RUBBLE_AT_LOCATION(RubbleAtLocationMessage.MESSAGE_LENGTH),
    JOIN_THE_FIGHT(JoinTheFightMessage.MESSAGE_LENGTH),
    ENEMY_FOUND(EnemyFoundMessage.MESSAGE_LENGTH);

    public final int standardSize;
    public final int ordinal;

    public static final MessageType[] values = MessageType.values();

    MessageType(int standardSize) {
      this.standardSize = standardSize;
      this.ordinal = ordinal();
    }
  }

  /**
   * header information present for any message
   */
  public static class Header {
    private static final int TOTAL_BITS_PER_INT = 16;

    private static final int PRIORITY_SIZE = 0;
    private static final int PRIORITY_START = TOTAL_BITS_PER_INT - PRIORITY_SIZE;
    private static final int PRIORITY_MAX = (1 << PRIORITY_SIZE) - 1;
//    public final int priority; // 0-3             -- 2 bits [15,14]

    private static final int TYPE_SIZE = 4;
    private static final int TYPE_START = PRIORITY_START - TYPE_SIZE;
    private static final int TYPE_MAX = (1 << TYPE_SIZE) - 1;
    public final MessageType type; // 0-7         -- 3 bits [13,11]

    private static final int NUM_INTS_SIZE = 0;
    private static final int NUM_INTS_START = TYPE_START - NUM_INTS_SIZE;
    private static final int NUM_INTS_MAX = (1 << NUM_INTS_SIZE) - 1;
//    public final int numInformationInts; // 0-63  -- 6 bits [10,5]

    private static final int ROUND_NUM_SIZE = 0;
    private static final int ROUND_NUM_START = NUM_INTS_START - ROUND_NUM_SIZE;
    private static final int ROUND_NUM_MAX = (1 << ROUND_NUM_SIZE) - 1;
    public static final int ROUND_NUM_CYCLE_SIZE = ROUND_NUM_MAX + 1;
//    public int cyclicRoundNum; // 0-31            -- 5 bits [4,0]

    public Header(MessageType type, int numInformationInts) {
      this.type = type;
//      this.numInformationInts = numInformationInts;
    }

    public static Header fromReadInt(int readInt) {
      return new Header(
          MessageType.values[(readInt >>> TYPE_START) & TYPE_MAX],
          (readInt >>> NUM_INTS_START) & NUM_INTS_MAX
          );
    }

    public int toInt() {
      return
          type.ordinal << TYPE_START
//          | numInformationInts << NUM_INTS_START
          ;
    }
    @Override
    public String toString() {
      return String.format("MsgHdr{%s}", type);
    }

    public void validate() {
//      if (type.standardSize != -1 && type.standardSize != numInformationInts) {
//        throw new RuntimeException("Invalid message header!");
//      }
    }
  }

  /**
   * information related to the writing of this message in the shared buffer
   */
  public static class WriteInfo {
    public int startIndex;

    public WriteInfo(int startIndex) {
      this.startIndex = startIndex;
    }
  }

  /**
   * the header for this message
   */
  public Header header;

  /**
   * the write information for this message
   */
  public WriteInfo writeInfo;

  /**
   * create a message with the given header
   * @param header the message meta-information
   */
  public Message(Header header) {
    this.header = header;
    this.writeInfo = null;
  }

  /**
   * create a message with the given meta-information
   * @param type message type (from the MessageType enum)
   * @param numInformationInts how many ints of information are needed for this message
   */
  public Message(MessageType type, int numInformationInts) {
    this(new Header(type, numInformationInts));
  }


  public static Message fromHeaderAndInfo0(Header header, int headerInt) {
    switch (header.type) {
      case ENEMY_FOUND: return new EnemyFoundMessage(header, headerInt);
      default: throw new RuntimeException("Provided message type has length != 0 : " + header.type);
    }
  }

  public static Message fromHeaderAndInfo1(Header header, int information) {
    switch (header.type) {
      case ARCHON_HELLO: return new ArchonHelloMessage(header, information);
      case LEAD_FOUND: return new LeadFoundMessage(header, information);
      case START_RAID: return new StartRaidMessage(header, information);
      case END_FIGHT: return new EndFightMessage(header, information);
      case SAVE_ME: return new SaveMeMessage(header, information);
      case ARCHON_SAVED: return new ArchonSavedMessage(header, information);
      case JOIN_THE_FIGHT: return new JoinTheFightMessage(header, information);
      default: throw new RuntimeException("Provided message type has length != 1 : " + header.type);
    }
  }

  public static Message fromHeaderAndInfo2(Header header, int information, int information2) {
    switch (header.type) {
      case LEAD_REQUEST: return new LeadRequestMessage(header, information, information2);
      case RUBBLE_AT_LOCATION: return new RubbleAtLocationMessage(header, information, information2);
      default: throw new RuntimeException("Provided message type has length != 2 : " + header.type);
    }
  }

  public Message setWriteInfo(WriteInfo writeInfo) {
    this.writeInfo = writeInfo;
    return this;
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

//  public void reschedule(int roundsToDelay) {
////    header.rescheduleBy(roundsToDelay);
//  }

  public int size() {
    return header.type.standardSize + 1;
  }
}
