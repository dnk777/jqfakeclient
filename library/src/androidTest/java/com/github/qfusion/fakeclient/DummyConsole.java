package com.github.qfusion.fakeclient;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class DummyConsole extends NativeBridgeConsole {

    private ArrayList<CharSequence> lines = new ArrayList<CharSequence>();

    private Charset charset = Charset.forName("UTF-8");

    public List<CharSequence> getLines() { return lines; }

    @Override
    protected AbstractIngoingBytesProcessor newIngoingBytesProcessor() {
        return new DummyIngoingBytesProcessor();
    }

    class DummyIngoingBytesProcessor extends AbstractIngoingBytesProcessor {
        StringBuilder currentLineBuilder = new StringBuilder();

        @Override
        protected void onNewBufferData(ByteBuffer buffer) {
            int bytesCount = buffer.remaining();
            byte[] bufferBytes = new byte[bytesCount];
            buffer.get(bufferBytes);
            String bufferString = new String(bufferBytes, charset);
            String s = bufferString.replace('\n', '!');
            Log.w(getClass().getName(), "BufferString: `" + s + "`");

            int i = 0;
            int start = 0;
            for (;;) {
                boolean newLine = false;
                while (i < bufferString.length()) {
                    if (bufferString.charAt(i) == '\n') {
                        newLine = true;
                        break;
                    }
                    i++;
                }
                currentLineBuilder.append(bufferString, start, i);
                if (newLine) {
                    lines.add(currentLineBuilder.toString());
                    currentLineBuilder.setLength(0);
                    i++;
                }
                start = i;
                if (i == bufferString.length()) {
                    break;
                }
            }
        }
    }
}
