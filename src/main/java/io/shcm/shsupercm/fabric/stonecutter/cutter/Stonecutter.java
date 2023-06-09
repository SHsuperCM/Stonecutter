package io.shcm.shsupercm.fabric.stonecutter.cutter;

import io.shcm.shsupercm.fabric.stonecutter.StonecutterBuildGradle;
import org.gradle.api.Project;
import org.gradle.api.Task;

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

        
    }
}
