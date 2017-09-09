package com.github.qfusion.fakeclient;

/**
 * A wrapper over a region of a byte array.
 * @see CharArrayView for a similar wrapper.
 * @see BufferLineTokensView for a similar wrapper.
 */
public final class ByteArrayView {
    byte[] arrayRef;
    int arrayOffset;
    int length;

    public final byte[] getArray() {
        return arrayRef;
    }

    public final int getArrayOffset() {
        return arrayOffset;
    }

    public final int getLength() {
        return length;
    }
}
