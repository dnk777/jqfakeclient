package com.github.qfusion.fakeclient;

import com.github.qfusion.fakeclient.RingBufferConsole.IngoingBytesProcessor;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class IngoingBytesProcessorTest extends TestCase {
    private final String[] testLines = {
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        "Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
            "laboris nisi ut aliquip ex ea commodo consequat. ",
        "Duis aute irure dolor in reprehenderit in voluptate velit " +
            " esse cillum dolore eu fugiat nulla pariatur.",
        "Excepteur sint occaecat cupidatat non proident, sunt in " +
            "culpa qui officia deserunt mollit anim id est laborum"
    };

    // This is an utility for testing only.
    // We do not want to make it a member of the RingLinesBuffer
    // (Disallow unefficient usage patterns and avoid jar bloating)
    private static List<String> ringBufferToList(RingLinesBuffer buffer) {
        ArrayList<String> result = new ArrayList<String>();
        RingLinesBuffer.OptimizedIterator iterator = buffer.optimizedIterator();
        while (iterator.hasNext()) {
            result.add(iterator.next().toString());
        }
        return result;
    }

    private static Charset charset = Charset.forName("UTF-8");

    private static ByteBuffer asBuffer(String string) {
        byte[] stringBytes = string.getBytes(charset);
        // Test whether the code works with ByteBuffer bounds properly
        byte[] wrappedBytes = new byte[stringBytes.length + 4];
        java.lang.System.arraycopy(stringBytes, 0, wrappedBytes, 2, stringBytes.length);
        // We need non-matching position and limit by default, use exactly this call
        ByteBuffer buffer = ByteBuffer.wrap(wrappedBytes);
        buffer.position(2);
        buffer.limit(2 + stringBytes.length);
        return buffer;
    }

    public void testNoNewlinesInInput() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(1);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer(testLines[0]));
        assertEquals(0, linesBuffer.size());
    }

    public void testSingleNewlineAtEndOfInput() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(2);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer(testLines[0] + "\n"));
        assertEquals(1, linesBuffer.size());
        assertEquals(testLines[0], linesBuffer.front().toString());
    }

    public void testSingleNewlineAtStartOfInput() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(2);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer("\n" + testLines[0]));
        assertEquals(1, linesBuffer.size());
        assertEquals("", linesBuffer.front().toString());
    }

    public void testSingleNewlineAtMidOfInput() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(4);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer(testLines[0] + "\n" + testLines[1]));
        assertEquals(1, linesBuffer.size());
        assertEquals(testLines[0], linesBuffer.front().toString());
    }

    public void testManyLinesJointByNewline() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(8);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        StringBuilder sb = new StringBuilder();
        for (String line: testLines) {
            sb.append(line).append('\n');
        }
        sb.setLength(sb.length() - 1);
        // Make sure our tests inputs are right.
        // Use framework assertions instead of an "unreliable" assert()
        assertEquals(4, testLines.length);
        assertTrue(linesBuffer.capacity() > 4);

        processor.onNewBufferData(asBuffer(sb.toString()));
        List<String> actualLines = ringBufferToList(linesBuffer);
        assertEquals(3, actualLines.size());

        assertEquals(testLines[0], actualLines.get(0));
        assertEquals(testLines[1], actualLines.get(1));
        assertEquals(testLines[2], actualLines.get(2));
        // The 4-th line remains in semi-built state, no newline has been met yet.
    }

    public void testManyLinesSurroundedByNewline() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(8);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        StringBuilder sb = new StringBuilder("\n");
        for (String line: testLines) {
            sb.append(line).append('\n');
        }
        // See the comment in the test above
        assertEquals(4, testLines.length);
        assertTrue(linesBuffer.capacity() > 5);

        processor.onNewBufferData(asBuffer(sb.toString()));
        List<String> actualLines = ringBufferToList(linesBuffer);
        assertEquals(5, actualLines.size());

        assertEquals("", actualLines.get(0));
        assertEquals(testLines[0], actualLines.get(1));
        assertEquals(testLines[1], actualLines.get(2));
        assertEquals(testLines[2], actualLines.get(3));
        assertEquals(testLines[3], actualLines.get(4));
    }

    public void testEmptyInput() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(1);
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer(""));
        assertEquals(0, linesBuffer.size());
    }

    public void testOnlyNewlines() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(1024);
        String newlines = "\n\n\n\n\n\n\n";
        assertTrue(newlines.length() < linesBuffer.capacity());
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer(newlines));
        List<String> actualLines = ringBufferToList(linesBuffer);
        assertEquals(newlines.length(), actualLines.size());
        for (String actualLine: actualLines) {
            assertEquals(0, actualLine.length());
        }
    }

    public void testSingleContinuedLine() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(1);
        String expectedLine = testLines[0] + testLines[1];
        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        processor.onNewBufferData(asBuffer(testLines[0]));
        assertEquals(0, linesBuffer.size());
        processor.onNewBufferData(asBuffer(testLines[1] + '\n'));
        assertEquals(1, linesBuffer.size());
        assertEquals(expectedLine, linesBuffer.front().toString());
    }

    public void testCharWiseInput() {
        RingLinesBuffer linesBuffer = new RingLinesBuffer(3);
        assertTrue(linesBuffer.capacity() == 3);
        assertTrue(testLines.length == 4);

        IngoingBytesProcessor processor = new IngoingBytesProcessor(linesBuffer);
        for (String line: testLines) {
            for (char ch: line.toCharArray()) {
                processor.onNewBufferData(asBuffer("" + ch));
            }
            processor.onNewBufferData(asBuffer("\n"));
        }

        assertEquals(3, linesBuffer.size());
        List<String> actualLines = ringBufferToList(linesBuffer);
        assertEquals(testLines[1], actualLines.get(0));
        assertEquals(testLines[2], actualLines.get(1));
        assertEquals(testLines[3], actualLines.get(2));
    }
}
