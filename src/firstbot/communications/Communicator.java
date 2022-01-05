package firstbot.communications;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import firstbot.communications.messages.Message;
import firstbot.communications.messages.SingleIntMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * This class will have a variety of methods related to communications between robots
 * There should be a generic interface for sending messages and reading them
 * Every robot should have a communicator instantiated with its own RobotController
 *    (used to read/write to the shared array)
 */
public class Communicator {
  private final RobotController rc;

  private final PriorityQueue<QueuedMessage> messageQueue;
  private final List<Message> sentMessages;

  private final List<Message> received;

  public Communicator(RobotController rc) {
    this.rc = rc;
    messageQueue = new PriorityQueue<>();
    sentMessages = new ArrayList<>();
    received = new ArrayList<>();
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
   * @param message the message to write
   * @throws GameActionException thrown if writing to array fails
   */
  private void sendMessage(Message message) throws GameActionException {
    int[] messageBits = message.toEncodedInts();
    int origin = 0;
    for (int messageChunk : messageBits) {
      rc.writeSharedArray(origin++ % GameConstants.SHARED_ARRAY_LENGTH, messageChunk);
    }
    sentMessages.add(message);
  }

  public void readMessages() throws GameActionException {

  }

  public Message readMessageAt(int messageOrigin) throws GameActionException {
    int headerInt = rc.readSharedArray(messageOrigin);
    Message.Header header = Message.Header.fromReadInt(headerInt);
    if (!header.fromRound(rc.getRoundNum())) {
      return null;
    }
    int[] information = new int[header.numInformationInts];
    for (int i = 0; i < header.numInformationInts; i++) {
      information[i] = rc.readSharedArray(messageOrigin + i + 1);
    }
    switch (header.type) {
      case SINGLE_INT: return new SingleIntMessage(header, information);
      default: throw new RuntimeException("Cannot read message with invalid type!");
    }
  }


}
