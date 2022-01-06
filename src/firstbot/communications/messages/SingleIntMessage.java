package firstbot.communications.messages;

/**
 * A simple message with a single integer of information
 * primarily used for testing
 */
public class SingleIntMessage extends Message {
  public static final int PRIORITY = 0;
  public static final MessageType TYPE = MessageType.SINGLE_INT;
  public static final int MESSAGE_LENGTH = 1;

  public int data;

  public SingleIntMessage(int priority, int datum, int roundNum) {
    super(priority, TYPE, MESSAGE_LENGTH, roundNum);
    this.data = datum;
  }

  public SingleIntMessage(int datum, int roundNum) {
    this(PRIORITY, datum, roundNum);
  }

  public SingleIntMessage(Header header, int[] information) {
    super(header);
    this.data = information[0];
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), data};
  }
}
