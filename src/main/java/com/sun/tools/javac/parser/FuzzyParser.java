package com.sun.tools.javac.parser;

import java.util.logging.Logger;

import static com.sun.tools.javac.parser.Tokens.TokenKind.SEMI;

public class FuzzyParser extends JavacParser {
    private static final Logger LOG = Logger.getLogger("main");

    protected FuzzyParser(ParserFactory parserFactory,
                          Lexer lexer,
                          boolean b, boolean b1, boolean b2) {
        super(parserFactory, lexer, b, b1, b2);
    }

    @Override
    /** If next input token matches given token, skip it, otherwise report
     *  an error.
     */
    public void accept(Tokens.TokenKind tk) {
        if (token.kind == tk) {
            nextToken();
        }
        else if (tk == SEMI) {
            // Pretend semicolon is present and continue parsing
            LOG.fine("Inserted semicolon");
        }
        else {
            super.accept(tk);
        }
    }
}
