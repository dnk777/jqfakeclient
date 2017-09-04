package com.github.qfusion.fakeclient;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link AbstractColoredTokensParser#parseRemovingColors(CharArrayView)} method deserves a separate tests set.
 */
public class RemovingColorsTokensParserTest extends TestCase {
    private List<ColoredToken> parse(String input) {
        // We add an start offset to test whether the parser code operates on indices properly.
        // However we do not add extra space at the end of chars buffer to spot an OOB access immediately.
        char[] chars = new char[input.length() + 2];
        input.getChars(0, input.length(), chars, 2);
        CharArrayView view = new CharArrayView(chars, 2, input.length());
        CharArrayViewColoredTokensParser parser = new CharArrayViewColoredTokensParser();
        parser.parseRemovingColors(view);
        return parser.getTokens();
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
        assertEquals(input, token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testBrokenEscapeSequenceAtStart() {
        String input = "^Malformed escape sequence";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input, token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testBrokenEscapeSequenceAtMid() {
        String input = "Malformed ^escape sequence";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input, token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testBrokenEscapeSequenceAtEnd() {
        String input = "Malformed escape sequence^";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input, token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testEscapedCircumflexAtStart() {
        String input = "^^Circumflex at Start";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input.substring(1), token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testEscapedCircumflexAtMid() {
        String input = "Circumflex at^^ Mid";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input.replace("^^", "^"), token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testEscapedCircumflexAtEnd() {
        String input = "Circumflex at End^^";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input.substring(0, input.length() - 1), token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testSingleColorEscapeAtStart() {
        String input = "^9Color escape at Start";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input.substring(2), token.toString());
        assertEquals(Color.GREY, token.getColor());
    }

    public void testSingleColorEscapeAtMid() {
        String input = "Color es^1cape at Mid";
        List<ColoredToken> tokens = parse(input);
        assertEquals(2, tokens.size());

        ColoredToken token1 = tokens.get(0);
        assertEquals(input.split("\\^")[0], token1.toString());
        assertEquals(Color.WHITE, token1.getColor());

        ColoredToken token2 = tokens.get(1);
        assertEquals(input.split("\\^")[1].substring(1), token2.toString());
        assertEquals(Color.RED, token2.getColor());
    }

    public void testSingleColorEscapeAtEnd() {
        String input = "Color escape at End^5";
        List<ColoredToken> tokens = parse(input);
        assertEquals(1, tokens.size());
        ColoredToken token = tokens.get(0);
        assertEquals(input.substring(0, input.length() - 2), token.toString());
        assertEquals(Color.WHITE, token.getColor());
    }

    public void testMixedInput1() {
        String input = "Mixed input with ^6color, circumflex^^ and mal^for^m^ed escape sequences";
        List<ColoredToken> tokens = parse(input);
        assertEquals(2, tokens.size());

        ColoredToken token1 = tokens.get(0);
        assertEquals(input.split("\\^")[0], token1.toString());
        assertEquals(Color.WHITE, token1.getColor());

        ColoredToken token2 = tokens.get(1);
        String expected = input.split("\\^", 2)[1];
        expected = expected.substring(1);
        expected = expected.replaceAll("\\^\\^", "^");
        assertEquals(expected, token2.toString());
        assertEquals(Color.MAGENTA, token2.getColor());
    }

    public void testMixedInput2() {
        String token1Chars = "Mixed input with ";
        String token2Chars = "colo^r^, ^malfo^rmed ";
        String token3Chars = "and circumflex ^^escape sequences^^";

        String input = "^1" + token1Chars + "^2" + token2Chars + "^7" + token3Chars + "^5";
        List<ColoredToken> tokens = parse(input);
        assertEquals(3, tokens.size());

        ColoredToken token1 = tokens.get(0);
        assertEquals(token1Chars, token1.toString());
        assertEquals(Color.RED, token1.getColor());

        ColoredToken token2 = tokens.get(1);
        assertEquals(token2Chars, token2.toString());
        assertEquals(Color.GREEN, token2.getColor());

        ColoredToken token3 = tokens.get(2);
        assertEquals(token3Chars.replace("^^", "^"), token3.toString());
        assertEquals(Color.WHITE, token3.getColor());
    }

    public void testTokensOfAllColors() {
        // Same as the notorious Quake III ANARKI name, but for Warsow and 10 color codes.
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

class CharArrayViewColoredTokensParser extends AbstractColoredTokensParser {
    @Override
    protected void addWrappedToken(CharSequence underlying, int startIndex, int length, byte colorNum) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void addWrappedToken(String underlying, int startIndex, int length, byte colorNum) {
        throw new UnsupportedOperationException();
    }

    private final List<ColoredToken> tokens = new ArrayList<ColoredToken>();

    List<ColoredToken> getTokens() {
        return tokens;
    }

    @Override
    protected void addWrappedToken(CharArrayView underlying, int startIndex, int length, byte colorNum) {
        tokens.add(new ColoredToken(underlying, startIndex, length, Color.values()[colorNum]));
    }
}
