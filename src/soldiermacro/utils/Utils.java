package soldiermacro.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.Random;

public class Utils {

  public enum MapSymmetry {
    ROTATIONAL,
    HORIZONTAL,
    VERTICAL,
  }

  /** Seeded RNG for use throughout the bot classes */
  public static Random rng;

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

  /** Array of the 4 possible angled directions. */
  public static final Direction[] ordinal_directions = {
          Direction.NORTHEAST,
          Direction.SOUTHEAST,
          Direction.SOUTHWEST,
          Direction.NORTHWEST,
  };

  /** Array of the 3x3 possible directions. */
  public static final Direction[] directionsNine = {
          Direction.CENTER,
          Direction.NORTH,
          Direction.NORTHEAST,
          Direction.EAST,
          Direction.SOUTHEAST,
          Direction.SOUTH,
          Direction.SOUTHWEST,
          Direction.WEST,
          Direction.NORTHWEST,
  };

  public static final int DSQ_1by1 = 2; // technically 3x3 but a 1tile boundary around center
  public static final int DSQ_2by2 = 8;
  public static final int DSQ_3by3plus = 18; // contains some extra tiles
//  public static final int DSQ_3by3 = 32;
//  public static final int DSQ_3by3 = 32;

  /**
   * mapping from comms integer value to MapSymmetry
   * 0 is invalid -> null
   * NOT_HORIZ NOT_VERT NOT_ROT
   * horiz = 011 = 3
   * vert = 101 = 5
   * rot = 110 = 6
   */
  public static final MapSymmetry[] commsSymmetryMap = {
      null,
      null,
      null,
      MapSymmetry.HORIZONTAL,
      null,
      MapSymmetry.VERTICAL,
      MapSymmetry.ROTATIONAL,
//      null,
  };
  public static final MapSymmetry[] commsSymmetryGuessMap = {
      MapSymmetry.ROTATIONAL, // 000
      MapSymmetry.HORIZONTAL, // 001
      MapSymmetry.ROTATIONAL, // 010
      MapSymmetry.HORIZONTAL, // 011
      MapSymmetry.ROTATIONAL, // 100
      MapSymmetry.VERTICAL,   // 101
      MapSymmetry.ROTATIONAL, // 110
//      MapSymmetry.ROTATIONAL, // 111
  };

  /** The number of chunks that the map should be split into */
  public static final int MAX_MAP_CHUNKS = 100;

  /** related to above */
  public static final int CHUNK_INFOS_PER_INT = 4;

  /*
   * the amount of lead that a single miner can claim from others (based on lead regen)
   * regenAmt*regenTime * FACTOR (~0.75) = 3*40*X = 100
   */
  public static final int LEAD_PER_MINER_CLAIM = 100;

  public static void setUpStatics() {
    rng = new Random(Global.rc.getID());
  }

  public static Direction randomDirection() {
    return directions[rng.nextInt(directions.length)];
  }

  /**
   * generate a direction either same as dir or random rotation to left/right
   *    uniform chance
   * @param dir base direction
   * @return the randomized direction
   */
  public static Direction randomSimilarDirection(Direction dir) {
    switch (Utils.rng.nextInt(3)) {
      default:
      case 0:
        return dir;
      case 1:
        return dir.rotateLeft();
      case 2:
        return dir.rotateRight();
    }
  }

  /**
   * generate a direction either same as dir or random rotation to left/right
   *    prefers passed in direction with 50% (25% each rotation)
   * @param dir base direction
   * @return the randomized direction
   */
  public static Direction randomSimilarDirectionPrefer(Direction dir) {
    switch (Utils.rng.nextInt(4)) {
      default:
      case 1:
        return dir;
      case 2:
        return dir.rotateLeft();
      case 3:
        return dir.rotateRight();
    }
  }

  public static MapLocation randomMapLocation() { return new MapLocation(randomMapLocationX(), randomMapLocationY());}

  public static int randomMapLocationX() { return rng.nextInt(Cache.Permanent.MAP_WIDTH);}

  public static int randomMapLocationY() { return rng.nextInt(Cache.Permanent.MAP_HEIGHT);}

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
  public static int encodeLocationLower(MapLocation location) {
    return (location.x << 6) | (location.y);
  }

