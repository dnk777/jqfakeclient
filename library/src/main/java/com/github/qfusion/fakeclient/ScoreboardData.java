package com.github.qfusion.fakeclient;

import android.support.annotation.VisibleForTesting;

public class ScoreboardData {
    public static final int PLAYER_NAME_SIZE = 32;
    public static final int TEAM_NAME_SIZE = 32;

    static final int UPDATE_CHARS_WRITTEN_OFFSET = 0;
    static final int UPDATE_HINT_READ_FULL_DATA_OFFSET = 2;
    static final int PLAYERS_UPDATE_MASK_OFFSET = 3;

    public static final int MAX_PLAYERS = 256;
    static final int SCOREBOARD_DATA_OFFSET = PLAYERS_UPDATE_MASK_OFFSET + MAX_PLAYERS / 2 + 1;

    static final int HAS_PLAYER_INFO_OFFSET = SCOREBOARD_DATA_OFFSET;
    static final int ADDRESS_OFFSET = HAS_PLAYER_INFO_OFFSET + 1;
    public static final int ADDRESS_SIZE = 48;

    // All buffers for string values have an extra character for string length
    // Also there might be extra characters at the start that represent an additional numeric value.
    // These extra characters come first (if any), then comes the string length, then the actual string data

    static final int SERVER_NAME_OFFSET = ADDRESS_OFFSET + ADDRESS_SIZE + 1;
    public static final int SERVER_NAME_SIZE = 64;

    static final int MODNAME_OFFSET = SERVER_NAME_OFFSET + SERVER_NAME_SIZE + 1;
    public static final int MODNAME_SIZE = 32;

    static final int GAMETYPE_OFFSET = MODNAME_OFFSET + MODNAME_SIZE + 1;
    public static final int GAMETYPE_SIZE = 32;

    static final int MAPNAME_OFFSET = GAMETYPE_OFFSET + GAMETYPE_SIZE + 1;
    public static final int MAPNAME_SIZE = 32;

    static final int TIME_MINUTES_OFFSET = MAPNAME_OFFSET + MAPNAME_SIZE + 1;
    // java.lang.Integer.MIN_VALUE.toString().length = 11
    public static final int TIME_MINUTES_SIZE = 12;

    // 2 extra bytes for a numeric value of the time minutes
    static final int LIMIT_MINUTES_OFFSET = TIME_MINUTES_OFFSET + TIME_MINUTES_SIZE + 1 + 2;
    public static final int LIMIT_MINUTES_SIZE = 12;

    // 2 extra bytes for a numeric value of the limit minutes
    static final int TIME_SECONDS_OFFSET = LIMIT_MINUTES_OFFSET + LIMIT_MINUTES_SIZE + 1 + 2;
    public static final int TIME_SECONDS_SIZE = 4;

    // 1 extra byte for a numeric value of the time seconds
    static final int LIMIT_SECONDS_OFFSET = TIME_SECONDS_OFFSET + TIME_SECONDS_SIZE + 1 + 1;
    public static final int LIMIT_SECONDS_SIZE = 4;

    // 1 extra byte for a numeric value of the limit seconds
    static final int TIME_FLAGS_OFFSET = LIMIT_SECONDS_OFFSET + LIMIT_SECONDS_SIZE + 1 + 1;

    static final int TIME_FLAG_WARMUP = 1 << 0;
    static final int TIME_FLAG_COUNTDOWN = 1 << 1;
    static final int TIME_FLAG_OVERTIME = 1 << 2;
    static final int TIME_FLAG_SUDDENDEATH = 1 << 3;
    static final int TIME_FLAG_FINISHED = 1 << 4;
    static final int TIME_FLAG_TIMEOUT = 1 << 5;

    static final int SCORE_OFFSET = TIME_FLAGS_OFFSET + 1;

    static final int ALPHA_NAME_OFFSET = SCORE_OFFSET;
    public static final int ALPHA_NAME_SIZE = TEAM_NAME_SIZE;

