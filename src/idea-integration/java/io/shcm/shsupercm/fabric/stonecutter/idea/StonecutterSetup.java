package io.shcm.shsupercm.fabric.stonecutter.idea;

import groovy.lang.*;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jetbrains.plugins.gradle.model.ExternalProject;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class StonecutterSetup {
    private final ExternalProject gradleProject;
    private final String currentActive;
    private final String[] versions;
    private final TokenMapper tokensCache;

    public StonecutterSetup(ExternalProject gradleProject, String currentActive) {
        this.gradleProject = gradleProject;
        this.currentActive = currentActive;

        List<String> versions = new ArrayList<>(); {
            for (Map.Entry<String, ? extends ExternalProject> entry : gradleProject.getChildProjects().entrySet()) {
                File versionedDir = entry.getValue().getProjectDir();
                if (versionedDir.getName().equals(entry.getKey()) && versionedDir.getParentFile().getName().equals("versions"))
                    versions.add(entry.getKey());
            }
        } this.versions = versions.toArray(String[]::new);

        Map<String, File> tokens = new HashMap<>();
        for (String version : this.versions)
            tokens.put(version, new File(gradleProject.getChildProjects().get(version).getProjectDir(), "tokens.gradle"));
        this.tokensCache = new TokenMapper(tokens);
    }

    public ExternalProject gradleProject() {
        return this.gradleProject;
    }

    public String currentActive() {
        return this.currentActive;
    }

    public Iterable<String> versions() {
        return Arrays.asList(this.versions);
    }

    public TokenMapper tokenCache() {
        return this.tokensCache;
    }

    public static class TokenMapper {
        public final Map<String, Map<String, Token>> tokensByVersion = new HashMap<>();
        public final Set<String> commonTokens = new HashSet<>(), missingTokens = new HashSet<>();

        public static class Token {
            public final String id;
            public final Pattern read;
            public final String write;
            public final boolean defaultEnabled;
            public final Predicate<File> fileFilter;

            public Token(String id, Pattern read, String write, boolean defaultEnabled, Predicate<File> fileFilter) {
                this.id = id;
                this.read = read;
                this.write = write;
                this.defaultEnabled = defaultEnabled;
                this.fileFilter = fileFilter;
            }
        }

        public TokenMapper(Map<String, File> tokenScripts) {
            CompilerConfiguration groovyCompiler = new CompilerConfiguration();
            groovyCompiler.setScriptBaseClass(TokensScriptClass.class.getName());
            GroovyClassLoader classLoader = new GroovyClassLoader(TokensScriptClass.class.getClassLoader(), groovyCompiler);
            GroovyShell groovy = new GroovyShell(classLoader, groovyCompiler);
            for (Map.Entry<String, File> entry : tokenScripts.entrySet()) {
                try {
                    TokensScriptClass tokensScript = (TokensScriptClass) groovy.parse(entry.getValue());
                    tokensScript.run();
                    for (Map.Entry<String, TokensScriptClass.Builder> token : tokensScript.tokenBuilders.entrySet())
                        try {
                            tokensByVersion.computeIfAbsent(entry.getKey(), v -> new HashMap<>())
                                    .put(token.getKey(), new Token(token.getKey(), token.getValue().read, token.getValue().write, token.getValue().defaultEnabled, token.getValue().fileFilter));
                        } catch (Exception ignored) { }
                } catch (Exception ignored) { }
            }

            for (Map.Entry<String, Map<String, Token>> entry : tokensByVersion.entrySet()) {
                for (String token : entry.getValue().keySet()) {
                    if (commonTokens.contains(token))
                        continue;

                    boolean common = true;
                    for (Map<String, Token> tokens : tokensByVersion.values())
                        if (!(common = tokens.containsKey(token)))
                            break;
                    (common ? commonTokens : missingTokens).add(token);
                }
            }
        }

        public static class TokensScriptClass extends Script {
            public final Map<String, Builder> tokenBuilders = new HashMap<>();

            @Override
            public Object run() {
                return null;
            }

            public void token(String id, Closure<Builder> builder) {
                tokenBuilders.put(id, new Builder(builder));
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
}
