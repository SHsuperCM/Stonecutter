package io.shcm.shsupercm.fabric.stonecutter.cutter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class StoneRegexTokenizer {
    private final Map<String, Token> tokens = new HashMap<>();

    public void token(String id, Pattern read, String write) {
        tokens.put(id, new Token(id, read, write));
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

            remapper.tokens.put(sourceToken.id, new Token(sourceToken.id, sourceToken.read(), targetToken.write()));
        }

        return remapper;
    }

    public void apply(StringBuilder value) {
        CharSequence applied = value;
        for (Token token : tokens.values())
            applied = token.apply(applied);
        value.setLength(0);
        value.append(applied);
    }

    public record Token(String id, Pattern read, String write) {
        public CharSequence apply(CharSequence value) {
            return read.matcher(value).replaceAll(write);
        }
    }
}
