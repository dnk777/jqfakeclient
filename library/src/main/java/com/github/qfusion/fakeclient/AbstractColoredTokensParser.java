package com.github.qfusion.fakeclient;

/**
 * Extracts colored parts from a given {@link String} or a {@link CharSequence}.
 * A part starts by a circumflex character followed by a digit.
 * Other circumflex-containing escape sequences are illegal, except a double circumflex yielding a circumflex itself.
 * There is a mapping from digits to 10 predefined colors in the Qfusion engine.
 */
public abstract class AbstractColoredTokensParser {
    /**
     * Should be overridden in a subclass if one needs extended tokens
     */
    protected abstract void addWrappedToken(CharSequence underlying, int startIndex, int length, byte colorNum);

    /**
     * Should be overridden in a subclass if one needs extended tokens
     */
    protected abstract void addWrappedToken(String underlying, int startIndex, int length, byte colorNum);

    public final void parse(CharSequence input) {
        parse0(input, 0, input.length());
    }

    protected static final byte COLOR_WHITE = 7;

    public final void parse(CharSequence input, int offset, int length) {
        int inputLength = input.length();
        if (offset < 0 || offset > inputLength) {
            String message = "Illegal offset " + offset + " for input.length() " + inputLength;
            throw new IllegalArgumentException(message);
        }
        if (length < 0 || offset + length > inputLength) {
            String message = "Illegal length " + length + " for input.length() " + inputLength + " and offset " + offset;
            throw new IllegalArgumentException(message);
        }

        parse0(input, offset, length);
    }

    public final void parse(String input) {
        parse0(input, 0, input.length());
    }

    public final void parse(String input, int offset, int length) {
        int inputLength = input.length();
        if (offset < 0 || offset > inputLength) {
            String message = "Illegal offset " + offset + " for input.length() " + inputLength;
            throw new IllegalArgumentException(message);
        }
        if (length < 0 || offset + length > inputLength) {
            String message = "Illegal length " + length + " for input.length() " + inputLength + " and offset " + offset;
            throw new IllegalArgumentException(message);
        }

        parse0(input, offset, length);
    }

    /**
     * Copies token characters to a {@link StringBuilder} instance skipping duplicated circumflex characters.
     * Note that it is not optimal but we won't complicate the parser code for this normally rare case.
     * @return A new or modified {@link StringBuilder} that acts as token chars buffer.
     */
    private StringBuilder addCopiedToken(StringBuilder charsBuffer, CharSequence input,
                                         int offset, int length, byte colorNum) {
        int startBufferOffset = 0;
        if (charsBuffer == null) {
            charsBuffer = new StringBuilder(input.length() - offset);
        } else {
            startBufferOffset = charsBuffer.length();
        }
        int i = offset;
        int resultLength = length;
        boolean escaping = false;
        while (i < offset + length) {
            char ch = input.charAt(i);
            if (ch != '^') {
                charsBuffer.append(ch);
                i++;
                continue;
            }

            if (!escaping) {
                charsBuffer.append('^');
                escaping = true;
            } else {
                resultLength--;
                escaping = false;
            }
            i++;
        }

        addWrappedToken(charsBuffer, startBufferOffset, resultLength, colorNum);
        return charsBuffer;
    }

    /**
     * A specialized version of {@link AbstractColoredTokensParser#addCopiedToken(StringBuilder, CharSequence, int, int, byte)}.
     * The difference is in avoiding char-wise operations
     * and preferring builtin {@link String} methods for fast search.
     */
    private StringBuilder addCopiedToken(StringBuilder charsBuffer, String input, int offset, int length, byte colorNum) {
        int startBufferOffset = 0;
        if (charsBuffer == null) {
            charsBuffer = new StringBuilder(input.length() - offset);
        } else {
            startBufferOffset = charsBuffer.length();
        }

        int i = offset;
        int resultLength = 0;

        for(;;) {
            int circumflexIndex = input.indexOf('^', i);
            // Make sure we operate inside allowed bounds
            if (circumflexIndex >= offset + length) {
                int chunkLength = input.length() - i;
                chunkLength -= (circumflexIndex - (offset + length));
                resultLength += chunkLength;
                charsBuffer.append(input, i, i + chunkLength);
                break;
            }

            if (circumflexIndex != -1) {
                int chunkLength = circumflexIndex - i + 1;
                if (chunkLength != 0) {
                    charsBuffer.append(input, i, circumflexIndex + 1);
                }
                resultLength += chunkLength;
                i = circumflexIndex + 1;
                if (i >= offset + length) {
                    break;
                }
                // Skip the following circumflex (if any)
                if (input.charAt(i) == '^') {
                    i++;
                }
                if (i >= offset + length) {
                    break;
                }
                continue;
            }

            int chunkLength = input.length() - i;
            charsBuffer.append(input, i, i + chunkLength);
            resultLength += chunkLength;
            break;
        }

        addWrappedToken(charsBuffer, startBufferOffset, resultLength, colorNum);
        return charsBuffer;
    }

