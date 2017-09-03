package com.github.qfusion.fakeclient;

/**
 * A part of a {@link CharSequence}, a {@link String} or a char array that has an associated {@link Color}.
 */
class ColoredToken implements CharSequence {
    CharSequence underlying;
    String underlyingString;
    char[] underlyingArray;
    int startIndex;
    int length;
    Color color;

    protected ColoredToken(CharSequence underlying, int startIndex, int length, Color color) {
        this.underlying = underlying;
        // We do not test whether the underlying sequence is a String for performance reasons.
        // If one wants to have the underlyingString set in this case,
        // test it in a caller and call the proper constructor.
        this.underlyingString = null;
        // Same for the array
        this.underlyingArray = null;
        this.startIndex = startIndex;
        this.length = length;
        this.color = color;
    }

    protected ColoredToken(String underlying, int startIndex, int length, Color color) {
        this.underlying = underlying;
        this.underlyingString = underlying;
        this.underlyingArray = null;
        this.startIndex = startIndex;
        this.length = length;
        this.color = color;
    }

    protected ColoredToken(char[] underlyingArray, int startIndex, int length, Color color) {
        this.underlying = null;
        this.underlyingString = null;
        this.underlyingArray = underlyingArray;
        this.startIndex = startIndex;
        this.length = length;
        this.color = color;
    }

    /**
     * Gets the underlying char sequence.
     * Note that it is not guaranteed to be the same sequence that was used in parsing.
     */
    public CharSequence getUnderlying() {
        return underlying;
    }

    /**
     * Gets an underlying {@link String} (if any).
     * Note that it is not guaranteed to be the same string that was used in parsing.
     * @return An underlying string or null if the token is not based on a {@link String}.
     */
    public String getUnderlyingStringOrNull() {
        return underlyingString;
    }

    /**
     * Gets an underlying char array (if any).
     * Note that it is not guaranteed to be the same array that was used in parsing.
     * @return An underlying array of null if the token is not based on a char array.
     */
    public char[] getUnderlyingArrayOrNull() { return underlyingArray; }

    public int getStartIndex() { return startIndex; }
    public int getLength() { return length; }
    public Color getColor() { return color; }

    public char charAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Illegal index " + index + " < 0");
        }
        if (index >= length) {
            throw new IllegalArgumentException("Illegal index " + index + " >= length " + length);
        }

        if (underlying != null) {
            return underlying.charAt(startIndex + index);
        }
        return underlyingArray[startIndex + index];
    }

    public int length() {
        return length;
    }

    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        if (underlyingString != null) {
            if (startIndex == 0 && length == underlyingString.length()) {
                return underlyingString;
            }
            char[] chars = new char[length];
            underlyingString.getChars(startIndex, startIndex + length, chars, 0);
            return new String(chars);
        }
        if (underlyingArray != null) {
            return new String(underlyingArray, startIndex, length);
        }

        char[] chars = new char[length];
        for (int i = 0, end = length; i < end; ++i) {
            chars[i] = underlying.charAt(startIndex + i);
        }
        return new String(chars);
    }
}
