package com.github.qfusion.fakeclient;

import android.support.annotation.VisibleForTesting;

import java.util.NoSuchElementException;

import static java.lang.System.arraycopy;

/**
 * A ring buffer for building and storing received character lines.
 */
public final class RingLinesBuffer {
    /**
     * An i-th element contains a reference to char[] array of an i-th line
     */
    char[][] arrayRefs;
    /**
     * An element #(i * 2 + 0) contains an offset of a line data inside arrayRefs[i]
     * An element #(i * 2 + 1) contains an length of a line data chunk inside arrayRefs[i]
     */
    int[] offsetsAndLengths;

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
    public final class OptimizedIterator {
        int i;
        int index;

        public OptimizedIterator() {
            rewind();
        }

        public boolean hasNext() {
            return i != linesCount;
        }

        public CharArrayView next(CharArrayView reuse) {
            reuse.arrayRef = arrayRefs[index];
            reuse.arrayOffset = offsetsAndLengths[index * 2 + 0];
            reuse.length = offsetsAndLengths[index * 2 + 1];
            i += 1;
            index = nextIndex(index);
            return reuse;
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        public CharArrayView next() {
            return next(new CharArrayView());
        }

        public void rewind() {
            i = 0;
            index = backIndex;
        }
    }

    public OptimizedIterator optimizedIterator() {
        return new OptimizedIterator();
    }

    public CharArrayView front(CharArrayView reuse) {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        reuse.arrayRef = arrayRefs[frontIndex];
        reuse.arrayOffset = offsetsAndLengths[frontIndex * 2 + 0];
        reuse.length = offsetsAndLengths[frontIndex * 2 + 1];
        return reuse;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public CharArrayView front() {
        return front(new CharArrayView());
    }

    public CharArrayView back(CharArrayView reuse) {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        reuse.arrayRef = arrayRefs[backIndex];
        reuse.arrayOffset = offsetsAndLengths[backIndex * 2 + 0];
        reuse.length = offsetsAndLengths[backIndex * 2 + 1];
        return reuse;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public CharArrayView back() {
        return back(new CharArrayView());
    }

    RingLinesBuffer(int capacity) {
        arrayRefs = new char[capacity + 1][];
        offsetsAndLengths = new int[2 * capacity + 2];
        defaultBufferSize = 1024;
        this.currCharsBuffer = new char[defaultBufferSize];
        resetValues();
    }

    RingLinesBuffer(int capacity, int defaultBufferSize) {
        arrayRefs = new char[capacity + 1][];
        offsetsAndLengths = new int[2 * capacity + 2];
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
        return arrayRefs.length - 1;
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
            arrayRefs[index] = null;
            index = nextIndex(index);
        }
        resetValues();
    }

    protected final int nextIndex(int index) {
        return (index + 1) % arrayRefs.length;
    }

    protected final void appendLinePart(String string) {
        appendLinePart(string, 0, string.length());
    }

    protected void appendLinePart(String string, int offset, int length) {
        if (arrayRefs[freeLineIndex] == null) {
            arrayRefs[freeLineIndex] = currCharsBuffer;
            offsetsAndLengths[freeLineIndex * 2 + 0] = currCharsBufferOffset;
            offsetsAndLengths[freeLineIndex * 2 + 1] = 0;
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
            int currLineLength = offsetsAndLengths[freeLineIndex * 2 + 1];
            if (currLineLength > 0) {
                int currLineOffset = offsetsAndLengths[freeLineIndex * 2 + 0];
                arraycopy(arrayRefs[freeLineIndex], currLineOffset, currCharsBuffer, 0, currLineLength);
                currCharsBufferOffset = currLineLength;
            }
            arrayRefs[freeLineIndex] = currCharsBuffer;
            offsetsAndLengths[freeLineIndex * 2 + 0] = 0;
        }

        // Avoid member access in a performance-sensitive loop
        char[] dest = arrayRefs[freeLineIndex];
        int destOffset = currCharsBufferOffset;
        for (int i = 0; i < length; ++i) {
            dest[destOffset + i] = string.charAt(offset + i);
        }

        offsetsAndLengths[freeLineIndex * 2 + 1] += length;
        currCharsBufferOffset += length;

        if (BuildConfig.DEBUG) {
            int currLineOffset = offsetsAndLengths[freeLineIndex * 2 + 0];
            int currLineLength = offsetsAndLengths[freeLineIndex * 2 + 1];
            // Test assertions here, otherwise we get exceptions later somewhere else
            boolean wereErrors = false;
            if (currLineOffset < 0 || currLineOffset >= arrayRefs[freeLineIndex].length) {
                wereErrors = true;
            } else if (currLineLength < 0 || currLineLength > arrayRefs[freeLineIndex].length) {
                wereErrors = true;
            } else if (currLineOffset + currLineLength > arrayRefs[freeLineIndex].length) {
                wereErrors = true;
            }
            if (wereErrors) {
                StringBuilder sb = new StringBuilder();
                sb.append("Malformed bounds: ");
                sb.append("curr line array offset = ").append(currLineOffset).append(", ");
                sb.append("curr line length = ").append(currLineLength).append(", ");
                sb.append("curr line array ref length = ").append(arrayRefs[freeLineIndex].length);
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
                if (arrayRefs[freeLineIndex] != null) {
                    throw new AssertionError();
                }
            }
            arrayRefs[freeLineIndex] = CharArrayView.EMPTY.arrayRef;
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

        arrayRefs[backIndex] = null;
        backIndex = nextIndex(backIndex);
        linesCount--;
        return true;
    }
}