    /**
     * An internal generic version for {@link CharSequence} input that skips arguments validation.
     */
    private void parse0(CharSequence input, int offset, int length) {
        // Try using a specialized algorithm if it's worth it
        if (length >= 8 && input instanceof String) {
            parse0((String) input, offset, length);
            return;
        }

        int i = offset;
        int tokenStart = 0;
        boolean canWrapInput = true;
        byte color = COLOR_WHITE;
        StringBuilder charsBuffer = null;
        while (i < offset + length) {
            char ch = input.charAt(i);
            if (ch != '^') {
                i++;
                continue;
            }

            // If we can do a single character lookahead
            if (i + 1 < offset + length) {
                char nextCh = input.charAt(i + 1);
                // If next token should be started
                if (nextCh >= '0' && nextCh <= '9') {
                    // Add the current token
                    if (i - tokenStart > 0) {
                        if (canWrapInput) {
                            addWrappedToken(input, tokenStart, i - tokenStart, color);
                        } else {
                            charsBuffer = addCopiedToken(charsBuffer, input, tokenStart, i - tokenStart, color);
                        }
                    }

                    i += 2;
                    // Start a new token
                    canWrapInput = true;
                    tokenStart = i;
                    color = (byte)(nextCh - '0');
                    continue;
                }

                // We have to skip duplicated circumflex characters in this case.
                if (nextCh == '^') {
                    canWrapInput = false;
                    i += 2;
                    continue;
                }

                i++;
            } else {
                // Just skip the last character
                i++;
            }
        }

        // If there is an unclosed token
        if (tokenStart != i) {
            if (canWrapInput) {
                addWrappedToken(input, tokenStart, i - tokenStart, color);
            } else {
                addCopiedToken(charsBuffer, input, tokenStart, i - tokenStart, color);
            }
        }
    }

    /**
     * An internal specialized version for {@link String} input that skips arguments validation.
     * The specialized version is provided for performance reasons.
     */
    private void parse0(String input, int offset, int length) {
        int i = offset;

        // Help range checker to elide bounds tests for each char
        if (i < 0 || offset + length > input.length())
            throw new AssertionError();

        int tokenStart = 0;
        boolean canWrapInput = true;
        byte color = COLOR_WHITE;
        StringBuilder charsBuffer = null;
        while (i < offset + length) {
            char ch = input.charAt(i);
            if (ch != '^') {
                i++;
                continue;
            }

            // If we can do a single character lookahead
            if (i + 1 < offset + length) {
                char nextCh = input.charAt(i + 1);
                // If next token should be started
                if (nextCh >= '0' && nextCh <= '9') {
                    // Add the current token
                    if (i - tokenStart > 0) {
                        if (canWrapInput) {
                            addWrappedToken(input, tokenStart, i - tokenStart, color);
                        } else {
                            charsBuffer = addCopiedToken(charsBuffer, input, tokenStart, i - tokenStart, color);
                        }
                    }

                    i += 2;
                    // Start a new token
                    canWrapInput = true;
                    tokenStart = i;
                    color = (byte)(nextCh - '0');
                    continue;
                }

                // We have to skip duplicated circumflex characters in this case.
                if (nextCh == '^') {
                    canWrapInput = false;
                    i += 2;
                    continue;
                }

                i++;
            } else {
                // Just skip the last character
                i++;
            }
        }

        // If there is an unclosed token
        if (tokenStart != i) {
            if (canWrapInput) {
                addWrappedToken(input, tokenStart, i - tokenStart, color);
            } else {
                addCopiedToken(charsBuffer, input, tokenStart, i - tokenStart, color);
            }
        }
    }
}
