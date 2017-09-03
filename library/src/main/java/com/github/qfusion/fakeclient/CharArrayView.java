package com.github.qfusion.fakeclient;

/**
 * A {@link CharSequence} that wraps a region of a char array.
 */
final class CharArrayView implements CharSequence {
    char[] arrayRef;
    int arrayOffset;
    int length;
    private int hash;

    CharArrayView() {}

    public CharArrayView(char[] arrayRef, int arrayOffset, int length) {
        if (arrayRef == null) {
            throw new IllegalArgumentException("The argument arrayRef is null");
        }
        if (arrayOffset < 0 || arrayOffset > arrayRef.length || length < 0 || arrayOffset + length > arrayRef.length) {
            throw new IllegalArgumentException(
                "arrayOffset=" + arrayOffset + ", length=" + length + ", arrayRef.length=" + arrayRef.length);
        }
        this.arrayRef = arrayRef;
        this.arrayOffset = arrayOffset;
        this.length = length;
    }

    public final char[] getArray() {
        return arrayRef;
    }

    public final int getArrayOffset() {
        return arrayOffset;
    }

    public final int getLength() {
        return length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return arrayRef[arrayOffset + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return new String(arrayRef, arrayOffset, length);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CharArrayView)) {
            return false;
        }
        return this.contentEquals((CharArrayView)o);
    }

    public boolean contentEquals(CharSequence cs) {
        if (cs.length() != length) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (arrayRef[i + arrayOffset] != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contentEquals(String s) {
        if (s.length() != length) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (arrayRef[i + arrayOffset] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contentEquals(CharArrayView that) {
        if (that.length != this.length) {
            return false;
        }
        // Do not force hashCode() computation, but check if computed results are already available
        if (this.hash != 0 && that.hash != 0) {
            if (this.hash != that.hash) {
                return false;
            }
        }
        for (int i = 0; i < this.length; ++i) {
            if (this.arrayRef[this.arrayOffset + i] != that.arrayRef[that.arrayOffset + i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        hash = 0;
        for (int i = 0; i < length; ++i) {
            hash = hash * 31 + arrayRef[arrayOffset + i];
        }
        return hash;
    }

    public static final CharArrayView EMPTY;

    static CharArrayView newForOffset(int offset) {
        CharArrayView result = new CharArrayView();
        result.arrayRef = EMPTY.arrayRef;
        result.arrayOffset = offset;
        return result;
    }

    static {
        EMPTY = new CharArrayView();
        EMPTY.arrayRef = new char[0];
        EMPTY.arrayOffset = 0;
        EMPTY.length = 0;
    }
}