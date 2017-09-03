package com.github.qfusion.fakeclient;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static com.github.qfusion.fakeclient.ScoreboardData.*;

public class FeedingUiThreadServerListListener extends NativeBridgeServerListListener {
    final MessagePipe uiThreadPipe;
    final ServerListListenerPipeEndpoint pipeEndpoint;

    final ScoreboardUpdatesDeltaEncoder deltaEncoder = new ScoreboardUpdatesDeltaEncoder(byteIoBuffer, charIoBuffer);

    public FeedingUiThreadServerListListener(MessagePipe uiThreadPipe, ServerListListenerPipeEndpoint pipeEndpoint) {
        this.uiThreadPipe = uiThreadPipe;
        this.pipeEndpoint = pipeEndpoint;
    }

    @Override
    public void onServerAdded(final int instanceId) {
        final char[] fullData = readFullBufferData();
        this.uiThreadPipe.post(new Runnable() {
            @Override
            public void run() {
                pipeEndpoint.addServer(instanceId, fullData);
            }
        });
    }

    @Override
    public void onServerUpdated(final int instanceId, final int serverInfoUpdateMask) {
        final DeltaUpdateMessage message = deltaEncoder.tryBuildDeltaMessage(serverInfoUpdateMask);
        if (message == null) {
            final char[] fullData = readFullBufferData();
            this.uiThreadPipe.post(new Runnable() {
                @Override
                public void run() {
                    pipeEndpoint.updateServerWithFullData(instanceId, serverInfoUpdateMask, fullData);
                }
            });
            return;
        }
        // Make final reference copies to pass in the closure
        this.uiThreadPipe.post(new Runnable() {
            @Override
            public void run() {
                pipeEndpoint.updateServerWithDelta(instanceId, serverInfoUpdateMask, message);
            }
        });
    }

    @Override
    public void onServerRemoved(final int instanceId) {
        this.uiThreadPipe.post(new Runnable() {
            @Override
            public void run() {
                pipeEndpoint.removeServer(instanceId);
            }
        });
    }

    private char[] readFullBufferData() {
        charIoBuffer.position(0);
        boolean hasPlayerInfo = charIoBuffer.get(HAS_PLAYER_INFO_OFFSET) != 0;
        int numClients = charIoBuffer.get(NUM_CLIENTS_OFFSET);
        int arraySize = PLAYERS_DATA_OFFSET - SCOREBOARD_DATA_OFFSET;
        if (hasPlayerInfo) {
            arraySize += numClients * PLAYER_DATA_STRIDE;
        }
        charIoBuffer.position(SCOREBOARD_DATA_OFFSET);
        char[] chars = new char[arraySize];
        charIoBuffer.get(chars);
        return chars;
    }
}

final class DeltaUpdateMessage {
    char[] deltaChars;
    byte[] playersUpdateBytes;
    int numClients;
    boolean hasPlayerInfo;
    boolean werePlayerInfoUpdates;

    DeltaUpdateMessage() {}
}

class ScoreboardUpdatesDeltaEncoder {
    private ByteBuffer byteIoBuffer;
    private CharBuffer charIoBuffer;

    ScoreboardUpdatesDeltaEncoder(ByteBuffer byteIoBuffer, CharBuffer charIoBuffer) {
        this.byteIoBuffer = byteIoBuffer;
        this.charIoBuffer = charIoBuffer;
    }

    private char[] newDeltaChars() {
        charIoBuffer.position(0);
        int size = 0;
        // Decode the integer parts
        size |= charIoBuffer.get(UPDATE_CHARS_WRITTEN_OFFSET) << 16;
        size |= charIoBuffer.get(UPDATE_CHARS_WRITTEN_OFFSET + 1);
        // TODO: Discover why the native code gives an insufficient estimation so we have to add some extra bytes
        size += 32;
        return new char[size];
    }

    private boolean shouldBuildDeltaMessage() {
        charIoBuffer.position(0);
        return charIoBuffer.get(UPDATE_HINT_READ_FULL_DATA_OFFSET) == 0;
    }

    private int getNumClients() {
        // We assume that numClients numeric field is always written by the native code
        // even if there were no clients count updates
        charIoBuffer.position(0);
        return charIoBuffer.get(NUM_CLIENTS_OFFSET);
    }

    private boolean hasPlayerInfo() {
        // The notice above applies to "has player info" flag too.
        charIoBuffer.position(0);
        return charIoBuffer.get(HAS_PLAYER_INFO_OFFSET) != 0;
    }

    DeltaUpdateMessage tryBuildDeltaMessage(int serverInfoUpdateMask) {
        if (!shouldBuildDeltaMessage()) {
            return null;
        }

        DeltaUpdateMessage message = new DeltaUpdateMessage();
        message.deltaChars = newDeltaChars();

        int updatesPtr = encodeServerInfo(message, serverInfoUpdateMask);

        int numClients = message.numClients = getNumClients();
        boolean hasPlayerInfo = message.hasPlayerInfo = hasPlayerInfo();
        boolean expectUpdates = (serverInfoUpdateMask & UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES) != 0;
        if (hasPlayerInfo && expectUpdates) {
            encodePlayersInfo(message, updatesPtr, numClients);
        }

        if (BuildConfig.DEBUG) {
            // This flag has been introduced for convenience of debugging
            // after encodePlayersInfo() method has been implemented.
            // So we just check whether these flags match a-posteriori
            if (message.werePlayerInfoUpdates ^ expectUpdates) {
                throw new AssertionError(
                    "Player info updates status mismatch: expect updates = " +
                        expectUpdates + ", were updates detected = " + message.werePlayerInfoUpdates);
            }
        }

        return message;
    }

