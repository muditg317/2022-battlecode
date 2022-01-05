package firstbot.communications.messages;

/**
 * A simple message with a single integer of information
 * primarily used for testing
 */
public class SingleIntMessage extends Message {
  public static final MessageType TYPE = MessageType.SINGLE_INT;
  public int data;

  public SingleIntMessage(int priority, int datum, int roundNum) {
    super(priority, TYPE, 1, roundNum);
    this.data = datum;
  }

  public SingleIntMessage(Header header, int[] information) {
    super(header);
    this.data = information[0];
  }

  public SingleIntMessage(int datum, int roundNum) {
    this(0, datum, roundNum);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), data};
  }
}