    static final int ALPHA_SCORE_OFFSET = ALPHA_NAME_OFFSET + ALPHA_NAME_SIZE + 1;
    public static final int ALPHA_SCORE_SIZE = 12;

    // These 2 extra bytes are for a numeric representation of the alpha score
    static final int BETA_NAME_OFFSET = ALPHA_SCORE_OFFSET + ALPHA_SCORE_SIZE + 1 + 2;
    public static final int BETA_NAME_SIZE = TEAM_NAME_SIZE;

    static final int BETA_SCORE_OFFSET = BETA_NAME_OFFSET + BETA_NAME_SIZE + 1;
    public static final int BETA_SCORE_SIZE = 12;

    // 2 extra bytes too
    static final int MAX_CLIENTS_OFFSET = BETA_SCORE_OFFSET + BETA_SCORE_SIZE + 1 + 2;
    public static final int MAX_CLIENTS_SIZE = 4;

    // This 1 extra byte is for a numeric representation of the max clients value
    static final int NUM_CLIENTS_OFFSET = MAX_CLIENTS_OFFSET + MAX_CLIENTS_SIZE + 1 + 1;
    public static final int NUM_CLIENTS_SIZE = 4;

    // 1 extra byte too
    static final int NUM_BOTS_OFFSET = NUM_CLIENTS_OFFSET + NUM_CLIENTS_SIZE + 1 + 1;
    public static final int NUM_BOTS_SIZE = 4;

    // 1 extra byte too
    static final int NEED_PASSWORD_OFFSET = NUM_BOTS_OFFSET + NUM_BOTS_SIZE + 1 + 1;
    // "yes" or "no"
    public static final int NEED_PASSWORD_SIZE = 4;

    // An offset of the following players data of variable size.
    // This value also equals the minimal size of the character buffer
    static final int PLAYERS_DATA_OFFSET = NEED_PASSWORD_OFFSET + NEED_PASSWORD_SIZE;

    // An offset from the start of the player data for an N-th player
    static final int PLAYER_PING_RELATIVE_OFFSET = 0;
    public static final int PLAYER_PING_SIZE = 6;

    // The 1 extra byte is for a numeric representation of the ping
    static final int PLAYER_SCORE_RELATIVE_OFFSET = PLAYER_PING_RELATIVE_OFFSET + PLAYER_PING_SIZE + 1 + 1;
    public static final int PLAYER_SCORE_SIZE = 12;

    // This field might contain color tokens
    // These 2 extra bytes are for a numeric representation of the player score
    static final int PLAYER_NAME_RELATIVE_OFFSET = PLAYER_SCORE_RELATIVE_OFFSET + PLAYER_SCORE_SIZE + 1 + 2;

    // An offset of the numeric value of a player's team
    static final int PLAYER_TEAM_RELATIVE_OFFSET = PLAYER_NAME_RELATIVE_OFFSET + PLAYER_NAME_SIZE + 1;
    // If a data for an N-th player starts from i-th character in the array,
    // the data for the next player starts from (i + PLAYER_DATA_STRIDE)-th character.
    static final int PLAYER_DATA_STRIDE = PLAYER_TEAM_RELATIVE_OFFSET + 8;

    static final int MAX_SCOREBOARD_DATA_SIZE = PLAYERS_DATA_OFFSET + MAX_PLAYERS * PLAYER_DATA_STRIDE;

    public static final int UPDATE_FLAG_ADDRESS = 1 << 0;

    public static final int UPDATE_FLAG_SERVER_NAME = 1 << 1;
    public static final int UPDATE_FLAG_MODNAME = 1 << 2;
    public static final int UPDATE_FLAG_GAMETYPE = 1 << 3;
    public static final int UPDATE_FLAG_MAPNAME = 1 << 4;

    public static final int UPDATE_FLAG_TIME_MINUTES = 1 << 8;
    public static final int UPDATE_FLAG_LIMIT_MINUTES = 1 << 9;
    public static final int UPDATE_FLAG_TIME_SECONDS = 1 << 10;
    public static final int UPDATE_FLAG_LIMIT_SECONDS = 1 << 11;
    public static final int UPDATE_FLAG_TIME_FLAGS = 1 << 12;

