package com.github.qfusion.fakeclient;

import static com.github.qfusion.fakeclient.ScoreboardData.*;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Half of tests in this class no longer make sense since the {@link ScoreboardData}
 * code has been greatly simplified, but we have decided to keep ones.
 */
public class ScoreboardDataTest extends TestCase {
    static void writeStringAndLength(char[] buffer, int offset, String value) {
        offset -= SCOREBOARD_DATA_OFFSET;
        char[] stringChars = new char[value.length()];
        value.getChars(0, value.length(), stringChars, 0);
        buffer[offset] = (char)stringChars.length;
        java.lang.System.arraycopy(stringChars, 0, buffer, offset + 1, stringChars.length);
    }

    static void writeIntAsBytesAndString(char[] buffer, int offset, int value) {
        buffer[offset + 0 - SCOREBOARD_DATA_OFFSET] = (char)((value >> 16) & 0xFFFF);
        buffer[offset + 1 - SCOREBOARD_DATA_OFFSET] = (char)((value >> 00) & 0xFFFF);
        writeStringAndLength(buffer, offset + 2, "" + value);
    }

    static void writeShortAsBytesAndString(char[] buffer, int offset, short value) {
        buffer[offset - SCOREBOARD_DATA_OFFSET] = (char)value;
        writeStringAndLength(buffer, offset + 1, "" + value);
    }

    static void setAddress(char[] buffer, String address) {
        writeStringAndLength(buffer, ADDRESS_OFFSET, address);
    }

    static void setServerName(char[] buffer, String name) {
        writeStringAndLength(buffer, SERVER_NAME_OFFSET, name);
    }

    static void setModName(char[] buffer, String modName) {
        writeStringAndLength(buffer, MODNAME_OFFSET, modName);
    }

    static void setGametype(char[] buffer, String gametype) {
        writeStringAndLength(buffer, GAMETYPE_OFFSET, gametype);
    }

    static void setMapName(char[] buffer, String mapName) {
        writeStringAndLength(buffer, MAPNAME_OFFSET, mapName);
    }

    static void setTimeMinutes(char[] buffer, int value) {
        writeIntAsBytesAndString(buffer, TIME_MINUTES_OFFSET, value);
    }

    static void setTimeSeconds(char[] buffer, short value) {
        writeShortAsBytesAndString(buffer, TIME_SECONDS_OFFSET, value);
    }

    static void setLimitMinutes(char[] buffer, int value) {
        writeIntAsBytesAndString(buffer, LIMIT_MINUTES_OFFSET, value);
    }

    static void setLimitSeconds(char[] buffer, short value) {
        writeShortAsBytesAndString(buffer, LIMIT_SECONDS_OFFSET, value);
    }

    static void setTimeFlags(char[] buffer, int value) {
        buffer[TIME_FLAGS_OFFSET - SCOREBOARD_DATA_OFFSET] = (char)value;
    }

    static void setAlphaName(char[] buffer, String name) {
        writeStringAndLength(buffer, ALPHA_NAME_OFFSET, name);
    }

    static void setAlphaScore(char[] buffer, int score) {
        writeIntAsBytesAndString(buffer, ALPHA_SCORE_OFFSET, score);
    }

    static void setBetaName(char[] buffer, String name) {
        writeStringAndLength(buffer, BETA_NAME_OFFSET, name);
    }

    static void setBetaScore(char[] buffer, int score) {
        writeIntAsBytesAndString(buffer, BETA_SCORE_OFFSET, score);
    }

    static void setMaxClients(char[] buffer, int maxClients) {
        writeShortAsBytesAndString(buffer, MAX_CLIENTS_OFFSET, (short)maxClients);
    }

    static void setNumClients(char[] buffer, int numClients) {
        writeShortAsBytesAndString(buffer, NUM_CLIENTS_OFFSET, (short)numClients);
    }

    static void setNumBots(char[] buffer, int numBots) {
        writeShortAsBytesAndString(buffer, NUM_BOTS_OFFSET, (short)numBots);
    }

    static void setNeedPassword(char[] buffer, boolean needPassword) {
        writeStringAndLength(buffer, NEED_PASSWORD_OFFSET, needPassword ? "yes" : "no");
    }

    static void setPlayerPing(int playerNum, char[] buffer, int ping) {
        int baseOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE;
        writeShortAsBytesAndString(buffer, baseOffset + PLAYER_PING_RELATIVE_OFFSET, (short)ping);
    }

