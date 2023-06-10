package io.shcm.shsupercm.fabric.stonecutter.cutter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileCutter {
    private final File file;
    private final Stonecutter stonecutter;

    public FileCutter(File file, Stonecutter stonecutter) {
        this.file = file;
        this.stonecutter = stonecutter;
    }

    private BufferedReader oldContents;
    private StringBuilder newContents;

    private boolean versionedCode = false, disabledCode = false;

    public void apply() throws Exception {
        newContents = new StringBuilder();
        try (BufferedReader oldContents = this.oldContents = Files.newBufferedReader(file.toPath(), StandardCharsets.ISO_8859_1)) {
            while (find("/*?")) {
                String expression = read("?*/");
                if (expression == null)
                    throw new StonecutterSyntaxException("Expected ?*/");
                expression = expression.trim();

                if (expression.startsWith("}")) { // closing versioned code
                    if (!versionedCode)
                        throw new StonecutterSyntaxException("Unexpected } closing non-versioned code");

                    versionedCode = false;
                    expression = expression.substring(1).stripLeading();
                } else if (expression.startsWith("{"))
                    throw new StonecutterSyntaxException("Unexpected { opening versioned code without a condition");

                if (!expression.isBlank()) {
                    boolean els = false;
                    if (expression.startsWith("else")) { // if not previous condition
                        expression = expression.substring(4).stripLeading();

                        els = true;//todo fix wrong behavior with multiple else statements
                    } else if (expression.startsWith("{"))
                        throw new StonecutterSyntaxException("Unexpected { opening versioned code without a condition");

                    if (expression.endsWith("{")) {
                        versionedCode = true;
                        expression = expression.substring(0, expression.length() - 1).stripTrailing();
                    }

                    if (expression.isBlank()) {
                        if (els)
                            disabledCode = !disabledCode;
                        else
                            throw new StonecutterSyntaxException("Unexpected { opening versioned code without a condition");
                    } else
                        disabledCode = (els && !disabledCode) || !stonecutter.testVersion(expression);

                    //todo append enable/disable code part
                }
            }
        }

        new String();

        //todo write new contents to file
    }

    private String read(String match) throws IOException {
        StringBuilder substring = new StringBuilder();

        int current;

        while ((current = oldContents.read()) != -1) {
            char ch = (char) current;
            substring.append(ch);
            newContents.append(ch);

            if (substring.toString().endsWith(match))
                return substring.substring(0, substring.length() - match.length());
        }

        return null;
    }

    private boolean find(String match) throws IOException {
        return read(match) != null;
    }
}
