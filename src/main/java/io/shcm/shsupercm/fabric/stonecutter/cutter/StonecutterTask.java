package io.shcm.shsupercm.fabric.stonecutter.cutter;

import io.shcm.shsupercm.fabric.stonecutter.StonecutterBuildGradle;
import io.shcm.shsupercm.fabric.stonecutter.version.StonecutterVersionChecker;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class StonecutterTask extends DefaultTask {
    @Input public abstract Property<File> getInputDir();
    @Input public abstract Property<File> getOutputDir();
    @Input public abstract Property<StonecutterBuildGradle.Version> getFromVersion();
    @Input public abstract Property<StonecutterBuildGradle.Version> getToVersion();
    @Input public abstract Property<Predicate<File>> getFileFilter(); { getFileFilter().convention(f -> true); }
    @Input public abstract Property<Function<Project, StonecutterVersionChecker>> getVersionChecker(); { getVersionChecker().convention(StonecutterVersionChecker.FABRIC_LOADER_API); }

    private Predicate<String> versionChecker = predicate -> false;

    private StoneRegexTokenizer remapTokenizer = null;

    @TaskAction
    public void run() {
        if (!getInputDir().isPresent() || !getOutputDir().isPresent() || !getFromVersion().isPresent() || !getToVersion().isPresent())
            throw new IllegalArgumentException();

        try {
            final StonecutterVersionChecker versionCheckerImplementation = getVersionChecker().get().apply(getProject());
            final Object targetCheckerVersion = versionCheckerImplementation.parseVersion(getToVersion().get().version());
            this.versionChecker = predicate -> versionCheckerImplementation.check(targetCheckerVersion, predicate);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create version checker implementation", e);
        }

        try {
            StoneRegexTokenizer sourceTokenizer = getFromVersion().get().tokenizer();
            StoneRegexTokenizer targetTokenizer = getToVersion().get().tokenizer();

            if (!targetTokenizer.tokens().containsAll(sourceTokenizer.tokens())) {
                Set<String> missing = new HashSet<>(sourceTokenizer.tokens());
                missing.removeAll(targetTokenizer.tokens());
                getLogger().warn("Target token set not complete! Skipping mapping for: [" + String.join(", ", missing) + "]");
            }

            this.remapTokenizer = StoneRegexTokenizer.remap(sourceTokenizer, targetTokenizer);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load tokenizer!", e);
        }

        try {
            transform(getInputDir().get(), getInputDir().get(), getOutputDir().get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errored while processing files", e);
        }
    }

    private void transform(File file, File inputRoot, File outputRoot) throws Exception {
        if (file == null || !file.exists())
            return;

        if (file.isDirectory())
            for (File subFile : Objects.requireNonNull(file.listFiles()))
                transform(subFile, inputRoot, outputRoot);
        else if (getFileFilter().get().test(file)) {
            File output = file;
            if (!inputRoot.equals(outputRoot)) {
                output = outputRoot.toPath().resolve(inputRoot.toPath().relativize(output.toPath())).toFile();
                output.getParentFile().mkdirs();
            }
            new FileCutter(file, this).write(output);
        }
    }

    public boolean testVersion(String predicate) {
        return this.versionChecker.test(predicate);
    }

    public StoneRegexTokenizer tokenRemapper() {
        return this.remapTokenizer;
    }
}
