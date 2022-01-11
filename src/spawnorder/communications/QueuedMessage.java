package spawnorder.communications;

import spawnorder.communications.messages.Message;

public class QueuedMessage implements Comparable<QueuedMessage> {
  public Message message;
  public int roundToSend;

  public QueuedMessage(Message message, int roundToSend) {
    this.message = message;
    this.roundToSend = roundToSend;
  }

  @Override
  public int compareTo(QueuedMessage o) {
    return this.roundToSend - o.roundToSend;
  }
}
