package com.github.qfusion.fakeclient;

public class ScoreboardData {
    static final int PLAYER_NAME_SIZE = 32;
    static final int TEAM_NAME_SIZE = 32;

    static final int UPDATE_CHARS_WRITTEN_OFFSET = 0;
    static final int UPDATE_HINT_READ_FULL_DATA_OFFSET = 2;
    static final int PLAYERS_UPDATE_MASK_OFFSET = 3;

    static final int MAX_PLAYERS = 256;
    static final int SCOREBOARD_DATA_OFFSET = PLAYERS_UPDATE_MASK_OFFSET + MAX_PLAYERS / 2 + 1;

    static final int HAS_PLAYER_INFO_OFFSET = SCOREBOARD_DATA_OFFSET;
    static final int ADDRESS_OFFSET = HAS_PLAYER_INFO_OFFSET + 1;
    static final int ADDRESS_SIZE = 48;

    // All buffers for string values have an extra character for string length
    // Also there might be extra characters at the start that represent an additional numeric value.
    // These extra characters come first (if any), then comes the string length, then the actual string data

    static final int SERVER_NAME_OFFSET = ADDRESS_OFFSET + ADDRESS_SIZE + 1;
    static final int SERVER_NAME_SIZE = 64;

    static final int MODNAME_OFFSET = SERVER_NAME_OFFSET + SERVER_NAME_SIZE + 1;
    static final int MODNAME_SIZE = 32;

    static final int GAMETYPE_OFFSET = MODNAME_OFFSET + MODNAME_SIZE + 1;
    static final int GAMETYPE_SIZE = 32;

    static final int MAPNAME_OFFSET = GAMETYPE_OFFSET + GAMETYPE_SIZE + 1;
    static final int MAPNAME_SIZE = 32;

    static final int TIME_MINUTES_OFFSET = MAPNAME_OFFSET + MAPNAME_SIZE + 1;
    // java.lang.Integer.MIN_VALUE.toString().length = 11
    static final int TIME_MINUTES_SIZE = 12;

    // 2 extra bytes for a numeric value of the time minutes
    static final int LIMIT_MINUTES_OFFSET = TIME_MINUTES_OFFSET + TIME_MINUTES_SIZE + 1 + 2;
    static final int LIMIT_MINUTES_SIZE = 12;

    // 2 extra bytes for a numeric value of the limit minutes
    static final int TIME_SECONDS_OFFSET = LIMIT_MINUTES_OFFSET + LIMIT_MINUTES_SIZE + 1 + 2;
    static final int TIME_SECONDS_SIZE = 4;

    // 1 extra byte for a numeric value of the time seconds
    static final int LIMIT_SECONDS_OFFSET = TIME_SECONDS_OFFSET + TIME_SECONDS_SIZE + 1 + 1;
    static final int LIMIT_SECONDS_SIZE = 4;

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
    static final int ALPHA_NAME_SIZE = TEAM_NAME_SIZE;

    static final int ALPHA_SCORE_OFFSET = ALPHA_NAME_OFFSET + ALPHA_NAME_SIZE + 1;
    static final int ALPHA_SCORE_SIZE = 12;

    // These 2 extra bytes are for a numeric representation of the alpha score
    static final int BETA_NAME_OFFSET = ALPHA_SCORE_OFFSET + ALPHA_SCORE_SIZE + 1 + 2;
    static final int BETA_NAME_SIZE = TEAM_NAME_SIZE;

    static final int BETA_SCORE_OFFSET = BETA_NAME_OFFSET + BETA_NAME_SIZE + 1;
    static final int BETA_SCORE_SIZE = 12;

    // 2 extra bytes too
    static final int MAX_CLIENTS_OFFSET = BETA_SCORE_OFFSET + BETA_SCORE_SIZE + 1 + 2;
    static final int MAX_CLIENTS_SIZE = 4;

    // This 1 extra byte is for a numeric representation of the max clients value
    static final int NUM_CLIENTS_OFFSET = MAX_CLIENTS_OFFSET + MAX_CLIENTS_SIZE + 1 + 1;
    static final int NUM_CLIENTS_SIZE = 4;

    // 1 extra byte too
    static final int NUM_BOTS_OFFSET = NUM_CLIENTS_OFFSET + NUM_CLIENTS_SIZE + 1 + 1;
    static final int NUM_BOTS_SIZE = 4;

