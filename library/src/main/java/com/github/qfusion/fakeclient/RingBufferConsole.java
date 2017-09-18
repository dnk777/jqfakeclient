package com.github.qfusion.fakeclient;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * A {@link NativeBridgeConsole} that dumps ingoing characters to a {@link RingLinesBuffer}
 */
public class RingBufferConsole extends NativeBridgeConsole {

    public interface Listener {
        void onNewLine(boolean backLineRemoved);
    }

    public void setListener(Listener listener) {
        if (BuildConfig.DEBUG) {
            // Check whether getIngoingBytesProcessor() calls are really idempotent.
            // Prevent losing a listener by setting it in some temporary processor object.
            if (getIngoingBytesProcessor() != getIngoingBytesProcessor()) {
                String message = "A contract has been violated: getIngoingBytesProcessor() calls are not idempotent";
                throw new AssertionError(message);
            }
        }

        ((IngoingBytesProcessor)getIngoingBytesProcessor()).listener = listener;
    }

    public void clear() {
        buffer.clear();
    }

    Listener listener = null;

    RingLinesBuffer buffer;

    public final RingLinesBuffer getBuffer() { return buffer; }

    public RingBufferConsole(int capacity) {
        buffer = new RingLinesBuffer(capacity);
    }

    /**
     * The class is static and uses explicit dependency injection via constructor to aid testing.
     */
    public static class IngoingBytesProcessor extends AbstractIngoingBytesProcessor {
        RingLinesBuffer buffer;
        Listener listener;

        public IngoingBytesProcessor(RingLinesBuffer buffer) {
            this.buffer = buffer;
        }

        private Charset charset = Charset.forName("UTF-8");

        @Override
        protected void onNewBufferData(ByteBuffer buffer) {
            int bytesCount = buffer.remaining();
            byte[] bufferByteArray = newBufferArray(bytesCount);
            buffer.get(bufferByteArray, 0, bytesCount);
            onNewBufferData(bufferByteArray, 0, bytesCount);
        }

        protected void onNewBufferData(byte[] bytes, int offset, int length) {
            int i = offset;
            int start = offset;

            String bufferString = new String(bytes, offset, length, charset);

            for (;;) {
                boolean newLine = false;
                while (i < length) {
                    if (bufferString.charAt(i) == '\n') {
                        newLine = true;
                        break;
                    }
                    i++;
                }
                appendLinePart(bufferString, start, i - start);
                if (newLine) {
                    completeLineBuilding();
                    i++;
                }
                start = i;
                if (i == offset + length) {
                    break;
                }
            }
        }

        byte[] bufferArray = null;

        protected byte[] newBufferArray(int length) {
            if (bufferArray == null) {
                bufferArray = new byte[length];
            } else if (bufferArray.length < length) {
                bufferArray = new byte[length];
            } else if (bufferArray.length > 4096 && bufferArray.length > (3 * length) / 2) {
                bufferArray = new byte[length];
            }
            return bufferArray;
        }

        protected void appendLinePart(String bufferString, int offset, int length) {
            buffer.appendLinePart(bufferString, offset, length);
        }

        protected void completeLineBuilding() {
            notifyOfNewLine(buffer.completeLineBuilding());
        }

        protected void notifyOfNewLine(boolean backLineRemoved) {
            if (listener != null) {
                listener.onNewLine(backLineRemoved);
            } else {
                LogProxy.w(getClass().getCanonicalName(),"The listener is not set, cannot call listener.onNewLine()");
            }
        }
    }

    @Override
    protected AbstractIngoingBytesProcessor newIngoingBytesProcessor() {
        return new IngoingBytesProcessor(buffer);
    }
}