    static void setPlayerScore(int playerNum, char[] buffer, int score) {
        int baseOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE;
        writeIntAsBytesAndString(buffer, baseOffset + PLAYER_SCORE_RELATIVE_OFFSET, score);
    }

    static void setPlayerName(int playerNum, char[] buffer, String name) {
        int baseOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE;
        writeStringAndLength(buffer, baseOffset + PLAYER_NAME_RELATIVE_OFFSET, name);
    }

    static void setPlayerTeam(int playerNum, char[] buffer, int team) {
        int baseOffset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE;
        writeShortAsBytesAndString(buffer, baseOffset + PLAYER_TEAM_RELATIVE_OFFSET, (short)team);
    }

    static void setHasPlayerInfo(char[] buffer, boolean value) {
        buffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = (char)(value ? 1 : 0);
    }

    private ScoreboardData newDefaultScoreboardData() {
        ScoreboardData result = new ScoreboardData();
        result.buffer = new char[PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET];
        return result;
    }

    public void testGetCommonStrings() {
        ScoreboardData scoreboardData = newDefaultScoreboardData();
        String address = "127.0.0.1:44400";
        setAddress(scoreboardData.buffer, address);
        String serverName = "Warsow Server";
        setServerName(scoreboardData.buffer, serverName);
        String modName = "basewsw";
        setModName(scoreboardData.buffer, modName);
        String gametype = "ffa";
        setGametype(scoreboardData.buffer, gametype);
        String mapName = "wca1";
        setMapName(scoreboardData.buffer, mapName);

        // It is easier to debug if these partial properties are tested prior to an actual value test
        assertEquals(ADDRESS_OFFSET + 1, scoreboardData.getAddress().arrayOffset + SCOREBOARD_DATA_OFFSET);
        assertEquals(address.length(), scoreboardData.getAddress().length);
        assertEquals(address, scoreboardData.getAddress().toString());

        assertEquals(SERVER_NAME_OFFSET + 1, scoreboardData.getServerName().arrayOffset + SCOREBOARD_DATA_OFFSET);
        assertEquals(serverName.length(), scoreboardData.getServerName().length);
        assertEquals(serverName, scoreboardData.getServerName().toString());

        assertEquals(MODNAME_OFFSET + 1, scoreboardData.getModName().arrayOffset + SCOREBOARD_DATA_OFFSET);
        assertEquals(modName.length(), scoreboardData.getModName().length());
        assertEquals(modName, scoreboardData.getModName().toString());

        assertEquals(GAMETYPE_OFFSET + 1, scoreboardData.getGametype().arrayOffset + SCOREBOARD_DATA_OFFSET);
        assertEquals(gametype.length(), scoreboardData.getGametype().length());
        assertEquals(gametype, scoreboardData.getGametype().toString());

        assertEquals(MAPNAME_OFFSET + 1, scoreboardData.getMapName().arrayOffset + SCOREBOARD_DATA_OFFSET);
        assertEquals(mapName.length(), scoreboardData.getMapName().length());
        assertEquals(mapName, scoreboardData.getMapName().toString());
    }

    public void testGetTimeParts() {
        ScoreboardData scoreboardData = newDefaultScoreboardData();
        int timeMinutes = 8;
        setTimeMinutes(scoreboardData.buffer, timeMinutes);
        short timeSeconds = 45;
        setTimeSeconds(scoreboardData.buffer, timeSeconds);
        int limitMinutes = 15;
        setLimitMinutes(scoreboardData.buffer, limitMinutes);
        short limitSeconds = 30;
        setLimitSeconds(scoreboardData.buffer, limitSeconds);

        int timeFlags = TIME_FLAG_OVERTIME | TIME_FLAG_TIMEOUT;
        setTimeFlags(scoreboardData.buffer, timeFlags);

        assertEquals(timeMinutes, scoreboardData.getMatchTimeMinutesValue());
        assertEquals("" + timeMinutes, scoreboardData.getMatchTimeMinutesChars().toString());

        assertEquals(timeSeconds, scoreboardData.getMatchTimeSecondsValue());
        assertEquals("" + timeSeconds, scoreboardData.getMatchTimeSecondsChars().toString());

        assertEquals(limitMinutes, scoreboardData.getTimeLimitMinutesValue());
        assertEquals("" + limitMinutes, scoreboardData.getTimeLimitMinutesChars().toString());

        assertEquals(limitSeconds, scoreboardData.getTimeLimitSecondsValue());
        assertEquals("" + limitSeconds, scoreboardData.getTimeLimitSecondsChars().toString());

        assertFalse(scoreboardData.isWarmup());
        assertFalse(scoreboardData.isCountdown());
        assertTrue(scoreboardData.isOvertime());
        assertFalse(scoreboardData.isSuddenDeath());
        assertFalse(scoreboardData.isFinished());
        assertTrue(scoreboardData.isTimeout());
    }

