package com.github.qfusion.fakeclient;

import junit.framework.TestCase;

public class RingLinesBufferTest extends TestCase {
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

    public void testNonEvictingAddition1() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        buffer.appendLinePart(testLines[0]);
        assertEquals(0, buffer.size());
        boolean completionResult = buffer.completeLineBuilding();

        assertEquals(false, completionResult);
        assertEquals(1, buffer.size());
        assertEquals(testLines[0], buffer.front().toString());
        assertEquals(testLines[0], buffer.back().toString());
    }

    public void testNonEvictingAddition2() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        // Append by a single character
        for (int i = 0; i < testLines[0].length(); ++i) {
            buffer.appendLinePart(testLines[0], i, 1);
        }
        assertEquals(0, buffer.size());
        boolean completionResult = buffer.completeLineBuilding();

        assertEquals(false, completionResult);
        assertEquals(1, buffer.size());
        assertEquals(testLines[0], buffer.front().toString());
        assertEquals(testLines[0], buffer.back().toString());
    }

    public void testNonEvictingAddition3() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 1024);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; ++i) {
            for (String line: testLines) {
                buffer.appendLinePart(line, 2, line.length() - 4);
                sb.append(line, 2, line.length() - 2);
            }
        }

        assertEquals(0, buffer.size());
        boolean completionResult = buffer.completeLineBuilding();
        assertEquals(false, completionResult);
        assertEquals(1, buffer.size());

        String expectedString = sb.toString();
        assertEquals(expectedString, buffer.front().toString());
        assertEquals(expectedString, buffer.back().toString());
    }

    public void testNonEvictingAddition4() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        buffer.appendLinePart(testLines[0]);
        assertEquals(0, buffer.size());
        assertEquals(false, buffer.completeLineBuilding());
        assertEquals(1, buffer.size());

        buffer.appendLinePart(testLines[1]);
        assertEquals(1, buffer.size());
        buffer.completeLineBuilding();
        assertEquals(2, buffer.size());

        buffer.appendLinePart(testLines[2]);
        assertEquals(2, buffer.size());
        buffer.completeLineBuilding();
        assertEquals(3, buffer.size());
    }

    public void testEvictingAddition1() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());
        assertEquals(4, testLines.length);

        buffer.appendLinePart(testLines[0]);
        assertEquals(false, buffer.completeLineBuilding());

        buffer.appendLinePart(testLines[1]);
        assertEquals(false, buffer.completeLineBuilding());

        buffer.appendLinePart(testLines[2]);
        assertEquals(false, buffer.completeLineBuilding());

        buffer.appendLinePart(testLines[3]);
        assertEquals(true, buffer.completeLineBuilding());

        assertEquals(3, buffer.size());
        assertEquals(3, buffer.capacity());

        assertEquals(testLines[3], buffer.front().toString());
        assertEquals(testLines[1], buffer.back().toString());
    }

    public void testEvictingAddition2() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());
        assertEquals(4, testLines.length);

        int evictionsCount = 0;
        for (String line: testLines) {
            buffer.appendLinePart(line);
            if (buffer.completeLineBuilding()) {
                evictionsCount++;
            }
        }

        assertEquals(1, evictionsCount);
        assertEquals(testLines[3], buffer.front().toString());
        assertEquals(testLines[1], buffer.back().toString());

        buffer.appendLinePart(testLines[0]);
        assertEquals(true, buffer.completeLineBuilding());
        assertEquals(testLines[0], buffer.front().toString());
        assertEquals(testLines[2], buffer.back().toString());

        buffer.appendLinePart(testLines[1]);
        assertEquals(true, buffer.completeLineBuilding());
        assertEquals(testLines[1], buffer.front().toString());
        assertEquals(testLines[3], buffer.back().toString());

        buffer.appendLinePart(testLines[2]);
        assertEquals(true, buffer.completeLineBuilding());
        assertEquals(testLines[2], buffer.front().toString());
        assertEquals(testLines[0], buffer.back().toString());

        buffer.appendLinePart(testLines[3]);
        assertEquals(true, buffer.completeLineBuilding());
        assertEquals(testLines[3], buffer.front().toString());
        assertEquals(testLines[1], buffer.back().toString());
    }

    public void testEvictingAddition3() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());
        assertEquals(4, testLines.length);

        int evictionsCount = 0;
        for (int i = 0; i < 3; ++i) {
            for (String line : testLines) {
                buffer.appendLinePart(line);
                if (buffer.completeLineBuilding()) {
                    evictionsCount++;
                }
            }
        }

        assertEquals(3 * testLines.length - buffer.capacity(), evictionsCount);
        // It has been proven with a sheet of paper :)
        assertEquals(testLines[3], buffer.front().toString());
        assertEquals(testLines[1], buffer.back().toString());
    }

    public void testEmptyLineAddition() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        for (int i = 0; i < testLines.length; ++i) {
            buffer.completeLineBuilding();
        }

        assertEquals("", buffer.front().toString());
        assertEquals("", buffer.back().toString());
    }

    public void testClear() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        for (String line: testLines) {
            buffer.appendLinePart(line);
            buffer.completeLineBuilding();
        }

        assertEquals(3, buffer.size());

        buffer.clear();

        assertEquals(0, buffer.size());
        assertEquals(3, buffer.capacity());

        for (char[] arrayRef: buffer.arrayRefs) {
            assertNull(arrayRef);
        }

        // Check whether the buffer can be reused after clear()

        for (String line: testLines) {
            buffer.appendLinePart(line);
            buffer.completeLineBuilding();
        }

        assertEquals(3, buffer.size());
        // This test assumes 4 test lines (do not use assert() due to an analyzer warning).
        assertEquals(4, testLines.length);
        assertEquals(testLines[3], buffer.front().toString());
        assertEquals(testLines[1], buffer.back().toString());
    }

    public void testIterationOverEmptyBuffer() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        RingLinesBuffer.OptimizedIterator iterator = buffer.optimizedIterator();
        assertEquals(false, iterator.hasNext());
    }

    public void testIterationOverSingleLineBuffer() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        buffer.appendLinePart(testLines[0]);

        RingLinesBuffer.OptimizedIterator iterator = buffer.optimizedIterator();
        assertEquals(false, iterator.hasNext());

        buffer.completeLineBuilding();
        iterator.rewind();
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[0], iterator.next().toString());
        assertEquals(false, iterator.hasNext());
    }

    public void testIterationOverFullBuffer() {
        RingLinesBuffer buffer = new RingLinesBuffer(3, 32);
        assertEquals(3, buffer.capacity());
        assertEquals(4, testLines.length);

        for (String line: testLines) {
            buffer.appendLinePart(line);
            buffer.completeLineBuilding();
        }

        RingLinesBuffer.OptimizedIterator iterator = buffer.optimizedIterator();
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[1], iterator.next().toString());
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[2], iterator.next().toString());
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[3], iterator.next().toString());
        assertEquals(false, iterator.hasNext());

        iterator.rewind();
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[1], iterator.next().toString());
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[2], iterator.next().toString());
        assertEquals(true, iterator.hasNext());
        assertEquals(testLines[3], iterator.next().toString());
        assertEquals(false, iterator.hasNext());
    }
}
