package firstbot;

import battlecode.common.Clock;
import battlecode.common.Direction;

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

  private static int start;
  private static boolean counting = false;
  public static void startByteCodeCounting() {
    if (counting) {
      System.out.println("Already counting bytecodes!!");
      return;
    }
    start = Clock.getBytecodeNum();
    counting = true;
  }

  private static int end;
  public static void finishByteCodeCounting(String reason) {
    if (!counting) {
      System.out.println("Not counting bytecodes yet!!!");
      return;
    }
    end = Clock.getBytecodeNum();
    System.out.printf("%d bytecodes used for %s\n", end-start, reason);
  }
}
