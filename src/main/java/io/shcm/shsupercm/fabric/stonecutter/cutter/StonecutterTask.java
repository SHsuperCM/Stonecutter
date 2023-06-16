package io.shcm.shsupercm.fabric.stonecutter.cutter;

import io.shcm.shsupercm.fabric.stonecutter.StonecutterBuildGradle;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class StonecutterTask extends DefaultTask {
    @Input public abstract Property<File> getInputDir();
    @Input public abstract Property<File> getOutputDir();
    @Input public abstract Property<StonecutterBuildGradle.VersionData> getFromVersion();
    @Input public abstract Property<StonecutterBuildGradle.VersionData> getToVersion();
    @Input public abstract Property<Predicate<File>> getFileFilter(); { getFileFilter().convention(f -> true); }

    private FabricLoaderAPI fabricLoaderAPI = null;
    private Object targetSemVersion;

    @TaskAction
    public void run() {
        if (!getInputDir().isPresent() || !getOutputDir().isPresent() || !getFromVersion().isPresent() || !getToVersion().isPresent())
            throw new IllegalArgumentException();

        try {
            this.fabricLoaderAPI = FabricLoaderAPI.fromDependencies(getProject().project(getToVersion().get().version()));
            this.targetSemVersion = this.fabricLoaderAPI.parseVersion(getToVersion().get().version());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not get fabric loader api from dependencies!", e);
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
                output = outputRoot.toPath().resolve(output.toPath().relativize(inputRoot.toPath())).toFile();
                output.getParentFile().mkdirs();
            }
            new FileCutter(file, this).write(output);
        }
    }

    public boolean testVersion(String predicate) {
        try {
            return Objects.requireNonNull(this.fabricLoaderAPI, "API not initialized")
                    .parseVersionPredicate(predicate).test(this.targetSemVersion);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
