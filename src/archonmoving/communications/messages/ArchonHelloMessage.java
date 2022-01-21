package archonmoving.communications.messages;

import battlecode.common.MapLocation;
import archonmoving.utils.Utils;

/**
 * A simple message with a single integer of information
 * primarily used for testing
 */
public class ArchonHelloMessage extends Message {
  public static final MessageType TYPE = MessageType.ARCHON_HELLO;
  public static final int MESSAGE_LENGTH = 1;

  private static final int HORIZ_MASK = 0b100;
  private static final int VERT_MASK = 0b10;
  private static final int ROT_MASK = 0b1;

  public MapLocation location;
  public boolean notHorizSym;
  public boolean notVertSym;
  public boolean notRotSym;

  public ArchonHelloMessage(MapLocation location, boolean notHorizSym, boolean notVertSym, boolean notRotSym) {
    super(TYPE);
    this.location = location;
    this.notHorizSym =  notHorizSym;
    this.notVertSym = notVertSym;
    this.notRotSym = notRotSym;
  }

  public ArchonHelloMessage(Header header, int information) {
    super(header);
    this.location = Utils.decodeLocation(information);
    this.decodeSymmetryData(information);
  }

  public int[] toEncodedInts() {
    return new int[]{getHeaderInt(), Utils.encodeLocation(location) | encodeSymmetryData()};
  }

  private int encodeSymmetryData() {
    return (notHorizSym ? HORIZ_MASK : 0) | (notVertSym ? VERT_MASK : 0) | (notRotSym ? ROT_MASK : 0);
  }

  private void decodeSymmetryData(int encoded) {
    this.notHorizSym = (encoded & HORIZ_MASK) > 0;
    this.notVertSym  = (encoded & VERT_MASK)  > 0;
    this.notRotSym   = (encoded & ROT_MASK)   > 0;
  }

}
