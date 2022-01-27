package firstbot.communications.messages;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class BuilderSpawnedMessage extends Message {
  public static final MessageType TYPE = MessageType.BUILDER_SPAWNED;
  public static final int MESSAGE_LENGTH = 0;

  public BuilderSpawnedMessage() {
    super(TYPE);
  }

  public BuilderSpawnedMessage(Header header, int headerInt) {
    super(header);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt()};
  }
}
