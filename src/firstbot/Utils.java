package firstbot;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Random;

public class Utils {

  /** Seeded RNG for use throughout the bot classes */
  public static Random rng;
  public static RobotController rc;
  public static int mapWidth;
  public static int mapHeight;

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

  public static void setUpStatics(RobotController rc) {
    Utils.rc = rc;
    rng = new Random(rc.getID());
    mapWidth = rc.getMapWidth();
    mapHeight = rc.getMapHeight();
  }

  public static Direction randomDirection() {
    return directions[rng.nextInt(directions.length)];
  }

  public static MapLocation randomMapLocation() { return new MapLocation(randomMapLocationX(), randomMapLocationY());}

  public static int randomMapLocationX() { return rng.nextInt(mapWidth);}

  public static int randomMapLocationY() { return rng.nextInt(mapHeight);}

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

  /**
   * calculate the distance between two locations based on the largest difference on a single axis
   * @param a first location
   * @param b second location
   * @return the distance metric
   */
  public static int maxSingleAxisDist(MapLocation a, MapLocation b) {
    return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
  }

  /**
   * do a lerp between the locations
   * @param from the location to start from
   * @param to the location to end at
   * @param amount the lerping distance
   * @return the lerped location
   */
  public static MapLocation lerpLocations(MapLocation from, MapLocation to, double amount) {
    return new MapLocation((int) ((to.x - from.x) * amount) + from.x, (int) ((to.y - from.y) * amount) + from.y);
  }

  /*

  * / // ================================== TOGGLE THIS OFF/ON

  private static Map<String, Integer> byteCodeMap = new HashMap<>();
  public static void startByteCodeCounting(String reason) {
    if (byteCodeMap.putIfAbsent(reason, Clock.getBytecodeNum()) != null) { // we're already counting!
      System.out.printf("Already counting for %s!!\n", reason);
    }
  }

  public static void finishByteCodeCounting(String reason) {
    int end = Clock.getBytecodeNum();
    Integer start = byteCodeMap.getOrDefault(reason, -1);
    if (start == -1) {
      System.out.printf("Not counting bytecodes for %s!!!\n", reason);
      return;
    }
    System.out.printf("%4d BC: %s\n", end-start, reason);
    byteCodeMap.remove(reason);
  }


  /*

  */ // ------------------------------ TOGGLE THIS ON/OFF

  public static void startByteCodeCounting(String reason) {}
  public static void finishByteCodeCounting(String reason) {}

  /*
   */

}
