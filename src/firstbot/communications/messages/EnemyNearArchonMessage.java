package firstbot.communications.messages;

/**
 * A message used when a wandering miner finds a good mining position
 */
public class EnemyNearArchonMessage extends Message {
  public static final MessageType TYPE = MessageType.ENEMY_NEAR_ARCHON;
  public static final int MESSAGE_LENGTH = 0;

  public EnemyNearArchonMessage() {
    super(TYPE);
  }

  public EnemyNearArchonMessage(Header header, int headerInt) {
    super(header);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt()};
  }
}
