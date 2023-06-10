package io.shcm.shsupercm.fabric.stonecutter.cutter;

import java.io.BufferedReader;
import java.io.File;
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
        try (BufferedReader oldContents = this.oldContents = Files.newBufferedReader(file.toPath(), stonecutter.charset())) {
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

                        els = true;
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

        //todo write new contents to file
    }

    private String read(String match) {
        //todo implement
        throw new java.lang.UnsupportedOperationException();
    }

    private boolean find(String match) {
        return read(match) != null;
    }
}