  /**
   * decode a location from the provided integer (encoding described above)
   * @param encoded the integer to extract location from
   * @return the decoded location
   */
  public static MapLocation decodeLocation(int encoded) {
    return new MapLocation((encoded >> 10) & 0x3f, (encoded >> 4) & 0x3f);
  }
  public static MapLocation decodeLocationLower(int encoded) {
    return new MapLocation((encoded >> 6) & 0x3f, (encoded) & 0x3f);
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

  /**
   * flip x-coord of location (mimic horizontal flip)
   * @param toFlip the coord to flip horiz
   * @return the flipped location
   */
  public static MapLocation flipLocationX(MapLocation toFlip) {
    return new MapLocation(Cache.Permanent.MAP_WIDTH - 1 - toFlip.x, toFlip.y);
  }

  /**
   * flip y-coord of location (mimic vertical flip)
   * @param toFlip the coord to flip vert
   * @return the flipped location
   */
  public static MapLocation flipLocationY(MapLocation toFlip) {
    return new MapLocation(toFlip.x, Cache.Permanent.MAP_HEIGHT - 1 - toFlip.y);
  }

  /**
   * flip x- and y-coords of location (mimic rotation)
   * @param toRot the coord to rotate
   * @return the rotated location
   */
  public static MapLocation rotateLocation180(MapLocation toRot) {
    return new MapLocation(Cache.Permanent.MAP_WIDTH - 1 - toRot.x, Cache.Permanent.MAP_HEIGHT - 1 - toRot.y);
  }

  /**
   * invert the given maploc based on the provided symmetry
   * @param location the coordinates to invert
   * @param symmetry how to invert the coordinates
   * @return the inverted location
   */
  public static MapLocation applySymmetry(MapLocation location, MapSymmetry symmetry) {
    switch (symmetry) {
      case HORIZONTAL:
        return flipLocationX(location);
      case VERTICAL:
        return flipLocationY(location);
      case ROTATIONAL:
        return rotateLocation180(location);
      default:
        throw new RuntimeException("Cannot apply unknown symmetry to map location: " + symmetry);
    }
  }

  public static double turnsTillNextCooldown(int c, int r) {
    int cooldownAfterMove = (int) Math.floor((1 + r/10.0) * c); //35
    //20 => 2
    return cooldownAfterMove / 10.0;
  }

  /**
   * converts a map location to a chunk index based on the chunk size calculated by cache
   * @param location the location to get its chunk
   * @return the chunk index
   */
  public static int locationToChunkIndex(MapLocation location) {
    int x = location.x / Cache.Permanent.CHUNK_WIDTH;
    int y = location.y / Cache.Permanent.CHUNK_HEIGHT;
    return x + y * Cache.Permanent.NUM_HORIZONTAL_CHUNKS;
  }

  /**
   * gets the center coord of the given chunk
   * @param chunkIndex the index of the chunk in our chunk info
   * @return the center location of the chunk
   */
  public static MapLocation chunkIndexToLocation(int chunkIndex) {
    int x = chunkIndex % Cache.Permanent.NUM_HORIZONTAL_CHUNKS * Cache.Permanent.CHUNK_WIDTH;
    int y = chunkIndex / Cache.Permanent.NUM_HORIZONTAL_CHUNKS * Cache.Permanent.CHUNK_HEIGHT;
    return new MapLocation(x + Cache.Permanent.CHUNK_WIDTH / 2, y + Cache.Permanent.CHUNK_HEIGHT / 2);
  }

  /**
   * convert the provided location to the center of its chunk
   * @param location the location to find the chunk center
   * @return the chunk center for the given location
   */
  public static MapLocation locationToChunkCenter(MapLocation location) {
    int x = location.x - (location.x % Cache.Permanent.CHUNK_WIDTH);
    int y = location.y - (location.y % Cache.Permanent.CHUNK_HEIGHT);
    return new MapLocation(x + Cache.Permanent.CHUNK_WIDTH / 2, y + Cache.Permanent.CHUNK_HEIGHT / 2);
  }

  public static void setPrint() {

  }

  public static void print(String s) {
    Cache.PerTurn.print.append(s).append("\n");
  }

  public static void print(String s, String s2) {
    print(s); print(s2);
  }

  public static void print(String s, String s2, String s3) {
    print(s); print(s2); print(s3);
  }
  public static void print(String s, String s2, String s3, String s4) {
    print(s); print(s2); print(s3); print(s4);
  }

  public static void cleanPrint() {
    Cache.PerTurn.print = new StringBuilder();
    Cache.PerTurn.print.append(" *** ");
    Cache.PerTurn.print.append(Cache.PerTurn.CURRENT_LOCATION);
    Cache.PerTurn.print.append(" ***\n");
  }

  public static void submitPrint() {
    if (Cache.PerTurn.print.toString().length() > 25) {
      //System.out.println(Cache.PerTurn.print);
    }
    cleanPrint();
  }

  /*

  * / // ================================== TOGGLE THIS OFF/ON

  private static Map<String, Integer> byteCodeMap = new HashMap<>();
  public static void startByteCodeCounting(String reason) {
    if (byteCodeMap.putIfAbsent(reason, Clock.getBytecodeNum()) != null) { // we're already counting!
      ////System.out.printf("Already counting for %s!!\n", reason);
    }
  }

  public static void finishByteCodeCounting(String reason) {
    int end = Clock.getBytecodeNum();
    Integer start = byteCodeMap.getOrDefault(reason, -1);
    if (start == -1) {
      ////System.out.printf("Not counting bytecodes for %s!!!\n", reason);
      return;
    }
    ////System.out.printf("%4d BC: %s\n", end-start, reason);
    byteCodeMap.remove(reason);
  }


  /*

  */ // ------------------------------ TOGGLE THIS ON/OFF

  public static void startByteCodeCounting(String reason) {}
  public static void finishByteCodeCounting(String reason) {}

  /*
   */

}
