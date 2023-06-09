package io.shcm.shsupercm.fabric.stonecutter.cutter;

import io.shcm.shsupercm.fabric.stonecutter.StonecutterBuildGradle;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Stonecutter {
    private final Project controllerProject;
    private final StonecutterBuildGradle.VersionData fromVersion;
    private final StonecutterBuildGradle.VersionData toVersion;

    public Stonecutter(Project controllerProject, StonecutterBuildGradle.VersionData fromVersion, StonecutterBuildGradle.VersionData toVersion) {
        this.controllerProject = controllerProject;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
    }

    public void run(Task task) {
        task.getLogger().quiet((toVersion.isActiveVersion() ? "> Reloading active Stonecutter version " : "> Switching active Stonecutter version to ") + "'" + toVersion.version() + "'");

        try {
            switchSourceSet();

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public void switchSourceSet() throws IOException {
        List<String> stonecutterGradleLines = new ArrayList<>(Files.readAllLines(controllerProject.getBuildFile().toPath()));
        stonecutterGradleLines.set(5, "stonecutter.current('" + toVersion.version() + "')");
        Files.write(controllerProject.getBuildFile().toPath(), stonecutterGradleLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
