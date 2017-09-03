package com.github.qfusion.fakeclient;

import static com.github.qfusion.fakeclient.ScoreboardData.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

public abstract class NativeBridgeServerListListener implements ServerListListener {
    /**
     * A size of the shared io buffer in bytes.
     */
    private static final int BUFFER_SIZE = 2 * (PLAYERS_DATA_OFFSET + MAX_PLAYERS * PLAYER_DATA_STRIDE);

    static {
        if (MAX_PLAYERS % 2 != 0) {
            String message = "MAX_PLAYERS is expected to be even (each char should correspond to 2 player update masks)";
            throw new AssertionError(message);
        }
    }

    final ByteBuffer byteIoBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
    final CharBuffer charIoBuffer = byteIoBuffer.asCharBuffer();
}