    public static final int UPDATE_FLAG_ALPHA_NAME = 1 << 16;
    public static final int UPDATE_FLAG_ALPHA_SCORE = 1 << 17;
    public static final int UPDATE_FLAG_BETA_NAME = 1 << 18;
    public static final int UPDATE_FLAG_BETA_SCORE = 1 << 19;

    public static final int UPDATE_FLAG_MAX_CLIENTS = 1 << 24;
    public static final int UPDATE_FLAG_NUM_CLIENTS = 1 << 25;
    public static final int UPDATE_FLAG_NUM_BOTS = 1 << 26;

    public static final int UPDATE_FLAG_NEED_PASSWORD = 1 << 29;
    public static final int UPDATE_FLAG_HAS_PLAYER_INFO = 1 << 30;
    public static final int UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES = 1 << 31;

    public static final byte PLAYERINFO_UPDATE_FLAG_PING = 1 << 0;
    public static final byte PLAYERINFO_UPDATE_FLAG_SCORE = 1 << 1;
    public static final byte PLAYERINFO_UPDATE_FLAG_NAME = 1 << 2;
    public static final byte PLAYERINFO_UPDATE_FLAG_TEAM = 1 << 3;

    public static final byte PLAYERINFO_ALL_FIELDS_MASK = (PLAYERINFO_UPDATE_FLAG_TEAM << 1) - 1;

    static final int[] STRING_UPDATES_FLAGS = {
        UPDATE_FLAG_ADDRESS,
        UPDATE_FLAG_SERVER_NAME,
        UPDATE_FLAG_MODNAME,
        UPDATE_FLAG_GAMETYPE,
        UPDATE_FLAG_MAPNAME,
        UPDATE_FLAG_TIME_MINUTES,
        UPDATE_FLAG_LIMIT_MINUTES,
        UPDATE_FLAG_TIME_SECONDS,
        UPDATE_FLAG_LIMIT_SECONDS,
        UPDATE_FLAG_ALPHA_NAME,
        UPDATE_FLAG_ALPHA_SCORE,
        UPDATE_FLAG_BETA_NAME,
        UPDATE_FLAG_BETA_SCORE,
        UPDATE_FLAG_MAX_CLIENTS,
        UPDATE_FLAG_NUM_CLIENTS,
        UPDATE_FLAG_NUM_BOTS,
        UPDATE_FLAG_NEED_PASSWORD
    };

    /**
     * The first value in a pair is a relative length offset inside an entry data,
     * The second value in a pair is an absolute entry data offset in the char io buffer
     */
    static final int[] STRING_UPDATES_BUFFER_OFFSETS = {
        0, ADDRESS_OFFSET,
        0, SERVER_NAME_OFFSET,
        0, MODNAME_OFFSET,
        0, GAMETYPE_OFFSET,
        0, MAPNAME_OFFSET,
        2, TIME_MINUTES_OFFSET,
        2, LIMIT_MINUTES_OFFSET,
        1, TIME_SECONDS_OFFSET,
        1, LIMIT_SECONDS_OFFSET,
        0, ALPHA_NAME_OFFSET,
        2, ALPHA_SCORE_OFFSET,
        0, BETA_NAME_OFFSET,
        2, BETA_SCORE_OFFSET,
        1, MAX_CLIENTS_OFFSET,
        1, NUM_CLIENTS_OFFSET,
        1, NUM_BOTS_OFFSET,
        0, NEED_PASSWORD_OFFSET
    };

    static final int[] PLAYERINFO_STRING_UPDATES_FLAGS = {
        PLAYERINFO_UPDATE_FLAG_PING,
        PLAYERINFO_UPDATE_FLAG_NAME,
        PLAYERINFO_UPDATE_FLAG_SCORE,
    };