    // 1 extra byte too
    static final int NEED_PASSWORD_OFFSET = NUM_BOTS_OFFSET + NUM_BOTS_SIZE + 1 + 1;
    // "yes" or "no"
    static final int NEED_PASSWORD_SIZE = 4;

    // An offset of the following players data of variable size.
    // This value also equals the minimal size of the character buffer
    static final int PLAYERS_DATA_OFFSET = NEED_PASSWORD_OFFSET + NEED_PASSWORD_SIZE;

    // An offset from the start of the player data for an N-th player
    static final int PLAYER_PING_RELATIVE_OFFSET = 0;
    static final int PLAYER_PING_SIZE = 6;

    // The 1 extra byte is for a numeric representation of the ping
    static final int PLAYER_SCORE_RELATIVE_OFFSET = PLAYER_PING_RELATIVE_OFFSET + PLAYER_PING_SIZE + 1 + 1;
    static final int PLAYER_SCORE_SIZE = 12;

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

    final CharArrayView[] stringFieldViews = new CharArrayView[17];

    private static final int VIEW_INDEX_ADDRESS = 0;
    private static final int VIEW_INDEX_SERVER_NAME = 1;
    private static final int VIEW_INDEX_MODNAME = 2;
    private static final int VIEW_INDEX_GAMETYPE = 3;
    private static final int VIEW_INDEX_MAPNAME = 4;
    private static final int VIEW_INDEX_TIME_MINUTES = 5;
    private static final int VIEW_INDEX_LIMIT_MINUTES = 6;
    private static final int VIEW_INDEX_TIME_SECONDS = 7;
    private static final int VIEW_INDEX_LIMIT_SECONDS = 8;
    private static final int VIEW_INDEX_ALPHA_NAME = 9;
    private static final int VIEW_INDEX_ALPHA_SCORE = 10;
    private static final int VIEW_INDEX_BETA_NAME = 11;
    private static final int VIEW_INDEX_BETA_SCORE = 12;
    private static final int VIEW_INDEX_MAX_CLIENTS = 13;
    private static final int VIEW_INDEX_NUM_CLIENTS = 14;
    private static final int VIEW_INDEX_NUM_BOTS = 15;
    private static final int VIEW_INDEX_NEED_PASSWORD = 16;

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

    public ScoreboardData() {
        for (int i = 0; i < stringFieldViews.length; ++i) {
            int lengthOffset = STRING_UPDATES_BUFFER_OFFSETS[i * 2 + 0];
            int bufferOffset = STRING_UPDATES_BUFFER_OFFSETS[i * 2 + 1];
            stringFieldViews[i] = CharArrayView.newForOffset(bufferOffset + lengthOffset + 1 - SCOREBOARD_DATA_OFFSET);
        }
    }

    public final CharArrayView getAddress() {
        return stringFieldViews[VIEW_INDEX_ADDRESS];
    }
    public final CharArrayView getServerName() {
        return stringFieldViews[VIEW_INDEX_SERVER_NAME];
    }
    public final CharArrayView getModName() {
        return stringFieldViews[VIEW_INDEX_MODNAME];
    }
    public final CharArrayView getGametype() {
        return stringFieldViews[VIEW_INDEX_GAMETYPE];
    }
    public final CharArrayView getMapName() {
        return stringFieldViews[VIEW_INDEX_MAPNAME];
    }

