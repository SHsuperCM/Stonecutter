package io.shcm.shsupercm.fabric.stonecutter.cutter;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.LinkedList;

public class FileCutter {
    private final File file;
    private final Stonecutter stonecutter;

    public FileCutter(File file, Stonecutter stonecutter) {
        this.file = file;
        this.stonecutter = stonecutter;
    }

    public void apply() throws Exception {
        StringBuilder transformedContents;
        try (Reader oldContents = Files.newBufferedReader(file.toPath(), StandardCharsets.ISO_8859_1)) {
            transformedContents = applyVersionedCodeComments(oldContents, new StringBuilder());
        }

        file.delete();
        Files.writeString(file.toPath(), transformedContents, StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE);
    }

    private StringBuilder applyVersionedCodeComments(Reader input, StringBuilder output) throws StonecutterSyntaxException, IOException {
        class Helper {
            private String read(String match) throws IOException {
                StringBuilder substring = new StringBuilder();

                int current;

                while ((current = input.read()) != -1) {
                    char ch = (char) current;
                    substring.append(ch);
                    output.append(ch);

                    if (substring.toString().endsWith(match))
                        return substring.substring(0, substring.length() - match.length());
                }

                return null;
            }

            private boolean find(String match) throws IOException {
                return read(match) != null;
            }
        } Helper helper = new Helper();

        Deque<Boolean> conditions = new LinkedList<>();
        while (helper.find("/*?")) {
            String expression = helper.read("?*/");
            if (expression == null)
                throw new StonecutterSyntaxException("Expected ?*/ to close stonecutter expression");
            expression = expression.trim();

            Boolean closedState = null;
            final boolean skip;

            if (expression.startsWith("}")) {
                if (conditions.isEmpty())
                    throw new StonecutterSyntaxException("Unexpected } symbol");

                skip = (closedState = conditions.pop()) == null;
                expression = expression.substring(1).stripLeading();
            } else
                skip = false;

            if (!expression.isBlank()) {
                if (expression.endsWith("{"))
                    expression = expression.substring(0, expression.length() - 1).stripTrailing();
                else
                    throw new StonecutterSyntaxException("Expected { symbol");

                if ((closedState != null && closedState) || ((skip || !conditions.isEmpty()) && (conditions.peek() == null || !conditions.peek()))) {
                    conditions.push(null);
                } else {
                    boolean conditionResult = true;
                    if (expression.startsWith("else"))
                        expression = expression.substring(4).stripLeading();
                    if (!expression.isBlank())
                        conditionResult = stonecutter.testVersion(expression);

                    conditions.push(conditionResult);
                }

                // skip 2 only if "/*" is next
                input.mark(2);
                if (input.read() != '/' || input.read() != '*')
                    input.reset();

                if (conditions.peek() == null || !conditions.peek())
                    output.append("/*");
            }
        }

        return output;
    }
}