    public void testGetTeamNamesAndScores() {
        ScoreboardData scoreboardData = newDefaultScoreboardData();
        setAlphaName(scoreboardData.buffer, "ALPHA");
        setBetaName(scoreboardData.buffer, "BETA");
        setAlphaScore(scoreboardData.buffer, 7);
        setBetaScore(scoreboardData.buffer, 8);

        assertEquals("ALPHA", scoreboardData.getAlphaName().toString());
        assertEquals("BETA", scoreboardData.getBetaName().toString());

        assertEquals(7, scoreboardData.getAlphaScoreValue());
        assertEquals("7", scoreboardData.getAlphaScoreChars().toString());

        assertEquals(8, scoreboardData.getBetaScoreValue());
        assertEquals("8", scoreboardData.getBetaScoreChars().toString());
    }

    public void testGetNumClientsAndBots() {
        ScoreboardData scoreboardData = newDefaultScoreboardData();
        setMaxClients(scoreboardData.buffer, 256);
        setNumClients(scoreboardData.buffer, 15);
        setNumBots(scoreboardData.buffer, 3);

        assertEquals(256, scoreboardData.getMaxClientsValue());
        assertEquals("256", scoreboardData.getMaxClientsChars().toString());

        assertEquals(15, scoreboardData.getNumClientsValue());
        assertEquals("15", scoreboardData.getNumClientsChars().toString());

        assertEquals(3, scoreboardData.getNumBotsValue());
        assertEquals("3", scoreboardData.getNumBotsChars().toString());
    }

    public void testGetNeedPassword() {
        ScoreboardData scoreboardData = newDefaultScoreboardData();

        writeStringAndLength(scoreboardData.buffer, NEED_PASSWORD_OFFSET, "no");

        assertEquals("no", scoreboardData.getNeedPasswordChars().toString());
        assertFalse(scoreboardData.getNeedPasswordValue());

        writeStringAndLength(scoreboardData.buffer, NEED_PASSWORD_OFFSET, "yes");

        assertEquals("yes", scoreboardData.getNeedPasswordChars().toString());
        assertTrue(scoreboardData.getNeedPasswordValue());
    }

    public void testBasicWrapBuffers() {
        char[] buffer = new char[PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET];
        String serverName = "Warsow server";
        writeStringAndLength(buffer, SERVER_NAME_OFFSET, serverName);
        ScoreboardData scoreboardData = new ScoreboardData();
        scoreboardData.wrapBuffers(buffer, new byte[MAX_PLAYERS]);

        assertSame(buffer, scoreboardData.buffer);
        assertEquals(serverName, scoreboardData.getServerName().toString());
    }

    public void testWrapBuffersAndGetPlayerFields() {
        char[] buffer = newBufferForPlayersCount(4);
        setPlayerPing(0, buffer, 120);
        setPlayerScore(1, buffer, 1337);
        setPlayerName(2, buffer, "Player");
        setPlayerTeam(3, buffer, 1);
        setHasPlayerInfo(buffer, true);

        ScoreboardData scoreboardData = new ScoreboardData();
        scoreboardData.wrapBuffers(buffer, new byte[MAX_PLAYERS]);

        assertEquals(120, scoreboardData.getPlayerPingValue(0));
        assertEquals(3, scoreboardData.getPlayerPingChars(0).length());
        assertEquals("120", scoreboardData.getPlayerPingChars(0).toString());

        assertEquals(1337, scoreboardData.getPlayerScoreValue(1));
        assertEquals("1337", scoreboardData.getPlayerScoreChars(1).toString());

        assertEquals("Player", scoreboardData.getPlayerName(2).toString());

        assertEquals(1, scoreboardData.getPlayerTeam(3));
    }

    private char[] newBufferForPlayersCount(int playersCount) {
        char[] buffer = new char[PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET + playersCount * PLAYER_DATA_STRIDE];
        buffer[NUM_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET] = (char)playersCount;
        return buffer;
    }

    public void test_resizeIfNeeded_noNewPlayerInfo_createNewBuffers() {
        ScoreboardData scoreboardData = new ScoreboardData();
        assertNull(scoreboardData.buffer);
        scoreboardData.resizeIfNeeded(0, 3, false);
        assertNotNull(scoreboardData.buffer);
    }

