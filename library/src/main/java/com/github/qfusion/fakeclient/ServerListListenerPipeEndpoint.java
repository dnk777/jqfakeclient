package com.github.qfusion.fakeclient;

import java.util.Arrays;

import static com.github.qfusion.fakeclient.ScoreboardData.*;

public abstract class ServerListListenerPipeEndpoint implements ServerListListener {
    protected ScoreboardUpdatesDeltaDecoder deltaDecoder = new ScoreboardUpdatesDeltaDecoder();

    void addServer(int instanceId, char[] fullServerData) {
        newScoreboardData(instanceId).wrapBuffers(fullServerData, allFieldsSetPlayersUpdateMask);
        this.onServerAdded(instanceId);
    }

    void removeServer(int instanceId) {
        deleteScoreboardData(instanceId);
        this.onServerRemoved(instanceId);
    }

    void updateServerWithFullData(int instanceId, int serverInfoUpdateMask, char[] fullServerData) {
        findScoreboardData(instanceId).wrapBuffers(fullServerData, allFieldsSetPlayersUpdateMask);
        this.onServerUpdated(instanceId, serverInfoUpdateMask);
    }

    void updateServerWithDelta(int instanceId, int serverInfoUpdateMask, DeltaUpdateMessage deltaMessage) {
        ScoreboardData scoreboardData = findScoreboardData(instanceId);

        int oldNumClients = scoreboardData.buffer != null ? scoreboardData.getNumClientsValue() : 0;
        scoreboardData.resizeIfNeeded(oldNumClients, deltaMessage.numClients, deltaMessage.hasPlayerInfo);

        deltaDecoder.decodeUpdateDelta(scoreboardData, serverInfoUpdateMask, deltaMessage);
        this.onServerUpdated(instanceId, serverInfoUpdateMask);
    }

    private static final byte[] allFieldsSetPlayersUpdateMask = new byte[MAX_PLAYERS];

    static {
        Arrays.fill(allFieldsSetPlayersUpdateMask, PLAYERINFO_ALL_FIELDS_MASK);
    }

    protected abstract ScoreboardData newScoreboardData(int instanceId);
    protected abstract ScoreboardData findScoreboardData(int instanceId);
    protected abstract void deleteScoreboardData(int instanceId);
}

class ScoreboardUpdatesDeltaDecoder {
    void decodeUpdateDelta(ScoreboardData scoreboardData, int serverInfoUpdateMask, DeltaUpdateMessage message) {
        int updatesPtr = decodeServerInfo(scoreboardData, message, serverInfoUpdateMask);
        if (message.werePlayerInfoUpdates) {
            decodePlayerInfo(scoreboardData, message, updatesPtr);
        }
    }

    /**
     * @return An updates pointer (an offset in delta chars array) after reading server info updates.
     */
    private int decodeServerInfo(ScoreboardData scoreboardData, DeltaUpdateMessage message, int serverInfoUpdateMask) {
        int oldNumClients = scoreboardData.buffer != null ? scoreboardData.getNumClientsValue() : 0;
        scoreboardData.resizeIfNeeded(oldNumClients, message.numClients, message.hasPlayerInfo);

        final char[] deltaChars = message.deltaChars;
        final char[] bufferChars = scoreboardData.buffer;
        final int[] updatesFlags = STRING_UPDATES_FLAGS;
        final int[] bufferOffsets = STRING_UPDATES_BUFFER_OFFSETS;
        int deltaPtr = 0;
        for (int i = 0; i < updatesFlags.length; ++i) {
            if ((serverInfoUpdateMask & updatesFlags[i]) == 0) {
                continue;
            }
            // A relative offset of the string length in an updated chunk and in the scoreboard data buffer
            int lengthOffset = bufferOffsets[i * 2 + 0];
            // An absolute offset of the updated chunk in the scoreboard data buffer
            int bufferOffset = bufferOffsets[i * 2 + 1] - SCOREBOARD_DATA_OFFSET;
            // This length is written as a first char in a delta entry, an actual chunk follows
            int totalChunkLength = deltaChars[deltaPtr];
            // Copy all updated chars (which might include binary parts along with string data)
            java.lang.System.arraycopy(deltaChars, deltaPtr + 1, bufferChars, bufferOffset, totalChunkLength);
            // Go to the next delta entry (skip the current updated chunk length and chunk data)
            deltaPtr += totalChunkLength + 1;
        }

        if ((serverInfoUpdateMask & UPDATE_FLAG_TIME_FLAGS) != 0) {
            bufferChars[TIME_FLAGS_OFFSET - SCOREBOARD_DATA_OFFSET] = deltaChars[deltaPtr++];
        }
        if ((serverInfoUpdateMask & UPDATE_FLAG_HAS_PLAYER_INFO) != 0) {
            bufferChars[HAS_PLAYER_INFO_OFFSET - SCOREBOARD_DATA_OFFSET] = deltaChars[deltaPtr++];
        }

        return deltaPtr;
    }

    private void decodePlayerInfo(ScoreboardData scoreboardData, DeltaUpdateMessage message, int deltaPtr) {
        final char[] deltaChars = message.deltaChars;
        final char[] bufferChars = scoreboardData.buffer;
        final byte[] playersUpdateBytes = message.playersUpdateBytes;
        scoreboardData.playersInfoUpdateMask = playersUpdateBytes;

        for (int clientNum = 0, end = message.numClients; clientNum < end; ++clientNum) {
            int updateFlags = playersUpdateBytes[clientNum];
            if (updateFlags == 0) {
                continue;
            }
            final int[] playerInfoUpdatesFlags = PLAYERINFO_STRING_UPDATES_FLAGS;
            final int[] playerInfoUpdatesOffsets = PLAYERINFO_STRING_UPDATES_BUFFERS_OFFSETS;
            for (int flagNum = 0; flagNum < playerInfoUpdatesFlags.length; flagNum++) {
                if ((updateFlags & playerInfoUpdatesFlags[flagNum]) == 0) {
                    continue;
                }
                // This length is written as a first char in a delta entry, an actual chunk follows
                int totalChunkLength = deltaChars[deltaPtr];
                // A relative offset of a string length in an updated chunk and in the scoreboard data buffer
                int lengthOffset = playerInfoUpdatesOffsets[flagNum * 2 + 0];
                // An absolute offset of an updated chunk in the scoreboard data buffer
                int bufferOffset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET + clientNum * PLAYER_DATA_STRIDE;
                bufferOffset += playerInfoUpdatesOffsets[flagNum * 2 + 1];
                // Copy all updated chars (which might include binary parts along with string data)
                java.lang.System.arraycopy(deltaChars, deltaPtr + 1, scoreboardData.buffer, bufferOffset, totalChunkLength);
                // Go to the next delta entry (skip the current updated chunk length and chunk data)
                deltaPtr += totalChunkLength + 1;
            }
            if ((updateFlags & PLAYERINFO_UPDATE_FLAG_TEAM) != 0) {
                int offset = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
                offset += clientNum * PLAYER_DATA_STRIDE + PLAYER_TEAM_RELATIVE_OFFSET;
                bufferChars[offset] = deltaChars[deltaPtr++];
            }
        }
    }
}
