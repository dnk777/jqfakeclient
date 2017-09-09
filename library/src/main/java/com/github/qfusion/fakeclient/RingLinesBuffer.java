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
    @VisibleForTesting char[][] arrayRefs;
    /**
     * An element #(i * 2 + 0) contains an offset of a line data inside arrayRefs[i]
     * An element #(i * 2 + 1) contains an length of a line data chunk inside arrayRefs[i]
     */
    @VisibleForTesting int[] offsetsAndLengths;

    /**
     * An i-th element contains a reference to byte[] colored tokens array of an i-th line.
     * Having a null in an element for a present line is legal.
     * Refer to {@link RingLinesBuffer#tokenColors} in this case.
     */
    @VisibleForTesting int[][] coloredTokens;

    /**
     * For most console messages there is only a single token.
     * Its contents matches the entire line (except an initial token prefix of 2 characters).
     * In this case we can avoid allocation of tiny int[3] arrays for each line.
     */
    @VisibleForTesting byte[] tokenColors;

    private final CharArrayView tmpCharArrayView = new CharArrayView();

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    @VisibleForTesting boolean isFrontLineCompleted;

    @VisibleForTesting char[] currCharsBuffer;
    @VisibleForTesting int defaultBufferSize;

    @VisibleForTesting int currCharsBufferOffset;

    @VisibleForTesting int freeLineIndex;
    @VisibleForTesting int frontIndex;
    @VisibleForTesting int backIndex;
    @VisibleForTesting int linesCount;

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
            getCharArrayViewAt(index, reuse);
            i += 1;
            index = nextIndex(index);
            return reuse;
        }

        public void next(CharArrayView charsToReuse, BufferLineTokensView tokensViewToReuse) {
            getCharArrayViewAt(index, charsToReuse);
            getTokensViewAt(index, tokensViewToReuse);
            i += 1;
            index = nextIndex(index);
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
        return getCharArrayViewAt(frontIndex, reuse);
    }

    public void front(CharArrayView charsToReuse, BufferLineTokensView tokensToReuse) {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        getCharArrayViewAt(frontIndex, charsToReuse);
        getTokensViewAt(frontIndex, tokensToReuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public CharArrayView front() {
        return front(new CharArrayView());
    }

    public CharArrayView back(CharArrayView reuse) {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        return getCharArrayViewAt(backIndex, reuse);
    }

    public void back(CharArrayView charsToReuse, BufferLineTokensView tokensToReuse) {
        if (linesCount == 0) {
            throw new NoSuchElementException();
        }
        getCharArrayViewAt(backIndex, charsToReuse);
        getTokensViewAt(backIndex, tokensToReuse);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public CharArrayView back() {
        return back(new CharArrayView());
    }

    private CharArrayView getCharArrayViewAt(int index, CharArrayView reuse) {
        reuse.arrayRef = arrayRefs[index];
        reuse.arrayOffset = offsetsAndLengths[index * 2 + 0];
        reuse.length = offsetsAndLengths[index * 2 + 1];
        return reuse;
    }

    private BufferLineTokensView getTokensViewAt(int index, BufferLineTokensView reuse) {
        int charsLength = offsetsAndLengths[index * 2 + 1];
        int[] tokens = coloredTokens[index];

        // If there is only a single token
        if (tokens == null) {
            if (charsLength != 0) {
                reuse.arrayOffset = 0;
                reuse.length = 2;
                tokens = reuse.intrinsicBuffer;
                // Set offset of a single token in the chars array
                tokens[0] = 0;
                tokens[1] = (tokenColors[index] << 28) | charsLength;
            } else {
                // The line is empty and there were no tokens produced
                reuse.arrayOffset = 0;
                reuse.length = 0;
                reuse.arrayRef = EMPTY_INT_ARRAY;
            }
        } else {
            reuse.arrayOffset = 1;
            reuse.length = tokens[0];
        }

        reuse.arrayRef = tokens;
        return reuse;
    }

    RingLinesBuffer(int capacity) {
        arrayRefs = new char[capacity + 1][];
        offsetsAndLengths = new int[2 * capacity + 2];
        coloredTokens = new int[capacity + 1][];
        tokenColors = new byte[capacity + 1];
        defaultBufferSize = 1024;
        this.currCharsBuffer = new char[defaultBufferSize];
        resetValues();
    }

    RingLinesBuffer(int capacity, int defaultBufferSize) {
        arrayRefs = new char[capacity + 1][];
        offsetsAndLengths = new int[2 * capacity + 2];
        coloredTokens = new int[capacity + 1][];
        tokenColors = new byte[capacity + 1];
        this.defaultBufferSize = defaultBufferSize;
        this.currCharsBuffer = new char[defaultBufferSize];
        resetValues();
    }

    protected void resetValues() {
        currCharsBufferOffset = 0;
        isFrontLineCompleted = true;
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
        isFrontLineCompleted = false;
        if (arrayRefs[freeLineIndex] == null) {
            arrayRefs[freeLineIndex] = currCharsBuffer;
            offsetsAndLengths[freeLineIndex * 2 + 0] = currCharsBufferOffset;
            offsetsAndLengths[freeLineIndex * 2 + 1] = 0;
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
                    throw new AssertionError("Array ref is already present for an empty line");
                }
                if (coloredTokens[freeLineIndex] != null) {
                    throw new AssertionError("Colored tokens are already present for an empty line");
                }
            }
            arrayRefs[freeLineIndex] = CharArrayView.EMPTY.arrayRef;
        } else {
            stripAndSaveTokens(freeLineIndex);
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
        coloredTokens[backIndex] = null;
        backIndex = nextIndex(backIndex);
        linesCount--;
        return true;
    }

    static class ColoredTokensParser extends AbstractColoredTokensParser {
        int bufferOffset;
        int lineIndex;
        int initialLineLength;
        int numTokens;
        int[] buffer;
        byte singleTokenColor;

        void prepare(int lineIndex, int initialLineLength) {
            this.lineIndex = lineIndex;
            this.initialLineLength = initialLineLength;
            this.numTokens = 0;
            this.bufferOffset = 1;
            this.buffer = null;
        }

        @Override
        protected void addWrappedToken(CharSequence underlying, int startIndex, int length, byte colorNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void addWrappedToken(String underlying, int startIndex, int length, byte colorNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void addWrappedToken(CharArrayView underlying, int startIndex, int length, byte colorNum) {
            if (numTokens == 0) {
                if (length == initialLineLength - 2) {
                    singleTokenColor = colorNum;
                    return;
                }
                if (length == initialLineLength - 4) {
                    // Check for a terminating escape sequence
                    char[] chars = underlying.getArray();
                    int charsOffset = underlying.getArrayOffset();
                    int charsLength = underlying.getLength();
                    if (chars[charsOffset + charsLength - 2] == '^') {
                        int nextCh = chars[charsOffset + charsLength - 1];
                        if (nextCh >= '0' && nextCh <= '9') {
                            singleTokenColor = colorNum;
                            return;
                        }
                    }
                }
                buffer = new int[3 * 4];
            } else if (bufferOffset == buffer.length) {
                int newSize = buffer.length < 1024 ? buffer.length * 2 : (3 * buffer.length) / 2;
                int newBuffer[] = new int[newSize];
                java.lang.System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }

            if (BuildConfig.DEBUG) {
                if (colorNum < 0 || colorNum > 9) {
                    throw new AssertionError();
                }
                if (length >= (1 << 27)) {
                    throw new AssertionError();
                }
            }

            buffer[bufferOffset++] = startIndex;
            buffer[bufferOffset++] = (colorNum << 28) | length;
            numTokens++;
        }
    }

    private ColoredTokensParser parser = new ColoredTokensParser();

    private void stripAndSaveTokens(int freeLineIndex) {
        int lineOffset = offsetsAndLengths[freeLineIndex * 2 + 0];
        int lineLength = offsetsAndLengths[freeLineIndex * 2 + 1];

        coloredTokens[freeLineIndex] = null;
        tokenColors[freeLineIndex] = AbstractColoredTokensParser.COLOR_WHITE;

        parser.prepare(freeLineIndex, lineLength);
        tmpCharArrayView.arrayRef = arrayRefs[freeLineIndex];
        tmpCharArrayView.arrayOffset = lineOffset;
        tmpCharArrayView.length = lineLength;

        // Note: it is possible to strip tokens during appendLinePart() calls while copying the part chars.
        // This should be a bit more efficient and do not leave unused space after stripping,
        // but we are not going to sacrifice maintainability.

        parser.parseRemovingColors(tmpCharArrayView);
        // Patch the length
        offsetsAndLengths[freeLineIndex * 2 + 1] = tmpCharArrayView.getLength();

        // Save tokens
        int[] tokens = coloredTokens[freeLineIndex] = parser.buffer;
        parser.buffer = null;
        if (tokens != null) {
            // Each token consumes 2 int cells
            tokens[0] = parser.numTokens * 2;
        } else {
            tokenColors[freeLineIndex] = parser.singleTokenColor;
        }
    }
}