    public void test_resizeIfNeeded_noNewPlayerInfo_keepExistingNullPlayersBuffers_1() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(0);
        setHasPlayerInfo(initialBuffer, false);

        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);
        assertEquals(scoreboardData.buffer, initialBuffer);

        scoreboardData.resizeIfNeeded(0, 0, false);

        assertSame(scoreboardData.buffer, initialBuffer);
    }

    public void test_resizeIfNeeded_noNewPlayerInfo_keepExistingNullPlayersBuffers_2() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(3);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 0;
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);
    }

    public void test_resizeIfNeeded_noNewPlayerInfo_nullifyExistingPlayersBuffers() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(3);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);

        scoreboardData.resizeIfNeeded(3, 0, false);

        assertNotSame(scoreboardData.buffer, initialBuffer);
    }

    public void test_resizeIfNeeded_hasNewPlayerInfo_createNewBuffers_2() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = new char[PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET];
        initialBuffer[NUM_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET] = 3;
        setHasPlayerInfo(initialBuffer, false);
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);

        scoreboardData.resizeIfNeeded(3, 3, true);
    }

    public void test_resizeIfNeeded_hasNewPlayerInfo_growPlayersBuffers() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(2);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);
        assertSame(scoreboardData.buffer, initialBuffer);

        setPlayerName(0, initialBuffer, "Player(0)");
        setPlayerName(1, initialBuffer, "Player(1)");

        setPlayerScore(0, initialBuffer, -1337);
        setPlayerScore(1, initialBuffer, +1337);

        // Force allocation of a CharArrayView for the second player
        CharArrayView secondPlayerScoreView = scoreboardData.getPlayerScoreChars(1);
        assertEquals("1337", secondPlayerScoreView.toString());

        scoreboardData.resizeIfNeeded(2, 15, true);

        assertEquals("Player(0)", scoreboardData.getPlayerName(0).toString());
        assertEquals("Player(1)", scoreboardData.getPlayerName(1).toString());

        assertEquals("-1337", scoreboardData.getPlayerScoreChars(0).toString());
        assertEquals("+1337", "+" + scoreboardData.getPlayerScoreChars(1).toString());
    }

    public void test_resizeIfNeeded_hasNewPlayerInfo_addToExistingPlayersBuffers() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(2);
        setHasPlayerInfo(initialBuffer, true);
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);
        assertSame(scoreboardData.buffer, initialBuffer);

        setPlayerName(0, initialBuffer, "Player(0)");
        setPlayerName(1, initialBuffer, "Player(1)");

        setPlayerScore(0, initialBuffer, 0);
        setPlayerScore(1, initialBuffer, 1);

        // Force allocation of a CharArrayView for the 2nd player score
        CharArrayView secondPlayerScoreView = scoreboardData.getPlayerScoreChars(1);
        assertEquals("1", secondPlayerScoreView.toString());

        scoreboardData.resizeIfNeeded(2, 3, true);

        assertEquals("Player(0)", scoreboardData.getPlayerName(0).toString());
        assertEquals("Player(1)", scoreboardData.getPlayerName(1).toString());

        assertEquals("0", scoreboardData.getPlayerScoreChars(0).toString());
        assertEquals("1", scoreboardData.getPlayerScoreChars(1).toString());
    }

    public void test_resizeIfNeeded_hasNewPlayerInfo_truncateKeepingExistingBuffers() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(8);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);

        for (int i = 0; i < 8; ++i) {
            setPlayerPing(i, initialBuffer, i);
            setPlayerName(i, initialBuffer, "Player(" + i + ")");
            setPlayerScore(i, initialBuffer, 8 - i);
        }

        List<CharArrayView[]> oldFieldsViews = new ArrayList<CharArrayView[]>();

        scoreboardData.resizeIfNeeded(8, 7, true);

        for (int i = 0; i < 7; ++i) {
            assertEquals(i, scoreboardData.getPlayerPingValue(i));
            assertEquals("" + i, scoreboardData.getPlayerPingChars(i).toString());
            assertEquals("Player(" + i + ")", scoreboardData.getPlayerName(i).toString());
            assertEquals(8 - i, scoreboardData.getPlayerScoreValue(i));
            assertEquals("" + (8 - i), scoreboardData.getPlayerScoreChars(i).toString());
        }
    }

    public void test_resizeIfNeeded_hasNewPlayerInfo_shrinkPlayersBuffers() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(72);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;
        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);
        assertSame(scoreboardData.buffer, initialBuffer);

        for (int i = 0; i < 72; ++i) {
            setPlayerPing(i, initialBuffer, i);
            setPlayerName(i, initialBuffer, "Player(" + i + ")");
            setPlayerScore(i, initialBuffer, 72 - i);
        }

        scoreboardData.resizeIfNeeded(72, 3, true);
        assertNotSame(scoreboardData.buffer, initialBuffer);

        for (int i = 0; i < 3; ++i) {
            assertEquals(i, scoreboardData.getPlayerPingValue(i));
            assertEquals("" + i, scoreboardData.getPlayerPingChars(i).toString());
            assertEquals("Player(" + i + ")", scoreboardData.getPlayerName(i).toString());
            assertEquals(72 - i, scoreboardData.getPlayerScoreValue(i));
            assertEquals("" + (72 - i), scoreboardData.getPlayerScoreChars(i).toString());
        }
    }

    private static List<ColoredToken> reconstructTokens(CharArrayView charsView, ByteArrayView tokensView) {
        if (BuildConfig.DEBUG) {
            if ((tokensView.getLength() % 3) != 0) {
                throw new AssertionError();
            }
        }

        List<ColoredToken> result = new ArrayList<ColoredToken>();
        byte[] packedTokens = tokensView.getArray();
        for (int i = tokensView.getArrayOffset(); i < tokensView.getArrayOffset() + tokensView.getLength(); i += 3) {
            Color color = Color.values()[packedTokens[i + 2]];
            result.add(new ColoredToken(charsView, packedTokens[i], packedTokens[i + 1], color));
        }

        return result;
    }

    public void testUpdateServerNameTokens() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(0);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 0;

        setServerName(initialBuffer, "^1W^2a^3r^4s^5o^6w ^7Server^0");
        setAlphaName(initialBuffer, "^8ALPHA^7");
        setBetaName(initialBuffer, "^9BETA^7");

        scoreboardData.wrapBuffers(initialBuffer, new byte[MAX_PLAYERS]);

        // There is no need to make tokens update explicitly, it is performed in the wrapBuffers() call

        assertEquals("Warsow Server", scoreboardData.getServerName().toString());
        assertEquals("ALPHA", scoreboardData.getAlphaName().toString());
        assertEquals("BETA", scoreboardData.getBetaName().toString());

        List<ColoredToken> serverNameTokens =
            reconstructTokens(scoreboardData.getServerName(), scoreboardData.getServerNameTokens());
        List<ColoredToken> alphaNameTokens =
            reconstructTokens(scoreboardData.getAlphaName(), scoreboardData.getAlphaNameTokens());
        List<ColoredToken> betaNameTokens =
            reconstructTokens(scoreboardData.getBetaName(), scoreboardData.getBetaNameTokens());

        assertEquals(7, serverNameTokens.size());
        assertEquals("Server", serverNameTokens.get(6).toString());

        assertEquals(1, alphaNameTokens.size());
        assertEquals("ALPHA", alphaNameTokens.get(0).toString());

        assertEquals(1, betaNameTokens.size());
        assertEquals("BETA", betaNameTokens.get(0).toString());
    }

    public void testUpdatePlayerNamesTokens() {
        ScoreboardData scoreboardData = new ScoreboardData();
        char[] initialBuffer = newBufferForPlayersCount(7);
        initialBuffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;

        for (int i = 0; i < 7; ++i) {
            setPlayerName(i, initialBuffer, "^0Player^" + i + "(" + i + ")^0");
        }

        byte[] playersInfoUpdateMask = new byte[MAX_PLAYERS];
        // Make sure masks for each player is set.
        // We do not do it for other tests since they there are no tokens in supplied strings
        Arrays.fill(playersInfoUpdateMask, Byte.MAX_VALUE);
        scoreboardData.wrapBuffers(initialBuffer, playersInfoUpdateMask);

        for (int i = 0; i < 7; ++i) {
            CharArrayView actualName = scoreboardData.getPlayerName(i);
            String expectedName = "Player(" + i + ")";
            assertEquals(expectedName, actualName.toString());
            List<ColoredToken> tokens = reconstructTokens(actualName, scoreboardData.getPlayerNameTokens(i));
            assertEquals(2, tokens.size());
            assertEquals("Player", tokens.get(0).toString());
            assertEquals("(" + i + ")", tokens.get(1).toString());
        }
    }
}
