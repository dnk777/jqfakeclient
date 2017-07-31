package com.github.qfusion.fakeclient;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * The native bindings expect instances of this class as listeners for {@link Client}.
 */
public abstract class NativeBridgeClientListener implements ClientListener {
    private ByteBuffer ioBuffer = ByteBuffer.allocateDirect(1024);
    private byte[] stringBuffer = new byte[1024];
    private Charset charset = Charset.forName("UTF-8");

    /**
     * Reads a {@link String} from the ioBuffer.
     * @param offset An offset in the underlying buffer memory in bytes
     * @param length A length of the UTF-8 encoded string data in bytes
     */
    private String getString(int offset, int length) {
        ioBuffer.position(offset);
        ioBuffer.get(stringBuffer, 0, length);
        return new String(stringBuffer, 0, length, charset);
    }

    /**
     * Called by the native code after it has filled the ioBuffer
     */
    private void onShownPlayerNameSet(int nameOffset, int nameLength) {
        this.onShownPlayerNameSet(getString(nameOffset, nameLength));
    }

    /**
     * Called by the native code after it has filled the ioBuffer
     */
    private void onMessageOfTheDaySet(int motdOffset, int motdLength) {
        this.onMessageOfTheDaySet(getString(motdOffset, motdLength));
    }

    /**
     * Called by the native code after it has filled the ioBuffer
     */
    private void onCenteredMessage(int messageOffset, int messageLength) {
        this.onCenteredMessage(getString(messageOffset, messageLength));
    }

    /**
     * Called by the native code after it has filled the ioBuffer
     */
    private void onChatMessage(int fromOffset, int fromLength, int messageOffset, int messageLength) {
        this.onChatMessage(getString(fromOffset, fromLength), getString(messageOffset, messageLength));
    }

    /**
     * Called by the native code after it has filled the ioBuffer
     */
    private void onTeamChatMessage(int fromOffset, int fromLength, int messageOffset, int messageLength) {
        this.onTeamChatMessage(getString(fromOffset, fromLength), getString(messageOffset, messageLength));
    }

    /**
     * Called by the native code after it has filled the ioBuffer
     */
    private void onTVChatMessage(int fromOffset, int fromLength, int messageOffset, int messageLength) {
        this.onTVChatMessage(getString(fromOffset, fromLength), getString(messageOffset, messageLength));
    }
}
