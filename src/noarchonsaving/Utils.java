package noarchonsaving;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Utils {

  /** Seeded RNG for use throughout the bot classes */
  public static final Random rng = new Random(69);

  /** Array of the 8 possible directions. */
  public static final Direction[] directions = {
      Direction.NORTH,
      Direction.NORTHEAST,
      Direction.EAST,
      Direction.SOUTHEAST,
      Direction.SOUTH,
      Direction.SOUTHWEST,
      Direction.WEST,
      Direction.NORTHWEST,
  };

  public static Direction randomDirection() {
    return directions[rng.nextInt(directions.length)];
  }

  /**
   * encode the location into an integer where
   *  bits 15-10 : x
   *  bits  9-4  : y
   *  bits  3-0  : free
   * @param location the location to encode
   * @return the encoded location as int
   */
  public static int encodeLocation(MapLocation location) {
    return (location.x << 10) | (location.y << 4);
  }

  /**
   * decode a location from the provided integer (encoding described above)
   * @param encoded the integer to extract location from
   * @return the decoded location
   */
  public static MapLocation decodeLocation(int encoded) {
    return new MapLocation((encoded >> 10) & 0x3f, (encoded >> 4) & 0x3f);
  }

  /**
   * flip the x component of a direction
   * @param toFlip the direction to slip x
   * @return the x-flipped direction
   */
  public static Direction flipDirX(Direction toFlip) {
    switch (toFlip) {
      case NORTH: return toFlip;
      case NORTHEAST: return Direction.NORTHWEST;
      case EAST: return Direction.WEST;
      case SOUTHEAST: return Direction.SOUTHWEST;
      case SOUTH: return toFlip;
      case SOUTHWEST: return Direction.SOUTHEAST;
      case WEST: return Direction.EAST;
      case NORTHWEST: return Direction.NORTHEAST;
      case CENTER: return Direction.CENTER;
    }
    throw new RuntimeException("Cannot flip x of invalid Direction! " + toFlip);
  }

  /**
   * flip the y component of a direction
   * @param toFlip the direction to slip y
   * @return the y-flipped direction
   */
  public static Direction flipDirY(Direction toFlip) {
    switch (toFlip) {
      case NORTH: return Direction.SOUTH;
      case NORTHEAST: return Direction.SOUTHEAST;
      case EAST: return Direction.EAST;
      case SOUTHEAST: return Direction.NORTHEAST;
      case SOUTH: return Direction.NORTH;
      case SOUTHWEST: return Direction.NORTHWEST;
      case WEST: return Direction.WEST;
      case NORTHWEST: return Direction.SOUTHWEST;
      case CENTER: return Direction.CENTER;
    }
    throw new RuntimeException("Cannot flip y of invalid Direction! " + toFlip);
  }

  public static int maxSingleAxisDist(MapLocation a, MapLocation b) {
    return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
  }

  /*

  * / // ================================== TOGGLE THIS OFF/ON

  private static Map<String, Integer> byteCodeMap = new HashMap<>();
  public static void startByteCodeCounting(String reason) {
    if (byteCodeMap.putIfAbsent(reason, Clock.getBytecodeNum()) != null) { // we're already counting!
      //System.out.printf("Already counting for %s!!\n", reason);
    }
  }

  public static void finishByteCodeCounting(String reason) {
    int end = Clock.getBytecodeNum();
    Integer start = byteCodeMap.getOrDefault(reason, -1);
    if (start == -1) {
      //System.out.printf("Not counting bytecodes for %s!!!\n", reason);
      return;
    }
    //System.out.printf("%4d BC: %s\n", end-start, reason);
    byteCodeMap.remove(reason);
  }


  /*

  */ // ------------------------------ TOGGLE THIS ON/OFF

  public static void startByteCodeCounting(String reason) {}
  public static void finishByteCodeCounting(String reason) {}

  /*
   */

}
