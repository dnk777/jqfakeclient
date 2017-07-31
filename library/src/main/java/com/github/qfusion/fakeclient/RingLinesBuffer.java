package com.github.qfusion.fakeclient;

import java.util.NoSuchElementException;

import static java.lang.System.arraycopy;

/**
 * A ring buffer for building and storing received character lines.
 */
public final class RingLinesBuffer {
    CharArrayView[] lines;
    boolean isFrontLineCompleted;

    char[] currCharsBuffer;
    int defaultBufferSize;

    int currCharsBufferOffset;

    int freeLineIndex;
    int frontIndex;
    int backIndex;
    int linesCount;

    /**
     * Do not confuse with {@link java.util.Iterator}.
     */
    public class OptimizedIterator {
        int i;
        int index;

        public OptimizedIterator() {
            rewind();
        }

        public boolean hasNext() {
            return i != linesCount;
        }

        public CharArrayView next() {
            CharArrayView result = lines[index];
            i += 1;
            index = nextIndex(index);
            return result;
        }

        public void rewind() {
            i = 0;
            index = backIndex;
        }
    }

    public OptimizedIterator optimizedIterator() {
        return new OptimizedIterator();
    }

    public CharArrayView front() {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        return lines[frontIndex];
    }

    public CharArrayView back() {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        return lines[backIndex];
    }

    RingLinesBuffer(int capacity) {
        lines = new CharArrayView[capacity + 1];
        defaultBufferSize = 1024;
        this.currCharsBuffer = new char[defaultBufferSize];
        resetValues();
    }

    RingLinesBuffer(int capacity, int defaultBufferSize) {
        lines = new CharArrayView[capacity + 1];
        this.defaultBufferSize = defaultBufferSize;
        this.currCharsBuffer = new char[defaultBufferSize];
        resetValues();
    }

    protected void resetValues() {
        currCharsBufferOffset = 0;
        isFrontLineCompleted = false;
        freeLineIndex = 0;
        frontIndex = 0;
        backIndex = 0;
        linesCount = 0;
    }

    public int capacity() {
        return lines.length - 1;
    }

    public int size() {
        return linesCount;
    }

    public boolean isEmpty() {
        return linesCount == 0;
    }

    public void clear() {
        int index = backIndex;
        for (int i = 0; i < linesCount; ++i) {
            lines[index] = null;
            index = nextIndex(index);
        }
        resetValues();
    }

    protected final int nextIndex(int index) {
        return (index + 1) % lines.length;
    }

    protected final void appendLinePart(String string) {
        appendLinePart(string, 0, string.length());
    }

    protected void appendLinePart(String string, int offset, int length) {
        CharArrayView currLine = lines[freeLineIndex];
        if (currLine == null) {
            currLine = new CharArrayView();
            currLine.arrayRef = currCharsBuffer;
            currLine.arrayOffset = currCharsBufferOffset;
            currLine.length = 0;
            lines[freeLineIndex] = currLine;
            isFrontLineCompleted = false;
        }

        // Check whether there is enough room in the current buffer for the newly added chars
        if (currCharsBuffer.length - currCharsBufferOffset < length) {
            int totalLength = currCharsBufferOffset + length;
            if (totalLength < defaultBufferSize) {
                currCharsBuffer = new char[defaultBufferSize];
            } else {
                currCharsBuffer = new char[totalLength + 16];
            }
            currCharsBufferOffset = 0;
            // Copy existing data to the new buffer in this case
            if (currLine.length > 0) {
                arraycopy(currLine.arrayRef, currLine.arrayOffset, currCharsBuffer, 0, currLine.length);
                currCharsBufferOffset = currLine.length;
            }
            currLine.arrayRef = currCharsBuffer;
            currLine.arrayOffset = 0;
        }

        // Avoid member access in a performance-sensitive loop
        char[] dest = currLine.arrayRef;
        int destOffset = currCharsBufferOffset;
        for (int i = 0; i < length; ++i) {
            dest[destOffset + i] = string.charAt(offset + i);
        }

        currLine.length += length;
        currCharsBufferOffset += length;

        if (BuildConfig.DEBUG) {
            // Test assertions here, otherwise we get exceptions later somewhere else
            boolean wereErrors = false;
            if (currLine.arrayOffset < 0 || currLine.arrayOffset >= currLine.arrayRef.length) {
                wereErrors = true;
            } else if (currLine.length < 0 || currLine.length > currLine.arrayRef.length) {
                wereErrors = true;
            } else if (currLine.arrayOffset + currLine.length > currLine.arrayRef.length) {
                wereErrors = true;
            }
            if (wereErrors) {
                StringBuilder sb = new StringBuilder();
                sb.append("Malformed bounds: ");
                sb.append("currLine.arrayOffset=").append(currLine.arrayOffset).append(", ");
                sb.append("currLine.length=").append(currLine.length).append(", ");
                sb.append("currLine.arrayRef.length=").append(currLine.arrayRef.length);
                throw new AssertionError(sb.toString());
            }
        }
    }

    /**
     * Completes the current line building.
     * Advances the buffer front position.
     * Occasionally removes the back line (if there is no room left).
     * @return True if a back line has been removed
     */
    protected boolean completeLineBuilding() {
        if (isFrontLineCompleted) {
            if (BuildConfig.DEBUG) {
                if (lines[freeLineIndex] != null) {
                    throw new AssertionError();
                }
            }
            lines[freeLineIndex] = CharArrayView.EMPTY;
        }
        isFrontLineCompleted = true;
        linesCount++;
        if (linesCount > 1) {
            frontIndex = nextIndex(frontIndex);
        }
        freeLineIndex = nextIndex(freeLineIndex);
        if (freeLineIndex != backIndex) {
            return false;
        }

        lines[backIndex] = null;
        backIndex = nextIndex(backIndex);
        linesCount--;
        return true;
    }
}