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

    public void apply() throws Exception {
        BufferedReader reader = Files.newBufferedReader(file.toPath(), stonecutter.charset());
    }
}
