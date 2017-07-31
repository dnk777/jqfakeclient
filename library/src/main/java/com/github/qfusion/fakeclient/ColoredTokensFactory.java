package com.github.qfusion.fakeclient;

public abstract class ColoredTokensFactory<T extends ColoredToken> {
    public abstract T newToken(CharSequence underlying, int offset, int length, Color color);
    public abstract T newToken(String underlying, int offset, int length, Color color);

    private static ColoredTokensFactory<ColoredToken> DEFAULT = new ColoredTokensFactory<ColoredToken>() {
        @Override
        public ColoredToken newToken(CharSequence underlying, int offset, int length, Color color) {
            return new ColoredToken(underlying, offset, length, color);
        }

        @Override
        public ColoredToken newToken(String underlying, int offset, int length, Color color) {
            return new ColoredToken(underlying, offset, length, color);
        }
    };

    public static ColoredTokensFactory<ColoredToken> getDefault() { return DEFAULT; }
}