    public final CharArrayView getMatchTimeMinutesChars() {
        return stringFieldViews[VIEW_INDEX_TIME_MINUTES];
    }
    public final int getMatchTimeMinutesValue() {
        return getIntFromBuffer(TIME_MINUTES_OFFSET - SCOREBOARD_DATA_OFFSET);
    }
    public final CharArrayView getTimeLimitMinutesChars() {
        return stringFieldViews[VIEW_INDEX_LIMIT_MINUTES];
    }
    public final int getTimeLimitMinutesValue() {
        return getIntFromBuffer(LIMIT_MINUTES_OFFSET - SCOREBOARD_DATA_OFFSET);
    }
    public final CharArrayView getMatchTimeSecondsChars() {
        return stringFieldViews[VIEW_INDEX_TIME_SECONDS];
    }
    public final int getMatchTimeSecondsValue() {
        return getNonNegativeByteFromBuffer(TIME_SECONDS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }
    public final CharArrayView getTimeLimitSecondsChars() {
        return stringFieldViews[VIEW_INDEX_LIMIT_SECONDS];
    }
    public final int getTimeLimitSecondsValue() {
        return getNonNegativeByteFromBuffer(LIMIT_SECONDS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    private int getTimeFlags() {
        return getNonNegativeByteFromBuffer(TIME_FLAGS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final boolean isWarmup() { return (getTimeFlags() & TIME_FLAG_WARMUP) != 0; }
    public final boolean isCountdown() { return (getTimeFlags() & TIME_FLAG_COUNTDOWN) != 0; }
    public final boolean isOvertime() { return (getTimeFlags() & TIME_FLAG_OVERTIME) != 0; }
    public final boolean isSuddenDeath() { return (getTimeFlags() & TIME_FLAG_SUDDENDEATH) != 0; }
    public final boolean isFinished() { return (getTimeFlags() & TIME_FLAG_FINISHED) != 0; }
    public final boolean isTimeout() { return (getTimeFlags() & TIME_FLAG_TIMEOUT) != 0; }

    public final CharArrayView getAlphaName() {
        return stringFieldViews[VIEW_INDEX_ALPHA_NAME];
    }
    public final CharArrayView getAlphaScoreChars() {
        return stringFieldViews[VIEW_INDEX_ALPHA_SCORE];
    }
    public final int getAlphaScoreValue() {
        return getIntFromBuffer(ALPHA_SCORE_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getBetaName() {
        return stringFieldViews[VIEW_INDEX_BETA_NAME];
    }
    public final CharArrayView getBetaScoreChars() {
        return stringFieldViews[VIEW_INDEX_BETA_SCORE];
    }
    public final int getBetaScoreValue() {
        return getIntFromBuffer(BETA_SCORE_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getMaxClientsChars() {
        return stringFieldViews[VIEW_INDEX_MAX_CLIENTS];
    }
    public final short getMaxClientsValue() {
        return getShortFromBuffer(MAX_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getNumClientsChars() {
        return stringFieldViews[VIEW_INDEX_NUM_CLIENTS];
    }
    public final short getNumClientsValue() {
        return getShortFromBuffer(NUM_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getNumBotsChars() {
        return stringFieldViews[VIEW_INDEX_NUM_BOTS];
    }
    public final short getNumBotsValue() {
        return getShortFromBuffer(NUM_BOTS_OFFSET - SCOREBOARD_DATA_OFFSET);
    }

    public final CharArrayView getNeedPasswordChars() {
        return stringFieldViews[VIEW_INDEX_NEED_PASSWORD];
    }

    public final boolean getNeedPasswordValue() {
        CharArrayView view = stringFieldViews[VIEW_INDEX_NEED_PASSWORD];
        // TODO: It relies on the fact that the underlying native code does not use localized strings
        // Thats what we check below.
        if (BuildConfig.DEBUG) {
            String asString = view.toString();
            if (!asString.equals("yes") && !asString.equals("no")) {
                throw new AssertionError("Illegal `needPassword` property chars `" + asString + '`');
            }
        }
        return view.arrayRef[view.arrayOffset] == 'y';
    }

    public final boolean hasPlayerInfo() {
        return getShortFromBuffer(HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET) != 0;
    }

    int instanceId;
    char[] buffer;

    // An element #i in playerFieldViews[(0|1)] is guaranteed to be non-null if i is in range [0, numClients)
    static final int PLAYER_VIEW_INDEX_PING = 0;
    static final int PLAYER_VIEW_INDEX_NAME = 1;
    // An element #i in playerFieldsViews[2] NOT guaranteed to be non-null even if i is in range[0, numClients)
    static final int PLAYER_VIEW_INDEX_SCORE = 2;

    final CharArrayView[][] playerFieldsViews = new CharArrayView[3][];

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

    public final CharArrayView getPlayerPingChars(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        return playerFieldsViews[PLAYER_VIEW_INDEX_PING][playerNum];
    }

    public final short getPlayerPingValue(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        int offset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
        offset += playerNum * PLAYER_DATA_STRIDE + PLAYER_PING_RELATIVE_OFFSET;
        return getShortFromBuffer(offset);
    }

    public final CharArrayView getPlayerName(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        return playerFieldsViews[PLAYER_VIEW_INDEX_NAME][playerNum];
    }

    public final CharArrayView getPlayerScoreChars(int playerNum) {
        if (BuildConfig.DEBUG) {
            checkPlayerNum(playerNum);
        }
        CharArrayView chars = playerFieldsViews[PLAYER_VIEW_INDEX_SCORE][playerNum];
        if (chars == null) {
            int scoreLengthOffset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET + PLAYER_DATA_STRIDE * playerNum;
            scoreLengthOffset += PLAYER_SCORE_RELATIVE_OFFSET + 2;
            chars = new CharArrayView(buffer, scoreLengthOffset + 1, buffer[scoreLengthOffset]);
            playerFieldsViews[PLAYER_VIEW_INDEX_SCORE][playerNum] = chars;
        }
        return chars;
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

    public final int getInstanceId() { return instanceId; }

    final void resizeIfNeeded(int oldNumClients, int newNumClients, boolean newHasPlayerInfo) {
        if (buffer == null) {
            if (newHasPlayerInfo) {
                createNewBuffers(newNumClients);
            } else {
                createNewBuffers(0);
            }
            updatePlayersViewsArrays(oldNumClients, newNumClients, newHasPlayerInfo);
            return;
        }

        boolean hasPlayerInfo = hasPlayerInfo();
        if (newHasPlayerInfo && !hasPlayerInfo) {
            createNewBuffers(newNumClients);
            updatePlayersViewsArrays(oldNumClients, newNumClients, newHasPlayerInfo);
            return;
        }

        if (!newHasPlayerInfo && hasPlayerInfo) {
            if (oldNumClients > 0) {
                resizeBuffer(0);
                clearPlayersArrays();
            }
            return;
        }

        if (hasPlayerInfo) {
            if (oldNumClients < newNumClients) {
                resizeBuffer(newNumClients);
                updatePlayersViewsArrays(oldNumClients, newNumClients, newHasPlayerInfo);
                return;
            }
            if (oldNumClients > 8 && oldNumClients / newNumClients > 2) {
                resizeBuffer(newNumClients);
                updatePlayersViewsArrays(oldNumClients, newNumClients, newHasPlayerInfo);
                return;
            }
            updatePlayersViewsArrays(oldNumClients, newNumClients, newHasPlayerInfo);
            return;
        }

        clearPlayersArrays();
    }

    final void wrapBuffers(char[] newCharsBuffer, byte[] playersInfoUpdateMask) {
        this.buffer = newCharsBuffer;
        this.playersInfoUpdateMask = playersInfoUpdateMask;
        this.updateServerDataCharArrayViews(true);
        this.updatePlayersViewsArrays(0, getNumClientsValue(), hasPlayerInfo());
    }

    private int newMainBufferSize(int newNumClients) {
        return (PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET) + newNumClients * PLAYER_DATA_STRIDE;
    }

    private void createNewBuffers(int numClients) {
        int newBufferSize = newMainBufferSize(numClients);
        buffer = new char[newBufferSize];
        updateServerDataCharArrayViews(true);
    }

    private void resizeBuffer(int newNumClients) {
        int newBufferSize = newMainBufferSize(newNumClients);
        char[] newBuffer = new char[newBufferSize];
        java.lang.System.arraycopy(buffer, 0, newBuffer, 0, Math.min(buffer.length, newBufferSize));
        buffer = newBuffer;
    }

    /**
     * Updates all server data {@link CharArrayView} fields for a new char buffer array.
     * Should be called if a new char buffer array is set.
     * If a full update is performed, {@link CharArrayView#arrayOffset} fields are read from the char array buffer.
     * Note: {@link CharArrayView#arrayOffset} fields remain the same (they are set once on construction).
     * @param fullUpdate Whether full update should be done.
     */
    final void updateServerDataCharArrayViews(boolean fullUpdate) {
        if (fullUpdate) {
            for (CharArrayView view: stringFieldViews) {
                int lengthOffset = view.arrayOffset - 1;
                view.arrayRef = buffer;
                view.length = buffer[lengthOffset];
            }
        } else {
            for (CharArrayView view: stringFieldViews) {
                view.arrayRef = buffer;
            }
        }
    }

    private void updatePlayerDataCharArrayViews(int startingFrom, int newNumClients) {
        CharArrayView[] playerPingViews = playerFieldsViews[PLAYER_VIEW_INDEX_PING];
        CharArrayView[] playerNameViews = playerFieldsViews[PLAYER_VIEW_INDEX_NAME];
        CharArrayView[] playerScoreViews = playerFieldsViews[PLAYER_VIEW_INDEX_SCORE];

        if (BuildConfig.DEBUG) {
            if (newNumClients < startingFrom) {
                throw new AssertionError("newNumClients " + newNumClients + " < " + startingFrom);
            }
            if (startingFrom < 0) {
                throw new AssertionError("startingFrom " + startingFrom + " < 0 ");
            }
            if (playerPingViews == null) {
                throw new AssertionError("playerPingViews is null");
            }
            if (playerNameViews == null) {
                throw new AssertionError("playerNameViews is null");
            }
            if (playerScoreViews == null) {
                throw new AssertionError("playerScoreViews is null");
            }
        }

        int baseOffset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
        for (int i = startingFrom; i < newNumClients; ++i) {
            int pingLengthOffset = baseOffset + PLAYER_PING_RELATIVE_OFFSET + 1;
            CharArrayView pingView = playerPingViews[i];
            pingView.arrayRef = buffer;
            pingView.arrayOffset = pingLengthOffset + 1;
            pingView.length = buffer[pingLengthOffset];
            int nameLengthOffset = baseOffset + PLAYER_NAME_RELATIVE_OFFSET;
            CharArrayView nameView = playerNameViews[i];
            nameView.arrayRef = buffer;
            nameView.arrayOffset = nameLengthOffset + 1;
            nameView.length = buffer[nameLengthOffset];
            CharArrayView scoreView = playerScoreViews[i];
            if (scoreView != null) {
                int scoreLengthOffset = baseOffset + PLAYER_SCORE_RELATIVE_OFFSET + 2;
                scoreView.arrayRef = buffer;
                scoreView.arrayOffset = scoreLengthOffset + 1;
                scoreView.length = buffer[scoreLengthOffset];
            }
            baseOffset += PLAYER_DATA_STRIDE;
        }
    }

    private void clearPlayersArrays() {
        playerFieldsViews[PLAYER_VIEW_INDEX_PING] = null;
        playerFieldsViews[PLAYER_VIEW_INDEX_NAME] = null;
        playerFieldsViews[PLAYER_VIEW_INDEX_SCORE] = null;
    }

    /*
    private void updatePlayersViewsArrays(int oldNumClients, int newNumClients) {
        updatePlayersViewsArrays(oldNumClients, newNumClients, true);
    }*/

    private void updatePlayersViewsArrays(int oldNumClients, int newNumClients, boolean hasPlayerInfo) {
        if (newNumClients == 0 || !hasPlayerInfo) {
            clearPlayersArrays();
            return;
        }

        if (newNumClients <= oldNumClients) {
            if (playerFieldsViews[0] != null) {
                if (BuildConfig.DEBUG) {
                    checkFieldsViewsConsistentNullity();
                }
                if (oldNumClients <= 8 || newNumClients * 2 > oldNumClients) {
                    // Try keep existing view arrays, just let the used items in buffers be garbage-collected
                    nullifyPlayersFieldsViewsRefs(newNumClients, oldNumClients - newNumClients);
                } else {
                    // Create new arrays. Do not hold references for redundant old memory chunks.
                    resizePlayersFieldsViews(newNumClients + 4, newNumClients);
                }
            } else {
                createNewPlayersViewsArrays(newNumClients + 4);
                fillByNewFieldsViewsRefs(0, newNumClients);
            }

            updatePlayerDataCharArrayViews(0, newNumClients);
            return;
        }

        if (playerFieldsViews[PLAYER_VIEW_INDEX_NAME] != null) {
            if (BuildConfig.DEBUG) {
                checkFieldsViewsConsistentNullity();
            }

            // There is an extra allocated space, and so there is no need to resize existing views arrays
            if (newNumClients <= playerFieldsViews[PLAYER_VIEW_INDEX_NAME].length) {
                // Add required non-null elements at the array end
                fillByNewFieldsViewsRefs(oldNumClients, newNumClients - oldNumClients);
                updatePlayerDataCharArrayViews(0, newNumClients);
                return;
            }
        }

        if (BuildConfig.DEBUG) {
            if (oldNumClients == 0 && playerFieldsViews[PLAYER_VIEW_INDEX_NAME] != null) {
                throw new AssertionError();
            }
        }

        int newArraySize = newNumClients + 8;
        if (oldNumClients > 0) {
            resizePlayersFieldsViews(newNumClients + 8, oldNumClients);
        } else {
            createNewPlayersViewsArrays(newArraySize);
        }

        // Add required non-null references at the array end
        fillByNewFieldsViewsRefs(oldNumClients, newNumClients - oldNumClients);
        updatePlayerDataCharArrayViews(0, newNumClients);
    }

    private void createNewPlayersViewsArrays(int newArraysSize) {
        for (int i = 0; i < playerFieldsViews.length; ++i) {
            playerFieldsViews[i] = new CharArrayView[newArraysSize];
        }
    }

    private void resizePlayersFieldsViews(int newArraysSize, int copiedElementsCount) {
        for (int i = 0; i < playerFieldsViews.length; ++i) {
            resizePlayersFieldsView(newArraysSize, copiedElementsCount, i);
        }
    }

    private void resizePlayersFieldsView(int newArraysSize, int copiedElementsCount, int viewIndex) {
        CharArrayView[] oldPlayerPingViews = playerFieldsViews[viewIndex];
        CharArrayView[] newPlayerPingViews = new CharArrayView[newArraysSize];
        java.lang.System.arraycopy(oldPlayerPingViews, 0, newPlayerPingViews, 0, copiedElementsCount);
        playerFieldsViews[viewIndex] = newPlayerPingViews;
    }

    private void checkFieldsViewsConsistentNullity() {
        boolean wasNull = playerFieldsViews[0] == null;
        for (int i = 1; i < playerFieldsViews.length; ++i) {
            if ((playerFieldsViews[i] == null) != wasNull) {
                throw new AssertionError();
            }
        }
    }

    private void checkFieldsViewsIndexBounds(int fromIndex, int length) {
        if (playerFieldsViews[0] == null) {
            throw new AssertionError("playersFieldsViews[0] is null");
        }
        checkFieldsViewsConsistentNullity();

        if (fromIndex < 0 || fromIndex > playerFieldsViews[0].length) {
            throw new AssertionError("fromIndex " + fromIndex + " is out of bounds");
        }
        if (length < 0 || length > playerFieldsViews[0].length) {
            throw new AssertionError("length " + length + " is out of bounds");
        }
        if (fromIndex + length > playerFieldsViews[0].length) {
            throw new AssertionError("fromIndex " + fromIndex + " + length " + length + " is out of bounds");
        }
    }

    private void nullifyPlayersFieldsViewsRefs(int fromIndex, int length) {
        if (BuildConfig.DEBUG) {
            checkFieldsViewsIndexBounds(fromIndex, length);
        }
        CharArrayView[] playerPingViews = playerFieldsViews[PLAYER_VIEW_INDEX_PING];
        CharArrayView[] playerNameViews = playerFieldsViews[PLAYER_VIEW_INDEX_NAME];
        CharArrayView[] playerScoreViews = playerFieldsViews[PLAYER_VIEW_INDEX_SCORE];
        for (int i = fromIndex, end = fromIndex + length; i < end; ++i) {
            playerPingViews[i] = null;
            playerNameViews[i] = null;
            playerScoreViews[i] = null;
        }
    }

    private void fillByNewFieldsViewsRefs(int fromIndex, int length) {
        if (BuildConfig.DEBUG) {
            checkFieldsViewsIndexBounds(fromIndex, length);
        }
        CharArrayView[] playerPingViews = playerFieldsViews[PLAYER_VIEW_INDEX_PING];
        CharArrayView[] playerNameViews = playerFieldsViews[PLAYER_VIEW_INDEX_NAME];
        for (int i = fromIndex, end = fromIndex + length; i < end; ++i) {
            playerPingViews[i] = new CharArrayView();
            playerNameViews[i] = new CharArrayView();
        }
    }

    byte[] playersInfoUpdateMask;
}
