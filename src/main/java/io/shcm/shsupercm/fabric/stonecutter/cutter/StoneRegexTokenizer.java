package io.shcm.shsupercm.fabric.stonecutter.cutter;

import groovy.lang.Closure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoneRegexTokenizer {
    private final Map<String, Token> tokens = new HashMap<>();

    public void token(String id, Closure<Token.Builder> builder) {
        if (id.chars().anyMatch(Character::isWhitespace))
            throw new IllegalArgumentException("Token identifier must not contain spaces");

        Token.Builder tokenBuilder = new Token.Builder(builder);
        if (tokenBuilder.read == null || tokenBuilder.write == null)
            throw new IllegalArgumentException("Token builder missing read/write values");

        tokens.put(id, new Token(id, tokenBuilder.read, tokenBuilder.write, tokenBuilder.defaultEnabled, tokenBuilder.fileFilter == null ? file -> true : tokenBuilder.fileFilter));
    }

    public Set<String> tokens() {
        return this.tokens.keySet();
    }

    public static StoneRegexTokenizer remap(StoneRegexTokenizer source, StoneRegexTokenizer target) {
        if (source == target)
            return source;

        StoneRegexTokenizer remapper = new StoneRegexTokenizer();

        for (Token sourceToken : source.tokens.values()) {
            Token targetToken = target.tokens.get(sourceToken.id);
            if (targetToken == null)
                continue;

            remapper.tokens.put(sourceToken.id, Token.remap(sourceToken, targetToken));
        }

        return remapper;
    }

    public void apply(File file, StringBuilder value) {
        if (tokens.isEmpty())
            return;
        CharSequence applied = value;
        for (Token token : tokens.values())
            if (token.fileFilter().test(file))
                applied = token.apply(applied);
        value.setLength(0);
        value.append(applied);
    }

    public record Token(String id, Pattern read, String write, boolean defaultEnabled, Predicate<File> fileFilter) {
        public CharSequence apply(CharSequence value) {
            class Static {
                static final Pattern PATTERN_TOKEN_SET_STATE = Pattern.compile("/\\*\\?\\$token (?<state>enable|disable) (?<id>.+)\\?\\*/");
            }

            StringBuilder output = new StringBuilder(), buffer = new StringBuilder();
            boolean enabled = defaultEnabled;

            Matcher flagsMatcher = Static.PATTERN_TOKEN_SET_STATE.matcher(value);
            while (flagsMatcher.find()) {
                flagsMatcher.appendReplacement(buffer, "");
                output.append(enabled ? read.matcher(buffer).replaceAll(write) : buffer)
                      .append(flagsMatcher.group());
                buffer.setLength(0);

                if (this.id.equals(flagsMatcher.group("id")))
                    enabled = "enable".equals(flagsMatcher.group("state"));
            }

            flagsMatcher.appendTail(buffer);
            output.append(enabled ? read.matcher(buffer).replaceAll(write) : buffer);

            return output;
        }

        public static Token remap(Token source, Token target) {
            return new Token(source.id, source.read, target.write, source.defaultEnabled, source.fileFilter);
        }

        public static class Builder {
            public Pattern read = null;
            public String write = null;
            public boolean defaultEnabled = false;
            public Predicate<File> fileFilter = null;

            public Builder(Closure<Builder> builder) {
                builder.setDelegate(this);
                builder.call();
            }

            public void read(Pattern read) {
                this.read = read;
            }

            public void write(String write) {
                this.write = write;
            }

            public void defaultEnabled(boolean defaultEnabled) {
                this.defaultEnabled = defaultEnabled;
            }

            public void fileFilter(Predicate<File> fileFilter) {
                this.fileFilter = fileFilter;
            }
        }
    }
}
