package com.github.qfusion.fakeclient;

/**
 * A reusable wrapper over colored tokens for a {@link RingLinesBuffer} line.
 * Token offset, length and color are packed in 2 ints.
 * The first int in pair is an offset of a token in a line.
 * The second int in pair is a complex packed value:
 * 4 high bits are used for color index, the rest of bits is a token length.
 * (We were really jealous to waste an extra integer on color or use a separate array of colors).
 * @see CharArrayView for a similar wrapper.
 * @see ByteArrayView for a similar wrapper.
 */
public final class BufferLineTokensView {
    int[] arrayRef;
    int arrayOffset;
    int length;

    /**
     * A temporary buffer that helps to avoid allocation and maintenance
     * of for tiny tokens lists (made of a single token) in a some container.
     * A container should use an optimized internal representation of a token,
     * fill this buffer on a tokens lists retrieval and set <tt>arrayRef</tt> to this array.
     */
    final int[] intrinsicBuffer = new int[2];

    public final int[] getArray() {
        return arrayRef;
    }

    public final int getArrayOffset() {
        return arrayOffset;
    }

    public final int getLength() {
        return length;
    }
}
