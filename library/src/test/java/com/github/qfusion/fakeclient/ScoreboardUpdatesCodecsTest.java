package com.github.qfusion.fakeclient;

import static com.github.qfusion.fakeclient.ScoreboardData.*;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class ScoreboardUpdatesCodecsTest extends TestCase {

    void writeStringAndLength(CharBuffer charBuffer, int offset, String value) {
        charBuffer.position(0);
        charBuffer.put(offset, (char)value.length());
        charBuffer.position(offset + 1);
        charBuffer.put(value.toCharArray());
    }

    void writeIntAsBytesAndString(CharBuffer charBuffer, int offset, int value) {
        charBuffer.position(0);
        charBuffer.put(offset + 0, (char)((value >> 16) & 0xFFFF));
        charBuffer.put(offset + 1, (char)((value >> 00) & 0xFFFF));
        writeStringAndLength(charBuffer, offset + 2, "" + value);
    }

    void writeShortAsBytesAndString(CharBuffer charBuffer, int offset, short value) {
        charBuffer.position(0);
        charBuffer.put(offset, (char)value);
        writeStringAndLength(charBuffer, offset + 1, "" + value);
    }

    void writeInt(CharBuffer charBuffer, int offset, int value) {
        charBuffer.position(0);
        charBuffer.put(offset + 0, (char)((value >> 16) & 0xFFFF));
        charBuffer.put(offset + 1, (char)((value >> 00) & 0xFFFF));
    }

    void writeShort(CharBuffer charBuffer, int offset, short value) {
        charBuffer.position(0);
        charBuffer.put(offset, (char)value);
    }

    void setAddress(CharBuffer charBuffer, String address) {
        writeStringAndLength(charBuffer, ADDRESS_OFFSET, address);
    }

    void setServerName(CharBuffer charBuffer, String serverName) {
        writeStringAndLength(charBuffer, SERVER_NAME_OFFSET, serverName);
    }

    void setModName(CharBuffer charBuffer, String modName) {
        writeStringAndLength(charBuffer, MODNAME_OFFSET, modName);
    }

    void setGametype(CharBuffer charBuffer, String gametype) {
        writeStringAndLength(charBuffer, GAMETYPE_OFFSET, gametype);
    }

    void setTimeMinutes(CharBuffer charBuffer, int value) {
        writeIntAsBytesAndString(charBuffer, TIME_MINUTES_OFFSET, value);
    }

    void setLimitMinutes(CharBuffer charBuffer, int value) {
        writeIntAsBytesAndString(charBuffer, LIMIT_MINUTES_OFFSET, value);
    }

    void setTimeSeconds(CharBuffer charBuffer, int value) {
        writeShortAsBytesAndString(charBuffer, TIME_SECONDS_OFFSET, (short)value);
    }

    void setLimitSeconds(CharBuffer charBuffer, int value) {
        writeShortAsBytesAndString(charBuffer, LIMIT_SECONDS_OFFSET, (short)value);
    }

    void setTimeFlags(CharBuffer charBuffer, int flags) {
        writeShort(charBuffer, TIME_FLAGS_OFFSET, (short)flags);
    }

    void setMapName(CharBuffer charBuffer, String mapName) {
        writeStringAndLength(charBuffer, MAPNAME_OFFSET, mapName);
    }

    void setAlphaName(CharBuffer charBuffer, String name) {
        writeStringAndLength(charBuffer, ALPHA_NAME_OFFSET, name);
    }

    void setAlphaScore(CharBuffer charBuffer, int score) {
        writeIntAsBytesAndString(charBuffer, ALPHA_SCORE_OFFSET, score);
    }

    void setBetaName(CharBuffer charBuffer, String name) {
        writeStringAndLength(charBuffer, BETA_NAME_OFFSET, name);
    }

    void setBetaScore(CharBuffer charBuffer, int score) {
        writeIntAsBytesAndString(charBuffer, BETA_SCORE_OFFSET, score);
    }

    void setMaxClients(CharBuffer charBuffer, int maxClients) {
        writeShortAsBytesAndString(charBuffer, MAX_CLIENTS_OFFSET, (short)maxClients);
    }

    void setNumClients(CharBuffer charBuffer, int numClients) {
        writeShortAsBytesAndString(charBuffer, NUM_CLIENTS_OFFSET, (short)numClients);
    }

    void setNumBots(CharBuffer charBuffer, int numBots) {
        writeShortAsBytesAndString(charBuffer, NUM_BOTS_OFFSET, (short)numBots);
    }

    void setHasPlayerInfo(CharBuffer charBuffer, boolean hasPlayerInfo) {
        writeShort(charBuffer, HAS_PLAYER_INFO_OFFSET, (short)(hasPlayerInfo ? 1 : 0));
    }

    void setNeedPassword(CharBuffer charBuffer, boolean needPassword) {
        writeStringAndLength(charBuffer, NEED_PASSWORD_OFFSET, needPassword ? "yes" : "no");
    }

    void setDummyUpdatesSizeForPlayersCount(CharBuffer charBuffer, int playersCount) {
        // Use the maximal feasible value
        int numChars = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET + playersCount * PLAYER_DATA_STRIDE;
        writeInt(charBuffer, UPDATE_CHARS_WRITTEN_OFFSET, numChars);
    }

    void setPlayerPing(int playerNum, CharBuffer charBuffer, int ping) {
        int offset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_PING_RELATIVE_OFFSET;
        writeShortAsBytesAndString(charBuffer, offset, (short)ping);
    }

    void setPlayerName(int playerNum, CharBuffer charBuffer, String name) {
        int offset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_NAME_RELATIVE_OFFSET;
        writeStringAndLength(charBuffer, offset, name);
    }

    void setPlayerScore(int playerNum, CharBuffer charBuffer, int score) {
        int offset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_SCORE_RELATIVE_OFFSET;
        writeIntAsBytesAndString(charBuffer, offset, score);
    }

    void setPlayerTeam(int playerNum, CharBuffer charBuffer, int team) {
        int offset = PLAYERS_DATA_OFFSET + playerNum * PLAYER_DATA_STRIDE + PLAYER_TEAM_RELATIVE_OFFSET;
        charBuffer.position(0);
        charBuffer.put(offset, (char)team);
    }

    ByteBuffer newByteBuffer(int playersCount) {
        // We allocate a bit more than its necessary but lets don't care
        return ByteBuffer.allocate(2 * (PLAYERS_DATA_OFFSET + playersCount * PLAYER_DATA_STRIDE));
    }

    ScoreboardUpdatesDeltaEncoder newEncoder(ByteBuffer byteBuffer, CharBuffer charBuffer) {
        return new ScoreboardUpdatesDeltaEncoder(byteBuffer, charBuffer);
    }

    ScoreboardUpdatesDeltaDecoder newDecoder() {
        return new ScoreboardUpdatesDeltaDecoder();
    }

    public void testEncodeAndDecodeServerInfo1() {
        ScoreboardData scoreboardData = new ScoreboardData();
        ByteBuffer byteBuffer = newByteBuffer(0);
        CharBuffer charBuffer = byteBuffer.asCharBuffer();

        setHasPlayerInfo(charBuffer, false);

        setDummyUpdatesSizeForPlayersCount(charBuffer, 0);

        int mask = 0;
        setAddress(charBuffer,"127.0.0.1");
        setModName(charBuffer, "basewsw");
        setMapName(charBuffer, "wca1");
        mask |= UPDATE_FLAG_ADDRESS | UPDATE_FLAG_MODNAME | UPDATE_FLAG_MAPNAME;

        setTimeMinutes(charBuffer, 15);
        setTimeSeconds(charBuffer, 30);
        mask |= UPDATE_FLAG_TIME_MINUTES | UPDATE_FLAG_TIME_SECONDS;

        setAlphaName(charBuffer, "ALPHA");
        setBetaName(charBuffer, "BETA");
        mask |= UPDATE_FLAG_ALPHA_NAME | UPDATE_FLAG_BETA_NAME;

        setNeedPassword(charBuffer, true);
        mask |= UPDATE_FLAG_NEED_PASSWORD;

        DeltaUpdateMessage message = newEncoder(byteBuffer, charBuffer).tryBuildDeltaMessage(mask);
        assertNotNull(message);

        newDecoder().decodeUpdateDelta(scoreboardData, mask, message);

        assertEquals("127.0.0.1", scoreboardData.getAddress().toString());
        assertEquals("basewsw", scoreboardData.getModName().toString());
        assertEquals("wca1", scoreboardData.getMapName().toString());

        assertEquals(15, scoreboardData.getMatchTimeMinutesValue());
        assertEquals("15", scoreboardData.getMatchTimeMinutesChars().toString());
        assertEquals(30, scoreboardData.getMatchTimeSecondsValue());
        assertEquals("30", scoreboardData.getMatchTimeSecondsChars().toString());

        assertEquals("ALPHA", scoreboardData.getAlphaName().toString());
        assertEquals("BETA", scoreboardData.getBetaName().toString());

        assertEquals(true, scoreboardData.getNeedPasswordValue());
        assertEquals("yes", scoreboardData.getNeedPasswordChars().toString());
    }

    public void testEncodeAndDecodeServerInfo2() {
        ScoreboardData scoreboardData = new ScoreboardData();
        ByteBuffer byteBuffer = newByteBuffer(0);
        CharBuffer charBuffer = byteBuffer.asCharBuffer();

        setHasPlayerInfo(charBuffer, false);

        setDummyUpdatesSizeForPlayersCount(charBuffer, 0);

        int mask = 0;
        setServerName(charBuffer, "Warsow server");
        setGametype(charBuffer, "ca");
        mask |= UPDATE_FLAG_SERVER_NAME | UPDATE_FLAG_GAMETYPE;

        setLimitMinutes(charBuffer, 2);
        setLimitSeconds(charBuffer, 30);
        setTimeFlags(charBuffer, TIME_FLAG_OVERTIME | TIME_FLAG_TIMEOUT);
        mask |= UPDATE_FLAG_LIMIT_MINUTES | UPDATE_FLAG_LIMIT_SECONDS | UPDATE_FLAG_TIME_FLAGS;

        setAlphaScore(charBuffer, 8);
        setBetaScore(charBuffer, -1);
        mask |= UPDATE_FLAG_ALPHA_SCORE | UPDATE_FLAG_BETA_SCORE;

        setMaxClients(charBuffer, 32);
        setNumClients(charBuffer, 20);
        setNumBots(charBuffer, 12);
        mask |= UPDATE_FLAG_MAX_CLIENTS | UPDATE_FLAG_NUM_CLIENTS | UPDATE_FLAG_NUM_BOTS;

        DeltaUpdateMessage message = newEncoder(byteBuffer, charBuffer).tryBuildDeltaMessage(mask);
        assertNotNull(message);

        newDecoder().decodeUpdateDelta(scoreboardData, mask, message);

        assertEquals("Warsow server", scoreboardData.getServerName().toString());
        assertEquals("ca", scoreboardData.getGametype().toString());

        assertEquals(2, scoreboardData.getTimeLimitMinutesValue());
        assertEquals("2", scoreboardData.getTimeLimitMinutesChars().toString());

        assertEquals(30, scoreboardData.getTimeLimitSecondsValue());
        assertEquals("30", scoreboardData.getTimeLimitSecondsChars().toString());

        assertTrue(scoreboardData.isOvertime());
        assertTrue(scoreboardData.isTimeout());

        assertEquals(8, scoreboardData.getAlphaScoreValue());
        assertEquals("8", scoreboardData.getAlphaScoreChars().toString());

        assertEquals(-1, scoreboardData.getBetaScoreValue());
        assertEquals("-1", scoreboardData.getBetaScoreChars().toString());

        assertEquals(32, scoreboardData.getMaxClientsValue());
        assertEquals("32", scoreboardData.getMaxClientsChars().toString());

        assertEquals(20, scoreboardData.getNumClientsValue());
        assertEquals("20", scoreboardData.getNumClientsChars().toString());

        assertEquals(12, scoreboardData.getNumBotsValue());
        assertEquals("12", scoreboardData.getNumBotsChars().toString());
    }

    public void test_encodeAndDecodePlayerInfo_hasAndHadPlayerInfo() {
        ByteBuffer byteBuffer = newByteBuffer(7);
        CharBuffer charBuffer = byteBuffer.asCharBuffer();

        setHasPlayerInfo(charBuffer, true);
        setNumClients(charBuffer, 7);

        int playerUpdateFlags = PLAYERINFO_UPDATE_FLAG_PING | PLAYERINFO_UPDATE_FLAG_SCORE;
        for (int i = 0; i < 7; ++i) {
            if (i % 2 != 1) {
                continue;
            }
            setPlayerPing(i, charBuffer, 10 + i * 5);
            setPlayerScore(i, charBuffer, 7 - i);
            setPlayerUpdateFlags(i, byteBuffer, playerUpdateFlags);
        }

        setDummyUpdatesSizeForPlayersCount(charBuffer, 7);
        int serverInfoMask = UPDATE_FLAG_HAS_PLAYER_INFO | UPDATE_FLAG_NUM_CLIENTS | UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES;
        DeltaUpdateMessage message = newEncoder(byteBuffer, charBuffer).tryBuildDeltaMessage(serverInfoMask);
        assertNotNull(message);
        assertTrue(message.hasPlayerInfo);
        assertTrue(message.werePlayerInfoUpdates);

        ScoreboardData scoreboardData = new ScoreboardData();
        scoreboardData.resizeIfNeeded(0, 7, true);
        scoreboardData.buffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;
        scoreboardData.buffer[NUM_CLIENTS_OFFSET - SCOREBOARD_DATA_OFFSET] = 7;

        newDecoder().decodeUpdateDelta(scoreboardData, serverInfoMask, message);

        for (int i = 0; i < 7; ++i) {
            if (i % 2 != 1) {
                continue;
            }
            int ping = 10 + i * 5;
            assertEquals(ping, scoreboardData.getPlayerPingValue(i));
            assertEquals("" + ping, scoreboardData.getPlayerPingChars(i).toString());
            int score = 7 - i;
            assertEquals(score, scoreboardData.getPlayerScoreValue(i));
            assertEquals("" + score, scoreboardData.getPlayerScoreChars(i).toString());
            setPlayerPing(i, charBuffer, 10 + i * 5);
            setPlayerScore(i, charBuffer, 7 - i);
            setPlayerUpdateFlags(i, byteBuffer, playerUpdateFlags);
        }
    }

    private void setPlayerUpdateFlags(int playerNum, ByteBuffer byteBuffer, int flags) {
        byteBuffer.position(PLAYERS_UPDATE_MASK_OFFSET * 2 + playerNum);
        byteBuffer.put((byte)flags);
    }

    public void test_encodeAndDecodePlayerInfo_hasAndDidNotHavePlayerInfo() {
        ScoreboardData scoreboardData = new ScoreboardData();
        ByteBuffer byteBuffer = newByteBuffer(5);
        CharBuffer charBuffer = byteBuffer.asCharBuffer();

        setHasPlayerInfo(charBuffer, true);
        setNumClients(charBuffer, 5);
        final int playerUpdateFlags =
            PLAYERINFO_UPDATE_FLAG_PING | PLAYERINFO_UPDATE_FLAG_NAME |
            PLAYERINFO_UPDATE_FLAG_TEAM | PLAYERINFO_UPDATE_FLAG_SCORE;

        for (int i = 0; i < 5; ++i) {
            setPlayerPing(i, charBuffer, 10 + i * 5);
            setPlayerName(i, charBuffer, "Player(" + i + ")");
            setPlayerScore(i, charBuffer, i);
            setPlayerTeam(i, charBuffer, i % 2 + 1);
            setPlayerUpdateFlags(i, byteBuffer, playerUpdateFlags);
        }

        setDummyUpdatesSizeForPlayersCount(charBuffer, 5);

        // Force scoreboard numClients update too (it was 0)
        int serverInfoMask = UPDATE_FLAG_HAS_PLAYER_INFO | UPDATE_FLAG_NUM_CLIENTS | UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES;
        DeltaUpdateMessage message = newEncoder(byteBuffer, charBuffer).tryBuildDeltaMessage(serverInfoMask);

        assertNotNull(message);
        assertTrue(message.hasPlayerInfo);
        assertTrue(message.werePlayerInfoUpdates);

        newDecoder().decodeUpdateDelta(scoreboardData, serverInfoMask, message);

        for (int i = 0; i < 5; ++i) {
            int ping = 10 + i * 5;
            assertEquals(ping, scoreboardData.getPlayerPingValue(i));
            assertEquals("" + ping, scoreboardData.getPlayerPingChars(i).toString());
            assertEquals("Player(" + i + ")", scoreboardData.getPlayerName(i).toString());
            assertEquals(i, scoreboardData.getPlayerScoreValue(i));
            assertEquals("" + i, scoreboardData.getPlayerScoreChars(i).toString());
            assertEquals(i % 2 + 1, scoreboardData.getPlayerTeam(i));
        }
    }

    public void test_encodeAndDecodePlayerInfo_doesNotHaveAndHadPlayerInfo() {
        ScoreboardData scoreboardData = new ScoreboardData();
        ByteBuffer byteBuffer = newByteBuffer(3);
        CharBuffer charBuffer = byteBuffer.asCharBuffer();

        scoreboardData.resizeIfNeeded(0, 3, true);
        scoreboardData.buffer[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = 1;
        scoreboardData.buffer[NUM_CLIENTS_OFFSET] = 3;

        setServerName(charBuffer, "Warsow server");
        setGametype(charBuffer, "ca");
        int serverInfoUpdateMask = UPDATE_FLAG_SERVER_NAME | UPDATE_FLAG_GAMETYPE;

        setHasPlayerInfo(charBuffer, false);
        setDummyUpdatesSizeForPlayersCount(charBuffer, 0);

        DeltaUpdateMessage message = newEncoder(byteBuffer, charBuffer).tryBuildDeltaMessage(serverInfoUpdateMask);
        assertNotNull(message);
        assertFalse(message.hasPlayerInfo);
        assertFalse(message.werePlayerInfoUpdates);

        newDecoder().decodeUpdateDelta(scoreboardData, serverInfoUpdateMask, message);

        assertEquals("Warsow server", scoreboardData.getServerName().toString());
        assertEquals("ca", scoreboardData.getGametype().toString());
    }
}
