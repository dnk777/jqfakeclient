package com.github.qfusion.fakeclient;

import java.util.ArrayList;
import java.util.List;

class ColoredTokensParser<T extends ColoredToken> extends AbstractColoredTokensParser {

    private List<T> results;
    private ColoredTokensFactory<T> tokensFactory;

    public ColoredTokensParser(ColoredTokensFactory<T> tokensFactory) {
        this.results = new ArrayList<T>();
        this.tokensFactory = tokensFactory;
    }

    public ColoredTokensParser(ColoredTokensFactory<T> tokensFactory, List<T> results) {
        this.results = results;
        this.tokensFactory = tokensFactory;
    }

    public List<T> getResults() {
        return results;
    }

    public void reset() {
        results.clear();
    }

    @Override
    protected final void addWrappedToken(CharSequence underlying, int startIndex, int length, byte colorNum) {
        results.add(tokensFactory.newToken(underlying, startIndex, length, Color.values()[colorNum]));
    }

    @Override
    protected final void addWrappedToken(String underlying, int startIndex, int length, byte colorNum) {
        results.add(tokensFactory.newToken(underlying, startIndex, length, Color.values()[colorNum]));
    }
}
