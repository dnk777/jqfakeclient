package com.github.qfusion.fakeclient;

import java.nio.ByteBuffer;

/**
 * The native bindings expect instances of this class as consoles for {@link System} and {@link Client}
 */
public abstract class NativeBridgeConsole {

    NativeBridgeConsole() {
        ioBuffer = ByteBuffer.allocateDirect(4096);
    }

    ByteBuffer ioBuffer;

    /**
     * Called by the native code to notify of new data available in the ioBuffer
     * @param offset An offset of an ingoing data in bytes from the beginning of the buffer.
     * @param length A length of an ingoing data in bytes.
     */
    void onNewBufferData(int offset, int length) {
        ioBuffer.position(offset);
        ioBuffer.limit(offset + length);
        getIngoingBytesProcessor().onNewBufferData(ioBuffer);
    }

    static abstract class AbstractIngoingBytesProcessor {
        protected abstract void onNewBufferData(ByteBuffer buffer);
    }

    private AbstractIngoingBytesProcessor ingoingBytesProcessor = null;

    /**
     * @return An ingoing bytes processor used.
     *         Note that this method calls are idempotent.
     *         A reference to the same object is returned.
     */
    protected final AbstractIngoingBytesProcessor getIngoingBytesProcessor() {
        if (ingoingBytesProcessor == null) {
            ingoingBytesProcessor = newIngoingBytesProcessor();
        }
        return ingoingBytesProcessor;
    }

    protected abstract AbstractIngoingBytesProcessor newIngoingBytesProcessor();
}

