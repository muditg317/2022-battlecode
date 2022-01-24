package bfspathing.communications;

import battlecode.common.*;
import bfspathing.communications.messages.Message;
import bfspathing.containers.FastQueue;
import bfspathing.utils.Cache;
import bfspathing.utils.Global;
import bfspathing.utils.Utils;

/**
 * This class will have a variety of methods related to communications between robots
 * There should be a generic interface for sending messages and reading them
 * Every robot should have a communicator instantiated with its own RobotController
 *    (used to read/write to the shared array)
 */
public class Communicator {

  public static class ArchonInfo {
    public static final int NUM_ARCHON_INTS = 8;
    public static final int ARCHON_INTS_START = GameConstants.SHARED_ARRAY_LENGTH - NUM_ARCHON_INTS;
    public static final int OUR_ARCHONS_1 = ARCHON_INTS_START;
    public static final int OUR_ARCHONS_2 = ARCHON_INTS_START+1;
    public static final int OUR_ARCHONS_3 = ARCHON_INTS_START+2;
    public static final int OUR_ARCHONS_4 = ARCHON_INTS_START+3;
    public static final int ENEMY_ARCHONS_1 = ARCHON_INTS_START+4;
    public static final int ENEMY_ARCHONS_2 = ARCHON_INTS_START+5;
    public static final int ENEMY_ARCHONS_3 = ARCHON_INTS_START+6;
    public static final int ENEMY_ARCHONS_4 = ARCHON_INTS_START+7;
    public static final int ARCHON_LOC_MASK = 0b1111111111110000;
    public static final int ARCHON_LOC_INVERTED_MASK = ~ARCHON_LOC_MASK;
    public static final int ARCHON_MOVING_MASK = 0b1000;
    public static final int ARCHON_NOT_MOVING_MASK = ~(ARCHON_MOVING_MASK);
//    public static final int LEFT_ARCHON_LOC_MASK = 0b1111111 << 8;
//    public static final int LEFT_ARCHON_LOC_INVERTED_MASK = ~(LEFT_ARCHON_LOC_MASK);
//    public static final int LEFT_ARCHON_MOVING_MASK = 0b10000000 << 8;
//    public static final int LEFT_ARCHON_NOT_MOVING_MASK = ~(LEFT_ARCHON_MOVING_MASK);
//    public static final int RIGHT_ARCHON_LOC_MASK = 0b1111111;
//    public static final int RIGHT_ARCHON_LOC_INVERTED_MASK = ~(RIGHT_ARCHON_LOC_MASK);
//    public static final int RIGHT_ARCHON_MOVING_MASK = 0b10000000;
//    public static final int RIGHT_ARCHON_NOT_MOVING_MASK = ~(RIGHT_ARCHON_MOVING_MASK);
//    public static final int SHIFT_PER_CHUNK_MOD_INTS = 16 / Utils.CHUNK_INFOS_PER_INT;

    //    public static final int ARCHON_INFO_SIZE = 2;
//    public static final int ARCHON_INTS_START = SYMMETRY_INFO_IND - ARCHON_INFO_SIZE;
    public MapLocation ourArchon1;
    public MapLocation ourArchon2;
    public MapLocation ourArchon3;
    public MapLocation ourArchon4;

    public MapLocation enemyArchon1;
    public MapLocation enemyArchon2;
    public MapLocation enemyArchon3;
    public MapLocation enemyArchon4;

