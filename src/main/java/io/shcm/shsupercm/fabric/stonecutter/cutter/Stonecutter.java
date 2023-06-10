package io.shcm.shsupercm.fabric.stonecutter.cutter;

import io.shcm.shsupercm.fabric.stonecutter.StonecutterBuildGradle;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Stonecutter {
    private final Project controllerProject;
    private final StonecutterBuildGradle.VersionData fromVersion;
    private final StonecutterBuildGradle.VersionData toVersion;

    private FabricLoaderAPI fabricLoaderAPI = null;
    private Object targetSemVersion;

    public Stonecutter(Project controllerProject, StonecutterBuildGradle.VersionData fromVersion, StonecutterBuildGradle.VersionData toVersion) {
        this.controllerProject = controllerProject;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
    }

    public void run(Task task) {
        task.getLogger().quiet((toVersion.isActiveVersion() ? "> Reloading active Stonecutter version " : "> Switching active Stonecutter version to ") + "'" + toVersion.version() + "'");

        try {
            switchSourceSet();

            this.fabricLoaderAPI = FabricLoaderAPI.fromDependencies(controllerProject.project(toVersion.version()));
            this.targetSemVersion = this.fabricLoaderAPI.parseVersion(toVersion.version());

            transformSourceSet(new File(controllerProject.getProjectDir(), "src"));
        } catch (Exception exception) {
            task.getLogger().error("Errored executing stonecutter processor!");
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    private void switchSourceSet() throws IOException {
        List<String> stonecutterGradleLines = new ArrayList<>(Files.readAllLines(controllerProject.getBuildFile().toPath()));
        stonecutterGradleLines.set(5, "stonecutter.current('" + toVersion.version() + "')");
        Files.write(controllerProject.getBuildFile().toPath(), stonecutterGradleLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void transformSourceSet(File file) throws Exception {
        if (file == null || !file.exists())
            return;

        if (file.isDirectory()) {
            for (File subFile : Objects.requireNonNull(file.listFiles()))
                transformSourceSet(subFile);

            return;
        }

        new FileCutter(file, this).apply();
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