    /**
     * @return An updates pointer (an offset in delta chars) after writing server info delta.
     */
    private int encodeServerInfo(DeltaUpdateMessage message, int serverInfoUpdateMask) {
        int deltaPtr = 0;
        final int[] updatesFlags = STRING_UPDATES_FLAGS;
        final int[] updatesOffsets = STRING_UPDATES_BUFFER_OFFSETS;
        final char[] deltaChars = message.deltaChars;
        for (int i = 0; i < updatesFlags.length; ++i) {
            if ((serverInfoUpdateMask & updatesFlags[i]) != 0) {
                int lengthOffset = updatesOffsets[i * 2 + 0];
                int bufferOffset = updatesOffsets[i * 2 + 1];
                int charsRead = readUpdatedChunk(lengthOffset, bufferOffset, deltaChars, deltaPtr + 1);
                deltaChars[deltaPtr] = (char)charsRead;
                deltaPtr += charsRead + 1;
            }
        }

        if ((serverInfoUpdateMask & UPDATE_FLAG_TIME_FLAGS) != 0) {
            charIoBuffer.position(TIME_FLAGS_OFFSET);
            deltaChars[deltaPtr++] = charIoBuffer.get();
        }

        if ((serverInfoUpdateMask & UPDATE_FLAG_HAS_PLAYER_INFO) != 0) {
            charIoBuffer.position(HAS_PLAYER_INFO_OFFSET);
            deltaChars[deltaPtr++] = charIoBuffer.get();
        }

        return deltaPtr;
    }

    private void encodePlayersInfo(DeltaUpdateMessage message, int deltaPtr, int numClients) {
        // All scoreboard offsets are specified in chars, so we have to scale this offset twice
        byteIoBuffer.position(PLAYERS_UPDATE_MASK_OFFSET * 2);
        byteIoBuffer.get(tmpByteArray, 0, numClients);

        final char[] deltaChars = message.deltaChars;
        boolean wereUpdates = false;
        for (int i = 0; i < numClients; ++i) {
            if (tmpByteArray[i] == 0) {
                continue;
            }
            wereUpdates = true;
            int flags = tmpByteArray[i] & 0xFF;
            int baseOffset = PLAYERS_DATA_OFFSET + i * PLAYER_DATA_STRIDE;
            final int[] playerInfoUpdatesFlags = PLAYERINFO_STRING_UPDATES_FLAGS;
            final int[] playerInfoUpdatesOffsets = PLAYERINFO_STRING_UPDATES_BUFFERS_OFFSETS;
            for (int j = 0; j < playerInfoUpdatesFlags.length; j++) {
                if ((flags & playerInfoUpdatesFlags[j]) == 0) {
                    continue;
                }
                // A relative offset of a string length in the updated chunk
                int lengthOffset = playerInfoUpdatesOffsets[j * 2 + 0];
                // An absolute offset of an updated chunk in the scoreboard data buffer
                int bufferOffset = baseOffset + playerInfoUpdatesOffsets[j * 2 + 1];
                // A total number of chars read including a string length and binary integer value parts (if any)
                int charsRead = readUpdatedChunk(lengthOffset, bufferOffset, deltaChars, deltaPtr + 1);
                deltaChars[deltaPtr] = (char)charsRead;
                deltaPtr += charsRead + 1;
            }
            if ((flags & PLAYERINFO_UPDATE_FLAG_TEAM) != 0) {
                charIoBuffer.position(baseOffset + PLAYER_TEAM_RELATIVE_OFFSET);
                message.deltaChars[deltaPtr++] = charIoBuffer.get();
            }
        }

        if (wereUpdates) {
            message.playersUpdateBytes = new byte[numClients];
            java.lang.System.arraycopy(tmpByteArray, 0, message.playersUpdateBytes, 0, numClients);
        } else {
            message.playersUpdateBytes = noUpdatesByteArray;
        }

        message.werePlayerInfoUpdates = wereUpdates;
    }

    /**
     * Reads an updated memory chunk for a scoreboard entry.
     * This chunk includes string length, string chars.
     * Preceding integer binary parts (1 or 2 chars composing an int or a short) are also read if present.
     * @param lengthOffset A relative offset of a string length in an updated chunk.
     * @param bufferOffset An absolute offset of an updated chunk from the buffer start.
     * @param deltaChars A delta updates array being built.
     * @param deltaCharsPtr An offset in the delta updates array.
     * @return A total count of chars read (including string length and integer binary parts).
     */
    private int readUpdatedChunk(int lengthOffset, int bufferOffset, char[] deltaChars, int deltaCharsPtr) {
        charIoBuffer.position(bufferOffset + lengthOffset);
        int valueLength = charIoBuffer.get();
        int totalLength = valueLength + lengthOffset + 1;
        charIoBuffer.position(bufferOffset);
        charIoBuffer.get(deltaChars, deltaCharsPtr, totalLength);
        return totalLength;
    }

    private byte[] noUpdatesByteArray = new byte[MAX_PLAYERS];
    private byte[] tmpByteArray = new byte[MAX_PLAYERS];
}

