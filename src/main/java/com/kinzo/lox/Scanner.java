package com.kinzo.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",     TokenType.AND);
        keywords.put("class",   TokenType.CLASS);
        keywords.put("else",    TokenType.ELSE);
        keywords.put("false",   TokenType.FALSE);
        keywords.put("for",     TokenType.FOR);
        keywords.put("fun",     TokenType.FUN);
        keywords.put("if",      TokenType.IF);
        keywords.put("nil",     TokenType.NIL);
        keywords.put("or",      TokenType.OR);
        keywords.put("print",   TokenType.PRINT);
        keywords.put("return",  TokenType.RETURN);
        keywords.put("super",   TokenType.SUPER);
        keywords.put("this",    TokenType.THIS);
        keywords.put("var",     TokenType.VAR);
        keywords.put("while",   TokenType.WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            // Single character tokens.
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            // One or two character tokens.
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL: TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL: TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL: TokenType.GREATER);
                break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    /*
                     * Stolen from https://github.com/svtm/jlox/blob/master/src/com/enielsen/lox/Scanner.java
                     */
                    int startLine = line;

                    while (!isAtEnd()) {
                        if (peek() == '*' && peekNext() == '/') {
                            break;
                        } else {
                            line++;
                        }
                        advance();
                    }
                    if (!isAtEnd()) {
                        // Consume closing */
                        advance();
                        advance();
                    } else {
                        Lox.error(startLine, "Block comment not closed");
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            // Whitespace and newlines.
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            // String literals.
            case '\"': string(); break;
            default:
                // Number literals.
                if (isDigit(c)) {
                    number();
                // Reserved words and identifiers.
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(
                TokenType.NUMBER,
                Double.parseDouble(source.substring(start, current))
        );
    }

    /**
     * Does not currently support escape sequences.
     */
    private void string() {
        while (peek() != '\"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); // the closing ".

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    /**
     * Only consume the current character if it's what we're looking for.
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Lookahead
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }


    /**
     * Consumes the next character in the source file and returns it.
     */
    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Grabs the text of the current lexeme and creates a new token for it.
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
