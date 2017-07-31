package com.github.qfusion.fakeclient;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class ColoredTokensParserTest extends TestCase {
    private static final String CHARSEQ_ADDRESS_MISMATCH =
        "Address of the underlying char sequence must match input (in this case)";
    private static final String STRING_ADDRESS_MISMATCH =
        "Address of the underlying string must match input (in this case)";

    // Note: in case when a circumflex starts or ends a token,
    // we may really avoid copying, but we do not want to complicate parser
    // for this marginal and very rare case.

    private static final String CHARSEQ_ADDRESS_MATCH =
        "Address of the underlying char sequence must not match input (in this case)";
    private static final String STRING_ADDRESS_NONNULL =
        "An underlying string should not be present (in this case)";

    private static List<ColoredToken> parse(CharSequence input) {
        List<ColoredToken> results = new ArrayList<ColoredToken>();
        new ColoredTokensParser<ColoredToken>(ColoredTokensFactory.getDefault(), results).parse(input);
        return results;
    }

    public void testEmptyInput() {
        List<ColoredToken> tokens = parse("");
        assertEquals(0, tokens.size());
    }

    public void testNoActualEscapeSequences() {
        String input = "Requesting configstrings...";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token.getUnderlyingStringOrNull(), input);
    }

    public void testBrokenEscapeSequenceAtStart() {
        String input = "^Malfored escape sequence";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token.getUnderlyingStringOrNull(), input);
    }

    public void testBrokenEscapeSequenceAtMid() {
        String input = "Malformed ^escape sequence";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token.getUnderlyingStringOrNull(), input);
    }

    public void testBrokenEscapeSequenceAtEnd() {
        String input = "Malformed escape sequence^";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token.getUnderlyingStringOrNull(), input);
    }

    public void testEscapedCircumflexAtStart() {
        String input = "^^Circumflex at Start";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertNotSame(CHARSEQ_ADDRESS_MATCH, token.getUnderlying(), input);
        assertNotSame(STRING_ADDRESS_NONNULL, token.getUnderlyingStringOrNull(), input);
    }

    public void testEscapedCircumflexAtMid() {
        String input = "Circumflex at^^ Mid";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertNotSame(CHARSEQ_ADDRESS_MATCH, token.getUnderlying(), input);
        assertNotSame(STRING_ADDRESS_NONNULL, token.getUnderlyingStringOrNull(), input);
    }

    public void testEscapedCirculflexAtEnd() {
        String input = "Circumflex at End^^";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertNotSame(CHARSEQ_ADDRESS_MATCH, token.getUnderlying(), input);
        assertNotSame(STRING_ADDRESS_NONNULL, token.getUnderlyingStringOrNull(), input);
        assertEquals(input.substring(0, input.length() - 1), token.toString());
    }

    public void testSingleColorEscapeAtStart() {
        String input = "^9Color escape at Start";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token.getUnderlyingStringOrNull(), input);
        assertEquals(input.substring(2), token.toString());
        assertEquals(Color.GREY, token.getColor());
    }

    public void testSingleColorEscapeAtMid() {
        String input = "Color es^1cape at Mid";
        List<ColoredToken> tokens = parse(input);
        assertEquals(2, tokens.size());

        ColoredToken token1 = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token1.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token1.getUnderlyingStringOrNull(), input);
        assertEquals(input.split("\\^")[0], token1.toString());
        assertEquals(Color.WHITE, token1.getColor());

        ColoredToken token2 = tokens.get(1);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token2.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token2.getUnderlyingStringOrNull(), input);
        assertEquals(input.split("\\^")[1].substring(1), token2.toString());
        assertEquals(Color.RED, token2.getColor());
    }

    public void testSingleColorEscapeAtEnd() {
        String input = "Color escape at End^5";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token.getUnderlyingStringOrNull(), input);
        assertEquals(input.substring(0, input.length() - 2), token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testMixedInput() {
        String input = "Mixed input with ^6color, circumflex^^ and mal^formed escape sequences";
        List<ColoredToken> tokens = parse(input);
        assertEquals(2, tokens.size());

        ColoredToken token1 = tokens.get(0);
        assertSame(CHARSEQ_ADDRESS_MISMATCH, token1.getUnderlying(), input);
        assertSame(STRING_ADDRESS_MISMATCH, token1.getUnderlyingStringOrNull(), input);
        assertEquals(input.split("\\^")[0], token1.toString());
        assertEquals(Color.WHITE, token1.getColor());

        ColoredToken token2 = tokens.get(1);
        assertNotSame(CHARSEQ_ADDRESS_MATCH, token2.getUnderlying(), input);
        assertNotSame(STRING_ADDRESS_NONNULL, token2.getUnderlyingStringOrNull(), input);
        String expected = input.split("\\^", 2)[1];
        expected = expected.substring(1);
        expected = expected.replaceAll("\\^\\^", "^");
        assertEquals(expected, token2.toString());
        assertEquals(Color.MAGENTA, token2.getColor());
    }

    public void testTokensOfAllColors() {
        // Same as the notourious Quake III ANARKI name, but for Warsow and 10 color codes.
        String name = "SILVERCLAW";
        if (name.length() != 10) {
            throw new AssertionError();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append('^').append(i).append(name.charAt(i));
        }
        String input = sb.toString();
        List<ColoredToken> tokens = parse(input);
        assertEquals(10, tokens.size());
        for (int i = 0; i < 10; ++i) {
            ColoredToken token = tokens.get(i);
            assertEquals("" + name.charAt(i), token.toString());
            assertEquals(Color.values()[i], token.getColor());
        }
    }
}
