package firstbot;

import battlecode.common.Direction;

import java.util.Random;

public class Constants {

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
}