    static final int[] PLAYERINFO_STRING_UPDATES_BUFFERS_OFFSETS = {
        1, PLAYER_PING_RELATIVE_OFFSET,
        0, PLAYER_NAME_RELATIVE_OFFSET,
        2, PLAYER_SCORE_RELATIVE_OFFSET,
    };

    static {
        if (BuildConfig.DEBUG) {
            if (STRING_UPDATES_FLAGS.length * 2 != STRING_UPDATES_BUFFER_OFFSETS.length) {
                throw new AssertionError();
            }
            if (PLAYERINFO_STRING_UPDATES_FLAGS.length * 2 != PLAYERINFO_STRING_UPDATES_BUFFERS_OFFSETS.length) {
                throw new AssertionError();
            }
        }
    }

    private CharArrayView getCharArrayView(int entryOffset, int lengthOffset, CharArrayView reuse) {
        int absoluteLengthOffset = entryOffset + lengthOffset - SCOREBOARD_DATA_OFFSET;
        reuse.arrayOffset = absoluteLengthOffset + 1;
        reuse.arrayRef = buffer;
        reuse.length = buffer[absoluteLengthOffset];
        return reuse;
    }

    private ByteArrayView getByteArrayView(int entryOffset, ByteArrayView reuse) {
        reuse.arrayOffset = entryOffset + 1;
        reuse.arrayRef = coloredTokens;
        reuse.length = coloredTokens[entryOffset];
        if (BuildConfig.DEBUG) {
            if ((reuse.length % 3) != 0) {
                throw new AssertionError("Reuse.length " + reuse.length);
            }
        }
        return reuse;
    }