    public void readOurArchonLocs() throws GameActionException {
      ourArchon1 = Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_1));
      ourArchon2 = Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_2));
      ourArchon3 = Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_3));
      ourArchon4 = Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_4));
    }

    public void readEnemyArchonLocs() throws GameActionException {
      enemyArchon1 = Utils.decodeLocation(Global.rc.readSharedArray(ENEMY_ARCHONS_1));
      enemyArchon2 = Utils.decodeLocation(Global.rc.readSharedArray(ENEMY_ARCHONS_2));
      enemyArchon3 = Utils.decodeLocation(Global.rc.readSharedArray(ENEMY_ARCHONS_3));
      enemyArchon4 = Utils.decodeLocation(Global.rc.readSharedArray(ENEMY_ARCHONS_4));
    }

    public void setOurArchonLoc(int whichArchon, MapLocation archonLoc) throws GameActionException {
//      //Printer.cleanPrint();
      switch (whichArchon) {
        case 1:
          ourArchon1 = archonLoc;
          Global.rc.writeSharedArray(OUR_ARCHONS_1, (Global.rc.readSharedArray(OUR_ARCHONS_1) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
//          //Printer.print("OUR_ARCHONS_1: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_1)));
          break;
        case 2:
          ourArchon2 = archonLoc;
          Global.rc.writeSharedArray(OUR_ARCHONS_2, (Global.rc.readSharedArray(OUR_ARCHONS_2) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
//          //Printer.print("OUR_ARCHONS_2: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_2)));
          break;
        case 3:
          ourArchon3 = archonLoc;
          Global.rc.writeSharedArray(OUR_ARCHONS_3, (Global.rc.readSharedArray(OUR_ARCHONS_3) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
//          //Printer.print("OUR_ARCHONS_3: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_3)));
          break;
        case 4:
          ourArchon4 = archonLoc;
          Global.rc.writeSharedArray(OUR_ARCHONS_4, (Global.rc.readSharedArray(OUR_ARCHONS_4) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
          break;
      }
//      //Printer.submitPrint();
    }

    public void setEnemyArchonLoc(int whichArchon, MapLocation archonLoc) throws GameActionException {
//      //Printer.cleanPrint();
      switch (whichArchon) {
        case 1:
          enemyArchon1 = archonLoc;
          Global.rc.writeSharedArray(ENEMY_ARCHONS_1, (Global.rc.readSharedArray(ENEMY_ARCHONS_1) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
//          //Printer.print("ENEMY_ARCHONS_1: " + Integer.toBinaryString(Global.rc.readSharedArray(ENEMY_ARCHONS_1)));
          break;
        case 2:
          enemyArchon2 = archonLoc;
          Global.rc.writeSharedArray(ENEMY_ARCHONS_2, (Global.rc.readSharedArray(ENEMY_ARCHONS_2) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
//          //Printer.print("ENEMY_ARCHONS_2: " + Integer.toBinaryString(Global.rc.readSharedArray(ENEMY_ARCHONS_2)));
          break;
        case 3:
          enemyArchon3 = archonLoc;
          Global.rc.writeSharedArray(ENEMY_ARCHONS_3, (Global.rc.readSharedArray(ENEMY_ARCHONS_3) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
//          //Printer.print("ENEMY_ARCHONS_3: " + Integer.toBinaryString(Global.rc.readSharedArray(ENEMY_ARCHONS_3)));
          break;
        case 4:
          enemyArchon4 = archonLoc;
          Global.rc.writeSharedArray(ENEMY_ARCHONS_4, (Global.rc.readSharedArray(ENEMY_ARCHONS_4) & ARCHON_LOC_INVERTED_MASK) | Utils.encodeLocation(archonLoc));
          break;
      }
//      //Printer.submitPrint();
    }

    public MapLocation getEnemyArchon(int whichArchon) {
      switch (whichArchon) {
        case 4:
          return enemyArchon4;
        case 3:
          return enemyArchon3;
        case 2:
          return enemyArchon2;
        case 1:
          return enemyArchon1;
      }
      return null;
    }

//    public boolean mirrored;
    public void mirrorSelfToEnemies() throws GameActionException {
      switch (Global.rc.getArchonCount()) {
        case 4:
          Global.rc.writeSharedArray(ENEMY_ARCHONS_4, Utils.encodeLocation(Utils.applySymmetry(Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_4)), Global.communicator.metaInfo.guessedSymmetry)));
        case 3:
          Global.rc.writeSharedArray(ENEMY_ARCHONS_3, Utils.encodeLocation(Utils.applySymmetry(Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_3)), Global.communicator.metaInfo.guessedSymmetry)));
        case 2:
          Global.rc.writeSharedArray(ENEMY_ARCHONS_2, Utils.encodeLocation(Utils.applySymmetry(Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_2)), Global.communicator.metaInfo.guessedSymmetry)));
        case 1:
          Global.rc.writeSharedArray(ENEMY_ARCHONS_1, Utils.encodeLocation(Utils.applySymmetry(Utils.decodeLocation(Global.rc.readSharedArray(OUR_ARCHONS_1)), Global.communicator.metaInfo.guessedSymmetry)));
      }
//      mirrored = true;
//      readEnemyArchonLocs();
//      //Printer.cleanPrint();
//      //Printer.print("Set enemy mirror");
//      //Printer.print("our 1: " + ourArchon1);
//      //Printer.print("our 2: " + ourArchon2);
//      //Printer.print("our 3: " + ourArchon3);
//      //Printer.print("our 4: " + ourArchon4);
//      //Printer.print("enemy 1: " + enemyArchon1);
//      //Printer.print("enemy 2: " + enemyArchon2);
//      //Printer.print("enemy 3: " + enemyArchon3);
//      //Printer.print("enemy 4: " + enemyArchon4);
//      //Printer.submitPrint();
    }

    public void setOurArchonMoving(int whichArchon) throws GameActionException {
//      //Printer.cleanPrint();
      switch (whichArchon) {
        case 1:
          Global.rc.writeSharedArray(OUR_ARCHONS_1, Global.rc.readSharedArray(OUR_ARCHONS_1) | ARCHON_MOVING_MASK);
//          //Printer.print("OUR_ARCHONS_12: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_12)));
          break;
        case 2:
          Global.rc.writeSharedArray(OUR_ARCHONS_2, Global.rc.readSharedArray(OUR_ARCHONS_2) | ARCHON_MOVING_MASK);
//          //Printer.print("OUR_ARCHONS_12: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_12)));
          break;
        case 3:
          Global.rc.writeSharedArray(OUR_ARCHONS_3, Global.rc.readSharedArray(OUR_ARCHONS_3) | ARCHON_MOVING_MASK);
//          //Printer.print("OUR_ARCHONS_34: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_34)));
          break;
        case 4:
          Global.rc.writeSharedArray(OUR_ARCHONS_4, Global.rc.readSharedArray(OUR_ARCHONS_4) | ARCHON_MOVING_MASK);
//          //Printer.print("OUR_ARCHONS_34: " + Integer.toBinaryString(Global.rc.readSharedArray(OUR_ARCHONS_34)));
          break;
      }
//      //Printer.submitPrint();
    }

    public void setOurArchonNotMoving(int whichArchon) throws GameActionException {
      switch (whichArchon) {
        case 1:
          Global.rc.writeSharedArray(OUR_ARCHONS_1, Global.rc.readSharedArray(OUR_ARCHONS_1) & ARCHON_NOT_MOVING_MASK);
          break;
        case 2:
          Global.rc.writeSharedArray(OUR_ARCHONS_2, Global.rc.readSharedArray(OUR_ARCHONS_2) & ARCHON_NOT_MOVING_MASK);
          break;
        case 3:
          Global.rc.writeSharedArray(OUR_ARCHONS_3, Global.rc.readSharedArray(OUR_ARCHONS_3) & ARCHON_NOT_MOVING_MASK);
          break;
        case 4:
          Global.rc.writeSharedArray(OUR_ARCHONS_4, Global.rc.readSharedArray(OUR_ARCHONS_4) & ARCHON_NOT_MOVING_MASK);
          break;
      }
    }

    public boolean ourArchonIsMoving(int whichArchon) throws GameActionException {
      switch (whichArchon) {
        case 1:
          return (Global.rc.readSharedArray(OUR_ARCHONS_1) & ARCHON_MOVING_MASK) > 0;
        case 2:
          return (Global.rc.readSharedArray(OUR_ARCHONS_2) & ARCHON_MOVING_MASK) > 0;
        case 3:
          return (Global.rc.readSharedArray(OUR_ARCHONS_3) & ARCHON_MOVING_MASK) > 0;
        case 4:
          return (Global.rc.readSharedArray(OUR_ARCHONS_4) & ARCHON_MOVING_MASK) > 0;
      }
      return false;
    }

    public MapLocation getNearestEnemyArchon(MapLocation from) throws GameActionException {
      MapLocation closestEnemyArchon = null;
      readEnemyArchonLocs();
      int dToClosest = 9999;
      switch (Global.rc.getArchonCount()) {
        case 4:
          if (enemyArchon4.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = enemyArchon4;
            dToClosest = enemyArchon4.distanceSquaredTo(from);
          }
        case 3:
          if (enemyArchon3.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = enemyArchon3;
            dToClosest = enemyArchon3.distanceSquaredTo(from);
          }
        case 2:
          if (enemyArchon2.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = enemyArchon2;
            dToClosest = enemyArchon2.distanceSquaredTo(from);
          }
        case 1:
          if (enemyArchon1.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = enemyArchon1;
//            dToClsoest = enemyArchon4.distanceSquaredTo(from);
          }
      }
      return closestEnemyArchon;
    }

    public int getNearestEnemyArchonIndex(MapLocation from) throws GameActionException {
      int closestEnemyArchon = -1;
      readEnemyArchonLocs();
      int dToClosest = 9999;
      switch (Global.rc.getArchonCount()) {
        case 4:
          if (enemyArchon4.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = 4;
            dToClosest = enemyArchon4.distanceSquaredTo(from);
          }
        case 3:
          if (enemyArchon3.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = 3;
            dToClosest = enemyArchon3.distanceSquaredTo(from);
          }
        case 2:
          if (enemyArchon2.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = 2;
            dToClosest = enemyArchon2.distanceSquaredTo(from);
          }
        case 1:
          if (enemyArchon1.isWithinDistanceSquared(from, dToClosest-1)) {
            closestEnemyArchon = 1;
//            dToClsoest = enemyArchon4.distanceSquaredTo(from);
          }
      }
      return closestEnemyArchon;
    }

    public MapLocation getNearestFriendlyArchon(MapLocation from) throws GameActionException {
      readOurArchonLocs();
      MapLocation closestFriendlyArchon = null;
      int dToClosest = 9999;
      switch (Global.rc.getArchonCount()) {
        case 4:
          if (ourArchon4.isWithinDistanceSquared(from, dToClosest-1)) {
            closestFriendlyArchon = ourArchon4;
            dToClosest = ourArchon4.distanceSquaredTo(from);
          }
        case 3:
          if (ourArchon3.isWithinDistanceSquared(from, dToClosest-1)) {
            closestFriendlyArchon = ourArchon3;
            dToClosest = ourArchon3.distanceSquaredTo(from);
          }
        case 2:
          if (ourArchon2.isWithinDistanceSquared(from, dToClosest-1)) {
            closestFriendlyArchon = ourArchon2;
            dToClosest = ourArchon2.distanceSquaredTo(from);
          }
        case 1:
          if (ourArchon1.isWithinDistanceSquared(from, dToClosest-1)) {
            closestFriendlyArchon = ourArchon1;
//            dToClosest = enemyArchon4.distanceSquaredTo(from);
          }
      }
      return closestFriendlyArchon;
    }
  }

  public static class SpawnInfo {
    public static final int NUM_SPAWN_INTS = 1;
    public static final int SPAWN_INTS_START = ArchonInfo.ARCHON_INTS_START - NUM_SPAWN_INTS;

    public static final int NUM_MINERS_NEEDED_IND = SPAWN_INTS_START;
    public static final int NUM_MINERS_MASK = 0b1111;
    public static final int NUM_MINERS_INVERTED_MASK = ~NUM_MINERS_MASK;

    public int getNumMinersNeeded() throws GameActionException {
      return Global.rc.readSharedArray(NUM_MINERS_NEEDED_IND) & NUM_MINERS_MASK;
    }

    /**
     * decrement the number of miners needed by the team
     * BETTER MAKE SURE THIS WAS >0 BEFORE
     * @throws GameActionException if reading/writing fails
     */
    public void decrNumMinersNeeded() throws GameActionException {
      Global.rc.writeSharedArray(NUM_MINERS_NEEDED_IND, Global.rc.readSharedArray(NUM_MINERS_NEEDED_IND) - 1);
    }

    /**
     * set the number of miners needed by the team
     * @param newNum the new number of miners needed
     * @throws GameActionException if reading/writing fails
     */
    public void setNumMinersNeeded(int newNum) throws GameActionException {
      Global.rc.writeSharedArray(NUM_MINERS_NEEDED_IND, (Global.rc.readSharedArray(NUM_MINERS_NEEDED_IND) & NUM_MINERS_INVERTED_MASK) + newNum);
    }

  }

  public class MetaInfo {
    public static final int NUM_META_INTS = 1;
    public static final int META_INT_START = SpawnInfo.SPAWN_INTS_START - NUM_META_INTS;

    public static final int VALID_REGION_IND = META_INT_START;
    private int validRegionStart; // 0-62    -- 6 bits [15,10]
    private int validRegionEnd;   // 0-62    -- 6 bits [9,4]
    public static final int EMPTY_REGION_INDICATOR = 61;

    public static final int SYMMETRY_INFO_SIZE = 0;
    public static final int SYMMETRY_INFO_IND = VALID_REGION_IND - SYMMETRY_INFO_SIZE;
    public Utils.MapSymmetry knownSymmetry; // determined by next three bools
    public Utils.MapSymmetry guessedSymmetry; // determined by next three bools
    private static final int NOT_HORIZ_MASK = 0b1000;
    public boolean notHorizontal;     // 0-1               -- 1 bit  [3]
    private static final int NOT_VERT_MASK = 0b100;
    public boolean notVertical;       // 0-1               -- 1 bit  [2]
    private static final int NOT_ROT_MASK = 0b10;
    public boolean notRotational;     // 0-1               -- 1 bit  [1]
    private static final int ALL_SYM_INFO_MASK = NOT_HORIZ_MASK|NOT_VERT_MASK|NOT_ROT_MASK;
    private static final int SYM_INFO_INVERTED_MASK = ~ALL_SYM_INFO_MASK;

//    public boolean dirty;


    public MetaInfo() {
//      knownSymmetry = null;
//      guessedSymmetry = null;
//      notHorizontal = false;
//      notVertical = false;
//      notRotational = false;
//
//      dirty = false;
    }

    /**
     * update the communicators meta information based on the meta ints from the shared array
     * @throws GameActionException if reading fails
     */
    public void updateFromShared() throws GameActionException {
      int validRegion = Global.rc.readSharedArray(VALID_REGION_IND);
      validRegionStart = (validRegion >>> 10) & 63;
      validRegionEnd = (validRegion >>> 4) & 63;

      int symmetryInfo = validRegion; //Global.rc.readSharedArray(SYMMETRY_INFO_IND);
      knownSymmetry = Utils.commsSymmetryMap[(symmetryInfo & ALL_SYM_INFO_MASK) >> 1];
      notHorizontal = (symmetryInfo & NOT_HORIZ_MASK) > 0;
      notVertical = (symmetryInfo & NOT_VERT_MASK) > 0;
      notRotational = (symmetryInfo & NOT_ROT_MASK) > 0;
      guessedSymmetry = knownSymmetry != null ? knownSymmetry : Utils.commsSymmetryGuessMap[(symmetryInfo & ALL_SYM_INFO_MASK) >> 1];

//      dirty = false;
    }

    public void initializeValidRegion() throws GameActionException {
      validRegionStart = validRegionEnd = EMPTY_REGION_INDICATOR;
//      dirty = true;
      writeValidRegion();
    }

    /**
     * update validstart/validend from shared array
     * @throws GameActionException if reading fails
     */
    public void reloadValidRegion() throws GameActionException {
      int validRegion = Global.rc.readSharedArray(VALID_REGION_IND);
      validRegionStart = (validRegion >>> 10) & 63;
      validRegionEnd = (validRegion >>> 4) & 63;
    }

    /**
     * convert the meta information about the communication buffer into a set of ints
     *    also write the ints to the shared array
     * @return true if updated
     * @throws GameActionException if writing fails
     */
    public boolean writeValidRegion() throws GameActionException {
//      if (!dirty) return false;
//      //System.out.printf("%s\n", this);
      Global.rc.writeSharedArray(VALID_REGION_IND,
            validRegionStart << 10
          | validRegionEnd << 4
          | ((notHorizontal ? NOT_HORIZ_MASK : 0) | (notVertical ? NOT_VERT_MASK : 0) | (notRotational ? NOT_ROT_MASK : 0)));
//      dirty = false;
      return true;
    }

    public void writeSymmetry() throws GameActionException {
      Global.rc.writeSharedArray(VALID_REGION_IND,
          (Global.rc.readSharedArray(VALID_REGION_IND) & SYM_INFO_INVERTED_MASK)
              | ((notHorizontal ? NOT_HORIZ_MASK : 0) | (notVertical ? NOT_VERT_MASK : 0) | (notRotational ? NOT_ROT_MASK : 0)));
    }

    @Override
    public String toString() {
      return String.format("ValidComms[%d:%d)", validRegionStart, validRegionEnd);
    }

    /**
     * update the meta info on map symmetry to disallow the gievn type of symmetry
     *    also updates knownSymmetry if possible
     *    sets dirty flag as well
     * @param blockedSymmetry the symmetry that is no longer allowed
     * @throws GameActionException if writing fails
     */
    public void setSymmetryCantBe(Utils.MapSymmetry blockedSymmetry) throws GameActionException {
      switch (blockedSymmetry) {
        case HORIZONTAL:
          notHorizontal = true;
          break;
        case VERTICAL:
          notVertical = true;
          break;
        case ROTATIONAL:
          notRotational = true;
          break;
      }
//      //System.out.println("Bot at " + Cache.PerTurn.CURRENT_LOCATION + " realized sym can't be " + blockedSymmetry);
      int index = ((notHorizontal ? NOT_HORIZ_MASK : 0) | (notVertical ? NOT_VERT_MASK : 0) | (notRotational ? NOT_ROT_MASK : 0)) >> 1;
      knownSymmetry = Utils.commsSymmetryMap[index];
      guessedSymmetry = Utils.commsSymmetryGuessMap[index];
//      //System.out.println("symIndex: " + index + " known: " + knownSymmetry + " -- guess: " + guessedSymmetry);
//      //System.out.printf("NEW SYMMETRY KNOWLEDGE\n\tnot:%s\n\tknown:%s\n\tguess:%s\n", blockedSymmetry, knownSymmetry, guessedSymmetry);
//      dirty = true;
      writeSymmetry();
      archonInfo.mirrorSelfToEnemies();
    }
  }

  private static final int MIN_BYTECODES_TO_SEND_MESSAGE = 1000;

  private final RobotController rc;
//  private final int[] sharedBuffer;

  public final MetaInfo metaInfo;
//  public final ChunkInfo chunkInfo;
  public final ArchonInfo archonInfo;

  private static final int NUM_MESSAGING_INTS = MetaInfo.META_INT_START;
  private final FastQueue<Message> messageQueue;
//  private final List<Message> sentMessages;
//  private final List<Message> received;
  private Message lastSentMessage;

  public Communicator() {
    this.rc = Global.rc;
//    sharedBuffer = new int[NUM_MESSAGING_INTS];
    metaInfo = new MetaInfo();
//    chunkInfo = new ChunkInfo();
    archonInfo = new ArchonInfo();

    messageQueue = new FastQueue<>(10);
//    sentMessages = new ArrayList<>(5);
//    received = new ArrayList<>();
  }

  /**
   * reset the sharedBuffer with the contents of the entire shared array
   */
  private void reloadBuffer() throws GameActionException {
    Utils.startByteCodeCounting("readShared");
    metaInfo.updateFromShared();
//    int toUpdate = (metaInfo.validRegionEnd - metaInfo.validRegionStart + 1 + NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS;
//    int ind;
//    for (int i = 0; i < toUpdate; i++) {
//      ind = (metaInfo.validRegionStart+i) % NUM_MESSAGING_INTS;
//      sharedBuffer[ind] = rc.readSharedArray(ind);
//    }
    Utils.finishByteCodeCounting("readShared");
  }

  /**
   * clean out own stale messages if they are at the start of the valid region
   * @return if cleaned
   * @throws GameActionException if writing fails
   */
  public boolean cleanStaleMessages() throws GameActionException {
    if (lastSentMessage != null) {
//      if (rc.getRoundNum() == 1471) {
//        //System.out.println("bounds before cleaning: " + metaInfo);
//      }
//      Message message = sentMessages.get(0);
//      int origin = metaInfo.validRegionStart;
//      int ending = metaInfo.validRegionEnd;
//      if (ending < origin) {
//        ending += NUM_MESSAGING_INTS;
//      }
//      if (ending == origin) { // no messages to read
//        return 0;
//      }
//      for (; origin < ending; origin += Message.Header.fromReadInt(rc.readSharedArray(origin%NUM_MESSAGING_INTS)).type.standardSize+1) {
//        if (message.writeInfo.startIndex == origin) {
//          //System.out.println("CLEAN " + message.header.type + ": " + origin);
          metaInfo.validRegionStart = (lastSentMessage.writeInfo.startIndex + lastSentMessage.size()) % NUM_MESSAGING_INTS;
          if (metaInfo.validRegionEnd == metaInfo.validRegionStart) {
            metaInfo.initializeValidRegion();
          } else {
            metaInfo.writeValidRegion();
          }
//          if (rc.getRoundNum() == 1471) {
//            //System.out.println("Cleaning " + (sentMessages.size() - sentMessages.indexOf(message)) + " messages!");
//            //System.out.println("Clearing messages! - starting from " + message.header.type + " on " + message.header.cyclicRoundNum + " at " + message.writeInfo.startIndex);
//            //System.out.println("Last message cleaned: " + last.header.type + " on " + message.header.cyclicRoundNum + " at " + last.writeInfo.startIndex);
//            //System.out.println("new bounds from cleaning: " + metaInfo);
//          }
//          //System.out.println("\ncleaned  - " + metaInfo);
          return true;
//        }
//      }
//      //System.out.printf("FAILED TO CLEAN MESSAGES\n%s\n1st: %s @ %d\n", metaInfo, message.header, message.writeInfo.startIndex);
//      sentMessages.clear();
    }
    return false;
  }

  /**
   * read all the messages in the sharedArray
   *    Also forward these to the robot!
   * @return the number of messages that were read
   * @throws GameActionException thrown if readMessageAt fails
   */
  public int readAndAckAllMessages() throws GameActionException {
    Utils.startByteCodeCounting("reloadBuffer");
    reloadBuffer();
//    //System.out.println("update meta - " + Clock.getBytecodeNum());
//    if (rc.getRoundNum() == 1471) {
//      //System.out.println("Reading on round 582 -- " + metaInfo);
//      //System.out.println(Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd- metaInfo.validRegionStart+1)));
//    }
    Utils.finishByteCodeCounting("reloadBuffer");
//    //System.out.println("\nstarting - " + metaInfo);

    cleanStaleMessages(); // clean out stale bois
    lastSentMessage = null;
//    //System.out.println("clean stale - " + Clock.getBytecodeNum());
    int origin = metaInfo.validRegionStart;
    int ending = metaInfo.validRegionEnd;
    if (ending < origin) {
      ending += NUM_MESSAGING_INTS;
    }
    if (ending == MetaInfo.EMPTY_REGION_INDICATOR) { // no messages to read
      return 0;
    }
//    //System.out.println("Reading messages: " + metaInfo);
    int messages = 0;
//    int lastAckdRound = received.isEmpty() ? 0 : getNthLastReceivedMessage(1).header.cyclicRoundNum;
//    if (!received.isEmpty()) {
//      Message last = getNthLastReceivedMessage(1);
//      //System.out.println("last message: " + last.header.type + "\t -- ");
//    }
//    int maxRoundNum = Message.Header.toCyclicRound(rc.getRoundNum());
//    if (maxRoundNum < lastAckdRound) maxRoundNum += Message.Header.ROUND_NUM_CYCLE_SIZE;
//    //System.out.println("ack messages within: (" + lastAckdRound + ", " + maxRoundNum + "]");
//    int thisRound = rc.getRoundNum();
    while (origin < ending) {
//      //System.out.println("\nBefore  read/ack message: " + Clock.getBytecodeNum());
      Message message = readMessageAt(origin % NUM_MESSAGING_INTS);
//      if (message == null) {
//        int tries = 1;
//        do {
//          message = readMessageAt(++origin % NUM_MESSAGING_INTS);
//        } while (--tries > 0 && message == null);
//      }
//      if (message != null) {
        Global.robot.ackMessage(message);
        messages++;
        origin += message.size();
////      //System.out.println("\nCost to read/ack message: " + Clock.getBytecodeNum());
//      } else {
//        metaInfo.initializeValidRegion();
//        metaInfo.encodeAndWrite();
//        break;
//      }
    }
    return messages;
  }

  /**
   * read a message from the shared array
   *    ASSUMES - messageOrigin is the start of a VALID message
   * @param messageOrigin where the message starts
   * @return the read message
   */
  private Message readMessageAt(final int messageOrigin) throws GameActionException {
//     assert messageOrigin < NUM_MESSAGING_INTS; // ensure that the message is within the messaging ints
    int headerInt = Global.rc.readSharedArray(messageOrigin);//sharedBuffer[messageOrigin];
    Message.Header header;
//    try {
//      int beforeReadHeader = Clock.getBytecodeNum();
      header = Message.Header.fromReadInt(headerInt);
//      //System.out.println("Cost to read header: " + (Clock.getBytecodeNum() - beforeReadHeader));
//      header.validate();
//    } catch (Exception e) {
//      //System.out.println("Failed to parse header! at: " + messageOrigin);
//      //System.out.println("Reading bounds: " + metaInfo);
//      //System.out.println("ints: " + Arrays.toString(readInts(metaInfo.validRegionStart, (metaInfo.validRegionEnd-metaInfo.validRegionStart + NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS)));
//      //System.out.println("Header int: " + headerInt);
//      //System.out.println("Header: " + header);
////      e.printStackTrace();
////      metaInfo.validRegionStart = metaInfo.validRegionEnd = 0;
////      return null;
////      if (messageOrigin < metaInfo.validRegionEnd || (metaInfo.validRegionStart < metaInfo.validRegionEnd && messageOrigin < NUM_MESSAGING_INTS)) {
////        return readMessageAt((messageOrigin+1) % NUM_MESSAGING_INTS);
////      }
//      throw e;
//    }

    switch (header.type.standardSize) {
      case 0:
        return Message.fromHeaderAndInfo0(header,
          headerInt
          ).setWriteInfo(new Message.WriteInfo(messageOrigin));
      case 1:
        return Message.fromHeaderAndInfo1(header,
            Global.rc.readSharedArray((messageOrigin + 1) % NUM_MESSAGING_INTS)
          ).setWriteInfo(new Message.WriteInfo(messageOrigin));
      case 2:
        return Message.fromHeaderAndInfo2(header,
            Global.rc.readSharedArray((messageOrigin + 1) % NUM_MESSAGING_INTS),
            Global.rc.readSharedArray((messageOrigin + 2) % NUM_MESSAGING_INTS)
          ).setWriteInfo(new Message.WriteInfo(messageOrigin));
      default:
        throw new RuntimeException("Not enough cases for big message! - " + header);
    }

//    int beforeMakeInfo = Clock.getBytecodeNum();
//    int[] information = new int[header.numInformationInts];
//    for (int i = 0; i < header.numInformationInts; i++) {
//      information[i] = Global.rc.readSharedArray((messageOrigin + i + 1) % NUM_MESSAGING_INTS);// sharedBuffer[(messageOrigin + i + 1) % NUM_MESSAGING_INTS];
//    }
//    //System.out.println("Cost to make info arr: " + (Clock.getBytecodeNum() - beforeMakeInfo));
//    try {
//      return Message.fromHeaderAndInfo(header, information).setWriteInfo(new Message.WriteInfo(messageOrigin));
//    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
//      //System.out.println("Message instantiation failed!");
//      //System.out.println("Reading bounds: " + metaInfo);
//      //System.out.println("ints: " + Arrays.toString(readInts(metaInfo.validRegionStart, metaInfo.validRegionEnd-metaInfo.validRegionStart + 1)));
//      //System.out.printf("Read at %d\n", messageOrigin);
//      //System.out.println("Header int: " + headerInt);
//      throw new RuntimeException("Failed to initialize message", e);
//    }
  }

  /**
   * add a message to the internal communicator queue
   * @param message the message to send
   */
  public void enqueueMessage(Message message) {
    messageQueue.push(message);
//    if (rc.getID() == 10618) {
//      //Printer.cleanPrint();
//      //Printer.print("Enqueued message: " + messageQueue.size(), "header: " + message.header);
//      //Printer.submitPrint();
//    }
  }

  /**
   * reschedule a message to be sent in some number of turns
   *    should NOT happen often
   * @param message the message to reschedule
   */
  public void rescheduleMessage(Message message) {
    if (message.header.type.shouldReschedule) {
      enqueueMessage(message);
    }
  }

  /**
   * send all messages that should be sent by now
   * @throws GameActionException thrown if sendMessage fails
   */
  public void sendQueuedMessages() throws GameActionException {
    while (!messageQueue.isEmpty() && sendMessage(messageQueue.popFront()));
  }

  /**
   * write a certain message to the shared array
   * starts message after validRegionEnd and bumps validRegionStart as needed if ints are overwritten
   * @param message the message to write
   * @returns true if sent, false if rescheduled
   * @throws GameActionException thrown if writing to array fails
   */
  private boolean sendMessage(Message message) throws GameActionException {
    if (Clock.getBytecodesLeft() < MIN_BYTECODES_TO_SEND_MESSAGE) {
      rescheduleMessage(message);
//      //System.out.println("reschedule for bc - " + message.header);
//      //System.out.printf("---\nRESCHEDULE  %s:\n%d - %s\n", message.header.type, Clock.getBytecodesLeft(), Arrays.toString(message.toEncodedInts()));
      return false;
    }
//    //System.out.println("pre-reload -- bc: " + Clock.getBytecodesLeft());
    metaInfo.reloadValidRegion();
//    //System.out.println("pre-sending -- bc: " + Clock.getBytecodesLeft());
//    //System.out.println("metaInfo: " + metaInfo);
//    //System.out.println("Current ints: " + ((metaInfo.validRegionEnd-metaInfo.validRegionStart+NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS) + "\nnew ints: " + message.size() + "\nlimit: " + NUM_MESSAGING_INTS);
    if (metaInfo.validRegionEnd != MetaInfo.EMPTY_REGION_INDICATOR
        && ((metaInfo.validRegionEnd-metaInfo.validRegionStart+NUM_MESSAGING_INTS) % NUM_MESSAGING_INTS) + message.size() >= NUM_MESSAGING_INTS) { // will try to write more ints than available
      rescheduleMessage(message);
//      //System.out.println("reschedule for out of space - " + message.header);
//      //System.out.printf("---\nRESCHEDULE  %s:\n%d - %s\n", message.header.type, Clock.getBytecodesLeft(), Arrays.toString(message.toEncodedInts()));
      return false;
    }
    boolean updateStart = metaInfo.validRegionStart == MetaInfo.EMPTY_REGION_INDICATOR; // no valid messages currently
    int[] messageBits = message.toEncodedInts();
    int origin = metaInfo.validRegionEnd % NUM_MESSAGING_INTS;
    int messageOrigin = origin;
//    //System.out.printf("---\nSEND  %s:\n%d - %s\n", message.header.type, messageOrigin, Arrays.toString(messageBits));
//    //System.out.printf("---\nSEND  %s:\n%d - %s\n%s\nbc: %d\n", message.header.type, messageOrigin, Arrays.toString(messageBits),metaInfo,Clock.getBytecodesLeft());
//    //Printer.print(String.format("SEND  %s:\n%d - %s\n", message.header.type, messageOrigin, Arrays.toString(messageBits)));
    //rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 0,0,0);
//    //System.out.println(message.header);
    for (int messageChunk : messageBits) {
//      if (origin == metaInfo.validRegionStart) { // about to overwrite the start!
//        Message messageAt = readMessageAt(origin);
//        metaInfo.validRegionStart += messageAt != null ? messageAt.size() : 1;
//        metaInfo.validRegionStart %= NUM_MESSAGING_INTS;
//        //System.out.println("OVERWROTE STALE MESSAGE");
//        //System.out.println(messageAt.header);
//        throw new RuntimeException("Shouldn't overwrite message buffer");
//      }
//      //System.out.println("Write to shared " + origin + ": " + messageChunk);
      rc.writeSharedArray(origin, messageChunk);
      origin = (origin + 1) % NUM_MESSAGING_INTS;
    }
    lastSentMessage = message;
    //rc.setIndicatorDot(Cache.PerTurn.CURRENT_LOCATION, 0,255,0);
    metaInfo.validRegionEnd = origin;//(origin+1) % NUM_MESSAGING_INTS;
    if (updateStart) { // first message!
      metaInfo.validRegionStart = messageOrigin;
//      if (metaInfo.validRegionStart < 0) metaInfo.validRegionStart += NUM_MESSAGING_INTS;
//      //System.out.println("Move start: " + metaInfo);
    }
    message.setWriteInfo(new Message.WriteInfo(messageOrigin));
//    metaInfo.dirty = true;
    metaInfo.writeValidRegion();
    return true;
  }

  /**
   * returns true if the integer at the specified index matches the provided message header
   *    ASSUMES data has already been read into sharedBuffer
   * @param headerIndex the index to check
   * @param header the message metadata to verify
   * @return the sameness
   * @throws GameActionException if reading fails
   */
  public boolean headerMatches(int headerIndex, Message.Header header) throws GameActionException {
//    //System.out.println("Checking header at " + headerIndex + ": " + sharedBuffer[headerIndex] + " -- " + header.toInt());
//    return sharedBuffer[headerIndex] == header.toInt();
    return Global.rc.readSharedArray(headerIndex) == header.toInt();
  }

  /**
   * reads numInts integers starting at startIndex into an integer array and sends them back for use
   *    reads from ALREADY processed sharedBuffer
   *    loops around based on NUM_MESSAGING_BITS
   * @param startIndex where to start
   * @param numInts the number of ints to read
   * @return the array of read ints
   * @throws GameActionException if reading fails
   */
  public int[] readInts(int startIndex, int numInts) throws GameActionException {
//    //System.out.println("Read ints at " + startIndex + ": " + numInts);
    int[] ints = new int[numInts];
    for (int i = 0; i < numInts; i++) {
      ints[i] = Global.rc.readSharedArray((startIndex+i) % NUM_MESSAGING_INTS);//sharedBuffer[(startIndex+i) % NUM_MESSAGING_INTS];
    }
    return ints;
  }

  /**
   * write a set of ints into the message buffer starting at the given index
   *    cycles indices based on NUM_MESSAGING_INTS
   * @param startIndex where to start writing
   * @param information the ints to write
   * @throws GameActionException if writing fails
   */
  public void writeInts(int startIndex, int[] information) throws GameActionException {
//    //System.out.println("Write ints at " + startIndex + ": " + Arrays.toString(information));
    for (int i = 0; i < information.length; i++) {
      rc.writeSharedArray((startIndex + i) % NUM_MESSAGING_INTS, information[i]);
    }
  }
}