    public final CharArrayView getAddress(CharArrayView reuse) {
        return getCharArrayView(ADDRESS_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getAddress() {
        return getAddress(new CharArrayView());
    }

    public final CharArrayView getServerName(CharArrayView reuse) {
        return getCharArrayView(SERVER_NAME_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getServerName() {
        return getServerName(new CharArrayView());
    }

    public final ByteArrayView getServerNameTokens(ByteArrayView reuse) {
        return getByteArrayView(TOKENS_SERVER_NAME_OFFSET, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ByteArrayView getServerNameTokens() {
        return getServerNameTokens(new ByteArrayView());
    }

    public final CharArrayView getModName(CharArrayView reuse) {
        return getCharArrayView(MODNAME_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getModName() {
        return getModName(new CharArrayView());
    }

    public final CharArrayView getGametype(CharArrayView reuse) {
        return getCharArrayView(GAMETYPE_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getGametype() {
        return getGametype(new CharArrayView());
    }

    public final CharArrayView getMapName(CharArrayView reuse) {
        return getCharArrayView(MAPNAME_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getMapName() {
        return getMapName(new CharArrayView());
    }


    public final CharArrayView getMatchTimeMinutesChars(CharArrayView reuse) {
        return getCharArrayView(TIME_MINUTES_OFFSET, 2, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getMatchTimeMinutesChars() {
        return getMatchTimeMinutesChars(new CharArrayView());
    }

    public final int getMatchTimeMinutesValue() {
        return getIntFromBuffer(TIME_MINUTES_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getTimeLimitMinutesChars(CharArrayView reuse) {
        return getCharArrayView(LIMIT_MINUTES_OFFSET, 2, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getTimeLimitMinutesChars() {
        return getTimeLimitMinutesChars(new CharArrayView());
    }

    public final int getTimeLimitMinutesValue() {
        return getIntFromBuffer(LIMIT_MINUTES_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getMatchTimeSecondsChars(CharArrayView reuse) {
        return getCharArrayView(TIME_SECONDS_OFFSET, 1, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getMatchTimeSecondsChars() {
        return getMatchTimeSecondsChars(new CharArrayView());
    }

    public final int getMatchTimeSecondsValue() {
        return getNonNegativeByteFromBuffer(TIME_SECONDS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getTimeLimitSecondsChars() {
        return getTimeLimitSecondsChars(new CharArrayView());
    }

    public final CharArrayView getTimeLimitSecondsChars(CharArrayView reuse) {
        return getCharArrayView(LIMIT_SECONDS_OFFSET, 1, reuse);
    }
    public final int getTimeLimitSecondsValue() {
        return getNonNegativeByteFromBuffer(LIMIT_SECONDS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    private int getTimeFlags() {
        return getNonNegativeByteFromBuffer(TIME_FLAGS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final boolean hasTimeFlags() { return getTimeFlags() != 0; }

    public final boolean isWarmup() { return (getTimeFlags() & TIME_FLAG_WARMUP) != 0; }
    public final boolean isCountdown() { return (getTimeFlags() & TIME_FLAG_COUNTDOWN) != 0; }
    public final boolean isOvertime() { return (getTimeFlags() & TIME_FLAG_OVERTIME) != 0; }
    public final boolean isSuddenDeath() { return (getTimeFlags() & TIME_FLAG_SUDDENDEATH) != 0; }
    public final boolean isFinished() { return (getTimeFlags() & TIME_FLAG_FINISHED) != 0; }
    public final boolean isTimeout() { return (getTimeFlags() & TIME_FLAG_TIMEOUT) != 0; }

    public final CharArrayView getAlphaName(CharArrayView reuse) {
        return getCharArrayView(ALPHA_NAME_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getAlphaName() {
        return getAlphaName(new CharArrayView());
    }

    public final ByteArrayView getAlphaNameTokens(ByteArrayView reuse) {
        return getByteArrayView(TOKENS_ALPHA_NAME_OFFSET, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ByteArrayView getAlphaNameTokens() {
        return getAlphaNameTokens(new ByteArrayView());
    }

    public final CharArrayView getAlphaScoreChars(CharArrayView reuse) {
        return getCharArrayView(ALPHA_SCORE_OFFSET, 2, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getAlphaScoreChars() {
        return getAlphaScoreChars(new CharArrayView());
    }

    public final int getAlphaScoreValue() {
        return getIntFromBuffer(ALPHA_SCORE_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getBetaName(CharArrayView reuse) {
        return getCharArrayView(BETA_NAME_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getBetaName() {
        return getBetaName(new CharArrayView());
    }

    public final ByteArrayView getBetaNameTokens(ByteArrayView reuse) {
        return getByteArrayView(TOKENS_BETA_NAME_OFFSET, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ByteArrayView getBetaNameTokens() {
        return getBetaNameTokens(new ByteArrayView());
    }

    public final CharArrayView getBetaScoreChars(CharArrayView reuse) {
        return getCharArrayView(BETA_SCORE_OFFSET, 2, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getBetaScoreChars() {
        return getBetaScoreChars(new CharArrayView());
    }

    public final int getBetaScoreValue() {
        return getIntFromBuffer(BETA_SCORE_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getMaxClientsChars(CharArrayView reuse) {
        return getCharArrayView(MAX_CLIENTS_OFFSET, 1, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getMaxClientsChars() {
        return getMaxClientsChars(new CharArrayView());
    }

    public final short getMaxClientsValue() {
        return getShortFromBuffer(MAX_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getNumClientsChars(CharArrayView reuse) {
        return getCharArrayView(NUM_CLIENTS_OFFSET, 1, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getNumClientsChars() {
        return getNumClientsChars(new CharArrayView());
    }

    public final short getNumClientsValue() {
        return getShortFromBuffer(NUM_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getNumBotsChars(CharArrayView reuse) {
        return getCharArrayView(NUM_BOTS_OFFSET, 1, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getNumBotsChars() {
        return getNumBotsChars(new CharArrayView());
    }

    public final short getNumBotsValue() {
        return getShortFromBuffer(NUM_BOTS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getNeedPasswordChars(CharArrayView reuse) {
        return getCharArrayView(NEED_PASSWORD_OFFSET, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getNeedPasswordChars() {
        return getNeedPasswordChars(new CharArrayView());
    }

    public final boolean getNeedPasswordValue() {
        // TODO: It relies on the fact that the underlying native code does not use localized strings
        // Thats what we check below.
        if (BuildConfig.DEBUG) {
            String asString = getNeedPasswordChars(new CharArrayView()).toString();
            if (!asString.equals("yes") && !asString.equals("no")) {
                throw new AssertionError("Illegal `needPassword` property chars `" + asString + '`');
            }
        }
        // Test the first "yes" or "no" string character following the string length
        return buffer[NEED_PASSWORD_OFFSET - SCOREBOARD_DATA_OFFSET + 1] == 'y';
    }

    public final boolean hasPlayerInfo() {
        return getShortFromBuffer(HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET) != 0;
    }

    int instanceId;
    char[] buffer;

    /**
     * A shared buffer for packed colored tokens.
     * An entry starts with the length of packed tokens sequence, and is followed by tokens.
     * Each token is packed in 3 bytes: offset, length and color.
     * Since there are no strings exceeding 64 characters, the byte type is satisfiable in this case.
     */
    byte[] coloredTokens;

    private static final int TOKENS_SERVER_NAME_OFFSET = 0;
    private static final int TOKENS_ALPHA_NAME_OFFSET = TOKENS_SERVER_NAME_OFFSET + 3 * SERVER_NAME_SIZE + 1;
    private static final int TOKENS_BETA_NAME_OFFSET = TOKENS_ALPHA_NAME_OFFSET + 3 * ALPHA_NAME_SIZE + 1;
    private static final int TOKENS_PLAYER_DATA_OFFSET = TOKENS_BETA_NAME_OFFSET + 3 * BETA_NAME_SIZE + 1;
    private static final int TOKENS_PLAYER_DATA_STRIDE = 3 * PLAYER_NAME_SIZE + 1;

    private int getIntFromBuffer(int offset) {
        return (buffer[offset] << 16) | buffer[offset + 1];
    }

    private short getShortFromBuffer(int offset) {
        return (short)buffer[offset];
    }

    private byte getNonNegativeByteFromBuffer(int offset) {
        char value = buffer[offset];
        if (BuildConfig.DEBUG) {
            if (value >= 128) {
                throw new AssertionError("Value " + (int)value + " at offset " + offset + " is out of range");
            }
        }
        return (byte)value;
    }

    private void checkPlayerNum(int playerNum) {
        int numClients = getNumClientsValue();
        if (playerNum < 0 || playerNum >= numClients) {
            String message = "playerNum " + playerNum + " is out of range [0, " + numClients + ")";
            throw new IllegalArgumentException(message);
        }
    }

    public final CharArrayView getPlayerPingChars(int playerNum, CharArrayView reuse) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int entryOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_PING_RELATIVE_OFFSET;
        return getCharArrayView(entryOffset, 1, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getPlayerPingChars(int playerNum) {
        return getPlayerPingChars(playerNum, new CharArrayView());
    }

    public final short getPlayerPingValue(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int offset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
        offset += playerNum * PLAYER_DATA_STRIDE + PLAYER_PING_RELATIVE_OFFSET;
        return getShortFromBuffer(offset);
    }

    public final CharArrayView getPlayerName(int playerNum, CharArrayView reuse) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int entryOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_NAME_RELATIVE_OFFSET;
        return getCharArrayView(entryOffset, 0, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getPlayerName(int playerNum) {
        return getPlayerName(playerNum, new CharArrayView());
    }

    public final CharArrayView getPlayerScoreChars(int playerNum, CharArrayView reuse) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int entryOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_SCORE_RELATIVE_OFFSET;
        return getCharArrayView(entryOffset, 2, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CharArrayView getPlayerScoreChars(int playerNum) {
        return getPlayerScoreChars(playerNum, new CharArrayView());
    }

    public final int getPlayerScoreValue(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int offset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
        offset += playerNum * PLAYER_DATA_STRIDE + PLAYER_SCORE_RELATIVE_OFFSET;
        return getIntFromBuffer(offset);
    }

    public final short getPlayerTeam(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int offset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
        offset += playerNum * PLAYER_DATA_STRIDE + PLAYER_TEAM_RELATIVE_OFFSET;
        return getShortFromBuffer(offset);
    }

    public final ByteArrayView getPlayerNameTokens(int playerNum, ByteArrayView reuse) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int entryOffset = TOKENS_PLAYER_DATA_OFFSET + playerNum * TOKENS_PLAYER_DATA_STRIDE;
        return getByteArrayView(entryOffset, reuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ByteArrayView getPlayerNameTokens(int playerNum) {
        return getPlayerNameTokens(playerNum, new ByteArrayView());
    }

    public final int getInstanceId() { return instanceId; }

    final void resizeIfNeeded(int oldNumClients, int newNumClients, boolean newHasPlayerInfo) {
        if (buffer == null) {
            if (newHasPlayerInfo) {
                createNewBuffers(newNumClients);
            } else {
                createNewBuffers(0);
            }
            return;
        }

        boolean hasPlayerInfo = hasPlayerInfo();
        if (newHasPlayerInfo && !hasPlayerInfo) {
            createNewBuffers(newNumClients);
            return;
        }

        if (!newHasPlayerInfo && hasPlayerInfo) {
            if (oldNumClients > 0) {
                resizeBuffers(0);
            }
            return;
        }

        if (hasPlayerInfo) {
            if (oldNumClients < newNumClients) {
                resizeBuffers(newNumClients);
                return;
            }
            if (oldNumClients > 8 && oldNumClients / newNumClients > 2) {
                resizeBuffers(newNumClients);
                return;
            }
        }
    }

    final void wrapBuffers(char[] newCharsBuffer, byte[] playersInfoUpdateMask) {
        // Since the scoreboard data is aware of colored tokens, we can't just set buffers and return
        int oldNumClients = 0;
        boolean oldHasPlayerInfo = false;
        if (this.buffer != null) {
            oldNumClients = this.getNumClientsValue();
            oldHasPlayerInfo = this.hasPlayerInfo();
        }

        this.buffer = newCharsBuffer;
        this.playersInfoUpdateMask = playersInfoUpdateMask;

        int numClients = getNumClientsValue();
        boolean hasPlayerInfo = hasPlayerInfo();

        // Check token buffers capacity
        if (this.coloredTokens != null) {
            if (oldHasPlayerInfo) {
                if (hasPlayerInfo) {
                    if (oldNumClients < numClients) {
                        coloredTokens = newColoredTokensBuffer(numClients);
                    } else if (oldNumClients > 8 && oldNumClients > numClients * 2) {
                        // Shrink buffers in this case
                        coloredTokens = newColoredTokensBuffer(numClients);
                    }
                } else {
                    if (coloredTokens.length > TOKENS_PLAYER_DATA_OFFSET) {
                        // Shrink buffers in this case
                        coloredTokens = newColoredTokensBuffer(0);
                    }
                }
            } else if (hasPlayerInfo) {
                coloredTokens = newColoredTokensBuffer(numClients);
            }
        } else {
            this.coloredTokens = newColoredTokensBuffer(numClients);
        }

        // Force tokens updates
        checkServerDataTokensUpdates(~0);
        if (hasPlayerInfo && numClients > 0) {
            updatePlayerNamesTokens();
        }
    }

    private int newMainBufferSize(int newNumClients) {
        return (PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET) + newNumClients * PLAYER_DATA_STRIDE;
    }

    private byte[] newColoredTokensBuffer(int newNumClients) {
        return new byte[TOKENS_PLAYER_DATA_OFFSET + newNumClients * TOKENS_PLAYER_DATA_STRIDE];
    }

    private void createNewBuffers(int numClients) {
        int newBufferSize = newMainBufferSize(numClients);
        buffer = new char[newBufferSize];
    }

    private void resizeBuffers(int newNumClients) {
        int newBufferSize = newMainBufferSize(newNumClients);
        char[] newBuffer = new char[newBufferSize];
        java.lang.System.arraycopy(buffer, 0, newBuffer, 0, Math.min(buffer.length, newBufferSize));
        buffer = newBuffer;

        byte[] newColoredTokens = newColoredTokensBuffer(newNumClients);
        int tokensBytesToCopy = Math.min(coloredTokens.length, newColoredTokens.length);
        java.lang.System.arraycopy(coloredTokens, 0, newColoredTokens, 0, tokensBytesToCopy);
        coloredTokens = newColoredTokens;
    }

    byte[] playersInfoUpdateMask;

    public final byte[] getPlayersInfoUpdateMask() { return playersInfoUpdateMask; }

    class ColoredTokensParser extends AbstractColoredTokensParser {
        int tokensArrayOffset;

        @Override
        protected void addWrappedToken(CharSequence underlying, int startIndex, int length, byte colorNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void addWrappedToken(String underlying, int startIndex, int length, byte colorNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void addWrappedToken(CharArrayView underlying, int startIndex, int length, byte colorNum) {
            byte[] coloredTokens = ScoreboardData.this.coloredTokens;
            coloredTokens[tokensArrayOffset++] = (byte)startIndex;
            coloredTokens[tokensArrayOffset++] = (byte)length;
            coloredTokens[tokensArrayOffset++] = colorNum;
        }
    }

    private final ColoredTokensParser parser = new ColoredTokensParser();
    /**
     * We assume nobody is going to use {@link ScoreboardData} from threads different from UI one.
     */
    private static final CharArrayView tmpCharArrayView = new CharArrayView();

    private void parseColoredTokens(CharArrayView view, int entryOffset, int viewLengthOffset) {
        parser.tokensArrayOffset = entryOffset + 1;
        parser.parseRemovingColors(view);
        if (BuildConfig.DEBUG) {
            int bytesWritten = parser.tokensArrayOffset - entryOffset - 1;
            if ((bytesWritten % 3) != 0) {
                throw new AssertionError();
            }
            if (bytesWritten < 0 || bytesWritten > Byte.MAX_VALUE) {
                throw new AssertionError();
            }
        }
        coloredTokens[entryOffset] = (byte)(parser.tokensArrayOffset - entryOffset - 1);
        // The view is a temporary object.
        // Its length property has been modifying after stripping circumflex escape sequences.
        // We have to modify buffer data as well to make the changes get saved
        buffer[viewLengthOffset - SCOREBOARD_DATA_OFFSET] = (char)view.length();
    }

    void checkServerDataTokensUpdates(int serverInfoUpdateMask) {
        if ((serverInfoUpdateMask & UPDATE_FLAG_SERVER_NAME) != 0) {
            parseColoredTokens(getServerName(tmpCharArrayView), TOKENS_SERVER_NAME_OFFSET, SERVER_NAME_OFFSET);
        }

        if ((serverInfoUpdateMask & UPDATE_FLAG_ALPHA_NAME) != 0) {
            parseColoredTokens(getAlphaName(tmpCharArrayView), TOKENS_ALPHA_NAME_OFFSET, ALPHA_NAME_OFFSET);
        }

        if ((serverInfoUpdateMask & UPDATE_FLAG_BETA_NAME) != 0) {
            parseColoredTokens(getBetaName(tmpCharArrayView), TOKENS_BETA_NAME_OFFSET, BETA_NAME_OFFSET);
        }
    }

    void updatePlayerNamesTokens() {
        for (int i = 0, numClients = getNumClientsValue(); i < numClients; ++i) {
            if ((playersInfoUpdateMask[i] & PLAYERINFO_UPDATE_FLAG_NAME) == 0) {
                continue;
            }
            int entryOffset = TOKENS_PLAYER_DATA_OFFSET + i * TOKENS_PLAYER_DATA_STRIDE;
            int viewLengthOffset = PLAYERS_DATA_OFFSET + i * PLAYER_DATA_STRIDE + PLAYER_NAME_RELATIVE_OFFSET;
            parseColoredTokens(getPlayerName(i, tmpCharArrayView), entryOffset, viewLengthOffset);
        }
    }
}
